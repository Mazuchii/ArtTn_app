package tn.esprit.museum.entities;

import java.time.LocalDateTime;

public class DemandeJob {
    private int id;
    private String candidatId;
    private String cvUrl;
    private String lettreMotivation;
    private String status;
    private int offreId;
    private LocalDateTime rdvDate;
    private String lienMeet;
    private OfferJob offer; // Objet joint pour l'affichage

    public DemandeJob() {}

    public DemandeJob(String candidatId, String cvUrl, String lettreMotivation, int offreId) {
        this.candidatId = candidatId;
        this.cvUrl = cvUrl;
        this.lettreMotivation = lettreMotivation;
        this.offreId = offreId;
        this.status = "pending";
    }

    // Getters et Setters critiques
    public String getLienMeet() { return lienMeet; }
    public void setLienMeet(String lienMeet) { this.lienMeet = lienMeet; }

    public LocalDateTime getRdvDate() { return rdvDate; }
    public void setRdvDate(LocalDateTime rdvDate) { this.rdvDate = rdvDate; }

    public OfferJob getOffer() { return offer; }
    public void setOffer(OfferJob offer) { this.offer = offer; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCandidatId() { return candidatId; }
    public void setCandidatId(String candidatId) { this.candidatId = candidatId; }

    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }

    public String getLettreMotivation() { return lettreMotivation; }
    public void setLettreMotivation(String lettreMotivation) { this.lettreMotivation = lettreMotivation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getOffreId() { return offreId; }
    public void setOffreId(int offreId) { this.offreId = offreId; }
}