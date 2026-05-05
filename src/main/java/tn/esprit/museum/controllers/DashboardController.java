package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Order;
import tn.esprit.museum.entities.OrderItem;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.OrderService;
import tn.esprit.museum.services.ProductService;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
public class DashboardController {

    // ==================== USER MANAGEMENT ====================
    @FXML private Label welcomeLabel;
    @FXML private Label statsTotalUsers;
    @FXML private Label statsActiveUsers;
    @FXML private Label statsAdmins;
    @FXML private Label statsInactiveUsers;
    @FXML private VBox inactiveCard;
    @FXML private VBox forumPanel;
    @FXML private VBox jobsPanel;
    @FXML private VBox eventsPanel;
    @FXML private VBox reservationsPanel;


    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, Boolean> activeColumn;


    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;

    // Graphiques
    @FXML private PieChart rolePieChart;
    @FXML private BarChart<String, Number> statsBarChart;

    // ==================== PRODUITS & COMMANDES ====================
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> productIdColumn;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, String> productDescColumn;
    @FXML private TableColumn<Product, Double> productPriceColumn;
    @FXML private TableColumn<Product, Integer> productStockColumn;
    @FXML private TableColumn<Product, String> productCategoryColumn;
    @FXML private TableColumn<Product, Boolean> productStatusColumn;
    @FXML private TableColumn<Product, Void> predictionColumn;

    @FXML private TextField productSearchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label statsProductsCount;
    @FXML private Label statsOrdersCount;
    @FXML private Label statsPendingCount;

    // Statistiques cliquables
    @FXML private VBox productsStatsCard;
    @FXML private VBox ordersStatsCard;
    @FXML private VBox pendingStatsCard;

    // Tableau des commandes (intégré dans la vue produits)
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, Integer> orderUserIdColumn;
    @FXML private TableColumn<Order, String> orderDateColumn;
    @FXML private TableColumn<Order, Double> orderTotalColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;
    @FXML private TableColumn<Order, String> orderAddressColumn;
    @FXML private TableColumn<Order, String> orderPaymentColumn;

    @FXML private TextField orderSearchField;
    @FXML private ComboBox<String> orderStatusFilter;
    @FXML private Label orderDetailsLabel;
    @FXML private ListView<String> orderItemsList;
    @FXML private HBox ordersActionsBox;
    @FXML private Button confirmOrderBtn;
    @FXML private Button shipOrderBtn;
    @FXML private Button deliverOrderBtn;
    @FXML private Button cancelOrderBtn;
    @FXML private Button deleteOrderBtn;

    // Vue des produits (liste) et vue des commandes (toggle)
    @FXML private VBox productsView;
    @FXML private VBox ordersView;
    @FXML private Button toggleViewButton;
    @FXML private Label sectionTitleLabel;

    // ==================== PANELS ====================
    @FXML private VBox usersPanel;
    @FXML private VBox productsMainPanel;
    @FXML private VBox statsProductsPanel;
    @FXML private ComboBox<String> statsPeriodCombo;  // Semaine, Mois, Année
    @FXML private ComboBox<String> statsCategoryCombo; // Catégories
    @FXML private Button refreshStatsButton;
    @FXML private TableView<ProductStat> topProductsTable;
    @FXML private TableColumn<ProductStat, String> productNameColumnStat;
    @FXML private TableColumn<ProductStat, Integer> totalSoldColumn;
    @FXML private TableColumn<ProductStat, Double> totalRevenueColumn;
    @FXML private TableColumn<ProductStat, String> categoryColumnStat;
    @FXML private Label statsPeriodLabel;
    @FXML private Label topProductNameLabel;
    @FXML private Label topProductSoldLabel;

    @FXML private Button btnDashboard;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnProduits;
    @FXML private Button btnEvenements;
    @FXML private Button btnReservation;
    @FXML private Button btnSponsors;
    @FXML private Button btnForum;

    private UserService userService;
    private ProductService productService;
    private OrderService orderService;
    private ObservableList<User> userList;
    private ObservableList<Product> productList;
    private ObservableList<Order> orderList;
    private User currentUser;
    private Parent forumDashboardView;
    private boolean showingOrders = false;

    public static class ProductStat {
        private String productName;
        private int totalSold;
        private double totalRevenue;
        private String category;

        public ProductStat(String productName, int totalSold, double totalRevenue, String category) {
            this.productName = productName;
            this.totalSold = totalSold;
            this.totalRevenue = totalRevenue;
            this.category = category;
        }

