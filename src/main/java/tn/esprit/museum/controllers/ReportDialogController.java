package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.Report;
import tn.esprit.museum.services.ReportServices;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ReportDialogController {

    @FXML private CheckBox reasonSpam;
    @FXML private CheckBox reasonToxic;
    @FXML private CheckBox reasonViolence;
    @FXML private CheckBox reasonIllegal;
    @FXML private CheckBox reasonOffensive;
    @FXML private CheckBox reasonOther;
    @FXML private TextArea detailsArea;
    @FXML private Label errorLabel;

    private int postId;
    private int userId;
    private Runnable onReportSuccess;

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setOnReportSuccess(Runnable onReportSuccess) {
        this.onReportSuccess = onReportSuccess;
    }

    @FXML
    private void initialize() {
        reasonOther.selectedProperty().addListener((obs, old, selected) -> {
            detailsArea.setVisible(selected);
            detailsArea.setManaged(selected);
        });
    }

    @FXML
    private void handleReport() {
        // Récupérer la raison cochée
        String reason = getSelectedReason();
        if (reason == null) {
            showError("Veuillez sélectionner une raison");
            return;
        }

        String details = reasonOther.isSelected() ? detailsArea.getText() : "";

        ReportServices reportService = new ReportServices();
        Report report = new Report(postId, userId, reason, details);

        if (reportService.addReport(report)) {
            // Succès
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Signalement envoyé");
            alert.setHeaderText(null);
            alert.setContentText("Merci pour votre signalement. Notre équipe va vérifier ce contenu.");
            alert.showAndWait();

            if (onReportSuccess != null) {
                onReportSuccess.run();
            }

            closeWindow();
        } else {
            showError("Vous avez déjà signalé ce post ou une erreur est survenue.");
        }
    }

    private String getSelectedReason() {
        if (reasonSpam.isSelected()) return "Spam / Publicité";
        if (reasonToxic.isSelected()) return "Contenu toxique / Harcèlement";
        if (reasonViolence.isSelected()) return "Incitation à la violence";
        if (reasonIllegal.isSelected()) return "Contenu illégal";
        if (reasonOffensive.isSelected()) return "Propos offensants";
        if (reasonOther.isSelected()) return "Autre";
        return null;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) errorLabel.getScene().getWindow();
        stage.close();
    }
}