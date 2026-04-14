package edu.cnx.controllers;

import edu.cnx.entités.OfferJob;
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

    private final OfferJobService offerService = new OfferJobService();
    private List<OfferJob> toutesLesOffres;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        toutesLesOffres = offerService.afficher();
        afficherOffres(toutesLesOffres);
    }

    @FXML
    void handleRecherche() {
        String query = (tfRecherche.getText() == null) ? "" : tfRecherche.getText().toLowerCase().trim();

        List<OfferJob> filtre = toutesLesOffres.stream()
                .filter(o -> o.getTitre().toLowerCase().contains(query) ||
                        o.getDescription().toLowerCase().contains(query))
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

        // Utilisation de ton style visuel "Museum"
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

        Label salaire = new Label("💰 " + offre.getSalaire() + " DT");
        salaire.setStyle("-fx-text-fill: #CC9933; -fx-font-weight: bold;");

        Label desc = new Label(offre.getDescription());
        desc.setWrapText(true);
        desc.setMinHeight(50);
        desc.setMaxHeight(50);
        desc.setStyle("-fx-text-fill: #666;");

        Button btnPostuler = new Button("Postuler");
        btnPostuler.getStyleClass().add("btn-ajouter-museum"); // Récupère le style doré du CSS
        btnPostuler.setMaxWidth(Double.MAX_VALUE);
        btnPostuler.setOnAction(e -> ouvrirFormulaireCandidature(offre));

        card.getChildren().addAll(titre, salaire, desc, btnPostuler);
        return card;
    }

    private void ouvrirFormulaireCandidature(OfferJob offre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddDemandeForm.fxml"));
            Parent root = loader.load();

            DemandeJobController controller = loader.getController();

            try {
                controller.initDonneesOffre(offre.getId());
            } catch (Exception e) {
                System.out.println("Note: initDonneesOffre n'a pas pu être exécuté.");
            }

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
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/DemandeWindow.fxml"));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Art.tn - Administration");
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors du passage au dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }
}