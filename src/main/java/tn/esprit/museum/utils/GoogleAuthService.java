package tn.esprit.museum.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleAuthService {

    // 🔑 REMPLACEZ PAR VOS IDENTIFIANTS
    public static final String CLIENT_ID = "402545437624-p9utcpvrff9ajvljb1ak1ngq90c6m0h7.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "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"; // À récupérer depuis Google Cloud Console
    public static final String REDIRECT_URI = "http://localhost:8080/callback";

    /**
     * Génère l'URL d'authentification Google
     */
    public static String getAuthorizationUrl() {
        try {
            return "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + CLIENT_ID +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8.toString()) +
                    "&response_type=code" +
                    "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8.toString()) +
                    "&access_type=offline";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Échange le code d'autorisation contre un token et récupère les infos utilisateur
     */
    public static GoogleUserInfo exchangeCodeForUserInfo(String authorizationCode) throws Exception {
        // 1. Échanger le code contre un token
        String tokenUrl = "https://oauth2.googleapis.com/token";
        String postData = "code=" + authorizationCode +
                "&client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8.toString()) +
                "&grant_type=authorization_code";

        HttpURLConnection tokenConn = (HttpURLConnection) new URL(tokenUrl).openConnection();
        tokenConn.setRequestMethod("POST");
        tokenConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        tokenConn.setDoOutput(true);

        try (OutputStream os = tokenConn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Lire la réponse du token
        BufferedReader tokenReader = new BufferedReader(new InputStreamReader(tokenConn.getInputStream()));
        StringBuilder tokenResponse = new StringBuilder();
        String line;
        while ((line = tokenReader.readLine()) != null) {
            tokenResponse.append(line);
        }
        tokenReader.close();

        // Extraire le token d'accès
        JsonObject tokenJson = JsonParser.parseString(tokenResponse.toString()).getAsJsonObject();
        String accessToken = tokenJson.get("access_token").getAsString();

        // 2. Récupérer les informations utilisateur
        return getUserInfo(accessToken);
    }

    /**
     * Récupère les informations de l'utilisateur à partir du token
     */
    private static GoogleUserInfo getUserInfo(String accessToken) throws Exception {
        URL url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

        GoogleUserInfo userInfo = new GoogleUserInfo();
        if (json.has("id")) userInfo.setId(json.get("id").getAsString());
        if (json.has("email")) userInfo.setEmail(json.get("email").getAsString());
        if (json.has("name")) userInfo.setName(json.get("name").getAsString());
        if (json.has("given_name")) userInfo.setGivenName(json.get("given_name").getAsString());
        if (json.has("family_name")) userInfo.setFamilyName(json.get("family_name").getAsString());
        if (json.has("picture")) userInfo.setPicture(json.get("picture").getAsString());
        if (json.has("verified_email")) userInfo.setVerifiedEmail(json.get("verified_email").getAsBoolean());

        return userInfo;
    }

    /**
     * Classe pour stocker les informations utilisateur Google
     */
    public static class GoogleUserInfo {
        private String id;
        private String email;
        private String name;
        private String givenName;
        private String familyName;
        private String picture;
        private boolean verifiedEmail;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getGivenName() { return givenName; }
        public void setGivenName(String givenName) { this.givenName = givenName; }

        public String getFamilyName() { return familyName; }
        public void setFamilyName(String familyName) { this.familyName = familyName; }

        public String getPicture() { return picture; }
        public void setPicture(String picture) { this.picture = picture; }

        public boolean isVerifiedEmail() { return verifiedEmail; }
        public void setVerifiedEmail(boolean verifiedEmail) { this.verifiedEmail = verifiedEmail; }

        @Override
        public String toString() {
            return "GoogleUserInfo{" +
                    "email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}