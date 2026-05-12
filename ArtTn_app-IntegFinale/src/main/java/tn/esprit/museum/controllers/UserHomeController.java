package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.ProductService;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.LocalizationManager;
import tn.esprit.museum.utils.LanguageSelector;
import tn.esprit.museum.utils.SessionManager;
import tn.esprit.museum.utils.ImageLoader;

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
import java.util.stream.Collectors;

public class UserHomeController {

    // ==================== CHAMPS EXISTANTS ====================
    @FXML private Label welcomeLabel;
    @FXML private Label statEventsLabel;
    @FXML private Label statProductsLabel;
    // Labels statiques de la homeView
    @FXML private Label heroSubtitleLabel;
    @FXML private Button heroBtnEvents;
    @FXML private Button heroBtnShop;
    @FXML private Label statEventsTextLabel;
    @FXML private Label statProductsTextLabel;
    @FXML private Label statMuseumsTextLabel;
    @FXML private Label statDigitalTextLabel;
    @FXML private Label sectionEventsTitleLabel;
    @FXML private Label sectionEventsSubLabel;
    @FXML private Button btnAllEvents;
    @FXML private Label sectionShopTitleLabel;
    @FXML private Label sectionShopSubLabel;
    @FXML private Button btnAllShop;
    @FXML private Label aboutTitleLabel;
    @FXML private Label aboutTextLabel;
    @FXML private Label feat1Label;
    @FXML private Label feat2Label;
    @FXML private Label feat3Label;
    @FXML private Label feat4Label;
    @FXML private Label contactTitleLabel;
    @FXML private Label footerLabel;
    @FXML private HBox homeEventsContainer;
    @FXML private HBox homeProductsContainer;
    @FXML private ImageView profileImageView;
    @FXML private ImageView topProfileImageView;
    @FXML private Node homeView;
    @FXML private VBox profileView;
    @FXML private VBox productsView;
    @FXML private Circle profileCircle;
    @FXML private TextField searchFieldHeader;
    @FXML private TextField searchFieldBoutique;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label profileMessageLabel;
    @FXML private BorderPane mainContainer;
    @FXML private HBox navbarContainer;
    @FXML private ComboBox<String> navLanguageCombo;
    @FXML public VBox forumView;
    @FXML private VBox jobFrontView;
    @FXML private VBox favoriteOffersView;
    @FXML private VBox eventsFrontView;
    @FXML private VBox mesCandidaturesView;
    @FXML private VBox mesReservationsView;
    @FXML private VBox userDonationsView;
    private MesCandidaturesController mesCandidaturesController;

    // ==================== PRODUITS & PANIER ====================
    @FXML private FlowPane productsContainer;
    @FXML private Label cartBadge;
    @FXML private ComboBox<String> categoryFilter;

    private ProductService productService;
    private EventService eventService;
    private List<Product> allProducts;
    private List<Product> cart;
    private List<Integer> cartQuantities;

    private User currentUser;
    private UserService userService;
    private static VBox staticForumView;
    private final LocalizationManager lm = LocalizationManager.getInstance();

    @FXML
    public void initialize() {
        userService = new UserService();
        staticForumView = forumView;

        // ── Listener localisation ────────────────────────────────────────────
        lm.addListener(this::updateTexts);

        // ── Sélecteur de langue dans la navbar ───────────────────────────────
        if (navLanguageCombo != null) {
            LanguageSelector.setup(navLanguageCombo);
        }
        if (profileMessageLabel != null) {
            profileMessageLabel.setVisible(false);
        }
        if (profileImageView != null) {
            makeImageCircular(profileImageView, 50);
        }

        productService = new ProductService();
        eventService = new EventService();
        allProducts = new ArrayList<>();
        cart = new ArrayList<>();
        cartQuantities = new ArrayList<>();

        if (categoryFilter != null) {
            categoryFilter.getItems().addAll("Tous", "Coffrets", "Livres", "Accessoires", "Décorations", "Numérique");
            categoryFilter.setValue("Tous");
            categoryFilter.valueProperty().addListener((obs, old, val) -> filterProducts());
        }

        if (searchFieldBoutique != null) {
            searchFieldBoutique.textProperty().addListener((obs, old, val) -> filterProducts());
        }

        loadProducts();
        updateCartBadge();
    }

