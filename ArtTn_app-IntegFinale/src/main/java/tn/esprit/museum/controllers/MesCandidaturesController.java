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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.museum.entities.DemandeJob;
import tn.esprit.museum.services.DemandeJobService;
import tn.esprit.museum.services.PythonCandidateMatchingService;
import tn.esprit.museum.utils.SessionManager;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @FXML private VBox weakPointsCard;
    @FXML private VBox weakPointsContainer;

    private final DemandeJobService demandeJobService = new DemandeJobService();
    private final PythonCandidateMatchingService matchingService = new PythonCandidateMatchingService();
    private String candidatId;
    private UserHomeController parentController;

    public void setParentController(UserHomeController parentController) {
        this.parentController = parentController;
    }

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
        boolean accepted = "accepted".equalsIgnoreCase(demande.getStatus());
        boolean declined = "declined".equalsIgnoreCase(demande.getStatus());

        if (accepted) {
            lblStatutTraitement.setText("✅ ACCEPTÉE");
            lblStatutTraitement.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: white; -fx-background-color: #27ae60;" +
                "-fx-background-radius: 8; -fx-padding: 6 14 6 14;"
            );
        } else if (declined) {
            lblStatutTraitement.setText("❌ REFUSÉE");
            lblStatutTraitement.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: white; -fx-background-color: #e74c3c;" +
                "-fx-background-radius: 8; -fx-padding: 6 14 6 14;"
            );
        } else {
            lblStatutTraitement.setText("⏳ EN ATTENTE");
            lblStatutTraitement.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: #1a1a2e; -fx-background-color: #f39c12;" +
                "-fx-background-radius: 8; -fx-padding: 6 14 6 14;"
            );
        }

        linkMeet.setVisible(accepted);
        linkMeet.setManaged(accepted);
        if (accepted) {
            String lienJitsi = construireLienJitsi(demande);
            linkMeet.setText("🔗 Rejoindre l'entretien (Meet)");
            linkMeet.setOnAction(e -> {
                try {
                    Desktop.getDesktop().browse(URI.create(lienJitsi));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }

        // 4. Points faibles si refusé
        if (declined) {
            afficherPointsFaibles(demande);
        } else {
            weakPointsCard.setVisible(false);
            weakPointsCard.setManaged(false);
        }

        // 5. Génération du QR Code vers l'offre sur ArtTN
        String urlOffre = "https://art.tn/offres/" + demande.getOffreId();
        qrCodeView.setImage(new Image("https://quickchart.io/qr?text=" + urlOffre, true));
    }

    /**
     * Appelle le script Python de matching en mode JSON et extrait les improvement_points
     * pour les afficher dans la carte "Points à améliorer".
     */
    private void afficherPointsFaibles(DemandeJob demande) {
        weakPointsContainer.getChildren().clear();

        if (demande.getOffer() == null) {
            weakPointsCard.setVisible(false);
            weakPointsCard.setManaged(false);
            return;
        }

        try {
            String jsonResult = matchingService.analyzeCandidateForOfferJson(demande.getOffer(), demande);
            List<String> points = extraireImprovementPoints(jsonResult);

            if (points.isEmpty()) {
                Label lblAucun = new Label("Aucun point spécifique identifié. Consultez votre score de correspondance.");
                lblAucun.setWrapText(true);
                lblAucun.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
                weakPointsContainer.getChildren().add(lblAucun);
            } else {
                for (String point : points) {
                    HBox row = new HBox(8);
                    row.setStyle("-fx-alignment: CENTER_LEFT;");

                    Label bullet = new Label("⚠");
                    bullet.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 14px; -fx-min-width: 20;");

                    Label lblPoint = new Label(point);
                    lblPoint.setWrapText(true);
                    lblPoint.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px;");
                    HBox.setHgrow(lblPoint, javafx.scene.layout.Priority.ALWAYS);

                    row.getChildren().addAll(bullet, lblPoint);
                    weakPointsContainer.getChildren().add(row);
                }
            }

            weakPointsCard.setVisible(true);
            weakPointsCard.setManaged(true);

        } catch (Exception e) {
            e.printStackTrace();
            weakPointsCard.setVisible(false);
            weakPointsCard.setManaged(false);
        }
    }

    /**
     * Extrait la liste improvement_points depuis le JSON retourné par le script Python.
     * Format attendu : "improvement_points": ["...", "..."]
     */
    private List<String> extraireImprovementPoints(String json) {
        List<String> points = new ArrayList<>();
        // Cherche le tableau improvement_points dans le JSON
        Pattern arrayPattern = Pattern.compile("\"improvement_points\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(json);
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            // Extrait chaque chaîne entre guillemets
            Pattern itemPattern = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher itemMatcher = itemPattern.matcher(arrayContent);
            while (itemMatcher.find()) {
                String point = itemMatcher.group(1)
                        .replace("\\n", " ")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .trim();
                if (!point.isBlank()) {
                    points.add(point);
                }
            }
        }
        return points;
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

    /**
     * Construit le lien Jitsi Meet identique à celui généré côté dashboard (DemandeJobController).
     */
    private String construireLienJitsi(DemandeJob demande) {
        String candidat = demande.getCandidatId() == null ? "candidat" : demande.getCandidatId();
        String salle = ("admin-candidat-" + demande.getOffreId() + "-" + candidat + "-" + demande.getId())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return "https://meet.jit.si/" + salle;
    }

    @FXML
    void handleShowJobFront(ActionEvent event) {
        if (parentController != null) {
            parentController.handleShowJobFront();
        } else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/JobFront.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("ArtTN - Offres d'emploi");
                stage.show();
            } catch (IOException e) { e.printStackTrace(); }
        }
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