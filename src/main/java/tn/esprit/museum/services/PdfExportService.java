package tn.esprit.museum.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import tn.esprit.museum.entities.Sponsor;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PdfExportService {

    public static void exportSponsorsToPdf(List<Sponsor> sponsors, File destFile) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(destFile));
        document.open();

        // Titre
        Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Paragraph titre = new Paragraph("Liste des Sponsors Acceptes", fontTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(10);
        document.add(titre);

        // Date de generation
        Font fontDate = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        String dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        Paragraph datePar = new Paragraph("Genere le : " + dateStr, fontDate);
        datePar.setAlignment(Element.ALIGN_RIGHT);
        datePar.setSpacingAfter(20);
        document.add(datePar);

        // Tableau
        PdfPTable table = new PdfPTable(5); // 5 colonnes
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);
        table.setWidths(new float[]{2f, 2f, 2f, 2f, 2f});

        // En-têtes
        String[] headers = {"Nom", "Type", "Email", "Telephone", "Date de creation"};
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
        BaseColor headerBg = new BaseColor(44, 62, 80); // #2c3e50

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Lignes
        Font fontCell = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        for (Sponsor s : sponsors) {
            table.addCell(createCell(s.getNom(), fontCell));
            table.addCell(createCell(s.getType(), fontCell));
            table.addCell(createCell(s.getEmail(), fontCell));
            table.addCell(createCell(s.getTelephone(), fontCell));
            String dateC = s.getDateCreation() != null ? sdf.format(s.getDateCreation()) : "N/A";
            table.addCell(createCell(dateC, fontCell));
        }

        document.add(table);
        document.close();
    }

    private static PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}

