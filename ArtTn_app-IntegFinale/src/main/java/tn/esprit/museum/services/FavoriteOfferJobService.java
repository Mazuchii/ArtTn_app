package tn.esprit.museum.services;

import tn.esprit.museum.entities.OfferJob;
import tn.esprit.museum.utils.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FavoriteOfferJobService {

    private final DatabaseConnection mc = DatabaseConnection.getInstance();

    public FavoriteOfferJobService() {
        initialiserTableFavoris();
    }

    private void initialiserTableFavoris() {
        String requete = """
                CREATE TABLE IF NOT EXISTS offer_job_favorite (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    candidat_id VARCHAR(255) NOT NULL,
                    offre_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_offer_job_favorite UNIQUE (candidat_id, offre_id),
                    CONSTRAINT fk_offer_job_favorite_offer FOREIGN KEY (offre_id) REFERENCES offer_job(id) ON DELETE CASCADE
                )
                """;
        try (Statement st = mc.getCnx().createStatement()) {
            st.execute(requete);
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'initialisation des favoris : " + e.getMessage());
        }
    }

    public boolean ajouterFavori(String candidatId, int offreId) {
        if (estFavori(candidatId, offreId)) {
            return true;
        }

        String requete = "INSERT INTO offer_job_favorite (candidat_id, offre_id) VALUES (?, ?)";
        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {
            pst.setString(1, candidatId);
            pst.setInt(2, offreId);
            pst.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout du favori : " + e.getMessage());
            return false;
        }
    }

    public boolean supprimerFavori(String candidatId, int offreId) {
        String requete = "DELETE FROM offer_job_favorite WHERE candidat_id = ? AND offre_id = ?";
        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {
            pst.setString(1, candidatId);
            pst.setInt(2, offreId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression du favori : " + e.getMessage());
            return false;
        }
    }

    public boolean estFavori(String candidatId, int offreId) {
        String requete = "SELECT COUNT(*) FROM offer_job_favorite WHERE candidat_id = ? AND offre_id = ?";
        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {
            pst.setString(1, candidatId);
            pst.setInt(2, offreId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la verification du favori : " + e.getMessage());
        }
        return false;
    }

    public List<OfferJob> afficherFavorisParCandidatId(String candidatId) {
        List<OfferJob> favoris = new ArrayList<>();
        String requete = """
                SELECT o.id, o.titre, o.description, o.salaire
                FROM offer_job_favorite f
                INNER JOIN offer_job o ON o.id = f.offre_id
                WHERE f.candidat_id = ?
                ORDER BY f.created_at DESC
                """;
        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {
            pst.setString(1, candidatId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                OfferJob offre = new OfferJob();
                offre.setId(rs.getInt("id"));
                offre.setTitre(rs.getString("titre"));
                offre.setDescription(rs.getString("description"));
                offre.setSalaire(rs.getDouble("salaire"));
                favoris.add(offre);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la recuperation des favoris : " + e.getMessage());
        }
        return favoris;
    }
}

