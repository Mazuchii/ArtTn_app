package edu.cnx.controllers;

import edu.cnx.entit\u00E9s.OfferJob;
import edu.cnx.services.FavoriteOfferJobService;
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

public class FavoriteOffersController implements Initializable {

    @FXML private TextField tfCandidatId;
    @FXML private Label lblInfo;
    @FXML private FlowPane flowPaneFavoris;

    private final FavoriteOfferJobService favoriteOfferJobService = new FavoriteOfferJobService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblInfo.setText("Entrez votre identifiant candidat pour consulter vos offres favorites.");
    }

    public void setCandidatId(String candidatId) {
        if (candidatId != null && !candidatId.isBlank()) {
            tfCandidatId.setText(candidatId);
            chargerFavoris();
        }
    }

    @FXML
    void handleChargerFavoris() {
        chargerFavoris();
    }

    private void chargerFavoris() {
        String candidatId = getCandidatId();
        if (candidatId == null) {
            lblInfo.setText("Entrez votre identifiant candidat pour consulter vos favoris.");
            flowPaneFavoris.getChildren().clear();
            return;
        }

        List<OfferJob> favoris = favoriteOfferJobService.afficherFavorisParCandidatId(candidatId);
        flowPaneFavoris.getChildren().clear();

        if (favoris.isEmpty()) {
            lblInfo.setText("Aucune offre favorite enregistree pour cet identifiant.");
            return;
        }

        for (OfferJob offre : favoris) {
            flowPaneFavoris.getChildren().add(creerCardOffre(offre));
        }

        lblInfo.setText(favoris.size() + " offre(s) favorite(s) trouvee(s).");
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

        Button btnSupprimer = new Button("Retirer favori");
        btnSupprimer.getStyleClass().add("btn-action-yellow");
        btnSupprimer.setPrefWidth(130);
        btnSupprimer.setOnAction(e -> supprimerFavori(offre));

        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("btn-ajouter-museum");
        btnPostuler.setPrefWidth(130);
        btnPostuler.setOnAction(e -> ouvrirFormulaireCandidature(offre));

        HBox actions = new HBox(10, btnPostuler, btnSupprimer);
        card.getChildren().addAll(titre, salaire, desc, actions);
        return card;
    }

    private void supprimerFavori(OfferJob offre) {
        String candidatId = getCandidatId();
        if (candidatId == null) {
            lblInfo.setText("Entrez votre identifiant candidat pour modifier vos favoris.");
            return;
        }

        boolean succes = favoriteOfferJobService.supprimerFavori(candidatId, offre.getId());
        if (!succes) {
            lblInfo.setText("Impossible de retirer cette offre des favoris.");
            return;
        }

        lblInfo.setText("L'offre a ete retiree de vos favoris.");
        chargerFavoris();
    }

    private String getCandidatId() {
        if (tfCandidatId == null || tfCandidatId.getText() == null) {
            return null;
        }

        String candidatId = tfCandidatId.getText().trim();
        return candidatId.isEmpty() ? null : candidatId;
    }

    private void ouvrirFormulaireCandidature(OfferJob offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddDemandeForm.fxml"));
            Parent root = loader.load();

            DemandeJobController controller = loader.getController();
            controller.initDonneesOffre(offre.getId());

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
