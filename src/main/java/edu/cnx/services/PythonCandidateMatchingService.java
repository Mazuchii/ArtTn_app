package edu.cnx.services;

import edu.cnx.entités.DemandeJob;
import edu.cnx.entités.OfferJob;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PythonCandidateMatchingService {

    private static final String SCRIPT_PATH = "python/candidate_matching.py";

    public String rankCandidatesForOffer(OfferJob offer, List<DemandeJob> candidatures) throws IOException, InterruptedException {
        Path tempInput = Files.createTempFile("candidate-matching-", ".json");

        try {
            String payload = buildPayload(offer, candidatures);
            Files.writeString(tempInput, payload, StandardCharsets.UTF_8);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python",
                    SCRIPT_PATH,
                    tempInput.toString(),
                    "--text"
            );
            processBuilder.redirectErrorStream(true);

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
            json.append("\"lettre_motivation\":\"").append(escapeJson(demande.getLettreMotivation())).append("\",");
            json.append("\"status\":\"").append(escapeJson(demande.getStatus())).append("\"");
            json.append("}");
        }

        json.append("]");
        json.append("}");
        return json.toString();
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
