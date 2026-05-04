package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.services.ProductService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductsViewController {

    @FXML private FlowPane productsContainer;
    @FXML private Label cartBadge;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;

    private ProductService productService;
    private List<Product> allProducts;
    private List<Product> cart;
    private List<Integer> cartQuantities;

    @FXML
    public void initialize() {
        productService = new ProductService();
        allProducts = new ArrayList<>();
        cart = new ArrayList<>();
        cartQuantities = new ArrayList<>();

        categoryFilter.getItems().addAll("Tous", "Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
        categoryFilter.setValue("Tous");

        searchField.textProperty().addListener((obs, old, val) -> filterProducts());
        categoryFilter.valueProperty().addListener((obs, old, val) -> filterProducts());

        loadProducts();
        updateCartBadge();
    }

    private void loadProducts() {
        try {
            allProducts = productService.getAvailableProducts();
            filterProducts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private void filterProducts() {
        String search = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            boolean matchSearch = search.isEmpty() ||
                    p.getName().toLowerCase().contains(search) ||
                    p.getDescription().toLowerCase().contains(search);
            boolean matchCategory = category.equals("Tous") || p.getCategory().equals(category);
            if (matchSearch && matchCategory) filtered.add(p);
        }
        displayProducts(filtered);
    }

    private void displayProducts(List<Product> products) {
        productsContainer.getChildren().clear();
        for (Product product : products) {
            productsContainer.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(280);
        card.setPrefHeight(450);
        card.setAlignment(Pos.TOP_CENTER);

        // Conteneur pour centrer l'image
        HBox imageContainer = new HBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-padding: 5 0;");

        // Image avec bords arrondis
        ImageView productImage = new ImageView();
        productImage.setFitWidth(220);
        productImage.setFitHeight(140);
        productImage.setPreserveRatio(false);

        Rectangle clip = new Rectangle(220, 140);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        productImage.setClip(clip);

        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                if (imageUrl.startsWith("/")) {
                    productImage.setImage(new Image(getClass().getResource(imageUrl).toExternalForm(), 220, 140, false, true));
                } else {
                    productImage.setImage(new Image(imageUrl, 220, 140, false, true));
                }
            } catch (Exception e) {
                productImage.setImage(new Image("https://picsum.photos/id/20/220/140", 220, 140, false, true));
            }
        } else {
            productImage.setImage(new Image("https://picsum.photos/id/20/220/140", 220, 140, false, true));
        }

        imageContainer.getChildren().add(productImage);

        // ⭐ Nom du produit en GRAS et MAJUSCULES ⭐
        Label nameLabel = new Label(product.getName().toUpperCase());
        nameLabel.setFont(Font.font("System Bold", 14));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(250);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Catégorie (normal, pas en majuscules)
        Label categoryLabel = new Label(product.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        categoryLabel.setAlignment(Pos.CENTER);
        categoryLabel.setMaxWidth(Double.MAX_VALUE);

        // ⭐ Description COMPLÈTE (sans limite) ⭐
        Label descLabel = new Label(product.getDescription());
        descLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        descLabel.setWrapText(true);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setPrefHeight(80);

        // Prix
        Label priceLabel = new Label(String.format("%.2f D", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 18px;");
        priceLabel.setAlignment(Pos.CENTER);
        priceLabel.setMaxWidth(Double.MAX_VALUE);

        // Stock en bleu marine
        String stockText = product.getStock() > 0 ? "Stock: " + product.getStock() : "Rupture de stock";
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle(product.getStock() > 0 ? "-fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        stockLabel.setAlignment(Pos.CENTER);
        stockLabel.setMaxWidth(Double.MAX_VALUE);

        // Quantité et bouton (avec limite de stock)
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        int maxStock = Math.max(1, product.getStock());
        Spinner<Integer> quantitySpinner = new Spinner<>(1, maxStock, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(70);

        // Empêcher la saisie manuelle de valeur > stock
        quantitySpinner.getEditor().textProperty().addListener((obs, old, newVal) -> {
            try {
                int value = Integer.parseInt(newVal);
                if (value > maxStock) {
                    quantitySpinner.getValueFactory().setValue(maxStock);
                } else if (value < 1) {
                    quantitySpinner.getValueFactory().setValue(1);
                }
            } catch (NumberFormatException e) {
                quantitySpinner.getValueFactory().setValue(1);
            }
        });

        Button cartButton = new Button("🛒 Ajouter");
        cartButton.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #2c3e50; -fx-background-radius: 20; -fx-font-weight: bold;");
        cartButton.setOnAction(e -> {
            int quantity = quantitySpinner.getValue();
            if (quantity > product.getStock()) {
                showAlert("Stock insuffisant", "Stock disponible: " + product.getStock());
            } else {
                addToCart(product, quantity);
            }
        });

        actionBox.getChildren().addAll(quantitySpinner, cartButton);

        // Bouton "Voir plus" en NOIR
        Button voirPlusButton = new Button("🔍 Voir plus");
        voirPlusButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #000000; -fx-font-size: 11px; -fx-underline: true; -fx-cursor: hand;");
        voirPlusButton.setMaxWidth(Double.MAX_VALUE);

        String qrUrl = getQRCodeUrl(product);
        voirPlusButton.setOnAction(e -> showQRCodeDialog(product.getName(), qrUrl));

        card.getChildren().addAll(imageContainer, nameLabel, categoryLabel, descLabel, priceLabel, stockLabel, actionBox, voirPlusButton);
        return card;
    }

    private String getQRCodeUrl(Product product) {
        String name = product.getName().toLowerCase();

        if (name.contains("mona lisa") || name.contains("joconde")) {
            return "https://fr.wikipedia.org/wiki/Lisa_Gherardini";
        } else if (name.contains("nuit étoilée") || name.contains("nuit etoilee")) {
            return "https://fr.wikipedia.org/wiki/La_Nuit_%C3%A9toil%C3%A9e";
        } else if (name.contains("jamais plus") || name.contains("it ends with us")) {
            return "https://fr.wikipedia.org/wiki/Colleen_Hoover";
        } else if (name.contains("coffret")) {
            return "https://fr.wikipedia.org/wiki/Coffret_cadeau";
        } else if (name.contains("catalogue")) {
            return "https://fr.wikipedia.org/wiki/Catalogue_d%27exposition";
        } else {
            return "https://www.google.com/search?q=" + product.getName().replace(" ", "+");
        }
    }

    private void showQRCodeDialog(String productName, String url) {
        Stage qrStage = new Stage();
        qrStage.setTitle("QR Code - " + productName);
        qrStage.setResizable(false);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label(productName);
        titleLabel.setFont(Font.font("System Bold", 16));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        Label urlLabel = new Label(url);
        urlLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        urlLabel.setWrapText(true);
        urlLabel.setMaxWidth(250);
        urlLabel.setAlignment(Pos.CENTER);

        String qrApiUrl = "https://quickchart.io/qr?text=" + url + "&size=200x200";
        Image qrImage = new Image(qrApiUrl, true);
        ImageView qrImageView = new ImageView(qrImage);
        qrImageView.setFitWidth(200);
        qrImageView.setFitHeight(200);

        Label instructionLabel = new Label("Scannez ce QR code avec votre téléphone\npour voir plus d'informations");
        instructionLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        instructionLabel.setAlignment(Pos.CENTER);

        Button closeButton = new Button("Fermer");
        closeButton.setStyle("-fx-background-color: #d4af37; -fx-text-fill: #2c3e50; -fx-background-radius: 20; -fx-font-weight: bold;");
        closeButton.setOnAction(e -> qrStage.close());

        root.getChildren().addAll(titleLabel, urlLabel, qrImageView, instructionLabel, closeButton);

        Scene scene = new Scene(root);
        qrStage.setScene(scene);
        qrStage.showAndWait();
    }

    private void addToCart(Product product, int quantity) {
        int index = cart.indexOf(product);
        if (index != -1) {
            int newQuantity = cartQuantities.get(index) + quantity;
            if (newQuantity <= product.getStock()) {
                cartQuantities.set(index, newQuantity);
            } else {
                showAlert("Stock insuffisant", "Stock disponible: " + product.getStock());
                return;
            }
        } else {
            cart.add(product);
            cartQuantities.add(quantity);
        }
        updateCartBadge();
        showAlert("Ajouté au panier", quantity + " x " + product.getName());
    }

    public void updateCartBadge() {
        Platform.runLater(() -> {
            int totalItems = cartQuantities.stream().mapToInt(Integer::intValue).sum();
            if (totalItems > 0) {
                cartBadge.setText(String.valueOf(totalItems));
                cartBadge.setVisible(true);
            } else {
                cartBadge.setVisible(false);
            }
        });
    }

    @FXML
    private void handleOpenCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cart_view.fxml"));
            Parent root = loader.load();

            CartController cartController = loader.getController();
            cartController.setCart(cart, cartQuantities);
            cartController.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle("Mon Panier");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le panier");
        }
    }

    @FXML
    private void handleOpenOrders() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/orders_history.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Mes Commandes");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir l'historique");
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
