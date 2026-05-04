package tn.esprit.museum.controllers;

import tn.esprit.museum.services.DemandeJobService;
import tn.esprit.museum.services.OfferJobService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import tn.esprit.museum.entities.DemandeJob;
import tn.esprit.museum.entities.OfferJob;
import tn.esprit.museum.utils.SessionManager;

public class MesCandidaturesController implements Initializable {

    @FXML private ListView<DemandeJob> listMesCandidatures;
    @FXML private TextArea taAnalyseCandidature;
    @FXML private Label lblInfo;

    @FXML private VBox scoreOverviewCard;
    @FXML private Label lblScoreValue;
    @FXML private Label lblScoreBand;
    @FXML private ProgressBar scoreProgressBar;
    @FXML private ScrollPane analysisScrollPane;
    @FXML private VBox strengthsSection;
    @FXML private VBox strengthsContainer;
    @FXML private VBox improvementsSection;
    @FXML private VBox improvementsContainer;
    @FXML private VBox skillsSection;
    @FXML private FlowPane languagesFlowPane;
    @FXML private FlowPane certificationsFlowPane;
    @FXML private VBox profileSection;
    @FXML private Label lblEducationLevel;
    @FXML private ProgressBar profileCompletenessBar;
    @FXML private Label lblProfileCompleteness;
    @FXML private VBox recommendationSection;
    @FXML private Label lblRecommendation;
    @FXML private VBox fallbackContainer;

    private String candidatId;

    private final DemandeJobService demandeJobService = new DemandeJobService();
    private final OfferJobService offerService = new OfferJobService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (lblInfo != null) lblInfo.setText("Vos candidatures:");
        
        // Tentative de récupération auto si déjà en session
        if (SessionManager.isLoggedIn()) {
            this.candidatId = String.valueOf(SessionManager.getCurrentUserId());
            handleChargerMesCandidatures();
        }

        listMesCandidatures.setCellFactory(param -> new ListCell<DemandeJob>() {
            @Override
            protected void updateItem(DemandeJob item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("Candidature #" + item.getId() + " - " + item.getStatus());
                    setGraphic(null);
                }
            }
        });
    }

    public void setCandidatId(String candidatId) {
        this.candidatId = candidatId;
        if (candidatId != null && !candidatId.isBlank()) {
            handleChargerMesCandidatures();
        }
    }

    @FXML
    public void handleChargerMesCandidatures() {
        if (this.candidatId == null || this.candidatId.isEmpty()) {
            if (SessionManager.isLoggedIn()) {
                this.candidatId = String.valueOf(SessionManager.getCurrentUserId());
            } else {
                return;
            }
        }
        try {
            List<DemandeJob> candidatures = demandeJobService.afficherParCandidatId(this.candidatId);
            listMesCandidatures.getItems().setAll(candidatures);
            if (lblInfo != null) lblInfo.setText(candidatures.size() + " candidatures trouvées.");
        } catch (Exception e) {
            if (lblInfo != null) lblInfo.setText("Erreur base de données.");
        }
    }

    @FXML
    public void handleChargerMesCandidatures(ActionEvent event) {
        handleChargerMesCandidatures();
    }

    @FXML
    public void handleAnalyserMaCandidature() {
        lblInfo.setText("Analyse indisponible.");
    }

    @FXML
    public void handleAnalyserMaCandidature(ActionEvent event) {
        handleAnalyserMaCandidature();
    }

    @FXML
    void handleShowJobFront(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobFront.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            if (lblInfo != null) lblInfo.setText("Erreur navigation.");
        }
    }
}