    // ==================== LOCALISATION ====================
    private void updateTexts() {
        javafx.application.Platform.runLater(() -> {
            // ── Navbar boutons — identification par userData (fiable toutes langues) ──
            if (navbarContainer != null) {
                for (javafx.scene.Node node : navbarContainer.getChildren()) {
                    if (node instanceof Button btn && btn.getUserData() instanceof String key) {
                        String icon = switch (key) {
                            case "nav.home"         -> "🏠 ";
                            case "nav.boutique"     -> "🛍️ ";
                            case "nav.forum"        -> "💬 ";
                            case "nav.jobs"         -> "💼 ";
                            case "nav.events"       -> "🎭 ";
                            case "nav.candidatures" -> "📋 ";
                            case "nav.logout"       -> "🚪 ";
                            default -> "";
                        };
                        btn.setText(icon + lm.getString(key));
                    }
                }
            }
            // ── Hero ─────────────────────────────────────────────────────────
            if (heroSubtitleLabel    != null) heroSubtitleLabel.setText(lm.getString("home.hero.subtitle"));
            if (heroBtnEvents        != null) heroBtnEvents.setText("🎭 " + lm.getString("home.hero.btn.events"));
            if (heroBtnShop          != null) heroBtnShop.setText("🛍️ " + lm.getString("home.hero.btn.shop"));
            // ── Stats band ───────────────────────────────────────────────────
            if (statEventsTextLabel  != null) statEventsTextLabel.setText(lm.getString("home.stats.events"));
            if (statProductsTextLabel!= null) statProductsTextLabel.setText(lm.getString("home.stats.products"));
            if (statMuseumsTextLabel != null) statMuseumsTextLabel.setText(lm.getString("home.stats.museums"));
            if (statDigitalTextLabel != null) statDigitalTextLabel.setText(lm.getString("home.stats.digital"));
            // ── Section événements ───────────────────────────────────────────
            if (sectionEventsTitleLabel != null) sectionEventsTitleLabel.setText("🎭 " + lm.getString("home.section.events"));
            if (sectionEventsSubLabel   != null) sectionEventsSubLabel.setText(lm.getString("home.section.events.sub"));
            if (btnAllEvents            != null) btnAllEvents.setText(lm.getString("home.section.events.all"));
            // ── Section boutique ─────────────────────────────────────────────
            if (sectionShopTitleLabel != null) sectionShopTitleLabel.setText("🛍️ " + lm.getString("home.section.shop"));
            if (sectionShopSubLabel   != null) sectionShopSubLabel.setText(lm.getString("home.section.shop.sub"));
            if (btnAllShop            != null) btnAllShop.setText(lm.getString("home.section.shop.all"));
            // ── À propos ─────────────────────────────────────────────────────
            if (aboutTitleLabel   != null) aboutTitleLabel.setText(lm.getString("home.about.title"));
            if (aboutTextLabel    != null) aboutTextLabel.setText(lm.getString("home.about.text"));
            if (feat1Label        != null) feat1Label.setText(lm.getString("nav.reservations"));
            if (feat2Label        != null) feat2Label.setText(lm.getString("nav.events"));
            if (feat3Label        != null) feat3Label.setText(lm.getString("nav.boutique"));
            if (feat4Label        != null) feat4Label.setText(lm.getString("nav.forum"));
            if (contactTitleLabel != null) contactTitleLabel.setText(lm.getString("home.contact.title"));
            if (footerLabel       != null) footerLabel.setText(lm.getString("home.footer"));
            // ── Sync ComboBox (sans déclencher l'action) ─────────────────────
            if (navLanguageCombo != null) {
                String code = lm.getCurrentCode();
                int idx = switch (code) { case "en" -> 1; case "ar" -> 2; default -> 0; };
                navLanguageCombo.setOnAction(null);
                navLanguageCombo.getSelectionModel().select(idx);
                LanguageSelector.setup(navLanguageCombo);
            }
            // ── Orientation RTL/LTR ──────────────────────────────────────────
            if (mainContainer != null) lm.applyOrientation(mainContainer);
        });
    }

