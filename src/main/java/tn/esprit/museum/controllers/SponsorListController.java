package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Sponsor;
import tn.esprit.museum.services.EmailService;
import tn.esprit.museum.services.SponsorAIService;
import tn.esprit.museum.services.SponsorService;
import tn.esprit.museum.services.PdfExportService;
import tn.esprit.museum.utils.WindowManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SponsorListController implements Initializable {

    private static final String FILTRE_ACCEPTE = "accepte";
    private static final String FILTRE_REFUSE = "refuse";
    private static final String FILTRE_EN_ATTENTE = "en attente";
    private static final int MAX_COLS = 2;

    @FXML
    private GridPane cardsContainer;
    @FXML
    private Label nbTotalSponsors;
    @FXML
    private Label nbSponsorsActifs;
    @FXML
    private Label totalDons;
    @FXML
    private TextField tfRecherche;
    @FXML
    private Button btnFiltreAccepte;
    @FXML
    private Button btnFiltreRefuse;
    @FXML
    private Button btnFiltreEnAttente;
    @FXML
    private ComboBox<Event> cbEvenement;

    private final SponsorService service = new SponsorService();
    private List<Sponsor> listeSponsorsComplete;
    private String filtreActif = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> appliquerFiltres());
        rafraichirListe();
    }

    private void rafraichirListe() {
        listeSponsorsComplete = service.getData();
        mettreAJourStats(listeSponsorsComplete);
        appliquerFiltres();
    }

    private void appliquerFiltres() {
        if (listeSponsorsComplete == null) {
            return;
        }

        String recherche = tfRecherche.getText() == null ? "" : tfRecherche.getText().trim().toLowerCase();

        List<Sponsor> filteredList = listeSponsorsComplete.stream()
                .filter(this::correspondAuFiltreActif)
                .filter(s -> recherche.isEmpty()
                        || (s.getNom() != null && s.getNom().toLowerCase().contains(recherche)))
                .collect(Collectors.toList());

        afficherSponsors(filteredList);
        mettreEnEvidenceFiltreActif();
    }

    private boolean correspondAuFiltreActif(Sponsor sponsor) {
        if (filtreActif == null) {
            return true;
        }
        switch (filtreActif) {
            case FILTRE_ACCEPTE:
                return estAccepte(sponsor);
            case FILTRE_REFUSE:
                return estRefuse(sponsor);
            default:
                return estEnAttente(sponsor);
        }
    }

    public void afficherSponsors(List<Sponsor> sponsors) {
        cardsContainer.getChildren().clear();
        int numCols = Math.min(MAX_COLS, sponsors.size());
        if (numCols == 0)
            numCols = 1; // for empty case
        cardsContainer.getColumnConstraints().clear();
        for (int i = 0; i < numCols; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            cardsContainer.getColumnConstraints().add(col);
        }
        int col = 0;
        int row = 0;
        for (Sponsor s : sponsors) {
            cardsContainer.add(creerCard(s), col, row);
            col++;
            if (col >= numCols) {
                col = 0;
                row++;
            }
        }

        if (sponsors.isEmpty()) {
            Label vide = new Label("Aucun sponsor pour ce statut.");
            vide.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 10;");
            cardsContainer.add(vide, 0, 0);
        }
    }

    private void mettreAJourStats(List<Sponsor> list) {
        nbTotalSponsors.setText(String.valueOf(list.size()));
        long actifs = list.stream().filter(this::estAccepte).count();
        nbSponsorsActifs.setText(String.valueOf(actifs));
        totalDons.setText("12.500 DT");
    }

    private boolean estAccepte(Sponsor sponsor) {
        String status = normalizeStatus(sponsor.getStatus());
        return FILTRE_ACCEPTE.equals(status) || "actif".equals(status);
    }

    private boolean estRefuse(Sponsor sponsor) {
        return FILTRE_REFUSE.equals(normalizeStatus(sponsor.getStatus()));
    }

    private boolean estEnAttente(Sponsor sponsor) {
        String status = normalizeStatus(sponsor.getStatus());
        return FILTRE_EN_ATTENTE.equals(status) || (!estAccepte(sponsor) && !estRefuse(sponsor));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return FILTRE_EN_ATTENTE;
        }
        return status.trim().toLowerCase();
    }

    private VBox creerCard(Sponsor s) {
        VBox card = new VBox(12);
        card.getStyleClass().add("sponsor-card");
        card.setStyle("-fx-background-color: white; -fx-padding: 18; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); " +
                "-fx-border-color: #dcdde1; -fx-border-radius: 15;");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label nom = new Label(s.getNom() != null ? s.getNom().toUpperCase() : "SPONSOR");
        nom.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label type = new Label("Type: " + (s.getType() != null ? s.getType() : "Partenaire"));
        type.setStyle("-fx-text-fill: #CC9933; -fx-font-weight: bold; -fx-font-size: 13;");
        titleBox.getChildren().addAll(nom, type);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String btnStyle = "-fx-background-color: transparent; -fx-text-fill: black; " +
                "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 5 8; " +
                "-fx-border-color: #eee; -fx-border-radius: 5;";

        Button btnEdit = new Button("Edit");
        btnEdit.setStyle(btnStyle);
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnStyle + "-fx-background-color: #f5f5f5;"));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnStyle));
        btnEdit.setOnAction(e -> handleModifier(s));

        Button btnDelete = new Button("Suppr");
        btnDelete.setStyle(btnStyle);
        btnDelete.setOnMouseEntered(
                e -> btnDelete.setStyle(btnStyle + "-fx-background-color: #f5f5f5; -fx-text-fill: #e74c3c;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle(btnStyle));
        btnDelete.setOnAction(e -> handleSupprimer(s));

        HBox actions = new HBox(8, btnEdit, btnDelete);
        headerRow.getChildren().addAll(titleBox, spacer, actions);

        Text desc = new Text(s.getDescription() != null ? s.getDescription() : "Aucune description fournie.");
        desc.setWrappingWidth(400); // adapte le texte à la largeur de la card
        desc.setStyle("-fx-fill: #7f8c8d; -fx-font-style: italic;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(25);
        infoGrid.setVgap(10);
        infoGrid.add(creerLabelInfo("Email", s.getEmail()), 0, 0);
        infoGrid.add(creerLabelInfo("Site Web", s.getSiteWeb()), 1, 0);
        infoGrid.add(creerLabelInfo("Telephone", s.getTelephone()), 0, 1);
        infoGrid.add(creerLabelInfo("Adresse", s.getAdresse()), 1, 1);

        Label statut = new Label(getStatusText(s.getStatus()));
        statut.setStyle(buildStatusStyle(s.getStatus()));

        Button btnAccepter = new Button("\u2713");
        btnAccepter.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 18; -fx-font-size: 16; -fx-min-width: 38; -fx-min-height: 38;");
        btnAccepter.setOnAction(e -> handleStatusChange(s, FILTRE_ACCEPTE));

        Button btnRefuser = new Button("\u2717");
        btnRefuser.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 18; -fx-font-size: 16; -fx-min-width: 38; -fx-min-height: 38;");
        btnRefuser.setOnAction(e -> handleStatusChange(s, FILTRE_REFUSE));

        HBox statusActions = new HBox(10, statut, btnAccepter, btnRefuser);
        statusActions.setAlignment(Pos.CENTER_LEFT);

        if (estEnAttente(s)) {
            Button btnEvalAI = new Button("🤖 Évaluer");
            btnEvalAI.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 18; -fx-font-size: 14; -fx-padding: 5 15;");
            btnEvalAI.setOnAction(e -> handleAiEvaluation(s));
            statusActions.getChildren().add(btnEvalAI);
        }

        card.getChildren().addAll(headerRow, new Separator(), desc, infoGrid, statusActions);
        return card;
    }

    private String getStatusText(String status) {
        String normalized = normalizeStatus(status);
        switch (normalized) {
            case FILTRE_ACCEPTE:
            case "actif":
                return "ACCEPTE";
            case FILTRE_REFUSE:
                return "REFUSE";
            default:
                return "EN ATTENTE";
        }
    }

    private String buildStatusStyle(String status) {
        String normalized = normalizeStatus(status);
        String color;
        switch (normalized) {
            case FILTRE_ACCEPTE:
            case "actif":
                color = "#27ae60";
                break;
            case FILTRE_REFUSE:
                color = "#e74c3c";
                break;
            default:
                color = "#e67e22";
                break;
        }
        return "-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11;";
    }

    @FXML
    private void filtrerAcceptes() {
        filtreActif = FILTRE_ACCEPTE;
        appliquerFiltres();
    }

    @FXML
    private void filtrerRefuses() {
        filtreActif = FILTRE_REFUSE;
        appliquerFiltres();
    }

    @FXML
    private void filtrerEnAttente() {
        filtreActif = FILTRE_EN_ATTENTE;
        appliquerFiltres();
    }

    private void mettreEnEvidenceFiltreActif() {
        appliquerStyleFiltre(btnFiltreAccepte, FILTRE_ACCEPTE.equals(filtreActif), "#27ae60");
        appliquerStyleFiltre(btnFiltreRefuse, FILTRE_REFUSE.equals(filtreActif), "#e74c3c");
        appliquerStyleFiltre(btnFiltreEnAttente, FILTRE_EN_ATTENTE.equals(filtreActif), "#e67e22");
    }

    private void appliquerStyleFiltre(Button button, boolean actif, String color) {
        if (button == null) {
            return;
        }

        if (actif) {
            button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 18; -fx-padding: 8 18;");
        } else {
            button.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; " +
                    "-fx-font-weight: bold; -fx-background-radius: 18; -fx-border-color: #dcdde1; " +
                    "-fx-border-radius: 18; -fx-padding: 8 18;");
        }
    }

    private void handleStatusChange(Sponsor sponsor, String nouveauStatus) {
        service.updateStatus(sponsor.getId(), nouveauStatus);
        
        if (FILTRE_ACCEPTE.equals(nouveauStatus) || FILTRE_REFUSE.equals(nouveauStatus)) {
            EmailService.sendStatusEmail(sponsor.getEmail(), sponsor.getNom(), nouveauStatus);
        }
        
        rafraichirListe();
    }

    private void handleAiEvaluation(Sponsor s) {
        String typeSponsor = s.getType();
        String descriptionSponsor = s.getDescription();

        if (typeSponsor == null || typeSponsor.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Le type du sponsor n'est pas défini. Impossible de comparer avec la description.").showAndWait();
            return;
        }

        if (descriptionSponsor == null || descriptionSponsor.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Le sponsor n'a pas de description. Impossible d'évaluer le pourcentage de correspondance.")
                    .showAndWait();
            return;
        }

        double score = SponsorAIService.calculateMatchPercentage(descriptionSponsor, typeSponsor);
        String suggestedStatus = score >= 50.0 ? FILTRE_ACCEPTE : FILTRE_REFUSE;
        String decisionTexte = score >= 50.0 ? "ACCEPTER" : "REFUSER";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Évaluation AI");
        confirm.setHeaderText("Score de correspondance : " + String.format("%.1f%%", score));
        confirm.setContentText("L'événement avec lequel il a été comparé : " + typeSponsor + "\n\n" +
                "L'assistant AI suggère de " + decisionTexte + " ce sponsor.\n" +
                "Voulez-vous confirmer cette décision ?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            handleStatusChange(s, suggestedStatus);
        }
    }

    private void handleModifier(Sponsor s) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterSponsor.fxml"));
            Parent root = loader.load();
            SponsorController controller = loader.getController();
            controller.preRemplirChamps(s);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(cardsContainer.getScene().getWindow());
            stage.setTitle("Modifier Sponsor");
            WindowManager.applyStandardSize(stage);
            stage.showAndWait();
            
            rafraichirListe();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSupprimer(Sponsor s) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le sponsor : " + s.getNom() + " ?",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            String erreur = service.supprimerSponsor(s.getId());
            if (erreur != null) {
                new Alert(Alert.AlertType.WARNING, erreur).showAndWait();
                return;
            }
            rafraichirListe();
        }
    }

    private VBox creerLabelInfo(String titre, String valeur) {
        VBox box = new VBox(2);
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size: 11; -fx-text-fill: #95a5a6; -fx-font-weight: bold;");
        Label lblVal = new Label(valeur != null && !valeur.isEmpty() ? valeur : "N/A");
        lblVal.setStyle("-fx-font-size: 13; -fx-text-fill: #34495e;");
        lblVal.setWrapText(true);
        lblVal.setMaxWidth(300);
        box.getChildren().addAll(lblTitre, lblVal);
        return box;
    }

    @FXML
    private void navigateToPacks(ActionEvent event) {
        loadModuleOrScene(event, "/fxml/PackList.fxml");
    }

    @FXML
    private void navigateToDashboard(ActionEvent event) {
        loadDashboard(event, null);
    }

    @FXML
    private void navigateToUsers(ActionEvent event) {
        loadDashboard(event, DashboardController::showUsersPanel);
    }

    @FXML
    private void navigateToProducts(ActionEvent event) {
        loadDashboard(event, DashboardController::showProductsPanel);
    }

    @FXML
    private void navigateToEvents(ActionEvent event) {
        loadDashboard(event, DashboardController::handleShowEvents);
    }

    @FXML
    private void navigateToReservations(ActionEvent event) {
        loadDashboard(event, DashboardController::handleShowReservations);
    }

    @FXML
    private void navigateToSponsors(ActionEvent event) {
        rafraichirListe();
    }

    @FXML
    private void navigateToOffers(ActionEvent event) {
        loadDashboard(event, DashboardController::showOffersPanel);
    }

    @FXML
    private void navigateToDemandes(ActionEvent event) {
        loadDashboard(event, DashboardController::showDemandesPanel);
    }

    @FXML
    private void navigateToForum(ActionEvent event) {
        loadDashboard(event, DashboardController::handleShowForum);
    }

    @FXML
    private void handleRefreshSidebar(ActionEvent event) {
        rafraichirListe();
    }

    @FXML
    private void handleLogoutSidebar(ActionEvent event) {
        loadPage(event, "/fxml/login.fxml", false);
    }

    @FXML
    private void ouvrirFormulaireAjout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AjouterSponsor.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(cardsContainer.getScene().getWindow());
            stage.setTitle("Ajouter Sponsor");
            WindowManager.applyStandardSize(stage);
            stage.showAndWait();
            
            rafraichirListe();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateToNavPage(ActionEvent event) {
        loadDashboard(event, null);
    }

    private void loadPage(ActionEvent event, String fxmlPath) {
        loadPage(event, fxmlPath, false);
    }

    private void loadPage(ActionEvent event, String fxmlPath, boolean forceMaximized) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            WindowManager.applyDashboardFill(root, false);
            stage.setScene(new Scene(root));
            WindowManager.applyStandardSize(stage);
            WindowManager.applyWindowState(stage, wasMaximized, wasFullScreen, forceMaximized);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadModuleOrScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Node currentModuleRoot = findCurrentModuleRoot((Node) event.getSource());
            Parent host = currentModuleRoot == null ? null : currentModuleRoot.getParent();

            if (host instanceof Pane hostPane) {
                WindowManager.applyDashboardFill(root, true);
                int index = hostPane.getChildren().indexOf(currentModuleRoot);
                if (index >= 0) {
                    hostPane.getChildren().set(index, root);
                } else {
                    hostPane.getChildren().setAll(root);
                }
                return;
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            WindowManager.applyDashboardFill(root, false);
            stage.setScene(new Scene(root));
            WindowManager.applyStandardSize(stage);
            WindowManager.applyWindowState(stage, wasMaximized, wasFullScreen, true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Node findCurrentModuleRoot(Node source) {
        Node current = source;
        while (current != null) {
            if (current instanceof BorderPane) {
                return current;
            }
            current = current.getParent();
        }
        return source;
    }

    private void loadDashboard(ActionEvent event, java.util.function.Consumer<DashboardController> afterLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            if (afterLoad != null) {
                afterLoad.accept(controller);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            boolean wasFullScreen = stage.isFullScreen();
            boolean wasMaximized = stage.isMaximized();
            WindowManager.applyDashboardFill(root, false);
            stage.setScene(new Scene(root));
            WindowManager.applyStandardSize(stage);
            WindowManager.applyWindowState(stage, wasMaximized, wasFullScreen, false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportPdf(ActionEvent event) {
        List<Sponsor> acceptes = service.getData().stream()
                .filter(this::estAccepte)
                .collect(Collectors.toList());

        if (acceptes.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucun sponsor accepte a exporter !").showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF des Sponsors");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        fileChooser.setInitialFileName("Sponsors_Acceptes.pdf");

        File destFile = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());

        if (destFile != null) {
            try {
                PdfExportService.exportSponsorsToPdf(acceptes, destFile);
                new Alert(Alert.AlertType.INFORMATION, "Export PDF reussi avec succes !").showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors de l'export PDF : " + e.getMessage()).showAndWait();
            }
        }
    }
}
