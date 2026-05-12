package tn.esprit.museum.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PriceOptimizationService {

    private static final String API_KEY = "AIzaSyCbq-Wf9CsEbjYc88HrmcOk2cWp2JASywU";
    private static final String MODEL   = "gemini-2.5-flash";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/" + MODEL + ":generateContent?key=" + API_KEY;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // ==================== ANALYSE GLOBALE ====================
    public String analyzeAndOptimizePrices(List<Event> events, List<Reservation> reservations) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un expert en optimisation de prix pour un musée digital en Tunisie. " +
                "Analyse ces données et propose des ajustements.\n\n");
        prompt.append("=== ÉVÉNEMENTS ===\n");
        for (Event ev : events) {
            int fillRate = ev.getMaxCapacity() > 0 ? (ev.getCurrentCapacity() * 100) / ev.getMaxCapacity() : 0;
            prompt.append(String.format("- ID:%d | %s | Prix: %.2f DT | Remplissage: %d%% (%d/%d) | Statut: %s | Catégorie: %s\n",
                    ev.getId(), ev.getTitle(), ev.getPrice(), fillRate,
                    ev.getCurrentCapacity(), ev.getMaxCapacity(),
                    ev.getStatus() != null ? ev.getStatus() : "N/A",
                    ev.getCategory() != null ? ev.getCategory() : "N/A"));
        }
        prompt.append("\n=== RÉSERVATIONS ===\n");
        prompt.append("Total: ").append(reservations.size()).append("\n");
        long confirmed = reservations.stream().filter(r -> "CONFIRMEE".equals(r.getStatus())).count();
        long cancelled = reservations.stream().filter(r -> "ANNULEE".equals(r.getStatus())).count();
        long pending   = reservations.stream().filter(r -> "EN_ATTENTE".equals(r.getStatus())).count();
        prompt.append("Confirmées: ").append(confirmed).append("\n");
        prompt.append("Annulées: ").append(cancelled).append("\n");
        prompt.append("En attente: ").append(pending).append("\n");
        prompt.append("\n=== FORMAT DE RÉPONSE ATTENDU ===\n");
        prompt.append("Pour chaque événement, réponds EXACTEMENT ainsi:\n\n");
        prompt.append("📌 [Titre de l'événement]\n");
        prompt.append("   💰 Prix actuel: X DT → Prix suggéré: Y DT\n");
        prompt.append("   📊 Taux remplissage: Z%\n");
        prompt.append("   🎯 Action: AUGMENTER / BAISSER / MAINTENIR\n");
        prompt.append("   💡 Raison: [justification en 1-2 phrases]\n\n");
        prompt.append("Termine par un résumé global.\nRéponds uniquement en français.");
        return callGemini(prompt.toString(), 2048);
    }

    // ==================== SUGGESTION POUR UN ÉVÉNEMENT UNIQUE ====================
    public PriceSuggestion suggestPriceForEvent(Event event, List<Reservation> allReservations) {
        try {
            String prompt = buildSingleEventPrompt(event, allReservations);
            String response = callGemini(prompt, 300);
            return parseSuggestion(response, event.getPrice().doubleValue());
        } catch (Exception e) {
            return fallbackSuggestion(event);
        }
    }

    private String buildSingleEventPrompt(Event event, List<Reservation> allReservations) {
        long eventReservations = allReservations.stream()
                .filter(r -> r.getEventId() == event.getId() && "CONFIRMEE".equals(r.getStatus()))
                .count();
        int fillRate = event.getMaxCapacity() > 0
                ? (event.getCurrentCapacity() * 100) / event.getMaxCapacity() : 0;
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un expert en tarification pour un musée en Tunisie.\n");
        prompt.append("Analyse l'événement suivant et propose un nouveau prix optimal en dinars tunisiens (DT).\n");
        prompt.append("Réponds UNIQUEMENT au format JSON valide, sans texte supplémentaire :\n");
        prompt.append("{\"suggested_price\": XX.XX, \"reason\": \"justification courte\"}\n\n");
        prompt.append("Événement :\n");
        prompt.append("- Titre : ").append(event.getTitle()).append("\n");
        prompt.append("- Catégorie : ").append(event.getCategory()).append("\n");
        prompt.append("- Prix actuel : ").append(event.getPrice()).append(" DT\n");
        prompt.append("- Capacité totale : ").append(event.getMaxCapacity()).append("\n");
        prompt.append("- Places réservées : ").append(event.getCurrentCapacity()).append("\n");
        prompt.append("- Taux de remplissage : ").append(fillRate).append("%\n");
        prompt.append("- Réservations confirmées : ").append(eventReservations).append("\n");
        return prompt.toString();
    }

    private String callGemini(String prompt, int maxTokens) throws IOException {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("topP", 0.8);
        requestBody.put("generationConfig", generationConfig);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) throw new IOException("Erreur API (" + response.code() + "): " + responseBody);
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).getString("text");
        }
    }

    private PriceSuggestion parseSuggestion(String geminiResponse, double currentPrice) {
        try {
            int start = geminiResponse.indexOf('{');
            int end = geminiResponse.lastIndexOf('}');
            if (start != -1 && end != -1) {
                String jsonStr = geminiResponse.substring(start, end + 1);
                JSONObject json = new JSONObject(jsonStr);
                double suggested = json.optDouble("suggested_price", -1);
                if (suggested > 0) {
                    String reason = json.optString("reason", "Aucune justification fournie.");
                    return new PriceSuggestion(suggested, reason);
                }
            }
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\d+\\.\\d{2}");
            java.util.regex.Matcher m = p.matcher(geminiResponse);
            if (m.find()) {
                double suggested = Double.parseDouble(m.group());
                return new PriceSuggestion(suggested, "Suggestion automatique basée sur l'analyse IA.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PriceSuggestion(currentPrice, "Conservation du prix (extraction impossible)");
    }

    private PriceSuggestion fallbackSuggestion(Event event) {
        if (event == null) return new PriceSuggestion(0, "Erreur de suggestion.");
        double currentPrice = event.getPrice().doubleValue();
        int fillRate = event.getMaxCapacity() > 0 ? (event.getCurrentCapacity() * 100) / event.getMaxCapacity() : 0;
        double categoryFactor = switch (event.getCategory() != null ? event.getCategory() : "") {
            case "SPECTACLE"   -> 1.05;
            case "CONFERENCE"  -> 0.95;
            case "ATELIER"     -> 0.9;
            default            -> 1.0;
        };
        double suggested;
        String reason;
        if (fillRate < 20) {
            suggested = currentPrice * 0.85 * categoryFactor;
            reason = "Faible remplissage, baisse recommandée";
        } else if (fillRate > 70) {
            suggested = currentPrice * 1.15 * categoryFactor;
            reason = "Forte demande, augmentation possible";
        } else {
            suggested = currentPrice * categoryFactor;
            reason = "Remplissage moyen, ajustement selon la catégorie";
        }
        suggested = Math.round(suggested * 2) / 2.0;
        return new PriceSuggestion(suggested, reason + " (simulation IA indisponible)");
    }

    public static class PriceSuggestion {
        private final double suggestedPrice;
        private final String reason;
        public PriceSuggestion(double suggestedPrice, String reason) {
            this.suggestedPrice = suggestedPrice;
            this.reason = reason;
        }
        public double getSuggestedPrice() { return suggestedPrice; }
        public String getReason() { return reason; }
    }
}

