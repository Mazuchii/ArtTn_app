package tn.esprit.museum.controllers;

import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Pack;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.PackService;
import tn.esprit.museum.utils.WindowManager;
import javafx.beans.binding.Bindings;
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
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class PackListController implements Initializable {

    @FXML private VBox contentContainer;
    @FXML private FlowPane statsContainer;
    @FXML private ScrollPane packsScrollPane;
    @FXML private FlowPane packsContainer;
    @FXML private ComboBox<String> categoryFilter;

    // CORRECTION DES IDS (doivent matcher le FXML)
    @FXML private Label nbTotalPacks;
    @FXML private Label prixMoyenPack;
    @FXML private Label maxPackValue;

    private final PackService service = new PackService();
    private final EventService eventService = new EventService();
    private List<Pack> allPacks;
    private Map<Integer, Event> evenementCache = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statsContainer.prefWrapLengthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(680, contentContainer.getWidth() - 40),
                contentContainer.widthProperty()
        ));
        statsContainer.maxWidthProperty().bind(statsContainer.prefWrapLengthProperty());

        packsContainer.prefWrapLengthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(520, packsScrollPane.getViewportBounds().getWidth() - 16),
                packsScrollPane.viewportBoundsProperty()
        ));
        packsContainer.maxWidthProperty().bind(packsContainer.prefWrapLengthProperty());

        // Charger les catégories
        chargerCategories();

        // Ajouter un listener pour le changement de catégorie
        categoryFilter.setOnAction(e -> appliquerFiltre());

        chargerPacks();
    }

    private void chargerCategories() {
        List<String> categories = eventService.getAllCategories();
        categoryFilter.getItems().add("Toutes les catégories");
        categoryFilter.getItems().addAll(categories);
        categoryFilter.setValue("Toutes les catégories");
    }

    private void appliquerFiltre() {
        String selectedCategory = categoryFilter.getValue();

        packsContainer.getChildren().clear();
        List<Pack> packsAffichees;

        if (selectedCategory == null || selectedCategory.equals("Toutes les catégories")) {
            packsAffichees = allPacks;
        } else {
            packsAffichees = service.getDataByEvenementCategory(selectedCategory);
        }

        for (Pack p : packsAffichees) {
            packsContainer.getChildren().add(creerCartePack(p));
        }

        mettreAJourStats(packsAffichees);
    }

    private void chargerPacks() {
        allPacks = service.getData();

        // Pré-charger les événements pour éviter les appels DB multiples
        evenementCache.clear();
        for (Pack pack : allPacks) {
            if (pack.getEvenementId() > 0) {
                Event evt = eventService.findById(pack.getEvenementId());
                if (evt != null) {
                    evenementCache.put(pack.getEvenementId(), evt);
                }
            }
        }

        appliquerFiltre();
    }

    private void mettreAJourStats(List<Pack> packs) {
        if (packs == null || packs.isEmpty()) {
            if(nbTotalPacks != null) nbTotalPacks.setText("0");
            if(prixMoyenPack != null) prixMoyenPack.setText("0 DT");
            if(maxPackValue != null) maxPackValue.setText("0 DT");
            return;
        }

        // 1. Total
        nbTotalPacks.setText(String.valueOf(packs.size()));

        // 2. Prix Moyen
        double moyenne = packs.stream()
                .mapToDouble(Pack::getMontant)
                .average()
                .orElse(0.0);
        prixMoyenPack.setText(String.format("%.1f DT", moyenne));

        // 3. Max
        double max = packs.stream()
                .mapToDouble(Pack::getMontant)
                .max()
                .orElse(0.0);
        maxPackValue.setText(String.format("%.1f DT", max));
    }

    private VBox creerCartePack(Pack p) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 18; -fx-border-color: #dcdde1; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);");
        card.setPrefWidth(260);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblNom = new Label(p.getNom().toUpperCase());
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 17px; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String btnStyle = "-fx-background-color: transparent; -fx-text-fill: black; -fx-font-size: 18px; -fx-cursor: hand;";

        Button btnEdit = new Button("✎");
        btnEdit.setStyle(btnStyle);
        btnEdit.setOnAction(e -> handleModifier(p));

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle(btnStyle);
        btnDelete.setOnAction(e -> handleSupprimer(p));

        HBox actions = new HBox(5, btnEdit, btnDelete);
        header.getChildren().addAll(lblNom, spacer, actions);

        // Récupérer l'événement depuis le cache
        Event event = evenementCache.get(p.getEvenementId());

        Label lblCategorie = new Label();
        if (event != null && event.getCategory() != null) {
            lblCategorie.setText(event.getCategory());
            lblCategorie.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #8e44ad; -fx-padding: 5 10;");
        } else {
            lblCategorie.setText("Sans catégorie");
            lblCategorie.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #95a5a6; -fx-padding: 5 10;");
        }

        Label lblMontant = new Label(p.getMontant() + " DT");
        lblMontant.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #b8860b;");

        Label lblAvantages = new Label(p.getAvantages());
        lblAvantages.setWrapText(true);
        lblAvantages.setStyle("-fx-text-fill: #4b6584; -fx-font-size: 13px; -fx-font-style: italic;");
        lblAvantages.setPrefWidth(220);
        lblAvantages.setMinHeight(60);

        card.getChildren().addAll(header, new Separator(), lblCategorie, lblMontant, lblAvantages);
        return card;
    }

    private void handleModifier(Pack p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ajouterPack.fxml"));
            Parent root = loader.load();
            PackController controller = loader.getController();
            controller.preRemplirChamps(p);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(packsContainer.getScene().getWindow());
            stage.setTitle("Modifier Pack");
            WindowManager.applyStandardSize(stage);
            stage.showAndWait();
            
            chargerPacks();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void handleSupprimer(Pack p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le pack " + p.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            service.deleteEntity(p.getId());
            chargerPacks();
        }
    }

    @FXML
    void ouvrirFormulaireAjout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ajouterPack.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.setTitle("Ajouter Pack");
            WindowManager.applyStandardSize(stage);
            stage.showAndWait();
            
            chargerPacks();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    void navigateToSponsors(ActionEvent event) {
        loadModuleOrScene(event, "/fxml/SponsorList.fxml");
    }

    @FXML
    void retourDashboard(ActionEvent event) {
        navigateToSponsors(event);
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
        chargerPacks();
    }

    @FXML
    private void handleLogoutSidebar(ActionEvent event) {
        loadPage(event, "/fxml/login.fxml", false);
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
        } catch (IOException e) { e.printStackTrace(); }
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
        } catch (IOException e) { e.printStackTrace(); }
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
        } catch (IOException e) { e.printStackTrace(); }
    }
}
