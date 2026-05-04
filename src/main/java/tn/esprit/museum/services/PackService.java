package tn.esprit.museum.services;

import tn.esprit.museum.entities.Pack;
import tn.esprit.museum.interfaces.ISponsorService;
import tn.esprit.museum.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackService implements ISponsorService<Pack> {

    private static boolean schemaVerified = false;
    private static final String EVENT_COLUMN = "event_id";

    private Connection getConnection() {
        Connection connection = DatabaseConnection.getInstance().getCnx();
        if (connection == null) {
            System.err.println("Base de donnees indisponible: operation pack ignoree.");
            return null;
        }

        ensurePackSchema(connection);
        return connection;
    }

    private synchronized void ensurePackSchema(Connection connection) {
        if (schemaVerified) {
            return;
        }

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();

            boolean hasEventIdColumn = hasColumn(metaData, catalog, "pack", EVENT_COLUMN);
            boolean hasIdEvenementColumn = hasColumn(metaData, catalog, "pack", "id_evenement");
            boolean hasEvenementIdColumn = hasColumn(metaData, catalog, "pack", "evenement_id");

            if (!hasEventIdColumn) {
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate("ALTER TABLE pack ADD COLUMN " + EVENT_COLUMN + " INT NULL");
                    System.out.println("Colonne event_id ajoutee a la table pack");
                    hasEventIdColumn = true;
                }
            }

            if (hasIdEvenementColumn) {
                copyLegacyEventIds(connection, "id_evenement");
            }
            if (hasEvenementIdColumn) {
                copyLegacyEventIds(connection, "evenement_id");
            }

            ensureIndex(connection, metaData, catalog, "pack", "idx_pack_event_id", EVENT_COLUMN);
            ensureForeignKey(connection, metaData, catalog, "pack", "fk_pack_event", EVENT_COLUMN);

            schemaVerified = true;
        } catch (SQLException e) {
            System.err.println("Verification schema pack impossible: " + e.getMessage());
        }
    }

    private void copyLegacyEventIds(Connection connection, String legacyColumn) {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("UPDATE pack SET " + EVENT_COLUMN + " = " + legacyColumn + " " +
                    "WHERE " + EVENT_COLUMN + " IS NULL AND " + legacyColumn + " IS NOT NULL");
        } catch (SQLException e) {
            System.err.println("Migration " + legacyColumn + " vers " + EVENT_COLUMN + " impossible: " + e.getMessage());
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metaData.getColumns(catalog, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private void ensureIndex(Connection connection, DatabaseMetaData metaData, String catalog, String tableName,
                             String indexName, String columnName) throws SQLException {
        boolean hasIndex = false;
        try (ResultSet indexes = metaData.getIndexInfo(catalog, null, tableName, false, false)) {
            while (indexes.next()) {
                String currentIndexName = indexes.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(currentIndexName)) {
                    hasIndex = true;
                    break;
                }
            }
        }

        if (!hasIndex) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE INDEX " + indexName + " ON " + tableName + "(" + columnName + ")");
            } catch (SQLException e) {
                System.err.println("Index " + columnName + " non cree: " + e.getMessage());
            }
        }
    }

    private void ensureForeignKey(Connection connection, DatabaseMetaData metaData, String catalog, String tableName,
                                  String fkName, String columnName) throws SQLException {
        boolean hasForeignKey = false;
        try (ResultSet importedKeys = metaData.getImportedKeys(catalog, null, tableName)) {
            while (importedKeys.next()) {
                String fkColumn = importedKeys.getString("FKCOLUMN_NAME");
                if (columnName.equalsIgnoreCase(fkColumn)) {
                    hasForeignKey = true;
                    break;
                }
            }
        }

        if (!hasForeignKey) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate(
                        "ALTER TABLE " + tableName + " " +
                        "ADD CONSTRAINT " + fkName + " " +
                        "FOREIGN KEY (" + columnName + ") REFERENCES events(id) " +
                        "ON UPDATE CASCADE ON DELETE SET NULL"
                );
            } catch (SQLException e) {
                System.err.println("Cle etrangere " + columnName + " non creee: " + e.getMessage());
            }
        }
    }

    @Override
    public void addEntity(Pack p) {
        String req = buildInsertQuery();
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return;
            }
            PreparedStatement pst = connection.prepareStatement(req);
            pst.setString(1, p.getNom());
            pst.setDouble(2, p.getMontant());
            pst.setString(3, p.getAvantages());

            pst.setInt(4, p.getEvenementId());

            pst.executeUpdate();
            System.out.println("Pack ajoute avec succes");
        } catch (SQLException e) {
            System.err.println("Erreur addEntity: " + e.getMessage());
        }
    }

    public void deleteEntity(int id) {
        String req = "DELETE FROM pack WHERE id = ?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return;
            }
            PreparedStatement pst = connection.prepareStatement(req);
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Pack supprime avec succes");
        } catch (SQLException e) {
            System.err.println("Erreur deleteEntity: " + e.getMessage());
        }
    }

    @Override
    public void deleteEntity(Pack p) {
        deleteEntity(p.getId());
    }

    @Override
    public void updateEntity(int id, Pack p) {
        String req = buildUpdateQuery();
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return;
            }
            PreparedStatement pst = connection.prepareStatement(req);
            pst.setString(1, p.getNom());
            pst.setDouble(2, p.getMontant());
            pst.setString(3, p.getAvantages());

            pst.setInt(4, p.getEvenementId());
            pst.setInt(5, id);

            pst.executeUpdate();
            System.out.println("Pack mis a jour avec succes");
        } catch (SQLException e) {
            System.err.println("Erreur updateEntity: " + e.getMessage());
        }
    }

    @Override
    public List<Pack> getData() {
        List<Pack> data = new ArrayList<>();
        String req = "SELECT * FROM pack";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return data;
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                Pack p = new Pack();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setMontant(rs.getDouble("montant"));
                p.setAvantages(rs.getString("avantages"));
                p.setEvenementId(rs.getInt(EVENT_COLUMN));
                data.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getData: " + e.getMessage());
        }
        return data;
    }

    public List<Pack> getDataByEvenementId(int evenementId) {
        List<Pack> data = new ArrayList<>();
        String req = "SELECT * FROM pack WHERE " + EVENT_COLUMN + " = ?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return data;
            }
            PreparedStatement pst = connection.prepareStatement(req);
            pst.setInt(1, evenementId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Pack p = new Pack();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setMontant(rs.getDouble("montant"));
                p.setAvantages(rs.getString("avantages"));
                p.setEvenementId(rs.getInt(EVENT_COLUMN));
                data.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getDataByEvenementId: " + e.getMessage());
        }
        return data;
    }

    public List<Pack> getDataByEvenementCategory(String categorie) {
        List<Pack> data = new ArrayList<>();
        String req = "SELECT p.* FROM pack p " +
                     "INNER JOIN events e ON p." + EVENT_COLUMN + " = e.id " +
                     "WHERE e.category = ? ORDER BY p.nom";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return data;
            }
            PreparedStatement pst = connection.prepareStatement(req);
            pst.setString(1, categorie);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Pack p = new Pack();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setMontant(rs.getDouble("montant"));
                p.setAvantages(rs.getString("avantages"));
                p.setEvenementId(rs.getInt(EVENT_COLUMN));
                data.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getDataByEvenementCategory: " + e.getMessage());
        }
        return data;
    }

    private String buildInsertQuery() {
        return "INSERT INTO pack (nom, montant, avantages, " + EVENT_COLUMN + ") VALUES (?, ?, ?, ?)";
    }

    private String buildUpdateQuery() {
        return "UPDATE pack SET nom=?, montant=?, avantages=?, " + EVENT_COLUMN + "=? WHERE id=?";
    }
}