    // ==================== PAGE D'ACCUEIL ====================
    private void loadHomePageData() {
        // Événements (max 4)
        if (homeEventsContainer != null) {
            homeEventsContainer.getChildren().clear();
            try {
                // Utiliser getAll() + filtre Java pour être compatible avec tous les statuts Symfony
                List<Event> allEvents = eventService.getAll();
                List<Event> events = allEvents.stream()
                        .filter(e -> {
                            String s = e.getStatus();
                            return !Event.STATUS_TERMINE.equals(s) && !Event.STATUS_ANNULE.equals(s);
                        })
                        .limit(4)
                        .collect(java.util.stream.Collectors.toList());

                for (Event ev : events) {
                    homeEventsContainer.getChildren().add(createHomeEventCard(ev));
                }
                if (statEventsLabel != null) statEventsLabel.setText(String.valueOf(allEvents.stream()
                        .filter(e -> !Event.STATUS_TERMINE.equals(e.getStatus()) && !Event.STATUS_ANNULE.equals(e.getStatus()))
                        .count()));
            } catch (Exception e) {
                System.err.println("Erreur chargement événements accueil: " + e.getMessage());
                if (statEventsLabel != null) statEventsLabel.setText("0");
            }
        }

        // Produits (max 4)
        if (homeProductsContainer != null) {
            homeProductsContainer.getChildren().clear();
            try {
                List<Product> products = productService.getAvailableProducts();
                int count = Math.min(products.size(), 4);
                for (int i = 0; i < count; i++) {
                    homeProductsContainer.getChildren().add(createHomeProductCard(products.get(i)));
                }
                if (statProductsLabel != null) statProductsLabel.setText(String.valueOf(products.size()));
            } catch (Exception e) {
                if (statProductsLabel != null) statProductsLabel.setText("0");
            }
        }
    }

    private VBox createHomeEventCard(Event event) {
        VBox card = new VBox(0);
        card.setPrefWidth(230);
        card.setMinWidth(230);
        card.setMaxWidth(230);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);");

        // Image — loaded via ImageLoader (resolves from classpath resources or Symfony uploads)
        ImageView img = new ImageView();
        img.setFitWidth(230);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        Rectangle imgClip = new Rectangle(230, 140);
        imgClip.setArcWidth(12); imgClip.setArcHeight(12);
        img.setClip(imgClip);
        img.setImage(ImageLoader.load(event.getImageUrl()));

        // Contenu
        VBox content = new VBox(6);
        content.setStyle("-fx-padding: 12 14 14 14;");

        // Badge catégorie
        Label badge = new Label(event.getCategory() != null ? event.getCategory() : "ÉVÉNEMENT");
        badge.setStyle("-fx-background-color: #D4AF37; -fx-text-fill: #1a1a2e; -fx-font-size: 9px;" +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 2 8;");

        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a2e;");
        title.setWrapText(true);
        title.setMaxWidth(202);

        String dateStr = event.getEventDate() != null
                ? event.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) : "";
        Label date = new Label("📅 " + dateStr);
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        Label location = new Label("📍 " + (event.getLocation() != null ? event.getLocation() : ""));
        location.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        location.setMaxWidth(202);
        location.setWrapText(false);

