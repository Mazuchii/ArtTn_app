package tn.esprit.museum.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.museum.entities.User;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PDFExporter {

    /**
     * Exporte la liste des utilisateurs en PDF
     * @param users Liste des utilisateurs
     * @param stage Stage parent pour le FileChooser
     * @return true si l'export a réussi
     */
    public static boolean exportUsersToPDF(ObservableList<User> users, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setInitialFileName("utilisateurs_" + timestamp + ".pdf");

        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return false;
        }

        try {
            // Créer le document
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Ajouter le logo/titre
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Titre
            Paragraph title = new Paragraph("MUSEUM DIGITAL", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Sous-titre
            Paragraph subtitle = new Paragraph("Liste des utilisateurs - " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(Chunk.NEWLINE);

            // Créer le tableau (6 colonnes)
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Largeurs des colonnes
            float[] columnWidths = {0.5f, 1.5f, 2f, 1.5f, 1f, 1f};
            table.setWidths(columnWidths);

            // En-têtes
            String[] headers = {"ID", "Nom d'utilisateur", "Email", "Nom complet", "Rôle", "Statut"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setPadding(8);
                table.addCell(headerCell);
            }

            // Remplir le tableau
            for (User user : users) {
                // ID
                PdfPCell idCell = new PdfPCell(new Phrase(String.valueOf(user.getId()), cellFont));
                idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                idCell.setPadding(5);
                table.addCell(idCell);

                // Username
                PdfPCell usernameCell = new PdfPCell(new Phrase(user.getUsername(), cellFont));
                usernameCell.setPadding(5);
                table.addCell(usernameCell);

                // Email
                PdfPCell emailCell = new PdfPCell(new Phrase(user.getEmail(), cellFont));
                emailCell.setPadding(5);
                table.addCell(emailCell);

                // Full Name
                PdfPCell fullNameCell = new PdfPCell(new Phrase(user.getFullName(), cellFont));
                fullNameCell.setPadding(5);
                table.addCell(fullNameCell);

                // Role
                PdfPCell roleCell = new PdfPCell(new Phrase(user.getRole(), cellFont));
                roleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                roleCell.setPadding(5);
                table.addCell(roleCell);

                // Status
                String status = user.isActive() ? "Actif" : "Inactif";
                PdfPCell statusCell = new PdfPCell(new Phrase(status, cellFont));
                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                statusCell.setPadding(5);
                if (!user.isActive()) {
                    statusCell.setBackgroundColor(BaseColor.RED);
                    statusCell.setPhrase(new Phrase(status, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE)));
                } else {
                    statusCell.setBackgroundColor(BaseColor.GREEN);
                    statusCell.setPhrase(new Phrase(status, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE)));
                }
                table.addCell(statusCell);
            }

            document.add(table);

            // Ajouter le total
            Paragraph total = new Paragraph("Total: " + users.size() + " utilisateur(s)", subtitleFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            // Pied de page
            Paragraph footer = new Paragraph("Document généré le " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss")),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exporte les statistiques du dashboard en PDF
     */
    public static boolean exportStatsToPDF(int totalUsers, int activeUsers, int admins, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setInitialFileName("statistiques_" + timestamp + ".pdf");

        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return false;
        }

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);

            Paragraph title = new Paragraph("ART.TN", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(Chunk.NEWLINE);

            Paragraph subtitle = new Paragraph("Statistiques du Dashboard", headerFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(Chunk.NEWLINE);

            // Créer le tableau des statistiques
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);

            float[] columnWidths = {1f, 1f};
            table.setWidths(columnWidths);

            // En-têtes
            PdfPCell header1 = new PdfPCell(new Phrase("Métrique", headerFont));
            header1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            header1.setHorizontalAlignment(Element.ALIGN_CENTER);
            header1.setPadding(10);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Phrase("Valeur", headerFont));
            header2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            header2.setHorizontalAlignment(Element.ALIGN_CENTER);
            header2.setPadding(10);
            table.addCell(header2);

            // Données
            addStatCell(table, "Total Utilisateurs", String.valueOf(totalUsers));
            addStatCell(table, "Utilisateurs Actifs", String.valueOf(activeUsers));
            addStatCell(table, "Administrateurs", String.valueOf(admins));
            addStatCell(table, "Utilisateurs Inactifs", String.valueOf(totalUsers - activeUsers));
            addStatCell(table, "Date du rapport", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            document.add(table);
            document.close();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void addStatCell(PdfPTable table, String label, String value) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
        table.addCell(new PdfPCell(new Phrase(label, font)));
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(valueCell);
    }
}