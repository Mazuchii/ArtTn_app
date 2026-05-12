package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Pack;
import tn.esprit.museum.services.PackService;
import tn.esprit.museum.utils.WindowManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PackCatalogueController implements Initializable {

    @FXML private FlowPane catalogueContainer;
    @FXML private Label titreCatalogueLabel;
    @FXML private Label sousTitreCatalogueLabel;


    private final PackService service = new PackService();
    private Event evenementSelectionne;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerCatalogue();
    }

    public void setEvenementSelectionne(Event evenementSelectionne) {
        this.evenementSelectionne = evenementSelectionne;
        mettreAJourEntete();
        if (catalogueContainer != null) {
            chargerCatalogue();
        }
    }

    private void mettreAJourEntete() {
        if (titreCatalogueLabel == null || sousTitreCatalogueLabel == null) {
            return;
        }

        if (evenementSelectionne == null) {
            titreCatalogueLabel.setText("Catalogue des Offres");
            sousTitreCatalogueLabel.setText("Decouvrez nos differents niveaux de sponsoring");
            return;
        }

        titreCatalogueLabel.setText("Packs de " + evenementSelectionne.getTitle());
        sousTitreCatalogueLabel.setText("Seuls les packs lies a cet evenement sont affiches.");
    }

    private void chargerCatalogue() {
        mettreAJourEntete();
        catalogueContainer.getChildren().clear();

        List<Pack> packs = evenementSelectionne == null
                ? service.getData()
                : service.getDataByEvenementId(evenementSelectionne.getId());

        if (packs.isEmpty()) {
            Label emptyState = new Label("Aucun pack disponible pour cet evenement.");
            emptyState.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15px; -fx-padding: 30;");
            catalogueContainer.getChildren().add(emptyState);
            return;
        }

        for (Pack p : packs) {
            catalogueContainer.getChildren().add(creerCarteEpuree(p));
        }
    }

    private VBox creerCarteEpuree(Pack p) {
        VBox card = new VBox(15);
        card.getStyleClass().add("stat-card");
        card.setStyle("-fx-background-color: white; -fx-pref-width: 300; -fx-alignment: center; -fx-padding: 25;");
        card.setOnMouseClicked(event -> ouvrirFormulaireSponsor(p, event));
        card.setOnMouseEntered(event -> card.setStyle(
                "-fx-background-color: white; -fx-pref-width: 300; -fx-alignment: center; -fx-padding: 25; " +
                "-fx-cursor: hand; -fx-border-color: #d4af37; -fx-border-width: 2; -fx-border-radius: 12; " +
                "-fx-background-radius: 12;"
        ));
        card.setOnMouseExited(event -> card.setStyle(
                "-fx-background-color: white; -fx-pref-width: 300; -fx-alignment: center; -fx-padding: 25;"
        ));

        Label nom = new Label(p.getNom().toUpperCase());
        nom.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #d4af37;");

        Label prix = new Label(p.getMontant() + " DT");
        prix.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label avantages = new Label(p.getAvantages());
        avantages.setWrapText(true);
        avantages.setStyle("-fx-text-fill: #7f8c8d; -fx-text-alignment: center;");

        Label cta = new Label("Cliquer pour sponsoriser ce pack");
        cta.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        card.getChildren().addAll(nom, prix, avantages, cta);
        return card;
    }

    private void ouvrirFormulaireSponsor(Pack pack, MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterSponsor.fxml"));
            Parent root = loader.load();

            SponsorController controller = loader.getController();
            controller.preRemplirDepuisPack(pack);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(((VBox) event.getSource()).getScene().getWindow());
            stage.setTitle("Ajouter Sponsor");
            WindowManager.applyStandardSize(stage);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retour() {
        try {
            Stage stage = (Stage) catalogueContainer.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
