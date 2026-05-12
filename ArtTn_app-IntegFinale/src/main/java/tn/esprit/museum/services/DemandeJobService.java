package tn.esprit.museum.services;



import tn.esprit.museum.entities.DemandeJob;

import tn.esprit.museum.entities.OfferJob;

import tn.esprit.museum.interfaces.IService;

import tn.esprit.museum.utils.DatabaseConnection;



import java.sql.*;

import java.util.ArrayList;

import java.util.List;



public class DemandeJobService implements IService<DemandeJob> {



    private final DatabaseConnection mc = DatabaseConnection.getInstance();



    public boolean existeDeja(String candidatId, int offreId) {

        String requete = "SELECT COUNT(*) FROM demande_job WHERE candidat_id = ? AND offre_id = ?";

        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {

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

        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {

            pst.setString(1, demande.getCandidatId());

            pst.setInt(2, demande.getOffreId());

            pst.setString(3, demande.getCvUrl());

            pst.setString(4, demande.getLettreMotivation());

            pst.setString(5, "pending"); // Par défaut lors de l'ajout



            pst.executeUpdate();

            System.out.println("Candidature ajoutée avec succès !");

        } catch (SQLException e) {

            System.out.println("Erreur lors de l'ajout : " + e.getMessage());

        }

    }



    @Override

    public void supprimer(int id) {

        String requete = "DELETE FROM demande_job WHERE id = ?";

        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {

            pst.setInt(1, id);

            pst.executeUpdate();

        } catch (SQLException e) {

            System.out.println("Erreur lors de la suppression : " + e.getMessage());

        }

    }



    @Override

    public void modifier(DemandeJob demande) {

        String requete = "UPDATE demande_job SET candidat_id = ?, offre_id = ?, cv_url = ?, lettre_motivation = ?, status = ? WHERE id = ?";

        try (PreparedStatement pst = mc.getCnx().prepareStatement(requete)) {

            pst.setString(1, demande.getCandidatId());

            pst.setInt(2, demande.getOffreId());

            pst.setString(3, demande.getCvUrl());

            pst.setString(4, demande.getLettreMotivation());

            pst.setString(5, demande.getStatus());

            pst.setInt(6, demande.getId());

            pst.executeUpdate();

        } catch (SQLException e) {

            System.out.println("Erreur lors de la mise à jour : " + e.getMessage());

        }

    }



// --- MÉTHODES DE LECTURE AVEC JOINTURE (JOIN) ---



    @Override

    public List<DemandeJob> afficher() {

        String requete = "SELECT d.*, o.titre, o.description, o.salaire FROM demande_job d " +

                "JOIN offer_job o ON d.offre_id = o.id";

        return executerListeRequete(requete, null);

    }



    public List<DemandeJob> afficherParOffre(int offreId) {

        String requete = "SELECT d.*, o.titre, o.description, o.salaire FROM demande_job d " +

                "JOIN offer_job o ON d.offre_id = o.id WHERE d.offre_id = ?";

        return executerListeRequete(requete, String.valueOf(offreId));

    }



    public List<DemandeJob> afficherParCandidatId(String candidatId) {

        String requete = "SELECT d.*, o.titre, o.description, o.salaire FROM demande_job d " +

                "JOIN offer_job o ON d.offre_id = o.id WHERE d.candidat_id = ?";

        return executerListeRequete(requete, candidatId);

    }



    /**

     * Méthode utilitaire pour éviter la répétition de code lors du mapping ResultSet -> Objet

     */

    private List<DemandeJob> executerListeRequete(String sql, String param) {

        List<DemandeJob> data = new ArrayList<>();

        try (PreparedStatement pst = mc.getCnx().prepareStatement(sql)) {

            if (param != null) {

// On vérifie si le paramètre est un entier (offre_id) ou String (candidat_id)

                try {

                    pst.setInt(1, Integer.parseInt(param));

                } catch (NumberFormatException e) {

                    pst.setString(1, param);

                }

            }

            ResultSet rs = pst.executeQuery();



            while (rs.next()) {

// Mapping DemandeJob

                DemandeJob d = new DemandeJob();

                d.setId(rs.getInt("id"));

                d.setCandidatId(rs.getString("candidat_id"));

                d.setOffreId(rs.getInt("offre_id"));

                d.setCvUrl(rs.getString("cv_url"));

                d.setLettreMotivation(rs.getString("lettre_motivation"));

                d.setStatus(rs.getString("status"));

                if (rs.getTimestamp("rdv_date") != null) {

                    d.setRdvDate(rs.getTimestamp("rdv_date").toLocalDateTime());

                }



// Mapping de l'objet OfferJob associé (JOIN)

                OfferJob o = new OfferJob();

                o.setId(rs.getInt("offre_id"));

                o.setTitre(rs.getString("titre"));

                o.setDescription(rs.getString("description"));

                o.setSalaire(rs.getDouble("salaire"));



// On attache l'offre à la demande

                d.setOffer(o);



                data.add(d);

            }

        } catch (SQLException e) {

            System.out.println("Erreur SQL : " + e.getMessage());

        }

        return data;

    }

}