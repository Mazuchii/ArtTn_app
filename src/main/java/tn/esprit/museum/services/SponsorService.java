package tn.esprit.museum.services;

import tn.esprit.museum.entities.Sponsor;
import tn.esprit.museum.entities.UserDonation;
import tn.esprit.museum.interfaces.ISponsorService;
import tn.esprit.museum.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SponsorService implements ISponsorService<Sponsor> {
    private static boolean sponsorSchemaChecked = false;

    private Connection getConnection() {
        Connection connection = DatabaseConnection.getInstance().getCnx();
        if (connection == null) {
            System.err.println("Base de donnees indisponible: operation sponsor ignoree.");
        } else {
            ensureSponsorLinks(connection);
        }
        return connection;
    }

    private void ensureSponsorLinks(Connection connection) {
        if (sponsorSchemaChecked) {
            return;
        }
        try {
            if (!columnExists(connection, "sponsor", "user_id")) {
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate("ALTER TABLE sponsor ADD COLUMN user_id INT NULL");
                }
            }
            if (!columnExists(connection, "sponsor", "pack_id")) {
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate("ALTER TABLE sponsor ADD COLUMN pack_id INT NULL");
                }
            }
            sponsorSchemaChecked = true;
        } catch (SQLException e) {
            System.err.println("Impossible de verifier les colonnes sponsor user_id/pack_id: " + e.getMessage());
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    @Override
    public void addEntity(Sponsor s) {
        String requete = "INSERT INTO sponsor " +
                "(nom, description, type, logo_url, email, telephone, site_web, adresse, date_creation, status, user_id, pack_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return;
            }
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, s.getNom());
            pst.setString(2, s.getDescription());
            pst.setString(3, s.getType());
            pst.setString(4, s.getLogoUrl());
            pst.setString(5, s.getEmail());
            pst.setString(6, s.getTelephone());
            pst.setString(7, s.getSiteWeb());
            pst.setString(8, s.getAdresse());
            pst.setTimestamp(9, new Timestamp(s.getDateCreation().getTime()));
            pst.setString(10, s.getStatus());
            setNullableInt(pst, 11, s.getUserId());
            setNullableInt(pst, 12, s.getPackId());

            pst.executeUpdate();
            System.out.println("Sponsor ajoute avec succes");
        } catch (SQLException e) {
            System.err.println("Erreur addEntity: " + e.getMessage());
        }
    }

    public boolean existeDeja(String nom) {
        String requete = "SELECT COUNT(*) FROM sponsor WHERE LOWER(nom) = LOWER(?)";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return false;
            }
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, nom.trim());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur existeDeja: " + e.getMessage());
        }
        return false;
    }

    public String supprimerSponsor(int id) {
        String verification = "SELECT COUNT(*) FROM don WHERE sponsor_id = ?";
        String requete = "DELETE FROM sponsor WHERE id = ?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return "Base de donnees indisponible.";
            }

            PreparedStatement checkPst = connection.prepareStatement(verification);
            checkPst.setInt(1, id);
            ResultSet rs = checkPst.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return "Impossible de supprimer ce sponsor car il est lie a une ou plusieurs donations.";
            }

            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, id);
            int deletedRows = pst.executeUpdate();
            if (deletedRows == 0) {
                return "Sponsor introuvable.";
            }

            System.out.println("Sponsor supprime");
            return null;
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Erreur deleteEntity: " + e.getMessage());
            return "Impossible de supprimer ce sponsor car il est reference par d'autres donnees.";
        } catch (SQLException e) {
            System.err.println("Erreur deleteEntity: " + e.getMessage());
            return "Erreur lors de la suppression du sponsor.";
        }
    }

    public void deleteEntity(int id) {
        supprimerSponsor(id);
    }

    @Override
    public void deleteEntity(Sponsor s) {
        deleteEntity(s.getId());
    }

    @Override
    public void updateEntity(int id, Sponsor s) {
        String requete = "UPDATE sponsor SET nom=?, description=?, type=?, logo_url=?, email=?, telephone=?, site_web=?, adresse=?, status=?, user_id=?, pack_id=? WHERE id=?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return;
            }
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, s.getNom());
            pst.setString(2, s.getDescription());
            pst.setString(3, s.getType());
            pst.setString(4, s.getLogoUrl());
            pst.setString(5, s.getEmail());
            pst.setString(6, s.getTelephone());
            pst.setString(7, s.getSiteWeb());
            pst.setString(8, s.getAdresse());
            pst.setString(9, s.getStatus());
            setNullableInt(pst, 10, s.getUserId());
            setNullableInt(pst, 11, s.getPackId());
            pst.setInt(12, id);

            pst.executeUpdate();
            System.out.println("Sponsor mis a jour");
        } catch (SQLException e) {
            System.err.println("Erreur updateEntity: " + e.getMessage());
        }
    }

    public void updateStatus(int id, String status) {
        String requete = "UPDATE sponsor SET status=? WHERE id=?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return;
            }
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, status);
            pst.setInt(2, id);
            pst.executeUpdate();
            System.out.println("Statut sponsor mis a jour");
        } catch (SQLException e) {
            System.err.println("Erreur updateStatus: " + e.getMessage());
        }
    }

    public List<Sponsor> getData() {
        List<Sponsor> data = new ArrayList<>();
        String requete = "SELECT * FROM sponsor ORDER BY date_creation DESC";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return data;
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(requete);
            while (rs.next()) {
                Sponsor s = new Sponsor();
                s.setId(rs.getInt("id"));
                s.setNom(rs.getString("nom"));
                s.setDescription(rs.getString("description"));
                s.setType(rs.getString("type"));
                s.setLogoUrl(rs.getString("logo_url"));
                s.setEmail(rs.getString("email"));
                s.setTelephone(rs.getString("telephone"));
                s.setSiteWeb(rs.getString("site_web"));
                s.setAdresse(rs.getString("adresse"));
                s.setDateCreation(rs.getTimestamp("date_creation"));
                s.setStatus(rs.getString("status"));
                s.setUserId(getNullableInt(rs, "user_id"));
                s.setPackId(getNullableInt(rs, "pack_id"));
                data.add(s);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getData: " + e.getMessage());
        }
        return data;
    }

    public List<UserDonation> getDonationsByUserId(int userId) {
        List<UserDonation> donations = new ArrayList<>();
        String requete = """
                SELECT s.id AS sponsor_id,
                       s.nom AS sponsor_name,
                       s.status AS sponsor_status,
                       s.date_creation,
                       COALESCE(e.title, 'Evenement non defini') AS event_title,
                       COALESCE(p.nom, s.type, 'Pack non defini') AS pack_name,
                       COALESCE(p.montant, 0) AS amount
                FROM sponsor s
                LEFT JOIN pack p ON s.pack_id = p.id
                LEFT JOIN events e ON p.event_id = e.id
                WHERE s.user_id = ?
                  AND LOWER(COALESCE(s.status, 'en attente')) <> 'refuse'
                ORDER BY s.date_creation DESC
                """;
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return donations;
            }
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                donations.add(new UserDonation(
                        rs.getInt("sponsor_id"),
                        rs.getString("sponsor_name"),
                        rs.getString("event_title"),
                        rs.getString("pack_name"),
                        rs.getDouble("amount"),
                        rs.getString("sponsor_status"),
                        rs.getTimestamp("date_creation")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getDonationsByUserId: " + e.getMessage());
        }
        return donations;
    }

    private void setNullableInt(PreparedStatement pst, int index, Integer value) throws SQLException {
        if (value == null || value <= 0) {
            pst.setNull(index, Types.INTEGER);
        } else {
            pst.setInt(index, value);
        }
    }

    private Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}

