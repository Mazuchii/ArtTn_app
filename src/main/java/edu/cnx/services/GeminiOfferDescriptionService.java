package edu.cnx.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiOfferDescriptionService {

    private static final String[] MODELS = {
            "gemini-2.5-pro",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite"
    };
    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final Path LOCAL_SECRETS_PATH = Path.of("local-secrets.properties");
    private static final int MAX_RETRIES_PER_MODEL = 3;
    private static final int MIN_ACCEPTABLE_LENGTH = 260;
    private static final String DEFAULT_API_KEY = "AIzaSyC_KtY7CMO9rpUwr1ZEqK6OUrrrFmnYLeQ";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String generateOffer(String title, String userPrompt) throws IOException, InterruptedException {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("La cle API Gemini est absente.");
        }

        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedPrompt = userPrompt == null ? "" : userPrompt.trim();
        if (normalizedPrompt.isEmpty()) {
            throw new IllegalArgumentException("Le prompt de generation est obligatoire.");
        }

        String lastErrorMessage = "Generation Gemini indisponible.";

        for (String model : MODELS) {
            for (int attempt = 1; attempt <= MAX_RETRIES_PER_MODEL; attempt++) {
                String body = buildRequestBody(normalizedTitle, normalizedPrompt, attempt);
                HttpResponse<String> response = sendGenerateRequest(apiKey, body, model);
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String generatedText = extractGeneratedText(response.body());
                    if (generatedText.isBlank()) {
                        throw new IOException("Gemini n'a renvoye aucun texte exploitable.");
                    }
                    String cleanedText = formatOffer(generatedText);
                    if (isCompleteOffer(cleanedText)) {
                        return cleanedText;
                    }

                    lastErrorMessage = "La reponse Gemini etait incomplete.";
                    if (attempt < MAX_RETRIES_PER_MODEL) {
                        waitBeforeRetry(attempt);
                        continue;
                    }

                    continue;
                }

                lastErrorMessage = buildErrorMessage(response);
                if (!isRetriableStatus(response.statusCode())) {
                    throw new IOException(lastErrorMessage);
                }

                if (attempt < MAX_RETRIES_PER_MODEL) {
                    waitBeforeRetry(attempt);
                }
            }
        }

        throw new IOException(lastErrorMessage + " Merci de reessayer dans quelques instants.");
    }

    private String resolveApiKey() {
        String googleApiKey = System.getenv("GOOGLE_API_KEY");
        if (googleApiKey != null && !googleApiKey.isBlank()) {
            return googleApiKey.trim();
        }

        String geminiApiKey = System.getenv("GEMINI_API_KEY");
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return geminiApiKey.trim();
        }

        if (DEFAULT_API_KEY != null && !DEFAULT_API_KEY.isBlank()) {
            return DEFAULT_API_KEY.trim();
        }

        String localApiKey = resolveApiKeyFromLocalFile();
        if (localApiKey != null && !localApiKey.isBlank()) {
            return localApiKey.trim();
        }

        return null;
    }

    private String resolveApiKeyFromLocalFile() {
        if (!Files.exists(LOCAL_SECRETS_PATH)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(LOCAL_SECRETS_PATH)) {
            properties.load(inputStream);
            String googleApiKey = properties.getProperty("GOOGLE_API_KEY");
            if (googleApiKey != null && !googleApiKey.isBlank()) {
                return googleApiKey;
            }

            String geminiApiKey = properties.getProperty("GEMINI_API_KEY");
            if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                return geminiApiKey;
            }
        } catch (IOException ignored) {
            return null;
        }

        return null;
    }

    private HttpResponse<String> sendGenerateRequest(String apiKey, String body, String model)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT_TEMPLATE.formatted(model)))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private boolean isRetriableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }

    private void waitBeforeRetry(int attempt) throws InterruptedException {
        long delayMs = switch (attempt) {
            case 1 -> 1200L;
            case 2 -> 2500L;
            default -> 4000L;
        };
        Thread.sleep(delayMs);
    }

    private String buildRequestBody(String title, String userPrompt, int attempt) {
        String titleBlock = title == null || title.isBlank()
                ? "Titre du poste: non precise"
                : "Titre du poste: " + title;

        String completionRule = attempt <= 1
                ? ""
                : """
                
                Important:
                - La reponse precedente etait incomplete.
                - Tu dois fournir l'annonce complete en une seule fois.
                - Ne t'arrete pas apres le titre ou l'introduction.
                - Fournis obligatoirement toutes les sections jusqu'a la fin.
                """;

        String prompt = """
                Tu es un recruteur francophone.
                Redige une offre d'emploi de tres bonne qualite a partir des instructions utilisateur ci-dessous.

                %s

                Besoin utilisateur:
                %s

                Regles obligatoires:
                - Retourne uniquement le texte final de l'offre.
                - Ecris en francais naturel, professionnel et fluide.
                - Evite les phrases vagues, les repetitions et le texte generique.
                - Respecte le besoin utilisateur et integre les mots-cles importants demandes.
                - Adapte fortement le contenu au metier, au niveau d'experience, aux responsabilites et aux competences attendues.
                - Si des technologies, outils, missions ou soft skills sont mentionnes dans le besoin utilisateur, integre-les naturellement.
                - Si une information n'est pas fournie, reste credible sans inventer des details faux ou ultra specifiques.
                - N'ajoute ni salaire, ni entreprise, ni localisation.

                Format de sortie obligatoire:
                - Reponds uniquement avec un JSON valide
                - Sans bloc markdown
                - Sans texte avant ou apres le JSON

                Structure JSON obligatoire:
                {
                  "titre_suggere": "string",
                  "presentation": "string",
                  "missions": ["string", "string", "string", "string", "string"],
                  "profil": ["string", "string", "string", "string", "string"]
                }

                Contraintes sur le contenu:
                - "presentation": 3 a 4 phrases fluides
                - "missions": exactement 5 elements concrets
                - "profil": exactement 5 elements concrets
                - Ton moderne et professionnel
                - Formulation RH naturelle
                - Contenu utile pour une vraie annonce
                - Contenu riche en mots-cles metier pertinents
                - N'utilise pas le markdown comme ** ou ##
                %s
                """.formatted(titleBlock, userPrompt, completionRule);

        return """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.7,
                    "maxOutputTokens": 900,
                    "responseMimeType": "application/json",
                    "responseSchema": {
                      "type": "OBJECT",
                      "properties": {
                        "titre_suggere": { "type": "STRING" },
                        "presentation": { "type": "STRING" },
                        "missions": {
                          "type": "ARRAY",
                          "items": { "type": "STRING" }
                        },
                        "profil": {
                          "type": "ARRAY",
                          "items": { "type": "STRING" }
                        }
                      },
                      "required": ["titre_suggere", "presentation", "missions", "profil"]
                    }
                  }
                }
                """.formatted(escapeJson(prompt));
    }

    private String extractGeneratedText(String responseBody) {
        Matcher matcher = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(responseBody);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1)).trim();
        }
        return "";
    }

    private String formatOffer(String rawText) {
        String json = cleanupGeneratedText(rawText);
        String suggestedTitle = extractJsonString(json, "titre_suggere");
        String presentation = extractJsonString(json, "presentation");
        List<String> missions = extractJsonArray(json, "missions");
        List<String> profil = extractJsonArray(json, "profil");

        if (presentation.isBlank() || missions.isEmpty() || profil.isEmpty()) {
            return json;
        }

        StringJoiner joiner = new StringJoiner("\n");
        if (!suggestedTitle.isBlank()) {
            joiner.add(suggestedTitle);
            joiner.add("");
        }

        joiner.add("Presentation du poste :");
        joiner.add(presentation);
        joiner.add("");
        joiner.add("Missions principales :");
        for (String mission : missions) {
            joiner.add("- " + mission);
        }
        joiner.add("");
        joiner.add("Profil recherche :");
        for (String item : profil) {
            joiner.add("- " + item);
        }

        return joiner.toString().trim();
    }

    private String cleanupGeneratedText(String text) {
        return text.replace("```json", "")
                .replace("```markdown", "")
                .replace("```", "")
                .replace("**", "")
                .replace("##", "")
                .replace("\r\n", "\n")
                .trim();
    }

    private boolean isCompleteOffer(String text) {
        String normalized = text == null ? "" : text.toLowerCase();
        return normalized.length() >= MIN_ACCEPTABLE_LENGTH
                && normalized.contains("presentation du poste")
                && normalized.contains("missions principales")
                && normalized.contains("profil recherche");
    }

    private String extractJsonString(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1)).trim();
        }
        return "";
    }

    private List<String> extractJsonArray(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
        List<String> values = new ArrayList<>();
        if (!matcher.find()) {
            return values;
        }

        Matcher itemMatcher = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(matcher.group(1));
        while (itemMatcher.find()) {
            String value = unescapeJson(itemMatcher.group(1)).trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String buildErrorMessage(HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();
        Matcher matcher = Pattern.compile("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(body);
        if (matcher.find()) {
            return "Erreur Gemini (" + response.statusCode() + ") : " + unescapeJson(matcher.group(1));
        }
        return "Erreur Gemini (" + response.statusCode() + ").";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
