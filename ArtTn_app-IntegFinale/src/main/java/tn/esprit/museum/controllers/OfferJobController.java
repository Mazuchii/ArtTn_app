package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.OfferJob;
import tn.esprit.museum.services.GeminiOfferDescriptionService;
import tn.esprit.museum.services.DemandeJobService;
import tn.esprit.museum.services.OfferJobService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OfferJobController implements Initializable {

    @FXML
    private BorderPane mainContainer;
    @FXML
    private TextField tfTitre, tfSalaire, tfRecherche;
    @FXML
    private TextArea taDescription, taPromptIa;
    @FXML
    private Button btnGenerateIa;
    @FXML
    private TableView<OfferJob> tableOffers;
    @FXML
    private TableColumn<OfferJob, String> colTitre, colDesc;
    @FXML
    private TableColumn<OfferJob, Double> colSalaire;
    @FXML
    private TableColumn<OfferJob, Void> colAction;
    @FXML
    private Label labelTotalOffres;
    @FXML
    private Label labelOffresActives;
    @FXML
    private Label labelRecruteurs;

    private final OfferJobService service = new OfferJobService();
    private final GeminiOfferDescriptionService descriptionGenerator = new GeminiOfferDescriptionService();
    private final DemandeJobService demandeService = new DemandeJobService();
    private OfferJob selectedOffer = null;
    private ObservableList<OfferJob> masterData = FXCollections.observableArrayList();
    private boolean ascendant = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerTableau();
        rafraichirTable();
    }

    private void configurerTableau() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));

        if (tableOffers != null) {
            configurerBoutonDetails(); // Nom unique pour éviter l'ambiguïté
            tableOffers.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectedOffer = newSelection;
                    remplirChamps(newSelection);
                }
            });
        }
    }

    private void remplirChamps(OfferJob o) {
        if (tfTitre != null) tfTitre.setText(o.getTitre());
        if (taDescription != null) taDescription.setText(o.getDescription());
        if (taPromptIa != null) taPromptIa.clear();
        if (tfSalaire != null) tfSalaire.setText(String.valueOf(o.getSalaire()));
    }

    @FXML
    void handleSave() {
        if (!controleSaisie()) return;

        if (selectedOffer == null) {
            service.ajouter(new OfferJob(tfTitre.getText(), taDescription.getText(), Double.parseDouble(tfSalaire.getText())));
            showAlert("Succès", "Nouvelle offre ajoutée !.");
        } else {
            selectedOffer.setTitre(tfTitre.getText());
            selectedOffer.setDescription(taDescription.getText());
            selectedOffer.setSalaire(Double.parseDouble(tfSalaire.getText()));
            service.modifier(selectedOffer);
            showAlert("Succès", "Offre mise à jour !.");
        }

        rafraichirTable();
        Stage stage = (Stage) tfTitre.getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleGenerateDescription() {
        if (taPromptIa == null || taDescription == null) {
            return;
        }

        String titre = tfTitre.getText() == null ? "" : tfTitre.getText().trim();
        String promptUtilisateur = taPromptIa.getText() == null ? "" : taPromptIa.getText().trim();
        if (promptUtilisateur.isEmpty()) {
            showAlert("Prompt requis", "Veuillez ecrire un prompt de generation avant de lancer l'IA.");
            return;
        }

        String descriptionActuelle = taDescription.getText() == null ? "" : taDescription.getText().trim();
        if (!descriptionActuelle.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Remplacer la description");
            alert.setHeaderText(null);
            alert.setContentText("Une description existe deja. Voulez-vous la remplacer par une version generee ?");

            ButtonType remplacer = new ButtonType("Remplacer", ButtonBar.ButtonData.OK_DONE);
            ButtonType annuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(remplacer, annuler);

            if (alert.showAndWait().orElse(annuler) != remplacer) {
                return;
            }
        }

        Task<String> generationTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return descriptionGenerator.generateOffer(titre, promptUtilisateur);
            }
        };

        generationTask.setOnRunning(event -> {
            if (btnGenerateIa != null) {
                btnGenerateIa.setDisable(true);
                btnGenerateIa.setText("Generation...");
            }
        });

        generationTask.setOnSucceeded(event -> {
            taDescription.setText(generationTask.getValue());
            if (btnGenerateIa != null) {
                btnGenerateIa.setDisable(false);
                btnGenerateIa.setText("Generer avec IA");
            }
        });

        generationTask.setOnFailed(event -> {
            if (btnGenerateIa != null) {
                btnGenerateIa.setDisable(false);
                btnGenerateIa.setText("Generer avec IA");
            }

            Throwable error = generationTask.getException();
            String message = error == null ? "Erreur inconnue pendant la generation." : error.getMessage();
            if (message != null && message.contains("503")) {
                message = "Le service Gemini est temporairement surcharge. L'application a deja effectue plusieurs tentatives automatiques. Merci de reessayer dans quelques instants.";
            }
            showAlert("Generation impossible", message);
        });

        Thread generationThread = new Thread(generationTask, "gemini-offer-generation");
        generationThread.setDaemon(true);
        generationThread.start();
    }

    @FXML
    void handleModifier() {
        if (selectedOffer == null) {
            showAlert("Attention", "Veuillez sélectionner une offre dans le tableau.");
            return;
        }
        openEditPopup();
    }

    private void openEditPopup() {
        try {
            URL resource = getClass().getResource("/fxml/AddOfferForm.fxml");
            if (resource == null) return;
            FXMLLoader loader = new FXMLLoader(resource);
            loader.setController(this);
            Parent root = loader.load();
            remplirChamps(selectedOffer);

            Stage stage = new Stage();
            stage.setTitle("Modifier l'offre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Erreur chargement popup : " + e.getMessage());
        }
    }

    @FXML
    void showAddFormPopup(ActionEvent event) {
        selectedOffer = null;
        try {
            URL resource = getClass().getResource("/fxml/AddOfferForm.fxml");
            if (resource == null) return;
            FXMLLoader loader = new FXMLLoader(resource);
            loader.setController(this);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Nouvelle Offre");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            System.err.println("Erreur popup ajout : " + e.getMessage());
        }
    }

    private boolean controleSaisie() {
        if (tfTitre == null || tfTitre.getText().trim().isEmpty() ||
                tfSalaire == null || tfSalaire.getText().trim().isEmpty() ||
                taDescription == null || taDescription.getText().trim().isEmpty()) {
            showAlert("Champs requis", "Veuillez remplir tous les champs.");
            return false;
        }

        String titreSaisi = tfTitre.getText().trim();
        String descSaisie = taDescription.getText().trim();

        boolean doublonDetecte = masterData.stream().anyMatch(offre -> {
            boolean memeContenu = offre.getTitre().equalsIgnoreCase(titreSaisi) &&
                    offre.getDescription().equalsIgnoreCase(descSaisie);
            return (selectedOffer == null) ? memeContenu : (memeContenu && (offre.getId() != selectedOffer.getId()));
        });

        if (doublonDetecte) {
            showAlert("Redondance", "Une autre offre identique existe déjà.");
            return false;
        }

        try {
            if (Double.parseDouble(tfSalaire.getText()) <= 0) {
                showAlert("Erreur", "Le salaire doit être positif.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Format de salaire invalide.");
            return false;
        }
        return true;
    }

    private void rafraichirTable() {
        masterData = FXCollections.observableArrayList(service.afficher());
        tableOffers.setItems(masterData);
        mettreAJourStats();
    }

    private void mettreAJourStats() {
        int total = masterData.size();
        // Toutes les offres sont actives (pas de champ statut dans offer_job)
        int actives = total;
        // Nombre de candidats uniques ayant postulé
        long recruteurs = demandeService.afficher().stream()
                .map(d -> d.getCandidatId())
                .distinct()
                .count();

        if (labelTotalOffres  != null) labelTotalOffres.setText(String.valueOf(total));
        if (labelOffresActives != null) labelOffresActives.setText(String.valueOf(actives));
        if (labelRecruteurs    != null) labelRecruteurs.setText(String.valueOf(recruteurs));
    }

    @FXML
    void handleSupprimer() {
        if (selectedOffer == null) {
            showAlert("Attention", "Veuillez sélectionner une offre à supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Suppression de l'offre : " + selectedOffer.getTitre());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette offre ? Cette action est irréversible.");

        ButtonType boutonOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType boutonNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(boutonOui, boutonNon);

        alert.showAndWait().ifPresent(response -> {
            if (response == boutonOui) {
                service.supprimer(selectedOffer.getId());
                rafraichirTable();
                selectedOffer = null; // Reset de la sélection
            }
        });
    }

    @FXML
    void naviguerVersDemandes(ActionEvent event) {
        try {
            URL resource = getClass().getResource("/fxml/DemandeWindow.fxml");
            if (resource != null) {
                Parent root = FXMLLoader.load(resource);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
            }
        } catch (IOException ex) {
            System.err.println("Erreur navigation : " + ex.getMessage());
        }
    }

    private void showAlert(String t, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(c);
        a.show();
    }

    private void configurerBoutonDetails() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Détails");

            {
                btn.getStyleClass().add("btn-action-yellow");
                btn.setOnAction(event -> {
                    OfferJob offre = getTableView().getItems().get(getIndex());
                    afficherAlerteDetails(offre);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void afficherAlerteDetails(OfferJob offre) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'Offre");
        alert.setHeaderText(null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        Label titre = new Label("Titre : " + offre.getTitre());
        titre.setStyle("-fx-font-weight: bold; -fx-text-fill: #CC9933; -fx-font-size: 16px;");
        Label salaire = new Label("Salaire : " + offre.getSalaire() + " DT");
        salaire.setStyle("-fx-font-weight: bold;");
        Text descText = new Text(offre.getDescription());
        descText.setWrappingWidth(350);

        content.getChildren().addAll(titre, salaire, new Separator(), descText);
        alert.getDialogPane().setContent(content);

        URL cssResource = getClass().getResource("/css/style.css");
        if (cssResource != null) {
            alert.getDialogPane().getStylesheets().add(cssResource.toExternalForm());
        }
        alert.showAndWait();
    }

    @FXML
    void handleRecherche() {
        String keyword = tfRecherche.getText().toLowerCase().trim();
        List<OfferJob> filteredList = masterData.stream()
                .filter(o -> o.getTitre().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        tableOffers.setItems(FXCollections.observableArrayList(filteredList));
    }

    @FXML
    void handleTriSalaire() {
        ObservableList<OfferJob> list = FXCollections.observableArrayList(tableOffers.getItems());
        if (ascendant) list.sort(Comparator.comparingDouble(OfferJob::getSalaire));
        else list.sort(Comparator.comparingDouble(OfferJob::getSalaire).reversed());
        tableOffers.setItems(list);
        ascendant = !ascendant;
    }

    @FXML
    void handleStatistiques() {
        try {
            URL resource = getClass().getResource("/fxml/StatsPopUp.fxml");
            if (resource == null) return;
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            StatsController controller = loader.getController();
            controller.setOfferData(masterData);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Statistiques des Offres");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur stats : " + e.getMessage());
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    void switchToFront(ActionEvent event) {
        try {
            // Navigation vers l'interface Artiste (Front-Office)
            URL resource = getClass().getResource("/fxml/JobFront.fxml");

            if (resource != null) {
                Parent root = FXMLLoader.load(resource);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("Museum Digital - Espace Artiste");
                stage.show();
            } else {
                showAlert("Erreur", "Le fichier OffreFront.fxml est introuvable.");
            }
        } catch (IOException e) {
            System.err.println("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleNavigate(ActionEvent event) {
        System.out.println("Fermeture de l'application...");
        System.exit(0);
    }
}

