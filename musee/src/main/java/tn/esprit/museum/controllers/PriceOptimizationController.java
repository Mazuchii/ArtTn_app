package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.PriceOptimizationService;
import tn.esprit.museum.services.ReservationService;

import java.util.List;

public class PriceOptimizationController {

    @FXML private Label statusLabel;
    @FXML private TextArea resultArea;
    @FXML private Button analyzeBtn;
    @FXML private Button closeBtn;
    @FXML private VBox loadingBox;

    private final EventService eventService         = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final PriceOptimizationService aiService    = new PriceOptimizationService();

    @FXML
    public void initialize() {
        loadingBox.setVisible(false);
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setStyle(
                "-fx-font-size: 13px; -fx-font-family: 'Segoe UI';");
        statusLabel.setText(
                "Cliquez sur 'Analyser' pour obtenir des recommandations IA.");
    }

    @FXML
    private void handleAnalyze() {
        analyzeBtn.setDisable(true);
        loadingBox.setVisible(true);
        statusLabel.setText("🤖 Gemini analyse vos données...");
        resultArea.clear();

        new Thread(() -> {
            try {
                List<Event> events           = eventService.getAll();
                List<Reservation> reservations = reservationService.getAll();

                if (events.isEmpty()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Aucun événement à analyser.");
                        analyzeBtn.setDisable(false);
                        loadingBox.setVisible(false);
                    });
                    return;
                }

                String result = aiService.analyzeAndOptimizePrices(
                        events, reservations);

                Platform.runLater(() -> {
                    resultArea.setText(result);
                    statusLabel.setText("✅ Analyse Gemini terminée — "
                            + events.size() + " événements analysés.");
                    analyzeBtn.setDisable(false);
                    loadingBox.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur: " + e.getMessage());
                    resultArea.setText(
                            "Erreur lors de l'analyse:\n" + e.getMessage()
                                    + "\n\nVérifiez votre clé API Gemini sur :\n"
                                    + "https://aistudio.google.com/app/apikey");
                    analyzeBtn.setDisable(false);
                    loadingBox.setVisible(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }
}