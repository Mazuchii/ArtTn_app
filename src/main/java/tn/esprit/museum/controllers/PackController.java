package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Pack;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.PackAiGeneratorService;
import tn.esprit.museum.services.PackService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class PackController implements Initializable {

    @FXML private TextField nomTF;
    @FXML private TextField montantTF;
    @FXML private TextArea avantagesTA;
    @FXML private ComboBox<Event> evenementCB;
    @FXML private Button btnGenererIA;
    @FXML private Button btnValider;
    @FXML private Label aiHintLabel;

    private final PackService service = new PackService();
    private final EventService eventService = new EventService();
    private final PackAiGeneratorService packAiGeneratorService = new PackAiGeneratorService();
    private Pack packEnCours = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurerComboEvenements();
        chargerEvenements();
        configurerAssistantIA();
    }

    private void configurerComboEvenements() {
        evenementCB.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });

        evenementCB.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
    }

    private void chargerEvenements() {
        evenementCB.getItems().setAll(eventService.getData());
    }

    private void configurerAssistantIA() {
        if (btnGenererIA != null) {
            btnGenererIA.disableProperty().bind(evenementCB.getSelectionModel().selectedItemProperty().isNull());
        }

        evenementCB.valueProperty().addListener((observable, ancien, selection) -> {
            if (aiHintLabel == null) {
                return;
            }

            if (selection == null) {
                aiHintLabel.setText("Choisissez d'abord un evenement pour activer l'assistant IA.");
                return;
            }

            aiHintLabel.setText("Assistant pret pour \"" + selection.getTitle() + "\".");
        });

        if (aiHintLabel != null) {
            aiHintLabel.setText("Choisissez d'abord un evenement pour activer l'assistant IA.");
        }
    }

    public void preRemplirChamps(Pack p) {
        this.packEnCours = p;
        nomTF.setText(p.getNom());
        montantTF.setText(String.valueOf(p.getMontant()));
        avantagesTA.setText(p.getAvantages());

        if (p.getEvenementId() > 0) {
            Event evenementSelectionne = evenementCB.getItems().stream()
                    .filter(event -> event.getId() == p.getEvenementId())
                    .findFirst()
                    .orElseGet(() -> eventService.findById(p.getEvenementId()));

            if (evenementSelectionne != null) {
                if (!evenementCB.getItems().contains(evenementSelectionne)) {
                    evenementCB.getItems().add(evenementSelectionne);
                }
                evenementCB.getSelectionModel().select(evenementSelectionne);
            }
        }

        if (btnValider != null) {
            btnValider.setText("Mettre a jour");
        }
    }

    @FXML
    void handleGenererIA(ActionEvent event) {
        Event evenementSelectionne = evenementCB.getSelectionModel().getSelectedItem();
        if (evenementSelectionne == null) {
            showAlert("Assistant IA", "Veuillez choisir un evenement avant de generer un pack.");
            return;
        }

        if (champsContiennentDesValeurs()) {
            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Les valeurs actuelles seront remplacees par une suggestion IA. Continuer ?",
                    ButtonType.YES,
                    ButtonType.NO
            );
            confirmation.setTitle("Confirmation");
            confirmation.setHeaderText(null);
            if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
        }

        try {
            PackAiGeneratorService.PackSuggestion suggestion = packAiGeneratorService.generateSuggestion(evenementSelectionne);
            nomTF.setText(suggestion.getNom());
            montantTF.setText(String.format(java.util.Locale.US, "%.2f", suggestion.getMontant()));
            avantagesTA.setText(suggestion.getAvantages());

            if (aiHintLabel != null) {
                aiHintLabel.setText("Suggestion IA appliquee pour \"" + evenementSelectionne.getTitle() + "\".");
            }
        } catch (Exception e) {
            showAlert("Assistant IA", "Generation impossible : " + e.getMessage());
        }
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        StringBuilder erreurs = new StringBuilder();

        if (nomTF.getText().trim().isEmpty()) {
            erreurs.append("- Le nom du pack est obligatoire.\n");
        }

        double montant = 0;
        try {
            String montantString = montantTF.getText().replace(",", ".");
            montant = Double.parseDouble(montantString);
            if (montant <= 0) {
                erreurs.append("- Le montant doit etre superieur a 0.\n");
            }
        } catch (NumberFormatException e) {
            erreurs.append("- Le montant doit etre un nombre valide (ex: 1500.50).\n");
        }

        if (avantagesTA.getText().trim().isEmpty()) {
            erreurs.append("- Veuillez lister au moins un avantage.\n");
        }

        Event evenementSelectionne = evenementCB.getSelectionModel().getSelectedItem();
        if (evenementSelectionne == null) {
            erreurs.append("- Veuillez choisir l'evenement auquel appartient ce pack.\n");
        }

        if (erreurs.length() > 0) {
            showAlert("Erreur de validation", erreurs.toString());
            return;
        }

        try {
            Pack p = (packEnCours == null) ? new Pack() : packEnCours;
            p.setNom(nomTF.getText().trim());
            p.setMontant(montant);
            p.setAvantages(avantagesTA.getText().trim());
            p.setEvenementId(evenementSelectionne.getId());

            if (packEnCours == null) {
                service.addEntity(p);
            } else {
                service.updateEntity(p.getId(), p);
            }

            retourListe(event);
        } catch (Exception e) {
            showAlert("Erreur BDD", "Une erreur est survenue lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    void retourListe(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(String titre, String contenu) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(contenu);
        alert.showAndWait();
    }

    private boolean champsContiennentDesValeurs() {
        return !nomTF.getText().trim().isEmpty()
                || !montantTF.getText().trim().isEmpty()
                || !avantagesTA.getText().trim().isEmpty();
    }
}

