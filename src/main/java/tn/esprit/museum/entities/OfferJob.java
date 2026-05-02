package tn.esprit.museum.entities;

public class OfferJob {
    private int id;
    private String titre;
    private String description;
    private double salaire;

    public OfferJob() {
    }

    public OfferJob(String titre, String description, double salaire) {
        this.titre = titre;
        this.description = description;
        this.salaire = salaire;
    }

    public OfferJob(int id, String titre, String description, double salaire) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.salaire = salaire;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getSalaire() {
        return salaire;
    }

    public void setSalaire(double salaire) {
        this.salaire = salaire;
    }

    @Override
    public String toString() {
        return "OfferJob{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", salaire=" + salaire + " DT" +
                '}';
    }
}