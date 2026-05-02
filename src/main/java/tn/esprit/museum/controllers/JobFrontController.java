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
            afficherOffres(toutesLesOffres); // Rafraîchir pour voir les favoris
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
        return candidatId;
    }

    private boolean estFavoriPourCandidatActuel(OfferJob offre) {
        String candidatId = getFavoriCandidatId();
        return candidatId != null && favoriteOfferJobService.estFavori(candidatId, offre.getId());
    }

    private void basculerFavori(OfferJob offre, Button btnFavori) {
        String candidatId = getFavoriCandidatId();
        if (candidatId == null) {
            if (lblFavorisHint != null) lblFavorisHint.setText("Saisissez votre identifiant candidat pour ajouter une offre en favori.");
            return;
        }

        boolean estFavoriActuellement = favoriteOfferJobService.estFavori(candidatId, offre.getId());
        boolean succes;

        if (estFavoriActuellement) {
            succes = favoriteOfferJobService.supprimerFavori(candidatId, offre.getId());
        } else {
            succes = favoriteOfferJobService.ajouterFavori(candidatId, offre.getId());
        }

        if (!succes) {
            if (lblFavorisHint != null) lblFavorisHint.setText("Impossible de mettre à jour les favoris pour le moment.");
            return;
        }

        // Vérifier le nouvel état après la modification
        boolean maintenantFavori = favoriteOfferJobService.estFavori(candidatId, offre.getId());

        // Mettre à jour le bouton immédiatement
        btnFavori.setText(maintenantFavori ? "Retirer favori" : "Ajouter favori");

        // Recharger l'affichage complet
        afficherOffres(toutesLesOffres);

        if (lblFavorisHint != null) {
            lblFavorisHint.setText(maintenantFavori
                ? "L'offre a été ajoutée à vos favoris."
                : "L'offre a été retirée de vos favoris.");
        }
    }

    private void ouvrirFormulaireCandidature(OfferJob offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddDemandeForm.fxml"));
            Parent root = loader.load();

            DemandeJobController controller = loader.getController();
            controller.initDonneesOffre(offre.getId());
            controller.setCandidatId(candidatId); // ✅ Passe l'ID automatiquement

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Postuler - " + offre.getTitre());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture du formulaire de candidature : " + e.getMessage());
        }
    }

    @FXML
    void openFavorisPage(ActionEvent event) {
        ouvrirPage("/fxml/FavoriteOffers.fxml", "Mes favoris", (loader) -> {
            FavoriteOffersController controller = loader.getController();
            controller.setCandidatId(getFavoriCandidatId());
        }, event);
    }

    @FXML
    void openMesCandidaturesPage(ActionEvent event) {
        ouvrirPage("/fxml/MesCandidatures.fxml", "Mes candidatures", (loader) -> {
            MesCandidaturesController controller = loader.getController();
            controller.setCandidatId(getFavoriCandidatId());
        }, event);
    }

    private void ouvrirPage(String fxmlPath, String title, java.util.function.Consumer<FXMLLoader> controllerSetup, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            URL resource = loader.getLocation();

            if (resource == null) {
                if (lblFavorisHint != null) lblFavorisHint.setText("Impossible d'ouvrir la page : ressource non trouvée.");
                return;
            }

            // Charger le FXML et initialiser le contrôleur
            Parent root = loader.load();

            // Configurer le contrôleur APRÈS son initialisation
            controllerSetup.accept(loader);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            if (lblFavorisHint != null) lblFavorisHint.setText("Impossible d'ouvrir la page " + title + ".");
            System.err.println("Erreur lors du chargement de " + fxmlPath + " : " + e.getMessage());
        }
    }

    private void initialiserFavoris() {
        if (lblFavorisHint != null) {
            lblFavorisHint.setText("");
        }
    }

    @FXML
    void switchToBack(ActionEvent event) {
        try {
            URL resource = getClass().getResource("/fxml/DemandeWindow.fxml");
            if (resource == null) {
                System.err.println("Ressource non trouvée : /fxml/DemandeWindow.fxml");
                return;
            }
            Parent root = FXMLLoader.load(resource);
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
