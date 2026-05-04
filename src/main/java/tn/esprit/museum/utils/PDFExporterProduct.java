package tn.esprit.museum.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.museum.controllers.DashboardController;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PDFExporterProduct {

    public static void exportTopProductsPDF(ObservableList<DashboardController.ProductStat> topProducts, String period, String category, Stage stage) {
        try {
            System.out.println("=== DÉBUT EXPORT PDF ===");
            System.out.println("Nombre de produits: " + topProducts.size());

            if (topProducts == null || topProducts.isEmpty()) {
                System.out.println("❌ Aucune donnée");
                return;
            }

            // Choix du fichier
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.setInitialFileName("classement_ventes_" + System.currentTimeMillis() + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

            File file = fileChooser.showSaveDialog(stage);
            if (file == null) return;

            // ⭐ Configuration du document avec marges plus petites
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // ⭐ TITRE
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("🏛️ MUSEUM DIGITAL", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Classement des meilleures ventes", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(Chunk.NEWLINE);

            // ⭐ INFORMATIONS
            Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            Paragraph info = new Paragraph("Période: " + period + " | Catégorie: " + category + " | Date: " + dateStr, infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            document.add(info);

            document.add(Chunk.NEWLINE);

            // ⭐ TABLEAU
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            // Largeurs des colonnes
            float[] columnWidths = {0.7f, 2.5f, 1.2f, 1.5f};
            table.setWidths(columnWidths);

            // ⭐ EN-TÊTES avec couleurs
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
            BaseColor headerColor = new BaseColor(26, 42, 79);

            // N°
            PdfPCell cell1 = new PdfPCell(new Phrase("N°", headerFont));
            cell1.setBackgroundColor(headerColor);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell1.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell1.setPadding(12);
            table.addCell(cell1);

            // Produit
            PdfPCell cell2 = new PdfPCell(new Phrase("Produit", headerFont));
            cell2.setBackgroundColor(headerColor);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell2.setPadding(12);
            table.addCell(cell2);

            // Quantité
            PdfPCell cell3 = new PdfPCell(new Phrase("Quantité vendue", headerFont));
            cell3.setBackgroundColor(headerColor);
            cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell3.setPadding(12);
            table.addCell(cell3);

            // CA
            PdfPCell cell4 = new PdfPCell(new Phrase("Chiffre d'affaires (D)", headerFont));
            cell4.setBackgroundColor(headerColor);
            cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell4.setPadding(12);
            table.addCell(cell4);

            // ⭐ DONNÉES
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
            int rank = 1;
            double totalRevenue = 0;
            int totalQuantity = 0;

            for (DashboardController.ProductStat product : topProducts) {
                System.out.println("➕ Ajout au PDF: " + product.getProductName());

                // N°
                PdfPCell rankCell = new PdfPCell(new Phrase(String.valueOf(rank), dataFont));
                rankCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                rankCell.setPadding(8);
                table.addCell(rankCell);

                // Nom
                PdfPCell nameCell = new PdfPCell(new Phrase(product.getProductName(), dataFont));
                nameCell.setPadding(8);
                table.addCell(nameCell);

                // Quantité
                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(product.getTotalSold()), dataFont));
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qtyCell.setPadding(8);
                table.addCell(qtyCell);

                // CA
                PdfPCell revenueCell = new PdfPCell(new Phrase(String.format("%.2f", product.getTotalRevenue()), dataFont));
                revenueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                revenueCell.setPadding(8);
                table.addCell(revenueCell);

                totalRevenue += product.getTotalRevenue();
                totalQuantity += product.getTotalSold();
                rank++;
            }

            document.add(table);

            // ⭐ TOTAL
            document.add(Chunk.NEWLINE);
            Font totalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Paragraph totalPara = new Paragraph("Total général: " + totalQuantity + " unités vendues  |  " + String.format("%.2f", totalRevenue) + " Dinar", totalFont);
            totalPara.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalPara);

            // ⭐ PIED DE PAGE
            document.add(Chunk.NEWLINE);
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
            Paragraph footer = new Paragraph("© " + java.time.Year.now().getValue() + " Museum Digital - Tous droits réservés", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            System.out.println("✅ PDF créé: " + file.getAbsolutePath());
            System.out.println("   Taille: " + file.length() + " bytes");

            // ⭐ OUVERTURE AUTOMATIQUE DU PDF
            try {
                java.awt.Desktop.getDesktop().open(file);
                System.out.println("📄 PDF ouvert automatiquement");
            } catch (Exception e) {
                System.out.println("⚠️ Impossible d'ouvrir le PDF automatiquement");
            }

        } catch (Exception e) {
            System.err.println("❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
