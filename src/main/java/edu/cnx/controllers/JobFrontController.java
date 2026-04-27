package edu.cnx.controllers;

import edu.cnx.entit\u00E9s.OfferJob;
import edu.cnx.services.FavoriteOfferJobService;
import edu.cnx.services.OfferJobService;
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

public class JobFrontController implements Initializable {

    @FXML private FlowPane flowPane;
    @FXML private TextField tfRecherche;
    @FXML private TextField tfFavoriCandidatId;
    @FXML private Label lblFavorisHint;

    private final OfferJobService offerService = new OfferJobService();
    private final FavoriteOfferJobService favoriteOfferJobService = new FavoriteOfferJobService();
    private List<OfferJob> toutesLesOffres;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        toutesLesOffres = offerService.afficher();
        afficherOffres(toutesLesOffres);
        initialiserFavoris();
    }

    @FXML
    void handleRecherche() {
        String query = (tfRecherche.getText() == null) ? "" : tfRecherche.getText().toLowerCase().trim();

        List<OfferJob> filtre = toutesLesOffres.stream()
                .filter(o -> o.getTitre().toLowerCase().contains(query)
                        || o.getDescription().toLowerCase().contains(query))
                .collect(Collectors.toList());

        afficherOffres(filtre);
    }

    private void afficherOffres(List<OfferJob> liste) {
        flowPane.getChildren().clear();
        for (OfferJob offre : liste) {
            flowPane.getChildren().add(creerCardOffre(offre));
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

        Button btnFavori = new Button(estFavoriPourCandidatActuel(offre) ? "Retirer favori" : "Ajouter favori");
        btnFavori.getStyleClass().add("btn-action-yellow");
        btnFavori.setPrefWidth(130);
        btnFavori.setOnAction(e -> basculerFavori(offre, btnFavori));

        HBox actions = new HBox(10, btnPostuler, btnFavori);
        card.getChildren().addAll(titre, salaire, desc, actions);
        return card;
    }

    private String getFavoriCandidatId() {
        if (tfFavoriCandidatId == null || tfFavoriCandidatId.getText() == null) {
            return null;
        }

        String candidatId = tfFavoriCandidatId.getText().trim();
        return candidatId.isEmpty() ? null : candidatId;
    }

    private boolean estFavoriPourCandidatActuel(OfferJob offre) {
        String candidatId = getFavoriCandidatId();
        return candidatId != null && favoriteOfferJobService.estFavori(candidatId, offre.getId());
    }

    private void basculerFavori(OfferJob offre, Button btnFavori) {
        String candidatId = getFavoriCandidatId();
        if (candidatId == null) {
            lblFavorisHint.setText("Saisissez votre identifiant candidat pour ajouter une offre en favori.");
            return;
        }

        boolean estFavori = favoriteOfferJobService.estFavori(candidatId, offre.getId());
        boolean succes = estFavori
                ? favoriteOfferJobService.supprimerFavori(candidatId, offre.getId())
                : favoriteOfferJobService.ajouterFavori(candidatId, offre.getId());

        if (!succes) {
            lblFavorisHint.setText("Impossible de mettre a jour les favoris pour le moment.");
            return;
        }

        btnFavori.setText(estFavori ? "Ajouter favori" : "Retirer favori");
        afficherOffres(toutesLesOffres);
        lblFavorisHint.setText(estFavori
                ? "L'offre a ete retiree de vos favoris."
                : "L'offre a ete ajoutee a vos favoris.");
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
            e.printStackTrace();
        }
    }

    @FXML
    void openFavorisPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FavoriteOffers.fxml"));
            Parent root = loader.load();

            FavoriteOffersController controller = loader.getController();
            controller.setCandidatId(getFavoriCandidatId());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes favoris");
        } catch (IOException e) {
            lblFavorisHint.setText("Impossible d'ouvrir la page des favoris.");
        }
    }

    @FXML
    void openMesCandidaturesPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesCandidatures.fxml"));
            Parent root = loader.load();

            MesCandidaturesController controller = loader.getController();
            controller.setCandidatId(getFavoriCandidatId());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes candidatures");
        } catch (IOException e) {
            lblFavorisHint.setText("Impossible d'ouvrir la page Mes candidatures.");
        }
    }

    private void initialiserFavoris() {
        if (lblFavorisHint != null) {
            lblFavorisHint.setText("Saisissez votre identifiant candidat pour ouvrir vos favoris ou vos candidatures.");
        }
    }

    @FXML
    void switchToBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/DemandeWindow.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Administration - Candidatures");
        } catch (IOException e) {
            System.err.println("Erreur de chargement du dashboard admin : " + e.getMessage());
        }
    }

    @FXML
    void switchToAdmin(ActionEvent event) {
        switchToBack(event);
    }
}
