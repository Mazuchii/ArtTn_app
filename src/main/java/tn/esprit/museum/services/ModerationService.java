package tn.esprit.museum.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class ModerationService {

    private static final String API_URL = "http://localhost:5001/moderate";

    public static ModerationResult moderateText(String text) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = "{\"text\": \"" + text.replace("\"", "\\\"") + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject obj = new JSONObject(response.body());

            return new ModerationResult(
                    obj.getString("label"),
                    obj.getBoolean("is_toxic"),
                    obj.getBoolean("is_spam")
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new ModerationResult("normal", false, false);
        }
    }

    public static class ModerationResult {
        private final String label;
        private final boolean isToxic;
        private final boolean isSpam;

        public ModerationResult(String label, boolean isToxic, boolean isSpam) {
            this.label = label;
            this.isToxic = isToxic;
            this.isSpam = isSpam;
        }

        public String getLabel() { return label; }
        public boolean isToxic() { return isToxic; }
        public boolean isSpam() { return isSpam; }
    }
}
