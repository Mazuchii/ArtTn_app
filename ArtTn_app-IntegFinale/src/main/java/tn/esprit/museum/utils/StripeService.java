package tn.esprit.museum.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class StripeService {

    private static final String STRIPE_SECRET_KEY = "sk_test_51T4HzaCEP5dqnoFnFRssxZKR4Xr8DffoSUYv0XAOpifPGiOH9lBh2pkGZnPLslXOhq9yR5kAoxo8ZJw4WJ9NDkB200Z58q99uc";
    private static final String STRIPE_PUBLIC_KEY = "pk_test_51T4HzaCEP5dqnoFnUxYcaWu6nJVU82y52VIU20ppO0wc9epO3TGYV4VckYbg1w7LXwZ3loqwK1VOtYS9VBs1rt8c00CdtVu3ZQ";

    public static String getPublicKey() {
        return STRIPE_PUBLIC_KEY;
    }

    public static String createPaymentLink(double amount, String currency, int orderId, String successUrl, String cancelUrl) {
        try {
            long amountInCents = (long) (amount * 100);

            String urlParameters =
                    "line_items[0][price_data][currency]=" + currency +
                            "&line_items[0][price_data][product_data][name]=Commande%20Museum%20Digital%20%23" + orderId +
                            "&line_items[0][price_data][unit_amount]=" + amountInCents +
                            "&line_items[0][quantity]=1" +
                            "&payment_method_types[0]=card" +
                            "&after_completion[type]=redirect" +
                            "&after_completion[redirect][url]=" + URLEncoder.encode(successUrl, StandardCharsets.UTF_8.toString());


            System.out.println("📡 [API STRIPE] Appel à l'API REST");
            System.out.println("   Commande #" + orderId);
            System.out.println("   Montant: " + amount + " " + currency);


            URL url = new URL("https://api.stripe.com/v1/payment_links");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Authorization", "Bearer " + STRIPE_SECRET_KEY);
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(urlParameters);
                wr.flush();
            }

            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()));

            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (responseCode >= 200 && responseCode < 300) {
                String responseStr = response.toString();
                System.out.println("API Stripe - Code " + responseCode);

                JsonObject jsonObject = JsonParser.parseString(responseStr).getAsJsonObject();
                String paymentLink = jsonObject.get("url").getAsString();

                System.out.println("LIEN STRIPE GÉNÉRÉ: " + paymentLink);
                return paymentLink;
            } else {
                System.err.println("API Stripe - Erreur " + responseCode);
                System.err.println("Message: " + response.toString());
            }

        } catch (Exception e) {
            System.err.println("Erreur Stripe: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback
        System.out.println("Utilisation du lien de secours");
        return "https://buy.stripe.com/test_00wdR8dKUf1DfY5b3c7g400";
    }
}
