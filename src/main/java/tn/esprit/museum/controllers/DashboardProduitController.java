package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Order;
import tn.esprit.museum.entities.OrderItem;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.OrderService;
import tn.esprit.museum.services.ProductService;
import tn.esprit.museum.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DashboardProduitController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> productIdColumn;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, String> productDescColumn;
    @FXML private TableColumn<Product, Double> productPriceColumn;
    @FXML private TableColumn<Product, Integer> productStockColumn;
    @FXML private TableColumn<Product, String> productCategoryColumn;
    @FXML private TableColumn<Product, Boolean> productStatusColumn;

    @FXML private TextField productSearchField;
    @FXML private ComboBox<String> categoryFilter;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, Integer> orderUserIdColumn;
    @FXML private TableColumn<Order, String> orderDateColumn;
    @FXML private TableColumn<Order, Double> orderTotalColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;
    @FXML private TableColumn<Order, String> orderAddressColumn;
    @FXML private TableColumn<Order, String> orderPaymentColumn;

    @FXML private TextField orderSearchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label orderDetailsLabel;
    @FXML private ListView<String> orderItemsList;

    @FXML private Button confirmOrderBtn;
    @FXML private Button shipOrderBtn;
    @FXML private Button deliverOrderBtn;
    @FXML private Button cancelOrderBtn;

    @FXML private VBox productsPanel;
    @FXML private VBox ordersPanel;

    @FXML private Label welcomeLabel;
    @FXML private Label statsProducts;
    @FXML private Label statsOrders;
    @FXML private Label statsPending;
    @FXML private TableColumn<Product, Void> predictionColumn;

    private ProductService productService;
    private OrderService orderService;
    private ObservableList<Product> productList;
    private ObservableList<Order> orderList;

    @FXML
    public void initialize() {
        productService = new ProductService();
        orderService = new OrderService();
        productList = FXCollections.observableArrayList();
        orderList = FXCollections.observableArrayList();

        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        productDescColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        productPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        productStockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        productCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        productStatusColumn.setCellValueFactory(new PropertyValueFactory<>("available"));

        // ⭐ PERMETTRE LA SÉLECTION MULTIPLE POUR LES PRODUITS ⭐
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
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "✅ Disponible" : "❌ Indisponible");
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });

        categoryFilter.getItems().addAll("Tous", "Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
        categoryFilter.setValue("Tous");
        productSearchField.textProperty().addListener((obs, old, val) -> filterProducts());
        categoryFilter.valueProperty().addListener((obs, old, val) -> filterProducts());

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
                if (empty || item == null) {
                    setText(null);
                } else {
                    Order order = getTableView().getItems().get(getIndex());
                    setText(getStatusFrench(order.getStatus()));
                    if (order.getStatus().equals("DELIVERED")) {
                        setStyle("-fx-text-fill: green;");
                    } else if (order.getStatus().equals("PENDING")) {
                        setStyle("-fx-text-fill: orange;");
                    } else if (order.getStatus().equals("CANCELLED")) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("-fx-text-fill: #3498db;");
                    }
                }
            }
        });

        statusFilter.getItems().addAll("Tous", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED");
        statusFilter.setValue("Tous");
        orderSearchField.textProperty().addListener((obs, old, val) -> filterOrders());
        statusFilter.valueProperty().addListener((obs, old, val) -> filterOrders());

        ordersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> showOrderDetails(selected));

        loadProducts();
        loadOrders();
        updateStats();
        showProductsPanel();

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("👋 " + currentUser.getFullName());
        }
    }

    private String getStatusFrench(String status) {
        switch (status) {
            case "PENDING": return "⏳ En attente";
            case "CONFIRMED": return "✅ Confirmée";
            case "SHIPPED": return "📦 Expédiée";
            case "DELIVERED": return "🚚 Livrée";
            case "CANCELLED": return "❌ Annulée";
            default: return status;
        }
    }

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
            boolean matchSearch = search.isEmpty() ||
                    p.getName().toLowerCase().contains(search) ||
                    p.getDescription().toLowerCase().contains(search);
            boolean matchCategory = category.equals("Tous") || p.getCategory().equals(category);
            if (matchSearch && matchCategory) filtered.add(p);
        }
        productsTable.setItems(filtered);
    }

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    @FXML
    private void handleEditProduct() {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Erreur", "Sélectionnez un produit à modifier");
            return;
        }
        showProductDialog(selected);
    }

    @FXML
    private void handleDeleteProduct() {
        // ⭐ RÉCUPÉRER TOUS LES PRODUITS SÉLECTIONNÉS ⭐
        ObservableList<Product> selectedProducts = productsTable.getSelectionModel().getSelectedItems();

        if (selectedProducts == null || selectedProducts.isEmpty()) {
            showAlert("Erreur", "Veuillez sélectionner au moins un produit à supprimer");
            return;
        }

        // Construire la liste des noms pour le message
        String productNames = selectedProducts.stream()
                .map(Product::getName)
                .collect(Collectors.joining("\n• "));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer " + selectedProducts.size() + " produit(s)");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer les produits suivants ?\n\n• " + productNames);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int successCount = 0;
            int errorCount = 0;

            for (Product product : selectedProducts) {
                try {
                    productService.delete(product.getId());
                    successCount++;
                } catch (SQLException e) {
                    errorCount++;
                    System.err.println("Erreur lors de la suppression du produit ID " + product.getId() + ": " + e.getMessage());
                }
            }

            // Recharger la liste
            loadProducts();
            updateStats();

            // Afficher le résultat
            if (errorCount == 0) {
                showAlert("Succès", "✅ " + successCount + " produit(s) supprimé(s) avec succès !");
            } else {
                showAlert("Information", "⚠️ " + successCount + " produit(s) supprimé(s), " + errorCount + " erreur(s)");
            }
        }
    }

    // ==================== DIALOGUE PRODUIT AVEC VALIDATION ====================
    private void showProductDialog(Product product) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(product == null ? "Ajouter un produit" : "Modifier le produit");
        dialogStage.setResizable(false);

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: white;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Ex: Coffret Museum");
        nameField.setPrefWidth(350);

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description détaillée du produit...");
        descArea.setPrefRowCount(3);
        descArea.setPrefWidth(350);

        TextField priceField = new TextField();
        priceField.setPromptText("0.00");

        TextField stockField = new TextField();
        stockField.setPromptText("1-1000");

        TextField imageField = new TextField();
        imageField.setPromptText("/images/products/...");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");

        CheckBox availableCheck = new CheckBox("Disponible");

        Label nameError = new Label();
        nameError.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        nameError.setVisible(false);

        Label descError = new Label();
        descError.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        descError.setVisible(false);

        Label priceError = new Label();
        priceError.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        priceError.setVisible(false);

        Label stockError = new Label();
        stockError.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        stockError.setVisible(false);

        Label duplicateError = new Label();
        duplicateError.setStyle("-fx-text-fill: red; -fx-font-size: 12px; -fx-font-weight: bold;");
        duplicateError.setVisible(false);

        if (product != null) {
            nameField.setText(product.getName());
            descArea.setText(product.getDescription());
            priceField.setText(String.valueOf(product.getPrice()));
            stockField.setText(String.valueOf(product.getStock()));
            imageField.setText(product.getImageUrl());
            categoryCombo.setValue(product.getCategory());
            availableCheck.setSelected(product.isAvailable());
        } else {
            categoryCombo.setValue("Accessoires");
            availableCheck.setSelected(true);
        }

        int row = 0;
        grid.add(new Label("Nom du produit:*"), 0, row);
        grid.add(nameField, 1, row);
        grid.add(nameError, 1, ++row);
        row++;

        grid.add(new Label("Description:*"), 0, row);
        grid.add(descArea, 1, row);
        grid.add(descError, 1, ++row);
        row++;

        grid.add(new Label("Prix (€):*"), 0, row);
        grid.add(priceField, 1, row);
        grid.add(priceError, 1, ++row);
        row++;

        grid.add(new Label("Stock:*"), 0, row);
        grid.add(stockField, 1, row);
        grid.add(stockError, 1, ++row);
        row++;

        grid.add(new Label("Image URL:"), 0, row);
        grid.add(imageField, 1, row);
        row++;

        grid.add(new Label("Catégorie:"), 0, row);
        grid.add(categoryCombo, 1, row);
        row++;

        grid.add(availableCheck, 1, row);
        row++;

        grid.add(duplicateError, 1, row);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 5;");
        btnCancel.setOnAction(e -> dialogStage.close());

        Button btnSave = new Button("Enregistrer");
        btnSave.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #2c3e50; -fx-background-radius: 5; -fx-font-weight: bold;");

        UnaryOperator<TextFormatter.Change> priceFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*(\\.\\d{0,2})?")) return change;
            return null;
        };
        priceField.setTextFormatter(new TextFormatter<>(priceFilter));

        UnaryOperator<TextFormatter.Change> stockFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) return change;
            return null;
        };
        stockField.setTextFormatter(new TextFormatter<>(stockFilter));

        Runnable validate = () -> {
            boolean isValid = true;
            duplicateError.setVisible(false);

            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameError.setText("❌ Le nom est obligatoire");
                nameError.setVisible(true);
                isValid = false;
            } else if (name.length() > 100) {
                nameError.setText("❌ Ne doit pas dépasser 100 caractères");
                nameError.setVisible(true);
                isValid = false;
            } else {
                nameError.setVisible(false);
            }

            String desc = descArea.getText().trim();
            if (desc.isEmpty()) {
                descError.setText("❌ La description est obligatoire");
                descError.setVisible(true);
                isValid = false;
            } else if (desc.length() > 255) {
                descError.setText("❌ Ne doit pas dépasser 255 caractères");
                descError.setVisible(true);
                isValid = false;
            } else if (desc.split(" ").length > 50) {
                descError.setText("❌ Ne doit pas dépasser 50 mots");
                descError.setVisible(true);
                isValid = false;
            } else {
                descError.setVisible(false);
            }

            String priceStr = priceField.getText().trim();
            if (priceStr.isEmpty()) {
                priceError.setText("❌ Le prix est obligatoire");
                priceError.setVisible(true);
                isValid = false;
            } else {
                try {
                    double price = Double.parseDouble(priceStr);
                    if (price <= 0) {
                        priceError.setText("❌ Le prix doit être > 0");
                        priceError.setVisible(true);
                        isValid = false;
                    } else {
                        priceError.setVisible(false);
                    }
                } catch (NumberFormatException e) {
                    priceError.setText("❌ Prix invalide");
                    priceError.setVisible(true);
                    isValid = false;
                }
            }

            String stockStr = stockField.getText().trim();
            if (stockStr.isEmpty()) {
                stockError.setText("❌ Le stock est obligatoire");
                stockError.setVisible(true);
                isValid = false;
            } else {
                try {
                    int stock = Integer.parseInt(stockStr);
                    if (stock < 1) {
                        stockError.setText("❌ Le stock doit être ≥ 1");
                        stockError.setVisible(true);
                        isValid = false;
                    } else if (stock > 1000) {
                        stockError.setText("❌ Le stock ne doit pas dépasser 1000");
                        stockError.setVisible(true);
                        isValid = false;
                    } else {
                        stockError.setVisible(false);
                    }
                } catch (NumberFormatException e) {
                    stockError.setText("❌ Stock invalide");
                    stockError.setVisible(true);
                    isValid = false;
                }
            }

            btnSave.setDisable(!isValid);
        };

        nameField.textProperty().addListener((obs, old, val) -> validate.run());
        descArea.textProperty().addListener((obs, old, val) -> validate.run());
        priceField.textProperty().addListener((obs, old, val) -> validate.run());
        stockField.textProperty().addListener((obs, old, val) -> validate.run());

        validate.run();

        btnSave.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                String description = descArea.getText().trim();
                double price = Double.parseDouble(priceField.getText().trim());
                int stock = Integer.parseInt(stockField.getText().trim());

                if (product == null) {
                    if (productService.productExists(name, description, price)) {
                        duplicateError.setText("❌ Un produit avec le même nom, prix et description existe déjà !");
                        duplicateError.setVisible(true);
                        return;
                    }

                    Product newProduct = new Product(name, description, price, stock, categoryCombo.getValue(), imageField.getText().trim());
                    newProduct.setAvailable(availableCheck.isSelected());
                    productService.insert(newProduct);
                    showAlert("Succès", "✅ Produit ajouté avec succès !");
                    dialogStage.close();

                } else {
                    if (!product.getName().equals(name) ||
                            product.getPrice() != price ||
                            !product.getDescription().equals(description)) {

                        if (productService.productExists(name, description, price, product.getId())) {
                            duplicateError.setText("❌ Un produit avec le même nom, prix et description existe déjà !");
                            duplicateError.setVisible(true);
                            return;
                        }
                    }

                    product.setName(name);
                    product.setDescription(description);
                    product.setPrice(price);
                    product.setStock(stock);
                    product.setCategory(categoryCombo.getValue());
                    product.setImageUrl(imageField.getText().trim());
                    product.setAvailable(availableCheck.isSelected());
                    productService.update(product);
                    showAlert("Succès", "✅ Produit modifié avec succès !");
                    dialogStage.close();
                }
                loadProducts();
                updateStats();

            } catch (SQLException ex) {
                showAlert("Erreur", "❌ " + ex.getMessage());
            }
        });

        buttonBox.getChildren().addAll(btnCancel, btnSave);
        mainContainer.getChildren().addAll(grid, buttonBox);

        Scene scene = new Scene(mainContainer);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
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
        String status = statusFilter.getValue();

        ObservableList<Order> filtered = FXCollections.observableArrayList();
        for (Order o : orderList) {
            boolean matchSearch = search.isEmpty() || String.valueOf(o.getId()).contains(search);
            boolean matchStatus = status.equals("Tous") || o.getStatus().equals(status);
            if (matchSearch && matchStatus) filtered.add(o);
        }
        ordersTable.setItems(filtered);
    }
    public void setCurrentUser(User user) {
        // Pour passer l'utilisateur courant si nécessaire
        if (welcomeLabel != null) {
            welcomeLabel.setText(user.getFullName());
        }
    }

    private void showOrderDetails(Order order) {
        if (order == null) {
            orderDetailsLabel.setText("Sélectionnez une commande");
            orderItemsList.getItems().clear();
            confirmOrderBtn.setDisable(true);
            shipOrderBtn.setDisable(true);
            deliverOrderBtn.setDisable(true);
            cancelOrderBtn.setDisable(true);
            return;
        }

        orderDetailsLabel.setText(String.format("Commande #%d - Total: %.2f €",
                order.getId(), order.getTotalAmount()));

        orderItemsList.getItems().clear();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String productName = item.getProduct() != null ? item.getProduct().getName() : "Produit #" + item.getProductId();
                orderItemsList.getItems().add(
                        String.format("• %s x%d = %.2f €", productName, item.getQuantity(), item.getSubtotal()));
            }
        }

        String status = order.getStatus();
        confirmOrderBtn.setDisable(!status.equals("PENDING"));
        shipOrderBtn.setDisable(!status.equals("CONFIRMED"));
        deliverOrderBtn.setDisable(!status.equals("SHIPPED"));
        cancelOrderBtn.setDisable(!status.equals("PENDING"));
    }

    @FXML
    private void handleConfirmOrder() {
        updateOrderStatus("CONFIRMED");
    }

    @FXML
    private void handleShipOrder() {
        updateOrderStatus("SHIPPED");
    }

    @FXML
    private void handleDeliverOrder() {
        updateOrderStatus("DELIVERED");
    }

    @FXML
    private void handleCancelOrder() {
        updateOrderStatus("CANCELLED");
    }

    private void updateOrderStatus(String status) {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            orderService.updateOrderStatus(selected.getId(), status);
            loadOrders();
            updateStats();
            showAlert("Succès", "Commande #" + selected.getId() + " : " + getStatusFrench(status));
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la commande #" + selected.getId() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                orderService.deleteOrder(selected.getId());
                loadOrders();
                updateStats();
                showAlert("Succès", "Commande supprimée !");
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    public void showProductsPanel() {
        productsPanel.setVisible(true);
        productsPanel.setManaged(true);
        ordersPanel.setVisible(false);
        ordersPanel.setManaged(false);
    }

    @FXML
    public void showOrdersPanel() {
        productsPanel.setVisible(false);
        productsPanel.setManaged(false);
        ordersPanel.setVisible(true);
        ordersPanel.setManaged(true);
        loadOrders();
    }

    @FXML
    private void handleRefresh() {
        loadProducts();
        loadOrders();
        updateStats();
        showAlert("Actualisation", "✅ Données actualisées !");
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - Museum Digital");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        try {
            int products = productService.getAll().size();
            List<Order> orders = orderService.getAllOrders();
            int totalOrders = orders.size();
            long pending = orders.stream().filter(o -> o.getStatus().equals("PENDING")).count();

            statsProducts.setText("📦 Produits: " + products);
            statsOrders.setText("📋 Commandes: " + totalOrders);
            statsPending.setText("⏳ En attente: " + pending);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
