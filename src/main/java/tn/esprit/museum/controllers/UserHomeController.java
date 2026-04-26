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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.ProductService;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserHomeController {

    // ==================== CHAMPS EXISTANTS ====================
    @FXML private Label welcomeLabel;
    @FXML private ImageView profileImageView;
    @FXML private ImageView topProfileImageView;
    @FXML private VBox homeView;
    @FXML private VBox profileView;
    @FXML private Circle profileCircle;
    @FXML private TextField searchFieldBoutique;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label profileMessageLabel;

    // ==================== PRODUITS & PANIER ====================
    @FXML private FlowPane productsContainer;
    @FXML private Label cartBadge;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private VBox productsView;

    private ProductService productService;
    private List<Product> allProducts;
    private List<Product> cart;
    private List<Integer> cartQuantities;

    private User currentUser;
    private UserService userService;

    @FXML
    public void initialize() {
        // Initialisation UserService
        userService = new UserService();
        if (profileMessageLabel != null) {
            profileMessageLabel.setVisible(false);
        }
        makeImageCircular(profileImageView, 50);

        // Initialisation Produits
        productService = new ProductService();
        allProducts = new ArrayList<>();
        cart = new ArrayList<>();
        cartQuantities = new ArrayList<>();

        if (categoryFilter != null) {
            categoryFilter.getItems().addAll("Tous", "Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
            categoryFilter.setValue("Tous");
        }

        if (categoryFilter != null) {
            categoryFilter.valueProperty().addListener((obs, old, val) -> filterProducts());
        }

        if (searchFieldBoutique != null) {
            searchFieldBoutique.textProperty().addListener((obs, old, val) -> filterProducts());
        }


        loadProducts();
        updateCartBadge();
    }

    // ==================== PRODUITS ====================
    private void loadProducts() {
        try {
            allProducts = productService.getAvailableProducts();
            System.out.println("=== PRODUITS CHARGÉS ===");
            System.out.println("Nombre de produits: " + allProducts.size());
            for (Product p : allProducts) {
                System.out.println("  - " + p.getName());
            }
            filterProducts();
        } catch (SQLException e) {
            System.out.println("❌ ERREUR SQL: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }

    private void filterProducts() {
        String search = "";
        if (searchFieldBoutique != null) {
            search = searchFieldBoutique.getText().toLowerCase();
        }
        String category = categoryFilter != null ? categoryFilter.getValue() : "Tous";

        System.out.println("=== FILTRAGE PRODUITS ===");
        System.out.println("Recherche: '" + search + "'");
        System.out.println("Catégorie: " + category);
        System.out.println("Nombre total de produits: " + allProducts.size());

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            boolean matchSearch = search.isEmpty() ||
                    p.getName().toLowerCase().contains(search) ||
                    p.getDescription().toLowerCase().contains(search);
            boolean matchCategory = category.equals("Tous") || p.getCategory().equals(category);
            if (matchSearch && matchCategory) {
                filtered.add(p);
                System.out.println("  ✅ Produit trouvé: " + p.getName());
            }
        }

        System.out.println("Produits filtrés: " + filtered.size());
        displayProducts(filtered);
    }

    private void displayProducts(List<Product> products) {
        if (productsContainer == null) return;
        productsContainer.getChildren().clear();

        // Ajouter les produits au centre
        for (Product product : products) {
            productsContainer.getChildren().add(createProductCard(product));
        }

        // Forcer l'alignement central
        productsContainer.setAlignment(Pos.CENTER);
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(280);
        card.setPrefHeight(450);
        card.setAlignment(Pos.TOP_CENTER);

        HBox imageContainer = new HBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setStyle("-fx-padding: 5 0;");

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

        Label nameLabel = new Label(product.getName().toUpperCase());
        nameLabel.setFont(Font.font("System Bold", 14));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(250);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label categoryLabel = new Label(product.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        categoryLabel.setAlignment(Pos.CENTER);
        categoryLabel.setMaxWidth(Double.MAX_VALUE);

        Label descLabel = new Label(product.getDescription());
        descLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        descLabel.setWrapText(true);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setPrefHeight(80);

        Label priceLabel = new Label(String.format("%.2f D", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 18px;");
        priceLabel.setAlignment(Pos.CENTER);
        priceLabel.setMaxWidth(Double.MAX_VALUE);

        String stockText = product.getStock() > 0 ? "Stock: " + product.getStock() : "Rupture de stock";
        Label stockLabel = new Label(stockText);
        stockLabel.setStyle(product.getStock() > 0 ? "-fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-font-weight: bold;" : "-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
        stockLabel.setAlignment(Pos.CENTER);
        stockLabel.setMaxWidth(Double.MAX_VALUE);

        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);

        int maxStock = Math.max(1, product.getStock());
        Spinner<Integer> quantitySpinner = new Spinner<>(1, maxStock, 1);
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(70);

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

    // ==================== PANIER & HISTORIQUE ====================
    @FXML
    private void handleOpenCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cart_view.fxml"));
            Parent root = loader.load();

            CartController cartController = loader.getController();
            cartController.setCart(cart, cartQuantities);
            // Note: setParentController est optionnel, commenté si méthode n'existe pas
            // cartController.setParentController(this);

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

    // ==================== NAVIGATION ====================
    @FXML
    private void handleShowHome() {
        showHome();
    }

    @FXML
    private void handleShowBoutique() {
        showBoutique();
    }

    public void showHome() {
        if (homeView != null) homeView.setVisible(true);
        if (profileView != null) profileView.setVisible(false);
        if (productsView != null) productsView.setVisible(false);
    }

    public void showBoutique() {
        System.out.println("=== showBoutique() appelée ===");

        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
            System.out.println("homeView cachée");
        }
        if (profileView != null) {
            profileView.setVisible(false);
            profileView.setManaged(false);
            System.out.println("profileView cachée");
        }
        if (productsView != null) {
            productsView.setVisible(true);
            productsView.setManaged(true);
            System.out.println("productsView affichée");
            System.out.println("productsView visible: " + productsView.isVisible());
        } else {
            System.out.println("❌ productsView est NULL !");
        }

        loadProducts();
    }

    public void showDashboard() {
        showHome();
    }

    // ==================== PROFIL ====================
    private void makeImageCircular(ImageView imageView, double radius) {
        if (imageView == null) return;
        Circle clip = new Circle();
        clip.centerXProperty().bind(imageView.fitWidthProperty().divide(2));
        clip.centerYProperty().bind(imageView.fitHeightProperty().divide(2));
        clip.radiusProperty().bind(imageView.fitWidthProperty().divide(2));
        imageView.setClip(clip);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionManager.login(user);
        loadUserData();
        showHome();
    }

    public void refreshUserInfo() {
        loadUserData();
    }

    private void loadUserData() {
        if (currentUser != null) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Bienvenue, " + currentUser.getFullName() + "!");
            }

            if (fullNameField != null) fullNameField.setText(currentUser.getFullName());
            if (usernameField != null) usernameField.setText(currentUser.getUsername());
            if (emailField != null) emailField.setText(currentUser.getEmail());

            loadProfilePicture();
        }
    }

    private void loadProfilePicture() {
        try {
            String picturePath = userService.getProfilePicture(currentUser.getId());
            Image image = null;

            if (picturePath != null && !picturePath.isEmpty()) {
                File file = new File(picturePath);
                if (file.exists()) {
                    image = new Image(file.toURI().toString());
                }
            }

            if (image == null) {
                image = getDefaultProfileImage();
            }

            if (profileImageView != null) profileImageView.setImage(image);
            if (topProfileImageView != null) topProfileImageView.setImage(image);

        } catch (SQLException e) {
            setDefaultProfilePicture();
            e.printStackTrace();
        }
    }

    private Image getDefaultProfileImage() {
        try {
            return new Image(getClass().getResourceAsStream("/images/default-avatar.png"));
        } catch (Exception e) {
            return null;
        }
    }

    private void setDefaultProfilePicture() {
        Image defaultImage = getDefaultProfileImage();
        if (profileImageView != null) profileImageView.setImage(defaultImage);
        if (topProfileImageView != null) topProfileImageView.setImage(defaultImage);
    }

    @FXML
    private void handleChangeProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedFile != null) {
            try {
                String userDir = "uploads/profile_pictures/";
                File dir = new File(userDir);
                if (!dir.exists()) dir.mkdirs();

                String fileExtension = getFileExtension(selectedFile.getName());
                String fileName = currentUser.getId() + "_" + UUID.randomUUID().toString() + fileExtension;
                Path destination = Paths.get(userDir + fileName);

                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                String picturePath = destination.toAbsolutePath().toString();
                userService.updateProfilePicture(currentUser.getId(), picturePath);
                currentUser.setProfilePicture(picturePath);

                Image image = new Image(selectedFile.toURI().toString());
                if (profileImageView != null) profileImageView.setImage(image);
                if (topProfileImageView != null) topProfileImageView.setImage(image);

                showProfileMessage("✅ Photo de profil mise à jour !", "success");

            } catch (IOException | SQLException e) {
                showProfileMessage("❌ Erreur: " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot) : ".png";
    }

    @FXML
    private void handleSaveChanges() {
        String newFullName = fullNameField.getText().trim();
        String newUsername = usernameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newFullName.isEmpty() || newUsername.isEmpty() || newEmail.isEmpty()) {
            showProfileMessage("Veuillez remplir tous les champs", "error");
            return;
        }

        if (newUsername.length() < 3) {
            showProfileMessage("Username minimum 3 caractères", "error");
            return;
        }

        if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showProfileMessage("Email invalide", "error");
            return;
        }

        if (!newUsername.equals(currentUser.getUsername())) {
            try {
                if (userService.isUsernameTaken(newUsername)) {
                    showProfileMessage("Username déjà pris", "error");
                    return;
                }
            } catch (SQLException e) { showProfileMessage("Erreur", "error"); return; }
        }

        if (!newEmail.equals(currentUser.getEmail())) {
            try {
                if (userService.isEmailTaken(newEmail)) {
                    showProfileMessage("Email déjà utilisé", "error");
                    return;
                }
            } catch (SQLException e) { showProfileMessage("Erreur", "error"); return; }
        }

        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                showProfileMessage("Mot de passe minimum 6 caractères", "error");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showProfileMessage("Les mots de passe ne correspondent pas", "error");
                return;
            }
        }

        try {
            currentUser.setFullName(newFullName);
            currentUser.setUsername(newUsername);
            currentUser.setEmail(newEmail);

            userService.update(currentUser);

            if (!newPassword.isEmpty()) {
                userService.updatePassword(currentUser.getId(), newPassword);
            }

            loadUserData();
            newPasswordField.clear();
            confirmPasswordField.clear();

            showProfileMessage("✅ Informations mises à jour !", "success");

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> showHome());
            }).start();

        } catch (SQLException e) {
            showProfileMessage("Erreur: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleCancel() {
        loadUserData();
        newPasswordField.clear();
        confirmPasswordField.clear();
        showProfileMessage("Modifications annulées", "info");
        showHome();
    }

    private void showProfileMessage(String message, String type) {
        if (profileMessageLabel != null) {
            profileMessageLabel.setText(message);
            switch (type) {
                case "error": profileMessageLabel.setStyle("-fx-text-fill: #e74c3c;"); break;
                case "success": profileMessageLabel.setStyle("-fx-text-fill: #27ae60;"); break;
                default: profileMessageLabel.setStyle("-fx-text-fill: #3498db;");
            }
            profileMessageLabel.setVisible(true);

            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                javafx.application.Platform.runLater(() -> profileMessageLabel.setVisible(false));
            }).start();
        }
    }

    @FXML
    private void handleShowProfilePage() {
        if (homeView != null) homeView.setVisible(false);
        if (productsView != null) productsView.setVisible(false);
        if (profileView != null) profileView.setVisible(true);
        loadUserData();
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir vous déconnecter?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                    Scene scene = new Scene(loader.load());
                    Stage stage = (Stage) profileImageView.getScene().getWindow();
                    stage.setScene(scene);
                    stage.setTitle("Connexion - Museum Digital");
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}