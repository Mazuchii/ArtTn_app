package tn.esprit.museum.entities;


public class Pack {

    private int id;
    private String nom;
    private double montant;
    private String avantages;
    private int evenementId; // Pour la clé étrangère visible dans ta DB

    // Constructeur vide
    public Pack() {
    }

    // Constructeur complet (utile pour la récupération depuis la DB)
    public Pack(int id, String nom, double montant, String avantages, int evenementId) {
        this.id = id;
        this.nom = nom;
        this.montant = montant;
        this.avantages = avantages;
        this.evenementId = evenementId;
    }

    // Constructeur sans ID (utile pour l'ajout)
    public Pack(String nom, double montant, String avantages, int evenementId) {
        this.nom = nom;
        this.montant = montant;
        this.avantages = avantages;
        this.evenementId = evenementId;
    }

    // --- Getters et Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public double getMontant() {
        return montant;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public String getAvantages() {
        return avantages;
    }

    public void setAvantages(String avantages) {
        this.avantages = avantages;
    }

    public int getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(int evenementId) {
        this.evenementId = evenementId;
    }

    @Override
    public String toString() {
        return "Pack{" +
                "nom='" + nom + '\'' +
                ", montant=" + montant + " DT" +
                '}';
    }
}
