package edu.cnx.services;

import edu.cnx.entités.DemandeJob;
import edu.cnx.interfaces.IService;
import edu.cnx.tools.MyConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DemandeJobService implements IService<DemandeJob> {

    private final MyConnection mc = MyConnection.getInstance();

    public boolean existeDeja(String candidatId, int offreId) {
        String requete = "SELECT COUNT(*) FROM demande_job WHERE candidat_id = ? AND offre_id = ?";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setString(1, candidatId);
            pst.setInt(2, offreId);
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
    public void ajouter(DemandeJob demande) {
        String requete = "INSERT INTO demande_job (candidat_id, offre_id, cv_url, lettre_motivation, status) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setString(1, demande.getCandidatId());
            pst.setInt(2, demande.getOffreId());
            pst.setString(3, demande.getCvUrl());
            pst.setString(4, demande.getLettreMotivation());
            pst.setString(5, demande.getStatus());

            pst.executeUpdate();
            System.out.println("Candidature ajoutée avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout de la candidature : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String requete = "DELETE FROM demande_job WHERE id = ?";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setInt(1, id);

            int rowsDeleted = pst.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Candidature supprimée avec succès !");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    @Override
    public void modifier(DemandeJob demande) {
        String requete = "UPDATE demande_job SET candidat_id = ?, offre_id = ?, cv_url = ?, lettre_motivation = ?, status = ? WHERE id = ?";
        try {
            PreparedStatement pst = mc.getCnx().prepareStatement(requete);
            pst.setString(1, demande.getCandidatId());
            pst.setInt(2, demande.getOffreId());
            pst.setString(3, demande.getCvUrl());
            pst.setString(4, demande.getLettreMotivation());
            pst.setString(5, demande.getStatus());
            pst.setInt(6, demande.getId());

            pst.executeUpdate();
            System.out.println("Candidature mise à jour avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    @Override
    public List<DemandeJob> afficher() {
        List<DemandeJob> data = new ArrayList<>();
        String requete = "SELECT * FROM demande_job";
        try {
            Statement st = mc.getCnx().createStatement();
            ResultSet rs = st.executeQuery(requete);

            while (rs.next()) {
                DemandeJob d = new DemandeJob();
                d.setId(rs.getInt("id"));
                d.setCandidatId(rs.getString("candidat_id"));
                d.setOffreId(rs.getInt("offre_id"));
                d.setCvUrl(rs.getString("cv_url"));
                d.setLettreMotivation(rs.getString("lettre_motivation"));
                d.setStatus(rs.getString("status"));

                data.add(d);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des candidatures : " + e.getMessage());
        }

        return data;
    }
}