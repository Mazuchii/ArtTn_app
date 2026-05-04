package tn.esprit.museum.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.museum.entities.DemandeJob;
import tn.esprit.museum.services.DemandeJobService;
import tn.esprit.museum.utils.SessionManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MesCandidaturesController implements Initializable {

    @FXML private ListView<DemandeJob> listMesCandidatures;
    @FXML private Label lblInfo;
    @FXML private Label lblStatutTraitement;
    @FXML private Label lblScoreValue;
    @FXML private Label lblScoreBand;
    @FXML private ProgressBar scoreProgressBar;
    @FXML private Hyperlink linkMeet;
    @FXML private ImageView qrCodeView;

    @FXML private Label lblProgressValue;
    @FXML private ProgressBar progressStepBar;
    @FXML private VBox stepsContainer;

    private final DemandeJobService demandeJobService = new DemandeJobService();
    private String candidatId;

    /**
     * Permet au JobFrontController de passer l'ID du candidat pour charger ses données.
     */
    public void setCandidatId(String candidatId) {
        this.candidatId = candidatId;
        if (candidatId != null && !candidatId.isEmpty()) {
            handleChargerMesCandidatures();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupListView();

        listMesCandidatures.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                afficherDetailsCandidature(newVal);
            }
        });

        // Sécurité : récupération automatique si la session est déjà active
        if (SessionManager.isLoggedIn()) {
            this.candidatId = String.valueOf(SessionManager.getCurrentUserId());
            handleChargerMesCandidatures();
        }
    }

    private void afficherDetailsCandidature(DemandeJob demande) {
        // 1. Appel du script Python pour le score de correspondance
        double scoreIA = calculerScoreAvecPython(demande);
        lblScoreValue.setText((int)(scoreIA * 100) + "%");
        scoreProgressBar.setProgress(scoreIA);
        lblScoreBand.setText(getLabelPourScore(scoreIA));

        // 2. Mise à jour du rapport d'avancement visuel
        mettreAJourEtatAvancement(demande);

        // 3. Gestion du lien Meet et du Statut
        linkMeet.setVisible(true);
        linkMeet.setManaged(true);
        lblStatutTraitement.setText(demande.getStatus() != null ? demande.getStatus().toUpperCase() : "EN ATTENTE");

        // 4. Génération du QR Code vers l'offre sur ArtTN
        String urlOffre = "https://art.tn/offres/" + demande.getOffreId();
        qrCodeView.setImage(new Image("https://quickchart.io/qr?text=" + urlOffre, true));
    }

    private double calculerScoreAvecPython(DemandeJob demande) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "matching_script.py", "--candidat_id", String.valueOf(demande.getId()));
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = in.readLine();
            return (line != null) ? Double.parseDouble(line) / 100.0 : 0.5;
        } catch (Exception e) {
            return 0.45; // Fallback si le script Python échoue
        }
    }

    private void mettreAJourEtatAvancement(DemandeJob demande) {
        stepsContainer.getChildren().clear();
        double prog = 0.0;

        if (demande.getStatus() == null || "pending".equalsIgnoreCase(demande.getStatus())) {
            prog = 0.4;
            ajouterEtape("✅ Candidature envoyée", true);
            ajouterEtape("⏳ Analyse technique par l'IA", false);
        } else {
            prog = 1.0;
            ajouterEtape("✅ Candidature envoyée", true);
            ajouterEtape("✅ Analyse terminée", true);
            ajouterEtape("✅ Décision rendue", true);
        }

        lblProgressValue.setText((int)(prog * 100) + "%");
        progressStepBar.setProgress(prog);
    }

    private void ajouterEtape(String msg, boolean done) {
        Label l = new Label(msg);
        l.setStyle(done ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;" : "-fx-text-fill: #7f8c8d;");
        stepsContainer.getChildren().add(l);
    }

    private String getLabelPourScore(double s) {
        if (s >= 0.75) return "Excellent profil";
        if (s >= 0.55) return "Bonne correspondance";
        return "Profil à renforcer";
    }

    @FXML
    void handleShowJobFront(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/JobFront.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("ArtTN - Offres d'emploi");
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleChargerMesCandidatures() {
        if (candidatId == null) return;
        try {
            List<DemandeJob> mesDemandes = demandeJobService.afficherParCandidatId(candidatId);
            listMesCandidatures.getItems().setAll(mesDemandes);
            if (mesDemandes.isEmpty()) {
                lblInfo.setText("Aucune candidature trouvée.");
            } else {
                lblInfo.setText(mesDemandes.size() + " candidature(s) trouvée(s).");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblInfo.setText("Erreur lors du chargement des données.");
        }
    }

    private void setupListView() {
        listMesCandidatures.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(DemandeJob item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String titre = (item.getOffer() != null) ? item.getOffer().getTitre() : "Offre #" + item.getOffreId();
                    setText("💼 " + titre);
                }
            }
        });
    }
}