package tn.esprit.museum.services;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import tn.esprit.museum.entities.DemandeJob;
import tn.esprit.museum.entities.OfferJob;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PythonCandidateMatchingService {

    private static final Path SCRIPT_PATH = Paths.get("ml", "candidate_matching.py");

    public String rankCandidatesForOffer(OfferJob offer, List<DemandeJob> candidatures) throws IOException, InterruptedException {
        Path tempInput = Files.createTempFile("candidate-matching-", ".json");

        try {
            String payload = buildPayload(offer, candidatures);
            Files.writeString(tempInput, payload, StandardCharsets.UTF_8);

            Path script = SCRIPT_PATH.toAbsolutePath().normalize();
            if (!Files.exists(script)) {
                throw new IOException("Script Python introuvable: " + script);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python",
                    script.toString(),
                    tempInput.toString(),
                    "--text"
            );
            processBuilder.redirectErrorStream(true);
            processBuilder.directory(Paths.get("").toAbsolutePath().toFile());

            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException("Le script Python a échoué: " + output);
            }

            return output.isBlank() ? "Aucun résultat retourné par le script Python." : output.trim();
        } finally {
            Files.deleteIfExists(tempInput);
        }
    }

    public String analyzeCandidateForOffer(OfferJob offer, DemandeJob candidature) throws IOException, InterruptedException {
        return rankCandidatesForOffer(offer, List.of(candidature));
    }

    public String analyzeCandidateForOfferJson(OfferJob offer, DemandeJob candidature) throws IOException, InterruptedException {
        Path tempInput = Files.createTempFile("candidate-matching-json-", ".json");
        try {
            String payload = buildPayload(offer, List.of(candidature));
            Files.writeString(tempInput, payload, StandardCharsets.UTF_8);

            Path script = SCRIPT_PATH.toAbsolutePath().normalize();
            ProcessBuilder processBuilder = new ProcessBuilder("python", script.toString(), tempInput.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) throw new IOException("Python script failed: " + output);
            return output.trim();
        } finally {
            Files.deleteIfExists(tempInput);
        }
    }

    private String buildPayload(OfferJob offer, List<DemandeJob> candidatures) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"offer\":{");
        json.append("\"titre\":\"").append(escapeJson(offer.getTitre())).append("\",");
        json.append("\"description\":\"").append(escapeJson(offer.getDescription())).append("\"");
        json.append("},");
        json.append("\"candidates\":[");

        for (int i = 0; i < candidatures.size(); i++) {
            DemandeJob demande = candidatures.get(i);
            if (i > 0) {
                json.append(",");
            }

            json.append("{");
            json.append("\"candidat_id\":\"").append(escapeJson(demande.getCandidatId())).append("\",");
            json.append("\"cv_url\":\"").append(escapeJson(demande.getCvUrl())).append("\",");
            json.append("\"cv_text\":\"").append(escapeJson(extractCvText(demande.getCvUrl()))).append("\",");
            json.append("\"lettre_motivation\":\"").append(escapeJson(demande.getLettreMotivation())).append("\",");
            json.append("\"status\":\"").append(escapeJson(demande.getStatus())).append("\"");
            json.append("}");
        }

        json.append("]");
        json.append("}");
        return json.toString();
    }

    private String extractCvText(String cvUrl) {
        if (cvUrl == null || cvUrl.isBlank()) {
            return "";
        }

        Path cvPath = Paths.get(cvUrl).toAbsolutePath().normalize();
        if (!Files.exists(cvPath) || !Files.isRegularFile(cvPath)) {
            return "";
        }

        String fileName = cvPath.getFileName().toString().toLowerCase();
        try {
            if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
                return Files.readString(cvPath, StandardCharsets.UTF_8);
            }

            if (fileName.endsWith(".pdf")) {
                return extractPdfText(cvPath);
            }
        } catch (IOException e) {
            return "";
        }

        return "";
    }

    private String extractPdfText(Path pdfPath) throws IOException {
        StringBuilder text = new StringBuilder();
        PdfReader reader = new PdfReader(pdfPath.toString());
        try {
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(reader, page)).append('\n');
            }
        } finally {
            reader.close();
        }
        return text.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }
}

