package tn.esprit.museum.services;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.services.EventService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatbotService {

    private EventService eventService;

    // Listes de mots-clés
    private static final List<String> CATEGORIES = List.of(
            "exposition", "conférence", "atelier", "visite guidée", "spectacle",
            "danse", "théâtre", "concert", "chant", "potterie", "artisanale"
    );
    private static final List<String> LIEUX = List.of(
            "tunis", "ariana", "esprit", "salle a", "salle b", "amphithéâtre", "studio"
    );

    public ChatbotService() {
        eventService = new EventService();
    }

    public String ask(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Bonjour ! Je suis l'assistant Museum Digital. Que cherchez-vous ?\n" +
                    "Exemples : 'je veux un événement de danse', 'je veux un événement le 01/05/2026', 'je veux un événement à tunis'.";
        }

        String lowerMsg = userMessage.toLowerCase().trim();
        List<Event> events;
        try {
            events = eventService.getAll();
        } catch (SQLException e) {
            return "Désolé, je n'arrive pas à accéder aux événements pour le moment.";
        }

        // 1. Extraire la catégorie
        String foundCategory = null;
        for (String cat : CATEGORIES) {
            if (lowerMsg.contains(cat)) {
                foundCategory = cat;
                break;
            }
        }

        // 2. Extraire le lieu
        String foundLocation = null;
        for (String loc : LIEUX) {
            if (lowerMsg.contains(loc)) {
                foundLocation = loc;
                break;
            }
        }

        // 3. Extraire la date (format jj/mm/aaaa)
        LocalDate foundDate = null;
        Pattern datePattern = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");
        Matcher matcher = datePattern.matcher(userMessage);
        if (matcher.find()) {
            try {
                foundDate = LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignored) {}
        }

        // 4. Filtrer les événements
        List<Event> matches = new ArrayList<>();

        for (Event e : events) {
            boolean match = false;

            if (foundCategory != null && e.getCategory().toLowerCase().contains(foundCategory)) {
                match = true;
            }
            if (foundLocation != null && e.getLocation().toLowerCase().contains(foundLocation)) {
                match = true;
            }
            if (foundDate != null && e.getEventDate().equals(foundDate)) {
                match = true;
            }
            // Si aucun des critères ci‑dessus n’est trouvé, la phrase entière est cherchée dans le titre, la catégorie ou le lieu
            if (foundCategory == null && foundLocation == null && foundDate == null) {
                if (e.getTitle().toLowerCase().contains(lowerMsg) ||
                        e.getCategory().toLowerCase().contains(lowerMsg) ||
                        e.getLocation().toLowerCase().contains(lowerMsg)) {
                    match = true;
                }
            }

            if (match) {
                matches.add(e);
            }
        }

        // Supprimer les doublons (même événement trouvé par plusieurs critères)
        matches = matches.stream().distinct().collect(Collectors.toList());

        if (!matches.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            StringBuilder reply = new StringBuilder("🎯 Voici ce que j'ai trouvé pour vous :\n\n");
            for (Event e : matches) {
                reply.append("📌 *").append(e.getTitle()).append("*")
                        .append(" (").append(e.getCategory()).append(")")
                        .append(" – le ").append(e.getEventDate().format(formatter))
                        .append(" à ").append(e.getLocation())
                        .append("\n");
            }
            return reply.toString();
        } else {
            return "😕 Désolé, je n'ai rien trouvé pour votre demande.\n" +
                    "Essayez avec une catégorie (exposition, danse...), un lieu (tunis, ariana...), une date (jj/mm/aaaa), ou le titre exact d’un événement.";
        }
    }
}