        public String getProductName() { return productName; }
        public int getTotalSold() { return totalSold; }
        public double getTotalRevenue() { return totalRevenue; }
        public String getCategory() { return category; }
    }
    @FXML
    public void initialize() {
        userService = new UserService();
        productService = new ProductService();
        orderService = new OrderService();
        userList = FXCollections.observableArrayList();
        productList = FXCollections.observableArrayList();
        orderList = FXCollections.observableArrayList();

        // ==================== CONFIGURATION USERS ====================
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        roleFilterCombo.getItems().addAll("Tous", "ADMIN", "USER");
        roleFilterCombo.setValue("Tous");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers());
        roleFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterUsers());

        // ==================== CONFIGURATION PRODUITS ====================
        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        productDescColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        productPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        productStockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        productCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        productStatusColumn.setCellValueFactory(new PropertyValueFactory<>("available"));

        productsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        productPriceColumn.setCellFactory(column -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f D", item));
            }
        });

        productStatusColumn.setCellFactory(column -> new TableCell<Product, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item ? "✅ Disponible" : "❌ Indisponible");
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });

        categoryFilter.getItems().addAll("Tous", "Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
        categoryFilter.setValue("Tous");
        productSearchField.textProperty().addListener((obs, old, val) -> filterProducts());
        categoryFilter.valueProperty().addListener((obs, old, val) -> filterProducts());

        // ==================== CONFIGURATION PRÉDICTION ====================
        predictionColumn.setCellFactory(column -> new TableCell<Product, Void>() {
            private final Button predictBtn = new Button("Voir prédiction");

            {
                predictBtn.getStyleClass().add("btn-action-primary");
                predictBtn.setCursor(javafx.scene.Cursor.HAND);
                predictBtn.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    showPredictionDialog(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(predictBtn);
                }
            }
        });

        // ==================== CONFIGURATION COMMANDES ====================
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderUserIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        orderDateColumn.setCellValueFactory(cellData -> {
            Timestamp timestamp = cellData.getValue().getOrderDate();
            String dateStr = timestamp != null ? timestamp.toString().substring(0, 19) : "";
            return new SimpleStringProperty(dateStr);
        });
        orderTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderAddressColumn.setCellValueFactory(new PropertyValueFactory<>("shippingAddress"));
        orderPaymentColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        orderTotalColumn.setCellFactory(column -> new TableCell<Order, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f D", item));
            }
        });

        orderStatusColumn.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    Order order = getTableView().getItems().get(getIndex());
                    setText(getStatusFrench(order.getStatus()));
                    if (order.getStatus().equals("DELIVERED")) setStyle("-fx-text-fill: green;");
                    else if (order.getStatus().equals("PENDING")) setStyle("-fx-text-fill: orange;");
                    else if (order.getStatus().equals("CANCELLED")) setStyle("-fx-text-fill: red;");
                    else setStyle("-fx-text-fill: #3498db;");
                }
            }
        });

        orderStatusFilter.getItems().addAll("Tous", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED");
        orderStatusFilter.setValue("Tous");
        orderSearchField.textProperty().addListener((obs, old, val) -> filterOrders());
        orderStatusFilter.valueProperty().addListener((obs, old, val) -> filterOrders());

        ordersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> showOrderDetails(selected));

        // Statistiques cliquables
        if (productsStatsCard != null) {
            productsStatsCard.setOnMouseClicked(e -> showProductsView());
            productsStatsCard.getStyleClass().add("clickable-stat-card");
        }
        if (ordersStatsCard != null) {
            ordersStatsCard.setOnMouseClicked(e -> showOrdersView());
            ordersStatsCard.getStyleClass().add("clickable-stat-card");
        }
        if (pendingStatsCard != null) {
            pendingStatsCard.setOnMouseClicked(e -> {
                showOrdersView();
                if (orderStatusFilter != null) orderStatusFilter.setValue("PENDING");
                filterOrders();
            });
            pendingStatsCard.getStyleClass().add("clickable-stat-card");
        }

        // ==================== CHARGEMENT DES DONNÉES ====================
        loadUsers();
        loadProducts();
        loadOrders();
        updateStats();
        updateProduitsStats();

        // Afficher le panel utilisateurs par défaut
        showUsersPanel();

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText(" " + currentUser.getFullName());
        }

        addHoverEffectToInactiveCard();
        updateCharts();

        // Vue par défaut : produits
        showProductsView();

        // ==================== CONFIGURATION STATISTIQUES PRODUITS ====================
        if (statsPeriodCombo != null) {
            statsPeriodCombo.getItems().addAll("Semaine", "Mois", "Année");
            statsPeriodCombo.setValue("Semaine");
            statsPeriodCombo.valueProperty().addListener((obs, old, val) -> loadTopProductsStats());
        }

        if (statsCategoryCombo != null) {
            try {
                List<String> categories = productService.getAll().stream()
                        .map(Product::getCategory)
                        .distinct()
                        .collect(Collectors.toList());
                statsCategoryCombo.getItems().addAll("Tous");
                statsCategoryCombo.getItems().addAll(categories);
                statsCategoryCombo.setValue("Tous");
                statsCategoryCombo.valueProperty().addListener((obs, old, val) -> loadTopProductsStats());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (productNameColumnStat != null) {
            productNameColumnStat.setCellValueFactory(new PropertyValueFactory<>("productName"));
            totalSoldColumn.setCellValueFactory(new PropertyValueFactory<>("totalSold"));
            totalRevenueColumn.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
            categoryColumnStat.setCellValueFactory(new PropertyValueFactory<>("category"));

            totalRevenueColumn.setCellFactory(column -> new TableCell<ProductStat, Double>() {
                @Override
                protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : String.format("%.2f D", item));
                }
            });
        }

        loadTopProductsStats();
    }

    @FXML
    public void handleShowForum() {
        try {
            hideMainPanels();

            // Afficher le panel forum
            forumPanel.setVisible(true);
            forumPanel.setManaged(true);
            forumPanel.getChildren().clear();

            // Charger le forum dashboard (ton module)
            if (forumDashboardView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_dashboard.fxml"));
                forumDashboardView = loader.load();
            }
            forumPanel.getChildren().add(forumDashboardView);
            VBox.setVgrow(forumDashboardView, Priority.ALWAYS);

            // Mettre à jour le style du bouton actif
            //updateActiveButton(btnForum);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le forum: " + e.getMessage());
        }
    }

    private void updateActiveButton(Button activeButton) {
        // Désactiver tous les boutons
        if (btnDashboard != null) btnDashboard.getStyleClass().removeAll("menu-button-active");
        if (btnUtilisateurs != null) btnUtilisateurs.getStyleClass().removeAll("menu-button-active");
        if (btnProduits != null) btnProduits.getStyleClass().removeAll("menu-button-active");
        if (btnEvenements != null) btnEvenements.getStyleClass().removeAll("menu-button-active");
        if (btnReservation != null) btnReservation.getStyleClass().removeAll("menu-button-active");
        if (btnSponsors != null) btnSponsors.getStyleClass().removeAll("menu-button-active");
        if (btnForum != null) btnForum.getStyleClass().removeAll("menu-button-active");

        // Activer le bouton cliqué
        if (activeButton != null) {
            activeButton.getStyleClass().removeAll("menu-button");
            activeButton.getStyleClass().add("menu-button-active");
        }
    }

    private String getStatusFrench(String status) {
        switch (status) {
            case "PENDING": return "⏳ En attente";
            case "CONFIRMED": return "✅ Confirmée";
            case "CANCELLED": return "❌ Annulée";
            default: return status;
        }
    }

    // ==================== NAVIGATION ====================
    @FXML public void showUsersPanel() {
        hideMainPanels();
        usersPanel.setVisible(true);
        usersPanel.setManaged(true);
    }

    @FXML public void showProductsPanel() {
        hideMainPanels();
        productsMainPanel.setVisible(true);
        productsMainPanel.setManaged(true);
        showProductsView();
        loadProducts();
        loadOrders();
        updateProduitsStats();
    }

    @FXML public void showOffersPanel() {
        showJobModule("/fxml/OfferWindow.fxml");
    }

    @FXML public void showDemandesPanel() {
        showJobModule("/fxml/DemandeWindow.fxml");
    }


    private void hideMainPanels() {
        usersPanel.setVisible(false);
        usersPanel.setManaged(false);
        productsMainPanel.setVisible(false);
        productsMainPanel.setManaged(false);
        forumPanel.setVisible(false);
        forumPanel.setManaged(false);
        jobsPanel.setVisible(false);
        jobsPanel.setManaged(false);
        eventsPanel.setVisible(false);
        eventsPanel.setManaged(false);
        reservationsPanel.setVisible(false);
        reservationsPanel.setManaged(false);
    }

    private void showJobModule(String fxmlPath) {
        try {
            hideMainPanels();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent moduleView = loader.load();
            if (moduleView instanceof BorderPane borderPane) {
                borderPane.setLeft(null);
            }

            jobsPanel.getChildren().setAll(moduleView);
            VBox.setVgrow(moduleView, Priority.ALWAYS);
            jobsPanel.setVisible(true);
            jobsPanel.setManaged(true);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le module emplois: " + e.getMessage());
        }
    }

    @FXML public void handleShowEvents() {
        hideMainPanels();
        if (eventsPanel.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/events.fxml"));
                Parent root = loader.load();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setLeft(null);
                    ((BorderPane) root).setStyle("-fx-background-color: #e8eef2;");
                }
                eventsPanel.getChildren().add(root);
                VBox.setVgrow(root, Priority.ALWAYS);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger le module Événements : " + e.getMessage());
                return;
            }
        }
        eventsPanel.setVisible(true);
        eventsPanel.setManaged(true);
        updateActiveButton(btnEvenements);
    }

    @FXML public void handleShowReservations() {
        hideMainPanels();
        if (reservationsPanel.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reservations.fxml"));
                Parent root = loader.load();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setLeft(null);
                    ((BorderPane) root).setStyle("-fx-background-color: #e8eef2;");
                }
                reservationsPanel.getChildren().add(root);
                VBox.setVgrow(root, Priority.ALWAYS);
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de charger le module Réservations : " + e.getMessage());
                return;
            }
        }
        reservationsPanel.setVisible(true);
        reservationsPanel.setManaged(true);
        updateActiveButton(btnReservation);
    }

    public void handleShowReservationsForEvent(int eventId, String eventTitle) {
        hideMainPanels();
        // Toujours recharger pour appliquer le filtre
        reservationsPanel.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reservations.fxml"));
            Parent root = loader.load();
            ReservationController rc = loader.getController();
            rc.setEventFilter(eventId, eventTitle);
            if (root instanceof BorderPane) {
                ((BorderPane) root).setLeft(null);
                ((BorderPane) root).setStyle("-fx-background-color: #e8eef2;");
            }
            reservationsPanel.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger les réservations : " + e.getMessage());
            return;
        }
        reservationsPanel.setVisible(true);
        reservationsPanel.setManaged(true);
        updateActiveButton(btnReservation);
    }

    @FXML public void handleShowSponsors() {
        try {
            hideMainPanels();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SponsorList.fxml"));
            Parent root = loader.load();
            if (root instanceof BorderPane borderPane) {
                borderPane.setLeft(null);
            }
            jobsPanel.getChildren().setAll(root);
            VBox.setVgrow(root, Priority.ALWAYS);
            jobsPanel.setVisible(true);
            jobsPanel.setManaged(true);
            updateActiveButton(btnSponsors);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le module Sponsors : " + e.getMessage());
        }
    }

    // ==================== VUE PRODUITS / COMMANDES (TOGGLE) ====================
    public void showProductsView() {
        showingOrders = false;
        productsView.setVisible(true);
        productsView.setManaged(true);
        ordersView.setVisible(false);
        ordersView.setManaged(false);
        if (sectionTitleLabel != null) sectionTitleLabel.setText(" Gestion des Produits");
        if (toggleViewButton != null) toggleViewButton.setText(" Voir les Commandes");
    }

    public void showOrdersView() {
        showingOrders = true;
        productsView.setVisible(false);
        productsView.setManaged(false);
        ordersView.setVisible(true);
        ordersView.setManaged(true);
        if (sectionTitleLabel != null) sectionTitleLabel.setText(" Gestion des Commandes");
        if (toggleViewButton != null) toggleViewButton.setText("Voir les Produits");
        loadOrders();
        updateProduitsStats();
    }
    /**
     * Exporte le classement des meilleures ventes en PDF
     */
    @FXML
    private void handleExportTopProductsPDF() {
        try {
            // Récupérer les données du tableau
            ObservableList<ProductStat> topProducts = topProductsTable.getItems();

            // Vérifier qu'il y a des données
            if (topProducts == null || topProducts.isEmpty()) {
                showAlert("Information", "Aucune donnée à exporter. Veuillez d'abord charger des statistiques.");
                return;
            }

            // Récupérer la période et la catégorie actuelles
            String period = statsPeriodCombo != null ? statsPeriodCombo.getValue() : "Semaine";
            String category = statsCategoryCombo != null ? statsCategoryCombo.getValue() : "Tous";

            // Appeler la méthode d'export
            PDFExporterProduct.exportTopProductsPDF(topProducts, period, category, (Stage) productsTable.getScene().getWindow());

            // Message de succès
            showAlert("Succès", "✅ Le fichier PDF a été généré avec succès !");

        } catch (Exception e) {
            showAlert("Erreur", "❌ Erreur lors de la génération du PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleView() {
        if (showingOrders) {
            showProductsView();
        } else {
            showOrdersView();
        }
    }

    // ==================== USERS ====================
    private void loadUsers() {
        try {
            userList.setAll(userService.getAll());
            usersTable.setItems(userList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
        }
    }

    private void filterUsers() {
        String searchText = searchField.getText().toLowerCase();
        String selectedRole = roleFilterCombo.getValue();
        ObservableList<User> filteredList = FXCollections.observableArrayList();
        for (User user : userList) {
            boolean matchesSearch = searchText.isEmpty() ||
                    user.getUsername().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText) ||
                    user.getFullName().toLowerCase().contains(searchText);
            boolean matchesRole = selectedRole.equals("Tous") || user.getRole().equals(selectedRole);
            if (matchesSearch && matchesRole) filteredList.add(user);
        }
        usersTable.setItems(filteredList);
    }

    private void updateStats() {
        try {
            statsTotalUsers.setText(String.valueOf(userService.getAll().size()));
            statsActiveUsers.setText(String.valueOf(countActiveUsers()));
            statsAdmins.setText(String.valueOf(countAdmins()));
            statsInactiveUsers.setText(String.valueOf(countInactiveUsers()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private int countActiveUsers() throws SQLException {
        return (int) userService.getAll().stream().filter(User::isActive).count();
    }

    private int countAdmins() throws SQLException {
        return (int) userService.getAll().stream().filter(u -> "ADMIN".equals(u.getRole())).count();
    }

    private int countInactiveUsers() throws SQLException {
        return (int) userService.getAll().stream().filter(u -> !u.isActive()).count();
    }

    private void updateCharts() {
        try {
            int totalUsers = userService.getAll().size();
            int admins = countAdmins();
            int activeUsers = countActiveUsers();
            int inactiveUsers = totalUsers - activeUsers;
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Administrateurs (" + admins + ")", admins),
                    new PieChart.Data("Utilisateurs (" + (totalUsers - admins) + ")", totalUsers - admins)
            );
            rolePieChart.setData(pieChartData);
            statsBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nombre d'utilisateurs");
            series.getData().add(new XYChart.Data<>("Actifs", activeUsers));
            series.getData().add(new XYChart.Data<>("Inactifs", inactiveUsers));
            statsBarChart.getData().add(series);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void addHoverEffectToInactiveCard() {
        if (inactiveCard == null) return;
        inactiveCard.setOnMouseEntered(e -> inactiveCard.setStyle("-fx-background-color: #c0392b; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 5); -fx-cursor: hand;"));
        inactiveCard.setOnMouseExited(e -> inactiveCard.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);"));
    }

    @FXML private void handleAddUser() { showAdminDialog(); }
    @FXML private void handleEditUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) showAlert("Erreur", "Sélectionnez un utilisateur");
        else showEditUserDialog(selected);
    }
    @FXML private void handleDeleteUser() { deleteSelectedUser(); }
    @FXML private void handleToggleUserStatus() { toggleUserStatus(); }
    @FXML private void handleExportPDF() { exportUsersPDF(); }
    @FXML private void handleExportStats() { exportStatsPDF(); }
    @FXML private void handleFraudDetection() { showFraudDetection(); }
    @FXML private void handleShowInactiveUsers() { showInactiveUsersDialog(); }

    // ==================== PRODUITS ====================
    private void loadProducts() {
        try {
            productList.setAll(productService.getAll());
            productsTable.setItems(productList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private void filterProducts() {
        String search = productSearchField.getText().toLowerCase();
        String category = categoryFilter.getValue();
        ObservableList<Product> filtered = FXCollections.observableArrayList();
        for (Product p : productList) {
            boolean matchSearch = search.isEmpty() || p.getName().toLowerCase().contains(search) || p.getDescription().toLowerCase().contains(search);
            boolean matchCategory = category.equals("Tous") || p.getCategory().equals(category);
            if (matchSearch && matchCategory) filtered.add(p);
        }
        productsTable.setItems(filtered);
    }

    private void updateProduitsStats() {
        try {
            int products = productService.getAll().size();
            List<Order> orders = orderService.getAllOrders();
            long pending = orders.stream().filter(o -> o.getStatus().equals("PENDING")).count();
            if (statsProductsCount != null) statsProductsCount.setText(String.valueOf(products));
            if (statsOrdersCount != null) statsOrdersCount.setText(String.valueOf(orders.size()));
            if (statsPendingCount != null) statsPendingCount.setText(String.valueOf(pending));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleAddProduct() { showProductDialog(null); }
    @FXML private void handleEditProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) showAlert("Erreur", "Sélectionnez un produit");
        else showProductDialog(selected);
    }
    @FXML private void handleDeleteProduct() { deleteSelectedProducts(); }

    private void deleteSelectedProducts() {
        ObservableList<Product> selected = productsTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) { showAlert("Erreur", "Sélectionnez au moins un produit"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Supprimer " + selected.size() + " produit(s) ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int success = 0;
            for (Product p : selected) {
                try { productService.delete(p.getId()); success++; } catch (SQLException e) { e.printStackTrace(); }
            }
            loadProducts();
            updateProduitsStats();
            showAlert("Succès", "✅ " + success + " produit(s) supprimé(s)");
        }
    }

    @FXML
    private void refreshStats() {
        loadTopProductsStats();
        showAlert("Actualisation", "✅ Statistiques actualisées !");
    }

    private void loadTopProductsStats() {
        try {
            String period = statsPeriodCombo != null ? statsPeriodCombo.getValue() : "Semaine";
            String category = statsCategoryCombo != null ? statsCategoryCombo.getValue() : "Tous";

            List<ProductStat> stats = getTopProducts(period, category);

            if (topProductsTable != null) {
                ObservableList<ProductStat> statsList = FXCollections.observableArrayList(stats);
                topProductsTable.setItems(statsList);
            }

            if (statsPeriodLabel != null) {
                statsPeriodLabel.setText(" Top produits - " + period + (category.equals("Tous") ? "" : " (" + category + ")"));
            }

            if (!stats.isEmpty() && topProductNameLabel != null && topProductSoldLabel != null) {
                ProductStat top = stats.get(0);
                topProductNameLabel.setText(top.getProductName());
                topProductSoldLabel.setText(top.getTotalSold() + " unités");
            }

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Récupère les produits les plus vendus selon la période et la catégorie
     */
    private List<ProductStat> getTopProducts(String period, String category) throws SQLException {
        List<ProductStat> stats = new ArrayList<>();

        String dateCondition = getDateCondition(period);
        String categoryCondition = category.equals("Tous") ? "" : "AND p.category = '" + category + "'";

        String sql = "SELECT p.id, p.name, p.category, SUM(oi.quantity) as total_sold, SUM(oi.quantity * oi.unit_price) as total_revenue " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.id " +
                "JOIN products p ON oi.product_id = p.id " +
                "WHERE o.status IN ('DELIVERED', 'CONFIRMED', 'SHIPPED') " +
                dateCondition +
                categoryCondition +
                "GROUP BY p.id, p.name, p.category " +
                "ORDER BY total_sold DESC " +
                "LIMIT 10";

        System.out.println("SQL Stats: " + sql);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String productName = rs.getString("name");
                int totalSold = rs.getInt("total_sold");
                double totalRevenue = rs.getDouble("total_revenue");
                String productCategory = rs.getString("category");

                stats.add(new ProductStat(productName, totalSold, totalRevenue, productCategory));
            }
        }

        return stats;
    }

    /**
     * Retourne la condition SQL pour la période
     */
    private String getDateCondition(String period) {
        switch (period) {
            case "Semaine":
                return "AND o.order_date >= DATE_SUB(NOW(), INTERVAL 7 DAY) ";
            case "Mois":
                return "AND o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) ";
            case "Année":
                return "AND o.order_date >= DATE_SUB(NOW(), INTERVAL 365 DAY) ";
            default:
                return "AND o.order_date >= DATE_SUB(NOW(), INTERVAL 7 DAY) ";
        }
    }



    private void showProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "Ajouter un produit" : "Modifier le produit");
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        TextField nameField = new TextField();
        TextArea descArea = new TextArea(); descArea.setPrefRowCount(3);
        TextField priceField = new TextField();
        TextField stockField = new TextField();
        TextField imageField = new TextField();
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
        CheckBox availableCheck = new CheckBox("Disponible");
        Label errorLabel = new Label(); errorLabel.setStyle("-fx-text-fill: red;");
        if (product != null) {
            nameField.setText(product.getName());
            descArea.setText(product.getDescription());
            priceField.setText(String.valueOf(product.getPrice()));
            stockField.setText(String.valueOf(product.getStock()));
            imageField.setText(product.getImageUrl());
            categoryCombo.setValue(product.getCategory());
            availableCheck.setSelected(product.isAvailable());
        } else { categoryCombo.setValue("Accessoires"); availableCheck.setSelected(true); }
        grid.add(new Label("Nom:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descArea, 1, 1);
        grid.add(new Label("Prix:"), 0, 2); grid.add(priceField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3); grid.add(stockField, 1, 3);
        grid.add(new Label("Image URL:"), 0, 4); grid.add(imageField, 1, 4);
        grid.add(new Label("Catégorie:"), 0, 5); grid.add(categoryCombo, 1, 5);
        grid.add(availableCheck, 1, 6);
        grid.add(errorLabel, 1, 7);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #2c3e50;");
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String name = nameField.getText().trim();
                String desc = descArea.getText().trim();
                String priceStr = priceField.getText().trim();
                String stockStr = stockField.getText().trim();
                if (name.isEmpty()) { errorLabel.setText("Nom requis"); return null; }
                if (desc.isEmpty()) { errorLabel.setText("Description requise"); return null; }
                double price; int stock;
                try { price = Double.parseDouble(priceStr); if (price <= 0) throw new Exception(); }
                catch (Exception e) { errorLabel.setText("Prix invalide"); return null; }
                try { stock = Integer.parseInt(stockStr); if (stock < 1 || stock > 1000) throw new Exception(); }
                catch (Exception e) { errorLabel.setText("Stock invalide (1-1000)"); return null; }
                if (product == null) {
                    return new Product(name, desc, price, stock, categoryCombo.getValue(), imageField.getText().trim());
                } else {
                    product.setName(name); product.setDescription(desc); product.setPrice(price);
                    product.setStock(stock); product.setCategory(categoryCombo.getValue());
                    product.setImageUrl(imageField.getText().trim()); product.setAvailable(availableCheck.isSelected());
                    return product;
                }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(result -> {
            try {
                if (product == null) productService.insert(result);
                else productService.update(result);
                loadProducts(); updateProduitsStats();
                showAlert("Succès", "Produit sauvegardé !");
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    // ==================== COMMANDES ====================
    private void loadOrders() {
        try {
            orderList.setAll(orderService.getAllOrders());
            ordersTable.setItems(orderList);
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les commandes: " + e.getMessage());
        }
    }

    private void filterOrders() {
        String search = orderSearchField.getText().toLowerCase();
        String status = orderStatusFilter.getValue();
        ObservableList<Order> filtered = FXCollections.observableArrayList();
        for (Order o : orderList) {
            boolean matchSearch = search.isEmpty() || String.valueOf(o.getId()).contains(search);
            boolean matchStatus = status.equals("Tous") || o.getStatus().equals(status);
            if (matchSearch && matchStatus) filtered.add(o);
        }
        ordersTable.setItems(filtered);
    }

    private void showOrderDetails(Order order) {
        if (order == null) {
            orderDetailsLabel.setText("Sélectionnez une commande");
            orderItemsList.getItems().clear();
            if (confirmOrderBtn != null) confirmOrderBtn.setDisable(true);
            if (shipOrderBtn != null) shipOrderBtn.setDisable(true);
            if (deliverOrderBtn != null) deliverOrderBtn.setDisable(true);
            if (cancelOrderBtn != null) cancelOrderBtn.setDisable(true);
            return;
        }
        orderDetailsLabel.setText(String.format("Commande #%d - Total: %.2f €", order.getId(), order.getTotalAmount()));
        orderItemsList.getItems().clear();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String productName = item.getProduct() != null ? item.getProduct().getName() : "Produit #" + item.getProductId();
                orderItemsList.getItems().add(String.format("• %s x%d = %.2f €", productName, item.getQuantity(), item.getSubtotal()));
            }
        }
        String status = order.getStatus();
        if (confirmOrderBtn != null) confirmOrderBtn.setDisable(!status.equals("PENDING"));
        if (shipOrderBtn != null) shipOrderBtn.setDisable(!status.equals("CONFIRMED"));
        if (deliverOrderBtn != null) deliverOrderBtn.setDisable(!status.equals("SHIPPED"));
        if (cancelOrderBtn != null) cancelOrderBtn.setDisable(!status.equals("PENDING"));
    }

    @FXML private void handleConfirmOrder() { updateOrderStatus("CONFIRMED"); }
    @FXML private void handleShipOrder() { updateOrderStatus("SHIPPED"); }
    @FXML private void handleDeliverOrder() { updateOrderStatus("DELIVERED"); }
    @FXML private void handleCancelOrder() { updateOrderStatus("CANCELLED"); }

    private void updateOrderStatus(String status) {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            orderService.updateOrderStatus(selected.getId(), status);
            loadOrders(); updateProduitsStats();
            showAlert("Succès", "Commande #" + selected.getId() + " : " + getStatusFrench(status));
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML private void handleDeleteOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Supprimer la commande #" + selected.getId() + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try { orderService.deleteOrder(selected.getId()); loadOrders(); updateProduitsStats();
                showAlert("Succès", "Commande supprimée !");
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        }
    }
    // ==================== PRÉDICTION DES VENTES ====================

    /**
     * Affiche le dialogue de prédiction pour un produit
     */
    private void showPredictionDialog(Product product) {
        // Créer le dialogue
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(" Prédiction des ventes - " + product.getName());
        dialog.setHeaderText("Prévisions de ventes basées sur l'IA");

        // ComboBox pour le mois
        ComboBox<String> monthCombo = new ComboBox<>();
        monthCombo.getItems().addAll("Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre");
        monthCombo.setValue(getCurrentMonthName());
        monthCombo.setStyle("-fx-background-radius: 20; -fx-padding: 5 10;");

        // Label pour le résultat
        Label resultLabel = new Label("Sélectionnez un mois pour voir la prédiction");
        resultLabel.setWrapText(true);
        resultLabel.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        // Progress indicator
        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setMaxSize(40, 40);

        HBox progressBox = new HBox(progress);
        progressBox.setAlignment(javafx.geometry.Pos.CENTER);
        progressBox.setVisible(false);

        // Conteneur principal
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
                new Label(" Sélectionnez le mois à prédire :"),
                monthCombo,
                new Separator(),
                resultLabel,
                progressBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(400);

        // Écouter le changement de mois
        monthCombo.valueProperty().addListener((obs, old, val) -> {
            int monthIndex = monthCombo.getSelectionModel().getSelectedIndex() + 1;
            loadPrediction(product.getId(), monthIndex, resultLabel, progressBox);
        });

        // Charger la prédiction initiale
        loadPrediction(product.getId(), getCurrentMonth(), resultLabel, progressBox);

        dialog.showAndWait();
    }

    /**
     * Charge la prédiction depuis l'API
     */
    private void loadPrediction(int productId, int month, Label resultLabel, HBox progressBox) {
        progressBox.setVisible(true);
        resultLabel.setText("Chargement en cours...");
        resultLabel.setStyle("-fx-text-fill: #7f8c8d;");

        new Thread(() -> {
            try {
                // Appel à l'API Python
                java.net.URL url = new java.net.URL("http://localhost:5002/predict/" + productId + "/" + month);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String json = response.toString();
                    System.out.println(" API Response: " + json);

                    // Extraire les données du JSON
                    double predictedSales = extractJsonValue(json, "predicted_sales");
                    String confidence = extractJsonString(json, "confidence");
                    String trend = extractJsonString(json, "trend");
                    String monthName = extractJsonString(json, "month_name");

                    double estimatedRevenue = predictedSales * getProductPrice(productId);

                    final String finalText = String.format(
                            " Ventes prédites : %.0f unités\n" +
                                    " Chiffre d'affaires estimé : %.2f D\n" +
                                    " Confiance : %s\n" +
                                    " Tendance : %s",
                            predictedSales, estimatedRevenue, confidence, trend
                    );

                    Platform.runLater(() -> {
                        progressBox.setVisible(false);
                        resultLabel.setText(finalText);
                        resultLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                    });
                } else {
                    Platform.runLater(() -> {
                        progressBox.setVisible(false);
                        resultLabel.setText("❌ Service IA indisponible (Code: " + responseCode + ")");
                        resultLabel.setStyle("-fx-text-fill: red;");
                    });
                }
                conn.disconnect();

            } catch (Exception e) {
                System.err.println("❌ Erreur API: " + e.getMessage());
                Platform.runLater(() -> {
                    progressBox.setVisible(false);
                    resultLabel.setText("❌ Impossible de contacter le service de prédiction.\nVérifiez que l'API Flask est démarrée sur le port 5002.");
                    resultLabel.setStyle("-fx-text-fill: red;");
                });
            }
        }).start();
    }

    /**
     * Extrait une valeur numérique du JSON
     */
    private double extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int startIndex = json.indexOf(search);
        if (startIndex != -1) {
            startIndex += search.length();
            int endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = json.indexOf("}", startIndex);
            if (endIndex == -1) endIndex = json.length();
            try {
                return Double.parseDouble(json.substring(startIndex, endIndex).trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Extrait une chaîne du JSON
     */
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int startIndex = json.indexOf(search);
        if (startIndex != -1) {
            startIndex += search.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex != -1) {
                return json.substring(startIndex, endIndex);
            }
        }
        return "N/A";
    }

    /**
     * Retourne le mois actuel (1-12)
     */
    private int getCurrentMonth() {
        return java.time.LocalDate.now().getMonthValue();
    }

    /**
     * Retourne le nom du mois actuel
     */
    private String getCurrentMonthName() {
        String[] months = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        return months[getCurrentMonth() - 1];
    }

    /**
     * Récupère le prix d'un produit par son ID
     */
    private double getProductPrice(int productId) {
        try {
            for (Product p : productList) {
                if (p.getId() == productId) {
                    return p.getPrice();
                }
            }
            Product p = productService.getById(productId);
            return p != null ? p.getPrice() : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    // ==================== COMMON ====================
    @FXML private void handleRefresh() { loadUsers(); loadProducts(); loadOrders(); updateStats(); updateProduitsStats(); updateCharts(); showAlert("Actualisation", "✅ Données actualisées !"); }

    @FXML private void handleLogout() {
        SessionManager.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null) {
            welcomeLabel.setText(" " + user.getFullName());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    // ==================== MÉTHODES USER MANAGEMENT ====================

    private void showAdminDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un administrateur");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 15;");

        Label titleLabel = new Label("➕ Créer un nouvel administrateur");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        GridPane.setColumnSpan(titleLabel, 2);
        grid.add(titleLabel, 0, 0);

        TextField usernameField = new TextField();
        usernameField.setPromptText("ex: mohamed ben mohamed");
        usernameField.setPrefHeight(45);
        usernameField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        TextField emailField = new TextField();
        emailField.setPromptText("ex: mohamed@museum.com");
        emailField.setPrefHeight(45);
        emailField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("ex: Mohamed Ben Mohamed");
        fullNameField.setPrefHeight(45);
        fullNameField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Minimum 6 caractères");
        passwordField.setPrefHeight(45);
        passwordField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.setPrefHeight(45);
        confirmPasswordField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");

        Label usernameError = new Label();
        usernameError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-padding: 5 0 0 0;");
        usernameError.setVisible(false);

        Label emailError = new Label();
        emailError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-padding: 5 0 0 0;");
        emailError.setVisible(false);

        Label passwordError = new Label();
        passwordError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px; -fx-padding: 5 0 0 0;");
        passwordError.setVisible(false);

        Label usernameLabel = new Label("Nom d'utilisateur");
        usernameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label emailLabel = new Label("Adresse email");
        emailLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label fullNameLabel = new Label("Nom complet");
        fullNameLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label passwordLabel = new Label("Mot de passe");
        passwordLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label confirmLabel = new Label("Confirmation");
        confirmLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");

        int row = 1;
        grid.add(usernameLabel, 0, row);
        grid.add(usernameField, 1, row);
        grid.add(usernameError, 1, ++row);
        row++;

        grid.add(emailLabel, 0, row);
        grid.add(emailField, 1, row);
        grid.add(emailError, 1, ++row);
        row++;

        grid.add(fullNameLabel, 0, row);
        grid.add(fullNameField, 1, row);
        row++;

        grid.add(passwordLabel, 0, row);
        grid.add(passwordField, 1, row);
        grid.add(passwordError, 1, ++row);
        row++;

        grid.add(confirmLabel, 0, row);
        grid.add(confirmPasswordField, 1, row);
        row++;

        Label infoLabel = new Label("⚠️ Seuls les administrateurs peuvent être créés depuis cette interface.");
        infoLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-size: 11px; -fx-padding: 15 0 0 0;");
        GridPane.setColumnSpan(infoLabel, 2);
        grid.add(infoLabel, 0, row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Créer l'administrateur");
        okButton.setStyle("-fx-background-color: #d4af37; -fx-background-radius: 8; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");
        okButton.setDisable(true);

        addFocusEffect(usernameField);
        addFocusEffect(emailField);
        addFocusEffect(fullNameField);
        addFocusEffect(passwordField);
        addFocusEffect(confirmPasswordField);

        Runnable validate = () -> {
            boolean isValid = true;

            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                usernameError.setText("⚠ Nom d'utilisateur requis");
                usernameError.setVisible(true);
                isValid = false;
            } else if (username.length() < 3) {
                usernameError.setText("⚠ Minimum 3 caractères");
                usernameError.setVisible(true);
                isValid = false;
            } else {
                try {
                    if (userService.isUsernameTaken(username)) {
                        usernameError.setText("⚠ Ce nom d'utilisateur est déjà pris");
                        usernameError.setVisible(true);
                        isValid = false;
                    } else {
                        usernameError.setVisible(false);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                emailError.setText("⚠ Email requis");
                emailError.setVisible(true);
                isValid = false;
            } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                emailError.setText("⚠ Format d'email invalide");
                emailError.setVisible(true);
                isValid = false;
            } else {
                try {
                    if (userService.isEmailTaken(email)) {
                        emailError.setText("⚠ Cet email est déjà utilisé");
                        emailError.setVisible(true);
                        isValid = false;
                    } else {
                        emailError.setVisible(false);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            String password = passwordField.getText();
            String confirm = confirmPasswordField.getText();

            if (password.isEmpty()) {
                passwordError.setText("⚠ Mot de passe requis");
                passwordError.setVisible(true);
                isValid = false;
            } else if (password.length() < 6) {
                passwordError.setText("⚠ Minimum 6 caractères");
                passwordError.setVisible(true);
                isValid = false;
            } else if (!password.equals(confirm)) {
                passwordError.setText("⚠ Les mots de passe ne correspondent pas");
                passwordError.setVisible(true);
                isValid = false;
            } else {
                passwordError.setVisible(false);
            }

            okButton.setDisable(!isValid);
        };

        usernameField.textProperty().addListener((obs, old, val) -> validate.run());
        emailField.textProperty().addListener((obs, old, val) -> validate.run());
        passwordField.textProperty().addListener((obs, old, val) -> validate.run());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> validate.run());

        validate.run();

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                User newAdmin = new User();
                newAdmin.setUsername(usernameField.getText().trim());
                newAdmin.setEmail(emailField.getText().trim());
                newAdmin.setFullName(fullNameField.getText().trim());
                newAdmin.setPassword(passwordField.getText());
                newAdmin.setRole("ADMIN");
                newAdmin.setActive(true);
                return newAdmin;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                userService.insert(result);
                loadUsers();
                updateStats();
                updateCharts();
                showAlert("Succès", "✅ Administrateur ajouté avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "❌ Impossible d'ajouter l'administrateur: " + e.getMessage());
            }
        });
    }

    private void showEditUserDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'utilisateur");
        dialog.setHeaderText("✏️ Modifier les informations de " + user.getUsername());

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 20, 20, 20));

        TextField usernameField = new TextField(user.getUsername());
        TextField emailField = new TextField(user.getEmail());
        TextField fullNameField = new TextField(user.getFullName());

        Label roleLabel = new Label(user.getRole());
        roleLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Laisser vide pour garder l'ancien");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le nouveau mot de passe");

        Label passwordError = new Label();
        passwordError.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        passwordError.setVisible(false);

        int row = 0;
        grid.add(new Label("Nom d'utilisateur:"), 0, row);
        grid.add(usernameField, 1, row);
        row++;
        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row);
        row++;
        grid.add(new Label("Nom complet:"), 0, row);
        grid.add(fullNameField, 1, row);
        row++;
        grid.add(new Label("Rôle:"), 0, row);
        grid.add(roleLabel, 1, row);
        row++;

        Separator separator = new Separator();
        separator.setPrefWidth(400);
        grid.add(separator, 0, row, 2, 1);
        row++;

        grid.add(new Label("Nouveau mot de passe:"), 0, row);
        grid.add(newPasswordField, 1, row);
        row++;
        grid.add(new Label("Confirmer:"), 0, row);
        grid.add(confirmPasswordField, 1, row);
        grid.add(passwordError, 1, ++row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        Runnable validatePassword = () -> {
            String newPass = newPasswordField.getText();
            String confirm = confirmPasswordField.getText();

            if (!newPass.isEmpty() && !newPass.equals(confirm)) {
                passwordError.setText("Les mots de passe ne correspondent pas");
                passwordError.setVisible(true);
                okButton.setDisable(true);
            } else if (!newPass.isEmpty() && newPass.length() < 6) {
                passwordError.setText("Minimum 6 caractères");
                passwordError.setVisible(true);
                okButton.setDisable(true);
            } else {
                passwordError.setVisible(false);
                okButton.setDisable(false);
            }
        };

        newPasswordField.textProperty().addListener((obs, old, val) -> validatePassword.run());
        confirmPasswordField.textProperty().addListener((obs, old, val) -> validatePassword.run());

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                user.setUsername(usernameField.getText().trim());
                user.setEmail(emailField.getText().trim());
                user.setFullName(fullNameField.getText().trim());

                String newPassword = newPasswordField.getText();
                if (!newPassword.isEmpty()) {
                    user.setPassword(newPassword);
                }
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                userService.update(result);
                String newPassword = newPasswordField.getText();
                if (!newPassword.isEmpty()) {
                    userService.updatePassword(result.getId(), newPassword);
                }
                loadUsers();
                updateStats();
                updateCharts();
                showAlert("Succès", "Utilisateur modifié avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de modifier: " + e.getMessage());
            }
        });
    }

    private void deleteSelectedUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un utilisateur à supprimer");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + selected.getFullName() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                userService.delete(selected.getId());
                loadUsers();
                updateStats();
                updateCharts();
                showAlert("Succès", "Utilisateur supprimé avec succès!");
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    private void toggleUserStatus() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Veuillez sélectionner un utilisateur");
            return;
        }

        try {
            if (selected.isActive()) {
                userService.deactivate(selected.getId());
                showAlert("Succès", "Utilisateur désactivé avec succès!");
            } else {
                userService.activate(selected.getId());
                showAlert("Succès", "Utilisateur activé avec succès!");
            }
            loadUsers();
            updateStats();
            updateCharts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de modifier le statut: " + e.getMessage());
        }
    }

    private void exportUsersPDF() {
        try {
            ObservableList<User> usersToExport = usersTable.getItems();
            if (usersToExport.isEmpty()) {
                showAlert("Information", "Aucun utilisateur à exporter.");
                return;
            }
            boolean success = PDFExporter.exportUsersToPDF(usersToExport, (Stage) usersTable.getScene().getWindow());
            if (success) {
                showAlert("Succès", "✅ Le fichier PDF a été généré avec succès !");
            } else {
                showAlert("Erreur", "❌ L'exportation a été annulée.");
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'exportation: " + e.getMessage());
        }
    }

    private void exportStatsPDF() {
        try {
            int totalUsers = Integer.parseInt(statsTotalUsers.getText());
            int activeUsers = Integer.parseInt(statsActiveUsers.getText());
            int admins = Integer.parseInt(statsAdmins.getText());
            boolean success = PDFExporter.exportStatsToPDF(totalUsers, activeUsers, admins, (Stage) usersTable.getScene().getWindow());
            if (success) {
                showAlert("Succès", "✅ Les statistiques ont été exportées avec succès !");
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'exportation: " + e.getMessage());
        }
    }





    private void showInactiveUsersDialog() {
        try {
            ObservableList<User> inactiveUsers = FXCollections.observableArrayList();
            for (User user : userList) {
                if (!user.isActive()) {
                    inactiveUsers.add(user);
                }
            }

            if (inactiveUsers.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Comptes désactivés");
                alert.setContentText(" Aucun compte désactivé pour le moment.");
                alert.showAndWait();
                return;
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Comptes désactivés");
            dialog.setHeaderText(" Liste des comptes désactivés (" + inactiveUsers.size() + ")");

            TableView<User> inactiveTable = new TableView<>();
            inactiveTable.setItems(inactiveUsers);
            inactiveTable.setPrefHeight(400);
            inactiveTable.setStyle("-fx-background-radius: 10;");

            TableColumn<User, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            idCol.setPrefWidth(60);

            TableColumn<User, String> usernameCol = new TableColumn<>("Nom d'utilisateur");
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
            usernameCol.setPrefWidth(150);

            TableColumn<User, String> emailCol = new TableColumn<>("Email");
            emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
            emailCol.setPrefWidth(220);

            TableColumn<User, String> fullNameCol = new TableColumn<>("Nom complet");
            fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
            fullNameCol.setPrefWidth(180);

            TableColumn<User, String> roleCol = new TableColumn<>("Rôle");
            roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
            roleCol.setPrefWidth(100);

            inactiveTable.getColumns().addAll(idCol, usernameCol, emailCol, fullNameCol, roleCol);

            Button reactivateButton = new Button(" Réactiver le compte");
            reactivateButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 10 20; -fx-cursor: hand;");
            reactivateButton.setOnAction(e -> {
                User selected = inactiveTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setContentText("Réactiver le compte de " + selected.getFullName() + " ?");
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        try {
                            userService.activate(selected.getId());
                            loadUsers();
                            updateStats();
                            updateCharts();
                            showAlert("Succès", "✅ Compte réactivé !");
                            dialog.close();
                        } catch (SQLException ex) {
                            showAlert("Erreur", ex.getMessage());
                        }
                    }
                } else {
                    showAlert("Information", "Sélectionnez un compte à réactiver.");
                }
            });

            VBox content = new VBox(15);
            content.setStyle("-fx-padding: 20;");
            content.getChildren().addAll(inactiveTable, reactivateButton);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(750);
            dialog.getDialogPane().setPrefHeight(550);

            dialog.showAndWait();

        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'afficher les comptes désactivés: " + e.getMessage());
        }
    }

    private void addFocusEffect(Control control) {
        control.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                control.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #d4af37; -fx-border-radius: 10; -fx-border-width: 2; -fx-padding: 0 15; -fx-font-size: 14px;");
            } else {
                control.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10; -fx-padding: 0 15; -fx-font-size: 14px;");
            }
        });
    }
    private void showFraudDetection() {
        // Créer un dialogue de chargement avec style amélioré
        Dialog<Void> loadingDialog = new Dialog<>();
        loadingDialog.setTitle("🔐 Détection de fraude");
        loadingDialog.setHeaderText(null);
        loadingDialog.initModality(Modality.WINDOW_MODAL);
        loadingDialog.initOwner(usersTable.getScene().getWindow());

        // Appliquer les styles CSS
        loadingDialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );
        loadingDialog.getDialogPane().getStyleClass().add("loading-dialog");

        // Contenu du dialogue de chargement
        VBox loadingContent = new VBox(25);
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 20; -fx-padding: 40;");

        // Icône animée
        Label iconLabel = new Label("🔍");
        iconLabel.setStyle("-fx-font-size: 55px; -fx-animation: pulse 1.5s infinite;");

        // Texte principal
        Label titleLabel = new Label("Analyse des comptes en cours");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Sous-titre
        Label subtitleLabel = new Label("Cela peut prendre quelques secondes...");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        // ProgressIndicator amélioré
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(70, 70);
        progressIndicator.setStyle("-fx-progress-color: #d4af37;");

        // Label de progression
        Label progressLabel = new Label("0 compte(s) suspect(s) trouvé(s)");
        progressLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #d4af37;");

        // Points d'attente animés
        HBox dotsBox = new HBox(8);
        dotsBox.setAlignment(Pos.CENTER);
        dotsBox.setStyle("-fx-padding: 10 0 0 0;");
        for (int i = 0; i < 3; i++) {
            Region dot = new Region();
            dot.setStyle("-fx-background-color: #d4af37; -fx-background-radius: 5; -fx-pref-width: 8; -fx-pref-height: 8;");
            dotsBox.getChildren().add(dot);
        }

        loadingContent.getChildren().addAll(
                iconLabel, titleLabel, subtitleLabel,
                progressIndicator, progressLabel, dotsBox
        );

        loadingDialog.getDialogPane().setContent(loadingContent);
        loadingDialog.getDialogPane().setPrefWidth(450);
        loadingDialog.getDialogPane().setPrefHeight(400);
        loadingDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Button cancelButton = (Button) loadingDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Annuler");
        cancelButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 25; -fx-font-weight: bold; -fx-cursor: hand;");
        cancelButton.setAlignment(Pos.CENTER);

        // Ajouter une barre d'espacement
        VBox.setMargin(cancelButton, new Insets(10, 0, 0, 0));

        // Copie de la liste utilisateurs
        List<User> usersToAnalyze = new ArrayList<>(userList);

        Thread analysisThread = new Thread(() -> {
            try {
                List<FraudDetectionService.FraudReport> reports = new ArrayList<>();

                for (User user : usersToAnalyze) {
                    if (Thread.currentThread().isInterrupted()) return;

                    FraudDetectionService.FraudReport report = FraudDetectionService.getDetailedReport(user);
                    if (report.isFraudulent()) {
                        reports.add(report);
                    }

                    final int current = reports.size();
                    Platform.runLater(() -> {
                        progressLabel.setText("🔍 " + current + " compte(s) suspect(s) trouvé(s)");
                    });
                }

                final List<FraudDetectionService.FraudReport> finalReports = new ArrayList<>(reports);

                Platform.runLater(() -> {
                    loadingDialog.close();
                    Platform.runLater(() -> showFraudResults(finalReports));
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingDialog.close();
                    showAlert("Erreur", "Erreur lors de la détection: " + e.getMessage());
                });
                e.printStackTrace();
            }
        });

        cancelButton.setOnAction(e -> {
            analysisThread.interrupt();
            loadingDialog.close();
        });

        analysisThread.start();
        loadingDialog.showAndWait();
    }

    /**
     * Affiche les résultats de la détection de fraude - VERSION SIMPLIFIÉE
     */
    private void showFraudResults(List<FraudDetectionService.FraudReport> reports) {
        System.out.println("📊 showFraudResults - Nombre de rapports: " + reports.size());

        if (reports == null || reports.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("🔐 Détection de fraude");
            alert.setHeaderText("Résultat de l'analyse");
            alert.setContentText("✅ Aucun compte suspect détecté !");
            alert.showAndWait();
            return;
        }

        // VERSION SIMPLIFIÉE - Utiliser un VBox simple avec ScrollPane
        Stage resultStage = new Stage();
        resultStage.setTitle("🔐 Détection de fraude - " + reports.size() + " compte(s) suspect(s)");
        resultStage.initModality(Modality.WINDOW_MODAL);
        resultStage.initOwner(usersTable.getScene().getWindow());

        VBox mainContainer = new VBox(10);
        mainContainer.setStyle("-fx-background-color: #F8F5F0; -fx-padding: 15;");

        // En-tête
        Label headerLabel = new Label("🚨 " + reports.size() + " compte(s) suspect(s) détecté(s)");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");
        mainContainer.getChildren().add(headerLabel);

        // ScrollPane pour les cartes
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox cardsContainer = new VBox(15);
        cardsContainer.setStyle("-fx-padding: 10;");

        for (FraudDetectionService.FraudReport report : reports) {
            VBox card = createSimpleCard(report);
            cardsContainer.getChildren().add(card);
        }

        scrollPane.setContent(cardsContainer);
        mainContainer.getChildren().add(scrollPane);

        // Bouton Fermer
        Button closeButton = new Button("Fermer");
        closeButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-cursor: hand;");
        closeButton.setOnAction(e -> resultStage.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 10 0 0 0;");
        mainContainer.getChildren().add(buttonBox);

        Scene scene = new Scene(mainContainer, 700, 650);
        resultStage.setScene(scene);
        resultStage.showAndWait();
    }

    /**
     * Crée une carte simple pour un compte suspect - VERSION TRÈS SIMPLE
     */
    private VBox createSimpleCard(FraudDetectionService.FraudReport report) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1;");

        // Ligne 1: Nom d'utilisateur + Score
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label usernameLabel = new Label("👤 " + report.getUsername());
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int score = report.getRiskScore();
        String scoreColor = score >= 70 ? "#e74c3c" : (score >= 40 ? "#f39c12" : "#27ae60");
        Label scoreLabel = new Label(score + "%");
        scoreLabel.setStyle("-fx-background-color: " + scoreColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 12px;");

        headerRow.getChildren().addAll(usernameLabel, spacer, scoreLabel);
        card.getChildren().add(headerRow);

        // Ligne 2: Email
        HBox emailRow = new HBox(5);
        emailRow.setAlignment(Pos.CENTER_LEFT);
        Label emailIcon = new Label("📧");
        emailIcon.setStyle("-fx-font-size: 12px;");
        Label emailLabel = new Label(report.getEmail());
        emailLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        emailRow.getChildren().addAll(emailIcon, emailLabel);
        card.getChildren().add(emailRow);

        // Ligne 3: Niveau de risque
        HBox riskRow = new HBox(5);
        riskRow.setAlignment(Pos.CENTER_LEFT);
        Label riskIcon = new Label("⚠️");
        riskIcon.setStyle("-fx-font-size: 12px;");
        Label riskLabel = new Label(report.getRiskLevelLabel());
        riskLabel.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        riskRow.getChildren().addAll(riskIcon, riskLabel);
        card.getChildren().add(riskRow);

        // Drapeaux rouges (si présents)
        if (report.getRedFlags() != null && !report.getRedFlags().isEmpty()) {
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #e0e0e0;");
            card.getChildren().add(sep);

            Label flagsTitle = new Label("🚩 Drapeaux rouges :");
            flagsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            card.getChildren().add(flagsTitle);

            for (String flag : report.getRedFlags()) {
                Label flagLabel = new Label("• " + flag);
                flagLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
                card.getChildren().add(flagLabel);
            }
        }

        // Boutons d'action
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setStyle("-fx-padding: 10 0 0 0;");

        Button viewBtn = new Button("👁️ Voir");
        viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 15; -fx-cursor: hand; -fx-font-size: 11px;");
        viewBtn.setOnAction(e -> {
            userList.stream().filter(u -> u.getId() == report.getId()).findFirst().ifPresent(selected -> {
                usersTable.getSelectionModel().select(selected);
                usersTable.scrollTo(selected);
                Stage stage = (Stage) viewBtn.getScene().getWindow();
                stage.close();
                showUsersPanel();
            });
        });

        Button disableBtn = new Button("🔒 Désactiver");
        disableBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 5 15; -fx-cursor: hand; -fx-font-size: 11px;");
        disableBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setContentText("Désactiver le compte " + report.getUsername() + " ?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    userService.deactivate(report.getId());
                    loadUsers();
                    updateStats();
                    updateCharts();
                    showAlert("Succès", "✅ Compte désactivé !");
                    Stage stage = (Stage) disableBtn.getScene().getWindow();
                    stage.close();
                } catch (SQLException ex) {
                    showAlert("Erreur", ex.getMessage());
                }
            }
        });

        actionRow.getChildren().addAll(viewBtn, disableBtn);
        card.getChildren().add(actionRow);

        return card;
    }
}

