package tn.esprit.museum.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SalesPredictionService {

    private static final String API_BASE_URL = "http://localhost:5002";

    /*Vérifie si le service de prédiction est disponible*/

    public static boolean isAvailable() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(API_BASE_URL + "/health");
            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (IOException e) {
            System.err.println("❌ Service IA indisponible: " + e.getMessage());
            return false;
        }
    }

    /*Prédit les ventes pour un produit et un mois donné*/

    public static PredictionResult predictSales(int productId, int month) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = API_BASE_URL + "/predict/" + productId + "/" + month;
            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = client.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                PredictionResult result = new PredictionResult();

                if (obj.has("error")) {
                    result.setSuccess(false);
                    result.setErrorMessage(obj.get("error").getAsString());
                    return result;
                }

                result.setSuccess(true);
                result.setProductId(obj.get("product_id").getAsInt());
                result.setMonth(obj.get("month").getAsInt());
                result.setMonthName(obj.get("month_name").getAsString());
                result.setPredictedSales(obj.get("predicted_sales").getAsDouble());
                result.setConfidence(obj.has("confidence") ? obj.get("confidence").getAsString() : "N/A");
                result.setTrend(obj.has("trend") ? obj.get("trend").getAsString() : "N/A");

                return result;
            }
        } catch (IOException e) {
            PredictionResult error = new PredictionResult();
            error.setSuccess(false);
            error.setErrorMessage("Erreur de connexion: " + e.getMessage());
            return error;
        }
    }

    /* Prédit les ventes pour tous les mois de l'année
     */
    public static YearlyPrediction predictYear(int productId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = API_BASE_URL + "/predict-year/" + productId;
            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = client.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                YearlyPrediction result = new YearlyPrediction();

                if (obj.has("error")) {
                    result.setSuccess(false);
                    result.setErrorMessage(obj.get("error").getAsString());
                    return result;
                }

                result.setSuccess(true);
                result.setProductId(obj.get("product_id").getAsInt());
                result.setTotalAnnual(obj.get("total_annual").getAsDouble());
                result.setAverageMonthly(obj.get("average_monthly").getAsDouble());

                List<MonthlyPrediction> predictions = new ArrayList<>();
                if (obj.has("predictions")) {
                    for (var elem : obj.getAsJsonArray("predictions")) {
                        JsonObject p = elem.getAsJsonObject();
                        MonthlyPrediction mp = new MonthlyPrediction();
                        mp.setMonth(p.get("month").getAsInt());
                        mp.setMonthName(p.get("month_name").getAsString());
                        mp.setPredictedSales(p.get("predicted_sales").getAsDouble());
                        predictions.add(mp);
                    }
                }
                result.setPredictions(predictions);

                return result;
            }
        } catch (IOException e) {
            YearlyPrediction error = new YearlyPrediction();
            error.setSuccess(false);
            error.setErrorMessage("Erreur de connexion: " + e.getMessage());
            return error;
        }
    }

    /* Trouve le meilleur mois de vente
     */
    public static BestMonthResult getBestMonth(int productId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = API_BASE_URL + "/best-month/" + productId;
            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = client.execute(request)) {
                String json = EntityUtils.toString(response.getEntity());
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                BestMonthResult result = new BestMonthResult();

                if (obj.has("error")) {
                    result.setSuccess(false);
                    result.setErrorMessage(obj.get("error").getAsString());
                    return result;
                }

                result.setSuccess(true);
                result.setProductId(obj.get("product_id").getAsInt());
                result.setBestMonth(obj.get("best_month").getAsInt());
                result.setBestMonthName(obj.get("best_month_name").getAsString());
                result.setPredictedSales(obj.get("predicted_sales").getAsDouble());

                return result;
            }
        } catch (IOException e) {
            BestMonthResult error = new BestMonthResult();
            error.setSuccess(false);
            error.setErrorMessage("Erreur de connexion: " + e.getMessage());
            return error;
        }
    }

    // ==================== CLASSES RÉSULTATS ====================

    public static class PredictionResult {
        private boolean success;
        private String errorMessage;
        private int productId;
        private int month;
        private String monthName;
        private double predictedSales;
        private String confidence;
        private String trend;

        // Getters et Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public String getMonthName() { return monthName; }
        public void setMonthName(String monthName) { this.monthName = monthName; }
        public double getPredictedSales() { return predictedSales; }
        public void setPredictedSales(double predictedSales) { this.predictedSales = predictedSales; }
        public String getConfidence() { return confidence; }
        public void setConfidence(String confidence) { this.confidence = confidence; }
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
    }

    public static class MonthlyPrediction {
        private int month;
        private String monthName;
        private double predictedSales;

        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public String getMonthName() { return monthName; }
        public void setMonthName(String monthName) { this.monthName = monthName; }
        public double getPredictedSales() { return predictedSales; }
        public void setPredictedSales(double predictedSales) { this.predictedSales = predictedSales; }
    }

    public static class YearlyPrediction {
        private boolean success;
        private String errorMessage;
        private int productId;
        private double totalAnnual;
        private double averageMonthly;
        private List<MonthlyPrediction> predictions;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }
        public double getTotalAnnual() { return totalAnnual; }
        public void setTotalAnnual(double totalAnnual) { this.totalAnnual = totalAnnual; }
        public double getAverageMonthly() { return averageMonthly; }
        public void setAverageMonthly(double averageMonthly) { this.averageMonthly = averageMonthly; }
        public List<MonthlyPrediction> getPredictions() { return predictions; }
        public void setPredictions(List<MonthlyPrediction> predictions) { this.predictions = predictions; }
    }

    public static class BestMonthResult {
        private boolean success;
        private String errorMessage;
        private int productId;
        private int bestMonth;
        private String bestMonthName;
        private double predictedSales;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }
        public int getBestMonth() { return bestMonth; }
        public void setBestMonth(int bestMonth) { this.bestMonth = bestMonth; }
        public String getBestMonthName() { return bestMonthName; }
        public void setBestMonthName(String bestMonthName) { this.bestMonthName = bestMonthName; }
        public double getPredictedSales() { return predictedSales; }
        public void setPredictedSales(double predictedSales) { this.predictedSales = predictedSales; }
    }
}