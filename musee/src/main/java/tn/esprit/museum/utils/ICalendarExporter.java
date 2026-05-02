package tn.esprit.museum.utils;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ICalendarExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    /**
     * Génère un fichier .ics pour une réservation et le sauvegarde sur le bureau.
     * @param reservation la réservation
     * @param event l'événement associé
     * @param userEmail l'email de l'utilisateur (pour l'organisateur)
     * @return le chemin du fichier créé, ou null en cas d'erreur
     */
    public static String exportToICS(Reservation reservation, Event event, String userEmail) {
        String fileName = "reservation_" + reservation.getId() + "_" + event.getTitle().replaceAll(" ", "_") + ".ics";
        String desktopPath = System.getProperty("user.home") + "/Desktop/" + fileName;
        try (FileWriter writer = new FileWriter(desktopPath)) {
            writer.write(generateICSContent(reservation, event, userEmail));
            return desktopPath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String generateICSContent(Reservation reservation, Event event, String userEmail) {
        LocalDateTime start = event.getEventDate().atTime(event.getEventTime());
        LocalDateTime end = start.plusHours(2); // durée par défaut de 2 heures

        String uid = reservation.getId() + "@museumdigital.com";
        String summary = "🎟️ " + event.getTitle() + " - " + reservation.getNumberOfTickets() + " place(s)";
        String description = "Événement : " + event.getTitle() + "\n"
                + "Lieu : " + event.getLocation() + "\n"
                + "Nombre de billets : " + reservation.getNumberOfTickets() + "\n"
                + "Sièges : " + (reservation.getSelectedSeats() != null ? reservation.getSelectedSeats() : "Non spécifiés") + "\n"
                + "Code promo utilisé : " + (reservation.getSpecialRequests() != null && reservation.getSpecialRequests().contains("Code promo") ? "ArtTn" : "Aucun");

        return "BEGIN:VCALENDAR\n"
                + "VERSION:2.0\n"
                + "PRODID:-//MuseumDigital//Reservation//FR\n"
                + "BEGIN:VEVENT\n"
                + "UID:" + uid + "\n"
                + "DTSTAMP:" + LocalDateTime.now().format(DATE_FORMATTER) + "\n"
                + "DTSTART:" + start.format(DATE_FORMATTER) + "\n"
                + "DTEND:" + end.format(DATE_FORMATTER) + "\n"
                + "SUMMARY:" + summary + "\n"
                + "DESCRIPTION:" + description.replace("\n", "\\n") + "\n"
                + "LOCATION:" + event.getLocation() + "\n"
                + "ORGANIZER;CN=Museum Digital:mailto:" + userEmail + "\n"
                + "BEGIN:VALARM\n"
                + "TRIGGER:-PT30M\n"  // rappel 30 minutes avant
                + "ACTION:DISPLAY\n"
                + "DESCRIPTION:Rappel : " + summary + "\n"
                + "END:VALARM\n"
                + "END:VEVENT\n"
                + "END:VCALENDAR\n";
    }
}