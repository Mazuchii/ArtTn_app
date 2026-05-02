package tn.esprit.museum.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TranslationService {

    // API gratuite (MyMemory - pas besoin de clé API)
    private static final String API_URL = "https://api.mymemory.translated.net/get";

    // Traduire un texte du français vers l'arabe
    public static String translateToArabic(String text) {
        if (text == null || text.isEmpty()) return "";

        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");
            String urlStr = API_URL + "?q=" + encodedText + "&langpair=fr|ar";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Extraire le texte traduit du JSON
            String json = response.toString();
            String translated = extractTranslatedText(json);

            conn.disconnect();
            return translated != null ? translated : "خطأ في الترجمة";

        } catch (Exception e) {
            System.err.println("Erreur traduction: " + e.getMessage());
            return "خطأ في الترجمة";
        }
    }

    private static String extractTranslatedText(String json) {
        try {
            // Recherche du texte traduit dans la réponse JSON
            String search = "\"translatedText\":\"";
            int start = json.indexOf(search);
            if (start != -1) {
                start += search.length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    String translated = json.substring(start, end);
                    // Décoder les caractères Unicode
                    return decodeUnicode(translated);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur extraction: " + e.getMessage());
        }
        return null;
    }

    private static String decodeUnicode(String input) {
        // Gérer les caractères Unicode comme \u0627
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (input.startsWith("\\u", i) && i + 5 < input.length()) {
                try {
                    String hex = input.substring(i + 2, i + 6);
                    char c = (char) Integer.parseInt(hex, 16);
                    result.append(c);
                    i += 6;
                } catch (NumberFormatException e) {
                    result.append(input.charAt(i));
                    i++;
                }
            } else {
                result.append(input.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}