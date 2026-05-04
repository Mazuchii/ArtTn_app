package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.OfferJob;
import tn.esprit.museum.services.FavoriteOfferJobService;
import tn.esprit.museum.services.OfferJobService;
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
import javafx.scene.layout.BorderPane;
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

    @FXML private BorderPane mainContainer;
    @FXML private FlowPane flowPane;
    @FXML private TextField tfRecherche;
    @FXML private Label lblFavorisHint;

    private String candidatId;

    private final OfferJobService offerService = new OfferJobService();
    private final FavoriteOfferJobService favoriteOfferJobService = new FavoriteOfferJobService();
    private List<OfferJob> toutesLesOffres;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        toutesLesOffres = offerService.afficher();
        afficherOffres(toutesLesOffres);
        initialiserFavoris();
    }

    public void setCandidatId(String candidatId) {
        this.candidatId = candidatId;
        if (candidatId != null && !candidatId.isBlank()) {
            afficherOffres(toutesLesOffres);
        }
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
        card.setStyle("-fx-padding: 20; -fx-border-color: #CC9933; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-background-color: #FFFFFF; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-pref-width: 280;");

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

    private boolean estFavoriPourCandidatActuel(OfferJob offre) {
        return candidatId != null && favoriteOfferJobService.estFavori(candidatId, offre.getId());
    }

    private void basculerFavori(OfferJob offre, Button btnFavori) {
        if (candidatId == null) {
            if (lblFavorisHint != null) lblFavorisHint.setText("Identifiez-vous pour ajouter des favoris.");
            return;
        }

        boolean estFavoriActuellement = favoriteOfferJobService.estFavori(candidatId, offre.getId());
        if (estFavoriActuellement) {
            favoriteOfferJobService.supprimerFavori(candidatId, offre.getId());
        } else {
            favoriteOfferJobService.ajouterFavori(candidatId, offre.getId());
        }

        btnFavori.setText(estFavoriPourCandidatActuel(offre) ? "Retirer favori" : "Ajouter favori");
        afficherOffres(toutesLesOffres);
    }

    private void ouvrirFormulaireCandidature(OfferJob offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddDemandeForm.fxml"));
            Parent root = loader.load();

            // RÉCUPÉRATION DU CONTRÔLEUR
            DemandeJobController controller = loader.getController();

            // APPEL DES MÉTHODES PUBLIQUES
            controller.initDonneesOffre(offre.getId());

            // Cette ligne cause l'erreur si DemandeJobController n'est pas compilé avec setCandidatId
            controller.setCandidatId(candidatId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Postuler - " + offre.getTitre());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Erreur FXML : " + e.getMessage());
        }
    }

    @FXML
    void openFavorisPage(ActionEvent event) {
        ouvrirPage("/fxml/FavoriteOffers.fxml", "Mes favoris", (loader) -> {
            FavoriteOffersController controller = loader.getController();
            controller.setCandidatId(candidatId);
        }, event);
    }

    @FXML
    void openMesCandidaturesPage(ActionEvent event) {
        ouvrirPage("/fxml/MesCandidatures.fxml", "Mes candidatures", (loader) -> {
            MesCandidaturesController controller = loader.getController();
            controller.setCandidatId(candidatId);
        }, event);
    }

    private void ouvrirPage(String fxmlPath, String title, java.util.function.Consumer<FXMLLoader> controllerSetup, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            controllerSetup.accept(loader);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Erreur navigation : " + e.getMessage());
        }
    }

    private void initialiserFavoris() {
        if (lblFavorisHint != null) lblFavorisHint.setText("");
    }

    @FXML
    void switchToAdmin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/DemandeWindow.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            System.err.println("Erreur admin : " + e.getMessage());
        }
    }
}