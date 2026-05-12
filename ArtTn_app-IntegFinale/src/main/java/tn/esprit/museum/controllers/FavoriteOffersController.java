package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.OfferJob;
import tn.esprit.museum.services.FavoriteOfferJobService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FavoriteOffersController implements Initializable {

    @FXML private TextField tfRecherche;
    @FXML private Label lblInfo;
    @FXML private FlowPane flowPaneFavoris;

    private final FavoriteOfferJobService favoriteOfferJobService = new FavoriteOfferJobService();

    private String candidatId;
    private List<OfferJob> tousLesFavoris;
    private UserHomeController parentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblInfo.setText("Chargement...");
    }

    public void setParentController(UserHomeController parentController) {
        this.parentController = parentController;
    }

    /** Appelé par la page précédente pour injecter l'ID du candidat connecté. */
    public void setCandidatId(String candidatId) {
        this.candidatId = candidatId;
        chargerFavoris();
    }

    @FXML
    void handleRecherche() {
        if (tousLesFavoris == null) return;
        String query = tfRecherche.getText() == null ? "" : tfRecherche.getText().toLowerCase().trim();
        List<OfferJob> filtre = tousLesFavoris.stream()
                .filter(o -> o.getTitre().toLowerCase().contains(query)
                        || o.getDescription().toLowerCase().contains(query))
                .collect(Collectors.toList());
        afficherOffres(filtre);
    }

    private void chargerFavoris() {
        if (candidatId == null || candidatId.isBlank()) {
            lblInfo.setText("Identifiant candidat introuvable.");
            flowPaneFavoris.getChildren().clear();
            return;
        }

        tousLesFavoris = favoriteOfferJobService.afficherFavorisParCandidatId(candidatId);
        flowPaneFavoris.getChildren().clear();

        if (tousLesFavoris.isEmpty()) {
            lblInfo.setText("Aucune offre favorite enregistrée.");
            return;
        }

        afficherOffres(tousLesFavoris);
        lblInfo.setText(tousLesFavoris.size() + " favori(s)");
    }

    private void afficherOffres(List<OfferJob> liste) {
        flowPaneFavoris.getChildren().clear();
        for (OfferJob offre : liste) {
            flowPaneFavoris.getChildren().add(creerCardOffre(offre));
        }
    }

    private VBox creerCardOffre(OfferJob offre) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-padding: 20;" +
                "-fx-border-color: #CC9933;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);" +
                "-fx-pref-width: 280;"
        );

        Label titre = new Label(offre.getTitre());
        titre.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #343a40;");

        Label salaire = new Label("Salaire: " + offre.getSalaire() + " DT");
        salaire.setStyle("-fx-text-fill: #CC9933; -fx-font-weight: bold;");

        Label desc = new Label(offre.getDescription());
        desc.setWrapText(true);
        desc.setMinHeight(50);
        desc.setMaxHeight(50);
        desc.setStyle("-fx-text-fill: #666;");

        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("btn-ajouter-museum");
        btnPostuler.setPrefWidth(130);
        btnPostuler.setOnAction(e -> ouvrirFormulaireCandidature(offre));

        Button btnFavori = new Button("Retirer favori");
        btnFavori.getStyleClass().add("btn-action-yellow");
        btnFavori.setPrefWidth(130);
        btnFavori.setOnAction(e -> basculerFavori(offre, btnFavori));

        HBox actions = new HBox(10, btnPostuler, btnFavori);
        card.getChildren().addAll(titre, salaire, desc, actions);
        return card;
    }

    private void basculerFavori(OfferJob offre, Button btnFavori) {
        if (candidatId == null) return;
        boolean estFavori = favoriteOfferJobService.estFavori(candidatId, offre.getId());
        if (estFavori) {
            favoriteOfferJobService.supprimerFavori(candidatId, offre.getId());
            lblInfo.setText("Offre retirée des favoris.");
            chargerFavoris();
        } else {
            favoriteOfferJobService.ajouterFavori(candidatId, offre.getId());
            btnFavori.setText("Retirer favori");
            lblInfo.setText(tousLesFavoris.size() + " favori(s)");
        }
    }

    private void ouvrirFormulaireCandidature(OfferJob offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddDemandeForm.fxml"));
            Parent root = loader.load();

            DemandeJobController controller = loader.getController();
            controller.initDonneesOffre(offre.getId());
            if (candidatId != null) controller.setCandidatId(candidatId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Postuler - " + offre.getTitre());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            lblInfo.setText("Impossible d'ouvrir le formulaire de candidature.");
        }
    }

    @FXML
    void handleRetourOffres(ActionEvent event) {
        if (parentController != null) {
            parentController.handleShowJobFront();
        } else {
            // fallback : ouvrir dans une nouvelle fenêtre
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/JobFront.fxml"));
                Parent root = loader.load();
                JobFrontController controller = loader.getController();
                if (candidatId != null) controller.setCandidatId(candidatId);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Offres d'emploi");
            } catch (IOException e) {
                lblInfo.setText("Impossible de retourner à la page des offres.");
            }
        }
    }

    @FXML
    void handleOpenMesCandidatures(ActionEvent event) {
        if (parentController != null) {
            parentController.handleShowMesCandidaturesPublic();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesCandidatures.fxml"));
                Parent root = loader.load();
                MesCandidaturesController controller = loader.getController();
                if (candidatId != null) controller.setCandidatId(candidatId);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Mes candidatures");
            } catch (IOException e) {
                lblInfo.setText("Impossible d'ouvrir la page des candidatures.");
            }
        }
    }
}
