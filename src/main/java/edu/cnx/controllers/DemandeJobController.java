package edu.cnx.controllers;

import edu.cnx.entités.DemandeJob;
import edu.cnx.entités.OfferJob;
import edu.cnx.services.DemandeJobService;
import edu.cnx.services.OfferJobService;
import edu.cnx.services.PythonCandidateMatchingService;
import edu.cnx.tools.ExcelExporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ScrollPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DemandeJobController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(DemandeJobController.class.getName());

    @FXML private TextField tfCandidatId, tfCvUrl, tfOffreId, tfRecherche;
    @FXML private TextArea taMotivation;
    @FXML private ComboBox<String> comboOffres;
    @FXML private TableView<DemandeJob> tableDemandes;
    @FXML private TableColumn<DemandeJob, String> colCandidat, colStatus;
    @FXML private TableColumn<DemandeJob, Integer> colOffre;
    @FXML private TableColumn<DemandeJob, Void> colAction;

    private final DemandeJobService service = new DemandeJobService();
    private final OfferJobService offerService = new OfferJobService();
    private final PythonCandidateMatchingService matchingService = new PythonCandidateMatchingService();

    private static DemandeJob selectedDemande = null;
    private ObservableList<DemandeJob> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerTableau();
        chargerDonneesInitiales();

        if (selectedDemande != null && tfCandidatId != null) {
            preRemplirFormulaire(selectedDemande);
        }
    }

    public void initDonneesOffre(int idOffre) {
        if (tfOffreId != null) {
            tfOffreId.setText(String.valueOf(idOffre));
            tfOffreId.setEditable(false);
        }
    }

    private void configurerTableau() {
        if (tableDemandes == null) return;

        colCandidat.setCellValueFactory(new PropertyValueFactory<>("candidatId"));
        colOffre.setCellValueFactory(new PropertyValueFactory<>("offreId"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        configurerRenduColonnes();

        if (colAction != null) ajouterBoutonsAction();

        tableDemandes.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedDemande = newSelection;
            }
        });
    }

    private void configurerRenduColonnes() {
        colCandidat.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                DemandeJob demande = (DemandeJob) getTableRow().getItem();

                Label titre = new Label("Candidat");
                titre.getStyleClass().add("admin-cell-label");

                Label valeur = new Label(demande.getCandidatId());
                valeur.getStyleClass().add("admin-cell-value");
                valeur.setWrapText(true);

                Label info = new Label(aUnCv(demande) ? "CV disponible" : "CV manquant");
                info.getStyleClass().add("admin-cell-meta");

                VBox box = new VBox(2, titre, valeur, info);
                box.getStyleClass().add("admin-cell-card");
                setGraphic(box);
                setText(null);
            }
        });

        colOffre.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                DemandeJob demande = (DemandeJob) getTableRow().getItem();
                OfferJob offre = offerService.recupererParId(demande.getOffreId());
                String titreOffre = offre == null ? "Offre inconnue" : offre.getTitre();
                String descriptionOffre = offre == null || offre.getDescription() == null || offre.getDescription().isBlank()
                        ? "Description non disponible"
                        : offre.getDescription();

                Label titre = new Label(titreOffre);
                titre.getStyleClass().add("admin-cell-value");
                titre.setWrapText(true);

                Label info = new Label("Offre #" + demande.getOffreId());
                info.getStyleClass().add("admin-cell-meta");

                Label description = new Label(descriptionOffre);
                description.getStyleClass().add("admin-cell-description");
                description.setWrapText(true);
                description.setMaxHeight(32);
                description.setText(resumerDescription(descriptionOffre));
                description.setTooltip(new Tooltip(descriptionOffre));

                VBox box = new VBox(2, titre, info, description);
                box.getStyleClass().add("admin-cell-card");
                setGraphic(box);
                setText(null);
            }
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(normaliserStatut(item));
                badge.getStyleClass().add("candidature-status-badge");
                badge.getStyleClass().add(styleStatut(item));

                VBox wrap = new VBox(badge);
                wrap.setAlignment(Pos.TOP_LEFT);
                wrap.getStyleClass().add("admin-status-wrap");
                setGraphic(wrap);
                setText(null);
            }
        });
    }

    private String normaliserStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return "En attente";
        }

        return switch (statut.toLowerCase(Locale.ROOT)) {
            case "accepted" -> "Acceptee";
            case "declined" -> "Refusee";
            case "pending", "en attente" -> "En attente";
            default -> statut;
        };
    }

    private String styleStatut(String statut) {
        if (statut == null) {
            return "status-pending";
        }

        return switch (statut.toLowerCase(Locale.ROOT)) {
            case "accepted" -> "status-accepted";
            case "declined" -> "status-declined";
            default -> "status-pending";
        };
    }

    private String resumerDescription(String description) {
        if (description == null) {
            return "";
        }

        String texte = description.trim().replaceAll("\\s+", " ");
        if (texte.length() <= 85) {
            return texte;
        }
        return texte.substring(0, 82) + "...";
    }


    private void chargerDonneesInitiales() {
        if (tfOffreId != null) tfOffreId.setEditable(false);
        if (comboOffres != null) chargerComboOffres();
        rafraichirTable();
    }

    private void rafraichirTable() {
        if (tableDemandes != null) {
            masterData.setAll(service.afficher());
            tableDemandes.setItems(masterData);
        }
    }

    private void chargerComboOffres() {
        if (comboOffres == null) return;
        List<OfferJob> offres = offerService.afficher();
        comboOffres.setItems(FXCollections.observableArrayList(
                offres.stream().map(o -> o.getId() + " - " + o.getTitre()).collect(Collectors.toList())
        ));
    }


    @FXML
    void handleModifier() {
        selectedDemande = tableDemandes.getSelectionModel().getSelectedItem();

        if (selectedDemande == null) {
            showAlert("Sélection requise",
                    "Veuillez sélectionner une candidature dans le tableau.",
                    Alert.AlertType.WARNING);
            return;
        }

        if ("Accepted".equalsIgnoreCase(selectedDemande.getStatus()) ||
                "Declined".equalsIgnoreCase(selectedDemande.getStatus())) {
            showAlert("Modification impossible",
                    "Cette candidature a déjà été traitée et ne peut plus être modifiée.",
                    Alert.AlertType.ERROR); // Remplacé STOP par ERROR
            return;
        }

        try {
            URL resource = getClass().getResource("/AddDemandeForm.fxml");
            if (resource == null) {
                LOGGER.log(Level.SEVERE, "Fichier FXML introuvable.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier la Candidature");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            selectedDemande = null;
            rafraichirTable();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture du formulaire", e);
        }
    }
    @FXML
    void handlePostuler() {
        if (!validerSaisie()) {
            showAlert("Champs incomplets", "L'ID du candidat et le CV sont obligatoires.", Alert.AlertType.ERROR);
            return;
        }

        try {
            String candidatId = tfCandidatId.getText().trim();
            int nouvelleOffreId;

            if (tfOffreId != null && !tfOffreId.getText().isEmpty()) {
                nouvelleOffreId = Integer.parseInt(tfOffreId.getText());
            } else if (comboOffres != null && comboOffres.getValue() != null) {
                nouvelleOffreId = Integer.parseInt(comboOffres.getValue().split(" - ")[0]);
            } else {
                showAlert("Erreur", "Veuillez sélectionner une offre.", Alert.AlertType.ERROR);
                return;
            }

            if (selectedDemande != null) {
                boolean offreAChange = (selectedDemande.getOffreId() != nouvelleOffreId);

                if (offreAChange) {
                    if (service.existeDeja(candidatId, nouvelleOffreId)) {
                        showAlert("Modification refusée",
                                "Le candidat " + candidatId + " a déjà une candidature en cours pour l'offre n°" + nouvelleOffreId + ". " +
                                        "Veuillez choisir une autre offre ou conserver l'offre originale.",
                                Alert.AlertType.WARNING);
                        return;
                    }
                }
            }
            else if (service.existeDeja(candidatId, nouvelleOffreId)) {
                showAlert("Doublon détecté", "Ce candidat a déjà postulé pour cette offre.", Alert.AlertType.WARNING);
                return;
            }

            DemandeJob d = (selectedDemande != null) ? selectedDemande : new DemandeJob();
            d.setCandidatId(candidatId);
            d.setOffreId(nouvelleOffreId);
            d.setCvUrl(tfCvUrl.getText().trim());
            d.setLettreMotivation(taMotivation.getText().trim());

            if (selectedDemande != null) {
                service.modifier(d);
                showAlert("Succès", "Modification enregistrée avec succès.", Alert.AlertType.INFORMATION);
            } else {
                d.setStatus("En attente");
                service.ajouter(d);
                showAlert("Succès", "Candidature créée avec succès.", Alert.AlertType.INFORMATION);
            }

            handleCancel(new ActionEvent(tfCandidatId, null));

        } catch (NumberFormatException e) {
            showAlert("Erreur de format", "L'identifiant de l'offre doit être un nombre.", Alert.AlertType.ERROR);
        }

    }
    public void preRemplirFormulaire(DemandeJob d) {
        tfCandidatId.setText(d.getCandidatId());
        tfCvUrl.setText(d.getCvUrl());
        taMotivation.setText(d.getLettreMotivation());
        if (tfOffreId != null) tfOffreId.setText(String.valueOf(d.getOffreId()));
    }

    @FXML
    void showAddDemandePopup(ActionEvent event) {
        selectedDemande = null; // S'assurer qu'on est en mode "Ajout"
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AddDemandeForm.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvelle Candidature");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            rafraichirTable();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement popup", e);
        }
    }

    @FXML
    void handleRecherche() {
        if (tfRecherche == null) return;
        String keyword = tfRecherche.getText().toLowerCase().trim();
        tableDemandes.setItems(masterData.stream()
                .filter(d -> d.getCandidatId().toLowerCase().contains(keyword) ||
                        d.getStatus().toLowerCase().contains(keyword))
                .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    }


    @FXML void handleNavigate() { System.exit(0); }
    @FXML void naviguerVersOffres(ActionEvent event) { changerScene(event, "/OfferWindow.fxml"); }
    @FXML void switchToFront(ActionEvent event) { changerScene(event, "/JobFront.fxml"); }

    private void changerScene(ActionEvent event, String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur navigation", e);
        }
    }


    private void modifierStatut(String statut) {
        selectedDemande = tableDemandes.getSelectionModel().getSelectedItem();
        if (selectedDemande != null) {
            selectedDemande.setStatus(statut);
            service.modifier(selectedDemande);
            rafraichirTable();

            if ("Accepted".equalsIgnoreCase(statut)) {
                showAlert(
                        "Lien Meet genere",
                        "Lien Jitsi du rendez-vous :\n" + construireLienJitsi(selectedDemande),
                        Alert.AlertType.INFORMATION
                );
            }
        }
    }

    private void remplirChamps(DemandeJob d) {
        if (tfCandidatId != null) tfCandidatId.setText(d.getCandidatId());
        if (tfCvUrl != null) tfCvUrl.setText(d.getCvUrl());
        if (taMotivation != null) taMotivation.setText(d.getLettreMotivation());
    }

    private boolean validerSaisie() {
        if (tfCandidatId == null || tfCvUrl == null) return false;
        return !tfCandidatId.getText().trim().isEmpty() && !tfCvUrl.getText().trim().isEmpty();
    }

    @FXML void handleAccepter() { modifierStatut("Accepted"); }
    @FXML void handleRefuser() { modifierStatut("Declined"); }
    @FXML
    void handleScoreMatching() {
        DemandeJob selected = tableDemandes.getSelectionModel().getSelectedItem();
        afficherScoreMatchingPourDemande(selected);
    }

    private void afficherScoreMatchingPourDemande(DemandeJob selected) {

        if (selected == null) {
            showAlert("Selection requise",
                    "Veuillez selectionner une candidature pour calculer son score matching.",
                    Alert.AlertType.WARNING);
            return;
        }

        if (!aUnCv(selected)) {
            showAlert("CV requis",
                    "Cette candidature ne possede pas de CV. L'analyse matching est impossible.",
                    Alert.AlertType.WARNING);
            return;
        }

        OfferJob offer = offerService.recupererParId(selected.getOffreId());
        if (offer == null) {
            showAlert("Offre introuvable",
                    "Impossible de retrouver l'offre liee a cette candidature.",
                    Alert.AlertType.ERROR);
            return;
        }

        try {
            String report = matchingService.analyzeCandidateForOffer(offer, selected);
            afficherRapportMatching(offer, 1, report);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du score matching", e);
            showAlert("Score matching impossible",
                    "Le calcul a echoue. Verifiez que Python est installe et que le script est accessible.",
                    Alert.AlertType.ERROR);
        }
    }

    @FXML void handleSupprimer() {
        DemandeJob selected = tableDemandes.getSelectionModel().getSelectedItem();

        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation de suppression");
            alert.setHeaderText("Suppression de la candidature");
            alert.setContentText("Êtes-vous sûr de vouloir supprimer la candidature de \""
                    + selected.getCandidatId() + "\" pour l'offre n°"
                    + selected.getOffreId() + " ?");

            ButtonType buttonTypeOK = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
            ButtonType buttonTypeCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonTypeOK, buttonTypeCancel);

            alert.showAndWait().ifPresent(response -> {
                if (response == buttonTypeOK) {
                    service.supprimer(selected.getId());

                    rafraichirTable();

                    showAlert("Succès", "La candidature a été supprimée avec succès.", Alert.AlertType.INFORMATION);
                } else {
                    System.out.println("Suppression annulée");
                }
            });

        } else {
            showAlert("Sélection requise", "Veuillez sélectionner une candidature dans le tableau avant de cliquer sur supprimer.", Alert.AlertType.WARNING);
        }
    }

    @FXML void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML void handleChoisirFichier(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showOpenDialog(((Node) event.getSource()).getScene().getWindow());
        if (f != null && tfCvUrl != null) tfCvUrl.setText(f.getAbsolutePath());
    }



    @FXML
    void afficherStatsCandidatures() {
        long enAttente = masterData.stream().filter(d -> "Pending".equalsIgnoreCase(d.getStatus())).count();
        long acceptes = masterData.stream().filter(d -> "Accepted".equalsIgnoreCase(d.getStatus())).count();
        long refuses = masterData.stream().filter(d -> "Declined".equalsIgnoreCase(d.getStatus())).count();

        PieChart.Data slice1 = new PieChart.Data("En attente (" + enAttente + ")", enAttente);
        PieChart.Data slice2 = new PieChart.Data("Acceptées (" + acceptes + ")", acceptes);
        PieChart.Data slice3 = new PieChart.Data("Refusées (" + refuses + ")", refuses);

        PieChart pieChart = new PieChart(FXCollections.observableArrayList(slice1, slice2, slice3));
        pieChart.setTitle("Répartition des Candidatures par Statut");
        pieChart.setLegendSide(javafx.geometry.Side.BOTTOM);

        slice2.getNode().setStyle("-fx-pie-color: #28a745;"); // Vert pour Accepté
        slice3.getNode().setStyle("-fx-pie-color: #dc3545;"); // Rouge pour Refusé
        slice1.getNode().setStyle("-fx-pie-color: #CC9933;"); // Jaune/Bordeaux pour En attente

        VBox root = new VBox(pieChart);
        root.setPadding(new javafx.geometry.Insets(15));
        root.setStyle("-fx-background-color: white;");

        Stage stage = new Stage();
        stage.setTitle("Statistiques des Candidatures");
        stage.setScene(new Scene(root, 600, 500));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.show();
    }

    private void ajouterBoutonsAction() {
        colAction.setCellFactory(p -> new TableCell<>() {
            private final Button btnCv = new Button("Voir CV");
            private final Button btnMeet = new Button("Meet");
            private final Button btnScore = new Button("Score");
            private final HBox actions = new HBox(8, btnCv, btnMeet, btnScore);
            {
                btnCv.getStyleClass().add("btn-action-yellow");
                btnMeet.getStyleClass().add("btn-action-green");
                btnScore.getStyleClass().add("btn-action-outline");

                btnCv.setOnAction(e -> {
                    DemandeJob d = getTableView().getItems().get(getIndex());
                    try { Desktop.getDesktop().open(new File(d.getCvUrl())); }
                    catch (Exception ex) { showAlert("Erreur", "Fichier introuvable."); }
                });

                btnMeet.setOnAction(e -> {
                    DemandeJob d = getTableView().getItems().get(getIndex());
                    ouvrirMeetJitsi(d);
                });

                btnScore.setOnAction(e -> {
                    DemandeJob d = getTableView().getItems().get(getIndex());
                    afficherScoreMatchingPourDemande(d);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                DemandeJob d = getTableView().getItems().get(getIndex());
                btnMeet.setDisable(!"Accepted".equalsIgnoreCase(d.getStatus()));
                btnScore.setDisable(!aUnCv(d));
                setGraphic(actions);
            }
        });
    }

    private boolean aUnCv(DemandeJob demande) {
        return demande != null
                && demande.getCvUrl() != null
                && !demande.getCvUrl().trim().isEmpty();
    }

    private void ouvrirMeetJitsi(DemandeJob demande) {
        if (demande == null) {
            showAlert("Selection requise", "Veuillez selectionner une candidature.", Alert.AlertType.WARNING);
            return;
        }

        if (!"Accepted".equalsIgnoreCase(demande.getStatus())) {
            showAlert("Meet indisponible", "Le lien Jitsi n'est disponible que pour une candidature acceptee.", Alert.AlertType.WARNING);
            return;
        }

        String lien = construireLienJitsi(demande);
        try {
            Desktop.getDesktop().browse(URI.create(lien));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible d'ouvrir le lien Jitsi", e);
            showAlert("Lien Meet", lien, Alert.AlertType.INFORMATION);
        }
    }

    private String construireLienJitsi(DemandeJob demande) {
        String candidat = demande.getCandidatId() == null ? "candidat" : demande.getCandidatId();
        String salle = ("admin-candidat-" + demande.getOffreId() + "-" + candidat + "-" + demande.getId())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return "https://meet.jit.si/" + salle;
    }
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type); // Utilise le type passé en paramètre (ERROR, WARNING, etc.)
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void afficherRapportMatching(OfferJob offer, int nombreCandidatures, String report) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Score Matching Candidats / Poste");

        Label titre = new Label("Analyse matching score");
        titre.getStyleClass().add("matching-stage-title");

        Label sousTitre = new Label("Visualisation du classement des candidats pour l'offre selectionnee.");
        sousTitre.getStyleClass().add("matching-stage-subtitle");

        VBox heroText = new VBox(6, titre, sousTitre);

        Label badge = new Label("Rapport IA");
        badge.getStyleClass().add("matching-stage-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hero = new HBox(16, heroText, spacer, badge);
        hero.setAlignment(Pos.CENTER_LEFT);

        VBox cardOffre = creerCarteResume("Offre analysee", offer == null ? "Offre inconnue" : offer.getTitre());
        VBox cardVolume = creerCarteResume("Candidatures comparees", String.valueOf(nombreCandidatures));
        int nombreLignes = report == null || report.isBlank() ? 0 : report.split("\\R").length;
        VBox cardLignes = creerCarteResume("Lignes du rapport", String.valueOf(nombreLignes));

        HBox cards = new HBox(14, cardOffre, cardVolume, cardLignes);

        Label aide = new Label("Le panneau ci-dessous met davantage en valeur les sections du rapport pour faciliter la lecture des scores, du classement et des recommandations.");
        aide.getStyleClass().add("matching-stage-callout");
        aide.setWrapText(true);

        VBox reportBox = new VBox(10);
        reportBox.getStyleClass().add("matching-report-box");
        reportBox.getChildren().addAll(construireBlocsRapport(report));

        ScrollPane scrollPane = new ScrollPane(reportBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("matching-report-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button btnFermer = new Button("Fermer");
        btnFermer.getStyleClass().add("btn-ajouter-museum");
        btnFermer.setOnAction(e -> stage.close());

        HBox footer = new HBox(btnFermer);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(18, hero, cards, aide, scrollPane, footer);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("matching-stage-root");

        Scene scene = new Scene(root, 940, 720);
        URL cssUrl = getClass().getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setScene(scene);
        stage.showAndWait();
    }

    private VBox creerCarteResume(String label, String valeur) {
        Label titre = new Label(label);
        titre.getStyleClass().add("matching-card-label");

        Label contenu = new Label(valeur);
        contenu.getStyleClass().add("matching-card-value");
        contenu.setWrapText(true);

        VBox card = new VBox(8, titre, contenu);
        card.getStyleClass().add("matching-summary-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private List<VBox> construireBlocsRapport(String report) {
        String contenu = report == null ? "" : report.trim();
        if (contenu.isEmpty()) {
            Label vide = new Label("Aucun contenu de matching n'a ete retourne.");
            vide.getStyleClass().add("matching-line-body");
            VBox bloc = new VBox(vide);
            bloc.getStyleClass().add("matching-report-block");
            return List.of(bloc);
        }

        return contenu.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.toLowerCase(Locale.ROOT).contains("meilleur candidat"))
                .map(this::creerBlocRapport)
                .collect(Collectors.toList());
    }

    private VBox creerBlocRapport(String line) {
        VBox block = new VBox(6);
        block.getStyleClass().add("matching-report-block");

        boolean isHeading = line.endsWith(":")
                || line.toLowerCase(Locale.ROOT).contains("score")
                || line.toLowerCase(Locale.ROOT).contains("classement")
                || line.toLowerCase(Locale.ROOT).contains("top")
                || line.startsWith("#");

        Label text = new Label(line.replaceFirst("^[-*]\\s*", ""));
        text.setWrapText(true);
        text.getStyleClass().add(isHeading ? "matching-line-heading" : "matching-line-body");

        block.getChildren().add(text);
        return block;
    }

    @FXML
    void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'export Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier Excel", "*.xlsx"));
        fileChooser.setInitialFileName("export_candidatures.xlsx");

        File file = fileChooser.showSaveDialog(tableDemandes.getScene().getWindow());

        if (file != null) {
            try {
                ExcelExporter.exportDemandes(masterData, file.getAbsolutePath());

                showAlert("Export réussi",
                        "Le fichier Excel a été généré avec succès.",
                        Alert.AlertType.INFORMATION);

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erreur export Excel", e);
                showAlert("Erreur d'export", "Impossible de générer le fichier Excel.", Alert.AlertType.ERROR);
            }
        }
    }
}
