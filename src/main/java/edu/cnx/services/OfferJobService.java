package edu.cnx.services;

import edu.cnx.entités.OfferJob;
import edu.cnx.interfaces.IService;
import edu.cnx.tools.MyConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OfferJobService implements IService<OfferJob> {

    private final MyConnection mc = MyConnection.getInstance();


    public boolean existeDeja(String titre, String description) {
        String requete = "SELECT COUNT(*) FROM offer_job WHERE titre = ? AND description = ?";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setString(1, titre);
            pst.setString(2, description);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la vérification du doublon : " + e.getMessage());
        }
        return false;
    }

    @Override
    public void ajouter(OfferJob offerJob) {
        String requete = "INSERT INTO offer_job (titre, description, salaire) VALUES (?, ?, ?)";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setString(1, offerJob.getTitre());
            pst.setString(2, offerJob.getDescription());
            pst.setDouble(3, offerJob.getSalaire());

            pst.executeUpdate();
            System.out.println("Offer added successfully!");
        } catch (SQLException e) {
            System.out.println("Error adding offer: " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String requete = "DELETE FROM offer_job WHERE id = ?";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setInt(1, id);

            int rowsDeleted = pst.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Offer deleted successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting offer: " + e.getMessage());
        }
    }

    @Override
    public void modifier(OfferJob offerJob) {
        String requete = "UPDATE offer_job SET titre = ?, description = ?, salaire = ? WHERE id = ?";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setString(1, offerJob.getTitre());
            pst.setString(2, offerJob.getDescription());
            pst.setDouble(3, offerJob.getSalaire());
            pst.setInt(4, offerJob.getId());

            pst.executeUpdate();
            System.out.println("Offer updated successfully!");
        } catch (SQLException e) {
            System.out.println("Error updating offer: " + e.getMessage());
        }
    }

    @Override
    public List<OfferJob> afficher() {
        List<OfferJob> data = new ArrayList<>();
        String requete = "SELECT * FROM offer_job";
        try {
            Statement st = mc.getCnx().createStatement();
            ResultSet rs = st.executeQuery(requete);

            while (rs.next()) {
                OfferJob o = new OfferJob();
                o.setId(rs.getInt("id"));
                o.setTitre(rs.getString("titre"));
                o.setDescription(rs.getString("description"));
                o.setSalaire(rs.getDouble("salaire"));

                data.add(o);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching data: " + e.getMessage());
        }

        return data;
    }
}