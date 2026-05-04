package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.Pack;
import tn.esprit.museum.entities.Sponsor;
import tn.esprit.museum.services.SponsorService;
import tn.esprit.museum.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

public class SponsorController implements Initializable {

    @FXML private TextField nomTF, emailTF, telephoneTF, siteWebTF, adresseTF, logoUrlTF, typeTF;
    @FXML private TextArea descriptionTA;
    @FXML private Button btnValider;
    @FXML private Label titreFormulaire;

    private final SponsorService service = new SponsorService();
    private Sponsor sponsorEnCours = null;
    private Pack selectedPack = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) { }

    public void preRemplirChamps(Sponsor s) {
        this.sponsorEnCours = s;
        nomTF.setText(s.getNom());
        emailTF.setText(s.getEmail());
        telephoneTF.setText(s.getTelephone());
        siteWebTF.setText(s.getSiteWeb());
        adresseTF.setText(s.getAdresse());
        logoUrlTF.setText(s.getLogoUrl());
        typeTF.setText(s.getType());
        descriptionTA.setText(s.getDescription());
        if (titreFormulaire != null) titreFormulaire.setText("Modifier le sponsor");
        if (btnValider != null) btnValider.setText("Mettre a jour");
    }

    public void preRemplirDepuisPack(Pack pack) {
        this.selectedPack = pack;
        if (pack == null) {
            return;
        }

        if (titreFormulaire != null) {
            titreFormulaire.setText("Nouveau sponsor - " + pack.getNom());
        }

        typeTF.setText(pack.getNom());
        if (descriptionTA.getText() == null || descriptionTA.getText().isBlank()) {
            descriptionTA.setText("Pack selectionne : " + pack.getNom() + "\nMontant : " + pack.getMontant() + " DT");
        }
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        StringBuilder erreurs = new StringBuilder();
        String nom = nomTF.getText().trim();

        if (nom.isEmpty()) erreurs.append("- Nom obligatoire\n");
        if (!telephoneTF.getText().matches("\\d{8}")) erreurs.append("- Telephone : 8 chiffres\n");
        if (!emailTF.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) erreurs.append("- Email invalide\n");

        if (sponsorEnCours == null && service.existeDeja(nom)) {
            erreurs.append("- Sponsor existant : Ce partenaire est deja enregistre.\n");
        }

        if (erreurs.length() > 0) {
            new Alert(Alert.AlertType.ERROR, erreurs.toString()).showAndWait();
            return;
        }

        Sponsor s = (sponsorEnCours == null) ? new Sponsor() : sponsorEnCours;
        s.setNom(nom);
        s.setEmail(emailTF.getText());
        s.setTelephone(telephoneTF.getText());
        s.setSiteWeb(siteWebTF.getText());
        s.setAdresse(adresseTF.getText());
        s.setLogoUrl(logoUrlTF.getText());
        s.setType(typeTF.getText());
        s.setDescription(descriptionTA.getText());
        if (selectedPack != null) {
            s.setPackId(selectedPack.getId());
        }
        int currentUserId = SessionManager.getCurrentUserId();
        if (currentUserId > 0) {
            s.setUserId(currentUserId);
        }

        if (sponsorEnCours == null) {
            s.setDateCreation(new Date());
            s.setStatus("en attente");
            service.addEntity(s);
            navigateToSponsorNavigation(event);
        } else {
            service.updateEntity(s.getId(), s);
            navigateToNavPage(event);
        }
    }

    @FXML
    void navigateToNavPage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void navigateToSponsorNavigation(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}

