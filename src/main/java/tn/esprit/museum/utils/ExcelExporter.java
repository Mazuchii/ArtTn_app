package tn.esprit.museum.utils;

import tn.esprit.museum.entities.DemandeJob;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {

    public static void exportDemandes(List<DemandeJob> list, String filePath) throws IOException {
        // 1. Créer le classeur Excel (.xlsx)
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Candidatures");

        // 2. Créer le style pour l'en-tête (Gras + Couleur)
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 3. Créer l'en-tête
        String[] columns = {"ID", "Candidat ID", "Offre ID", "Statut", "Lettre Motivation"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // 4. Remplir les données
        int rowNum = 1;
        for (DemandeJob d : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(d.getId());
            row.createCell(1).setCellValue(d.getCandidatId());
            row.createCell(2).setCellValue(d.getOffreId());
            row.createCell(3).setCellValue(d.getStatus());
            row.createCell(4).setCellValue(d.getLettreMotivation());
        }

        // 5. Ajuster automatiquement la largeur des colonnes
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 6. Écrire le fichier
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}