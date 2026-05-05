package tn.esprit.museum.services;

import tn.esprit.museum.entities.Event;

import java.util.*;

public class PackAiGeneratorService {

    private final Random random = new Random();

    public PackSuggestion generateSuggestion(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Un evenement est requis pour generer un pack.");
        }

        String titre = safe(event.getTitle(), "Evenement");
        String categorie = safe(event.getCategory(), "general");
        int capacite = Math.max(0, event.getMaxCapacity());
        double prix = event.getPrice() == null ? 0.0 : Math.max(0.0, event.getPrice().doubleValue());

        String niveau = determinerNiveau();
        String nomPack = "Pack " + niveau + " - " + titre;
        double montantSuggere = calculerMontant(prix, capacite, categorie);
        String avantages = construireAvantages(event, niveau, categorie);

        return new PackSuggestion(nomPack, montantSuggere, avantages);
    }

    private String determinerNiveau() {
        String[] niveaux = {"Standard", "Premium", "Signature", "Or", "Argent", "Platine", "VIP", "Elite", "Exclusif"};
        return niveaux[random.nextInt(niveaux.length)];
    }

    private double calculerMontant(double prix, int capacite, String categorie) {
        double baseBilletterie = prix > 0 ? prix * Math.max(15, capacite * 0.12) : 0;
        double baseAudience = Math.max(650, capacite * 7.5);
        double montantDeBase = (baseBilletterie + baseAudience) * multiplicateurCategorie(categorie);
        
        // Ajouter une variation aléatoire entre 0.7 (70%) et 1.5 (150%) pour avoir un prix différent à chaque fois
        double variation = 0.7 + (1.5 - 0.7) * random.nextDouble();
        double montantAleatoire = montantDeBase * variation;
        
        return arrondirAuDix(montantAleatoire);
    }

    private double multiplicateurCategorie(String categorie) {
        String normalized = categorie.toLowerCase(Locale.ROOT);
        if (normalized.contains("mus")) {
            return 1.25;
        }
        if (normalized.contains("thea") || normalized.contains("th")) {
            return 1.15;
        }
        if (normalized.contains("sport")) {
            return 1.30;
        }
        if (normalized.contains("art") || normalized.contains("expo")) {
            return 1.10;
        }
        return 1.00;
    }

    private String construireAvantages(Event event, String niveau, String categorie) {
        List<String> pool = new ArrayList<>();
        String titre = safe(event.getTitle(), "l'evenement");
        String lieu = safe(event.getLocation(), "");

        pool.add("Visibilite du logo sur les supports officiels de " + titre);
        pool.add("Mention du sponsor sur les publications reseaux sociaux et le teaser");
        pool.add("Logo present sur les billets et affiches de " + titre);
        pool.add("Insertion de flyers ou d'echantillons dans les sacs remis aux participants");
        pool.add("Stand d'activation dedie pour rencontrer le public");
        pool.add("Acces exclusif aux coulisses ou zones VIP");
        pool.add("Invitation a la soiree de lancement ou de cloture");
        pool.add("Prise de parole lors de l'inauguration de l'evenement");
        pool.add("Remise d'un prix special au nom du sponsor pendant un moment cle");
        pool.add("Droits d'utilisation de l'image de l'evenement pour vos campagnes");

        if (!lieu.isBlank()) {
            pool.add("Presence de marque physique sur site a " + lieu);
            pool.add("Signaletique personnalisee a l'entree de " + lieu);
        }

        String normalizedCategory = categorie.toLowerCase(Locale.ROOT);
        if (normalizedCategory.contains("mus")) {
            pool.add("Branding scene, backstage ou espace VIP selon disponibilite");
            pool.add("Possibilite d'organiser un Meet & Greet exclusif avec les artistes");
        } else if (normalizedCategory.contains("sport")) {
            pool.add("Visibilite forte sur les zones d'animation, dossards ou podium");
            pool.add("Naming officiel d'une epreuve specifique de l'evenement");
        } else if (normalizedCategory.contains("thea") || normalizedCategory.contains("th")) {
            pool.add("Insertion prioritaire du sponsor dans le programme et l'accueil public");
        } else {
            pool.add("Emplacement de choix pour votre stand d'activation");
        }

        // Melanger les avantages et en choisir 3, 4 ou 5 aleatoirement
        Collections.shuffle(pool, random);
        int nbAvantages = 3 + random.nextInt(3); 
        List<String> selection = new ArrayList<>(pool.subList(0, Math.min(nbAvantages, pool.size())));

        // Si le niveau est premium/or/signature, ajouter un bonus
        if (niveau.equals("Signature") || niveau.equals("Platine") || niveau.equals("VIP")) {
            selection.add(0, "Statut de Partenaire Officiel exclusif"); // Ajouter en premier
        }

        // Ajouter toujours la date de validite si elle existe
        if (event.getEventDate() != null) {
            String date = event.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            selection.add("Operation de visibilite valable pour l'evenement du " + date);
        }

        return "- " + String.join("\n- ", selection);
    }

    private double arrondirAuDix(double montant) {
        return Math.round(Math.max(200.0, montant) / 10.0) * 10.0;
    }

    private String safe(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    public static class PackSuggestion {
        private final String nom;
        private final double montant;
        private final String avantages;

        public PackSuggestion(String nom, double montant, String avantages) {
            this.nom = nom;
            this.montant = montant;
            this.avantages = avantages;
        }

        public String getNom() {
            return nom;
        }

        public double getMontant() {
            return montant;
        }

        public String getAvantages() {
            return avantages;
        }
    }
}

