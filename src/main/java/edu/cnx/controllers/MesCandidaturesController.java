package edu.cnx.controllers;

import edu.cnx.entit\u00E9s.DemandeJob;
import edu.cnx.entit\u00E9s.OfferJob;
import edu.cnx.services.DemandeJobService;
import edu.cnx.services.OfferJobService;
import edu.cnx.services.PythonCandidateMatchingService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MesCandidaturesController implements Initializable {

    @FXML private TextField tfSuiviCandidatId;
    @FXML private ListView<DemandeJob> listMesCandidatures;
    @FXML private TextArea taAnalyseCandidature;
    @FXML private Label lblInfo;

    private final DemandeJobService demandeJobService = new DemandeJobService();
    private final OfferJobService offerService = new OfferJobService();
    private final PythonCandidateMatchingService matchingService = new PythonCandidateMatchingService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblInfo.setText("Entrez votre identifiant candidat, chargez vos candidatures, puis analysez celle qui vous interesse.");
        listMesCandidatures.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(DemandeJob item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                OfferJob offre = offerService.recupererParId(item.getOffreId());
                String titreOffre = offre == null ? "Offre " + item.getOffreId() : offre.getTitre();

                Label lblTitre = new Label(titreOffre);
                lblTitre.getStyleClass().add("candidature-card-title");
                lblTitre.setWrapText(true);

                Label lblOffre = new Label("Offre liee #" + item.getOffreId());
                lblOffre.getStyleClass().add("candidature-card-meta");

                Label lblStatut = new Label(normaliserStatut(item.getStatus()));
                lblStatut.getStyleClass().add("candidature-status-badge");
                lblStatut.getStyleClass().add(styleStatut(item.getStatus()));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox header = new HBox(10, lblOffre, spacer, lblStatut);
                header.setAlignment(Pos.CENTER_LEFT);

                String cvText = aUnCv(item) ? "CV disponible" : "CV manquant";
                Label lblCv = new Label(cvText);
                lblCv.getStyleClass().add("candidature-card-meta");

                Label lblMotivation = new Label(item.getLettreMotivation() == null || item.getLettreMotivation().isBlank()
                        ? "Lettre de motivation non renseignee"
                        : "Motivation renseignee");
                lblMotivation.getStyleClass().add("candidature-card-meta");

                HBox footer = new HBox(14, lblCv, lblMotivation);
                footer.setAlignment(Pos.CENTER_LEFT);

                VBox card = new VBox(10, header, lblTitre, footer);
                card.getStyleClass().add("candidature-card");

                setText(null);
                setGraphic(card);
            }
        });
    }

    private boolean aUnCv(DemandeJob candidature) {
        return candidature != null
                && candidature.getCvUrl() != null
                && !candidature.getCvUrl().trim().isEmpty();
    }

    private String normaliserStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return "En attente";
        }

        return switch (statut.toLowerCase()) {
            case "accepted" -> "Acceptee";
            case "declined" -> "Refusee";
            case "pending", "en attente" -> "En attente";
            default -> statut;
        };
    }

    private String styleStatut(String statut) {
        if (statut == null) {
            return "status-pending";
        }

        return switch (statut.toLowerCase()) {
            case "accepted" -> "status-accepted";
            case "declined" -> "status-declined";
            default -> "status-pending";
        };
    }

    public void setCandidatId(String candidatId) {
        if (candidatId != null && !candidatId.isBlank()) {
            tfSuiviCandidatId.setText(candidatId);
        }
    }

    @FXML
    void handleChargerMesCandidatures() {
        String candidatId = tfSuiviCandidatId == null ? "" : tfSuiviCandidatId.getText().trim();
        if (candidatId.isEmpty()) {
            listMesCandidatures.getItems().clear();
            taAnalyseCandidature.setText("Entrez votre identifiant candidat pour consulter vos candidatures.");
            return;
        }

        List<DemandeJob> candidatures = demandeJobService.afficherParCandidatId(candidatId);
        listMesCandidatures.getItems().setAll(candidatures);

        if (candidatures.isEmpty()) {
            taAnalyseCandidature.setText("Aucune candidature trouvee pour cet identifiant.");
        } else {
            taAnalyseCandidature.setText("Selectionnez une candidature puis cliquez sur Analyser ma candidature pour voir le score complet et les points a ameliorer.");
        }
    }

    @FXML
    void handleAnalyserMaCandidature() {
        DemandeJob candidature = listMesCandidatures.getSelectionModel().getSelectedItem();
        if (candidature == null) {
            taAnalyseCandidature.setText("Selectionnez une candidature dans la liste.");
            return;
        }

        if (candidature.getCvUrl() == null || candidature.getCvUrl().trim().isEmpty()) {
            taAnalyseCandidature.setText("Analyse impossible : cette candidature ne possede pas de CV.");
            return;
        }

        OfferJob offre = offerService.recupererParId(candidature.getOffreId());
        if (offre == null) {
            taAnalyseCandidature.setText("Impossible de retrouver l'offre liee a cette candidature.");
            return;
        }

        try {
            String report = matchingService.analyzeCandidateForOffer(offre, candidature);
            taAnalyseCandidature.setText(report);
        } catch (IOException | InterruptedException e) {
            taAnalyseCandidature.setText("Analyse indisponible. Verifiez que Python est installe et que le script est accessible.");
        }
    }

    @FXML
    void handleRetourOffres(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JobFront.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Offres d'emploi");
        } catch (IOException e) {
            lblInfo.setText("Impossible de retourner a la page des offres.");
        }
    }
}