        Label price = new Label(event.getPrice() != null ? event.getPrice() + " DT" : "Gratuit");
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #D4AF37;");

        Button btn = new Button("Réserver →");
        btn.setStyle("-fx-background-color: #1a1a2e; -fx-text-fill: #D4AF37; -fx-background-radius: 18;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 16; -fx-font-size: 11px;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> handleShowEventsFront());

        content.getChildren().addAll(badge, title, date, location, price, btn);
        card.getChildren().addAll(img, content);
        return card;
    }

    private VBox createHomeProductCard(Product product) {
        VBox card = new VBox(0);
        card.setPrefWidth(230);
        card.setMinWidth(230);
        card.setMaxWidth(230);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);");

        // Image fixe
        ImageView img = new ImageView();
        img.setFitWidth(230);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        Rectangle imgClip = new Rectangle(230, 140);
        imgClip.setArcWidth(12); imgClip.setArcHeight(12);
        img.setClip(imgClip);
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                if (imageUrl.startsWith("/")) {
                    img.setImage(new Image(getClass().getResource(imageUrl).toExternalForm(), 230, 140, false, true));
                } else {
                    img.setImage(new Image(imageUrl, 230, 140, false, true));
                }
            } catch (Exception ignored) { img.setImage(new Image("https://picsum.photos/230/140", true)); }
        } else {
            img.setImage(new Image("https://picsum.photos/230/140", true));
        }

        // Contenu
        VBox content = new VBox(6);
        content.setStyle("-fx-padding: 12 14 14 14;");

        Label cat = new Label(product.getCategory() != null ? product.getCategory().toUpperCase() : "");
        cat.setStyle("-fx-font-size: 9px; -fx-text-fill: #999; -fx-font-weight: bold;");

        Label name = new Label(product.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a2e;");
        name.setWrapText(true);
        name.setMaxWidth(202);

        Label price = new Label(String.format("%.2f DT", product.getPrice()));
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #D4AF37;");

        String stockText = product.getStock() > 0 ? "✅ En stock (" + product.getStock() + ")" : "❌ Rupture";
        Label stock = new Label(stockText);
        stock.setStyle(product.getStock() > 0
                ? "-fx-font-size: 11px; -fx-text-fill: #27ae60;"
                : "-fx-font-size: 11px; -fx-text-fill: #e74c3c;");

        Button btn = new Button("Voir la boutique →");
        btn.setStyle("-fx-background-color: #D4AF37; -fx-text-fill: #1a1a2e; -fx-background-radius: 18;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 16; -fx-font-size: 11px;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> handleShowBoutique());

        content.getChildren().addAll(cat, name, price, stock, btn);
        card.getChildren().addAll(img, content);
        return card;
    }

    // ==================== PRODUITS ====================
    private void loadProducts() {
        try {
            allProducts = productService.getAvailableProducts();
            filterProducts();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les produits: " + e.getMessage());
        }
    }
    public static VBox getForumView() {
        return staticForumView;
    }

    private void filterProducts() {
        String search = searchFieldBoutique != null ? searchFieldBoutique.getText().toLowerCase() : "";
        String category = categoryFilter != null ? categoryFilter.getValue() : "Tous";

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            boolean matchSearch = search.isEmpty() ||
                    p.getName().toLowerCase().contains(search) ||
                    p.getDescription().toLowerCase().contains(search);
            boolean matchCategory = category.equals("Tous") || p.getCategory().equals(category);
            if (matchSearch && matchCategory) {
                filtered.add(p);
            }
        }
        displayProducts(filtered);
    }

    private void displayProducts(List<Product> products) {
        if (productsContainer == null) return;
        productsContainer.getChildren().clear();
        for (Product product : products) {
            productsContainer.getChildren().add(createProductCard(product));
        }
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
            cartBadge.setText(String.valueOf(totalItems));
            cartBadge.setVisible(totalItems > 0);
        });
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

    @FXML
    private void handleShowForum() {
        showForum();
    }

    @FXML
    private void handleShowProfilePage() {
        showProfile();
    }

    public void showHome() {
        showView(homeView);
        updateActiveButtonStyle("nav.home");
        loadHomePageData();
    }

    public void showBoutique() {
        showView(productsView);
        loadProducts();
        updateActiveButtonStyle("nav.boutique");
    }

    public void showForum() {
        if (forumView == null) return;
        showView(forumView);
        
        // On ne recharge que si c'est vide pour éviter de perdre l'état ou de clignoter
        if (forumView.getChildren().isEmpty()) {
            loadForumCategories();
        }
        
        updateActiveButtonStyle("nav.forum");
    }

    public void showJobFront() {
        if (jobFrontView == null) return;
        // Toujours recharger pour garantir qu'on affiche les offres (pas les favoris)
        jobFrontView.getChildren().clear();
        loadEmbeddedViewInto("/fxml/JobFront.fxml", jobFrontView);
        showView(jobFrontView);
        updateActiveButtonStyle("nav.jobs");
    }

    @FXML
    public void handleShowJobFront() {
        if (jobFrontView == null) return;
        // Toujours recharger pour garantir qu'on affiche les offres (pas les favoris)
        jobFrontView.getChildren().clear();
        loadEmbeddedViewInto("/fxml/JobFront.fxml", jobFrontView);
        showView(jobFrontView);
        updateActiveButtonStyle("nav.jobs");
    }

    @FXML
    public void handleShowFavoriteOffers() {
        if (favoriteOffersView == null) return;
        favoriteOffersView.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FavoriteOffers.fxml"));
            Parent root = loader.load();
            if (root instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) root).setTop(null);
            }
            FavoriteOffersController controller = loader.getController();
            controller.setParentController(this);
            User effectiveUser = (currentUser != null) ? currentUser : SessionManager.getCurrentUser();
            if (effectiveUser != null) {
                controller.setCandidatId(String.valueOf(effectiveUser.getId()));
            }
            favoriteOffersView.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        showView(favoriteOffersView);
        updateActiveButtonStyle("nav.jobs");
    }

    public void handleShowMesCandidaturesPublic() {
        if (mesCandidaturesView != null && mesCandidaturesView.getChildren().isEmpty()) {
            loadEmbeddedViewInto("/fxml/MesCandidatures.fxml", mesCandidaturesView);
        }
        if (mesCandidaturesController != null) {
            mesCandidaturesController.handleChargerMesCandidatures();
        }
        showView(mesCandidaturesView);
        updateActiveButtonStyle("nav.candidatures");
    }

    @FXML
    private void handleShowEventsFront() {
        if (eventsFrontView != null && eventsFrontView.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/events_front.fxml"));
                Parent root = loader.load();
                EventFrontController controller = loader.getController();
                controller.setParentController(this);
                // Passer l'email de l'utilisateur connecté
                User effectiveUser = (currentUser != null) ? currentUser : SessionManager.getCurrentUser();
                if (effectiveUser != null) {
                    controller.setCurrentUserEmail(effectiveUser.getEmail());
                }
                if (root instanceof javafx.scene.layout.BorderPane) {
                    ((javafx.scene.layout.BorderPane) root).setTop(null);
                }
                eventsFrontView.getChildren().clear();
                eventsFrontView.getChildren().add(root);
                javafx.scene.layout.VBox.setVgrow(root, javafx.scene.layout.Priority.ALWAYS);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de charger le module Événements : " + e.getMessage(), Alert.AlertType.ERROR);
                return;
            }
        }
        showView(eventsFrontView);
        updateActiveButtonStyle("nav.events");
    }

    public void showMesReservations(String userEmail) {
        // Toujours recharger pour avoir les données fraîches
        mesReservationsView.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/my_reservations.fxml"));
            Parent root = loader.load();
            MyReservationsController controller = loader.getController();
            controller.setParentController(this);
            controller.setUserEmail(userEmail);
            // Supprimer la navbar interne (on utilise celle de user_home)
            if (root instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) root).setTop(null);
            }
            mesReservationsView.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger vos réservations.", Alert.AlertType.ERROR);
            return;
        }
        showView(mesReservationsView);
        updateActiveButtonStyle("nav.reservations");
    }

    public void showUserDonations(int userId) {
        if (userDonationsView == null) {
            return;
        }
        userDonationsView.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDonations.fxml"));
            Parent root = loader.load();
            UserDonationsController controller = loader.getController();
            controller.setParentController(this);
            controller.setUserId(userId);
            if (root instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) root).setTop(null);
            }
            userDonationsView.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger vos donations.", Alert.AlertType.ERROR);
            return;
        }
        showView(userDonationsView);
        updateActiveButtonStyle("nav.reservations");
    }

    public void handleShowEventsFrontPublic(String userEmail) {
        // Forcer le rechargement pour mettre à jour l'email si nécessaire
        eventsFrontView.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/events_front.fxml"));
            Parent root = loader.load();
            EventFrontController controller = loader.getController();
            controller.setParentController(this);
            controller.setCurrentUserEmail(userEmail);
            if (root instanceof javafx.scene.layout.BorderPane) {
                ((javafx.scene.layout.BorderPane) root).setTop(null);
            }
            eventsFrontView.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements.", Alert.AlertType.ERROR);
            return;
        }
        showView(eventsFrontView);
        updateActiveButtonStyle("nav.events");
    }

    @FXML
    private void handleShowMesCandidatures(javafx.event.ActionEvent event) {
        // 1. Charger la vue si elle est vide
        if (mesCandidaturesView != null && mesCandidaturesView.getChildren().isEmpty()) {
            loadEmbeddedViewInto("/fxml/MesCandidatures.fxml", mesCandidaturesView);
        }

        // 2. S'assurer que le contrôleur est bien récupéré et rafraîchir les données
        // On peut tenter de le récupérer via la vue si mesCandidaturesController est encore null
        if (mesCandidaturesController != null) {
            mesCandidaturesController.handleChargerMesCandidatures();
        } else {
            // Optionnel : si le contrôleur n'est pas encore lié,
            // loadEmbeddedViewInto devrait s'en charger, mais on peut forcer une vérification
            System.out.println("Le contrôleur des candidatures est en cours d'initialisation...");
        }

        // 3. Affichage et style
        showView(mesCandidaturesView);
        updateActiveButtonStyle("nav.candidatures");
    }

    private void loadEmbeddedViewInto(String fxmlPath, VBox container) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            if (root instanceof BorderPane) {
                ((BorderPane) root).setTop(null);
            }

            User effectiveUser = (currentUser != null) ? currentUser : SessionManager.getCurrentUser();
            if (effectiveUser != null) {
                Object controller = loader.getController();
                String userIdStr = String.valueOf(effectiveUser.getId());
                if (controller instanceof JobFrontController jfc) {
                    jfc.setParentController(this);
                    jfc.setCandidatId(userIdStr);
                } else if (controller instanceof FavoriteOffersController foc) {
                    foc.setParentController(this);
                    foc.setCandidatId(userIdStr);
                } else if (controller instanceof MesCandidaturesController mcc) {
                    this.mesCandidaturesController = mcc;
                    mcc.setParentController(this);
                    this.mesCandidaturesController.setCandidatId(userIdStr);
                }
            }
            container.getChildren().clear();
            container.getChildren().add(root);
            VBox.setVgrow(root, Priority.ALWAYS);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur de chargement", "Impossible de charger " + fxmlPath + " : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showView(Node viewToShow) {
        if (viewToShow == null) return;
        hideAllViews();
        viewToShow.setVisible(true);
        viewToShow.setManaged(true);
    }

    private void hideAllViews() {
        Node[] views = {homeView, productsView, forumView, profileView, jobFrontView, favoriteOffersView, eventsFrontView, mesCandidaturesView, mesReservationsView, userDonationsView};
        for (Node v : views) {
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }
    }

    private Node loadEmbeddedView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // Masquer la top bar du FXML embarqué s'il y en a une redondante
            if (root instanceof BorderPane) {
                ((BorderPane) root).setTop(null);
            }

            // Passer l'ID de l'utilisateur au contrôleur
            User effectiveUser = (currentUser != null) ? currentUser : SessionManager.getCurrentUser();
            if (effectiveUser != null) {
                Object controller = loader.getController();
                String userIdStr = String.valueOf(effectiveUser.getId());
                
                if (controller instanceof JobFrontController) {
                    ((JobFrontController) controller).setCandidatId(userIdStr);
                } else if (controller instanceof MesCandidaturesController) {
                    this.mesCandidaturesController = (MesCandidaturesController) controller;
                    this.mesCandidaturesController.setCandidatId(userIdStr);
                }
            }

            // Ajouter le root au StackPane central (contenu dans le ScrollPane)
            // On cherche le StackPane qui contient homeView
            if (homeView != null && homeView.getParent() instanceof Pane) {
                ((Pane) homeView.getParent()).getChildren().add(root);
            }

            return root;
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue : " + fxmlPath);
            return null;
        }
    }

    private void updateActiveButtonStyle(String activeKey) {
        if (navbarContainer == null) return;
        for (javafx.scene.Node node : navbarContainer.getChildren()) {
            if (node instanceof Button btn) {
                String key = btn.getUserData() instanceof String s ? s : "";
                boolean isActive = key.equals(activeKey);
                boolean isLogout = "nav.logout".equals(key);
                if (isActive) {
                    btn.getStyleClass().remove("menu-button");
                    if (!btn.getStyleClass().contains("menu-button-active"))
                        btn.getStyleClass().add("menu-button-active");
                } else if (!isLogout) {
                    btn.getStyleClass().remove("menu-button-active");
                    if (!btn.getStyleClass().contains("menu-button"))
                        btn.getStyleClass().add("menu-button");
                }
            }
        }
    }

    public void showProfile() {
        hideAllViews();
        profileView.setVisible(true);
        profileView.setManaged(true);
        loadUserData();
        clearForumView();
        updateActiveButtonStyle("nav.profile");
    }


    private void clearForumView() {
        if (forumView != null) {
            forumView.getChildren().clear();
        }
    }

    private void loadForumCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/forum_categories.fxml"));
            Parent categoriesView = loader.load();

            ForumCategoriesController categoriesCtrl = loader.getController();
            categoriesCtrl.setParentController(this); // ✅ Important !

            forumView.getChildren().clear();
            forumView.getChildren().add(categoriesView);
            VBox.setVgrow(categoriesView, Priority.ALWAYS);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showForumPosts(Parent postsView) {
        forumView.getChildren().clear();
        forumView.getChildren().add(postsView);
        VBox.setVgrow(postsView, Priority.ALWAYS);
    }

    public void showForumCategories() {
        // Vider et recharger les catégories dans forumView
        forumView.getChildren().clear();
        loadForumCategories();
    }

    // ==================== PANIER & HISTORIQUE ====================
    @FXML
    private void handleOpenCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cart_view.fxml"));
            Parent root = loader.load();

            CartController cartController = loader.getController();
            cartController.setCart(cart, cartQuantities);

            Stage stage = new Stage();
            stage.setTitle("Mon Panier");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le panier", Alert.AlertType.ERROR);
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
            showAlert("Erreur", "Impossible d'ouvrir l'historique", Alert.AlertType.ERROR);
        }
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
        showAlert(title, content, Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    public void showDashboard() {
        showHome();
    }
}

