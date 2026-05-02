package tn.esprit.museum.controllers;

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
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.RecommendationService;
import tn.esprit.museum.services.ReservationService;
import tn.esprit.museum.services.UserService;
import tn.esprit.museum.utils.TextToSpeechUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EventFrontController {

    private static final Logger LOGGER = Logger.getLogger(EventFrontController.class.getName());

    @FXML private Label userWelcomeLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> sortCombo;
    @FXML private VBox eventsContainer;

    private EventService eventService;
    private ReservationService reservationService;
    private UserService userService;
    private ObservableList<Event> allEvents;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private String currentUserEmail;

    public void setCurrentUserEmail(String email) {
        this.currentUserEmail = email;
        if (userWelcomeLabel != null) {
            userWelcomeLabel.setText("Bienvenue, " + email);
        }
        loadRecommendations();
    }

    @FXML
    public void initialize() {
        try {
            eventService = new EventService();
            reservationService = new ReservationService();
            userService = new UserService();
            allEvents = FXCollections.observableArrayList();

            categoryFilter.getItems().addAll("Toutes", "EXPOSITION", "CONFERENCE", "ATELIER", "VISITE_GUIDEE", "SPECTACLE");
            categoryFilter.setValue("Toutes");
            sortCombo.getItems().addAll("Date (croissante)", "Date (décroissante)", "Prix (croissant)", "Prix (décroissant)");
            sortCombo.setValue("Date (croissante)");

            searchField.textProperty().addListener((obs, old, val) -> filterAndDisplay());
            categoryFilter.valueProperty().addListener((obs, old, val) -> filterAndDisplay());
            sortCombo.valueProperty().addListener((obs, old, val) -> filterAndDisplay());

            loadEvents();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur initialisation front", e);
        }
    }

    private void loadEvents() {
        try {
            List<Event> all = eventService.getAll();
            System.out.println("Total événements en BDD: " + all.size());
            List<Event> events = all.stream()
                    .filter(e -> "A_VENIR".equals(e.getStatus()) || "EN_COURS".equals(e.getStatus()))
                    .toList();
            System.out.println("Événements après filtre (A_VENIR/EN_COURS): " + events.size());
            allEvents.setAll(events);
            filterAndDisplay();
            loadRecommendations();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement événements", e);
            showAlert("Erreur", "Impossible de charger les événements.");
        }
    }

    private void loadRecommendations() {
        if (!eventsContainer.getChildren().isEmpty()
                && eventsContainer.getChildren().get(0) instanceof VBox
                && ((VBox) eventsContainer.getChildren().get(0)).getId() != null
                && "recoSection".equals(((VBox) eventsContainer.getChildren().get(0)).getId())) {
            eventsContainer.getChildren().remove(0);
        }
        if (currentUserEmail == null || currentUserEmail.isEmpty()) return;
        try {
            User user = userService.getByEmail(currentUserEmail);
            if (user == null) return;
            List<Event> recommendations = new RecommendationService().getRecommendationsForUser(user, 4);
            if (!recommendations.isEmpty()) {
                VBox recoSection = createRecommendationSection(recommendations);
                eventsContainer.getChildren().add(0, recoSection);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur recommandations", e);
        }
    }

    private VBox createRecommendationSection(List<Event> recommendations) {
        VBox section = new VBox(15);
        section.setId("recoSection");
        section.setPadding(new Insets(0, 0, 20, 0));
        Label title = new Label("🎯 Recommandés pour vous");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-border-color: #D4AF37; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 10 0;");
        FlowPane cardsPane = new FlowPane(20, 20);
        cardsPane.setAlignment(Pos.CENTER_LEFT);
        for (Event e : recommendations) {
            cardsPane.getChildren().add(createCompactEventCard(e));
        }
        section.getChildren().addAll(title, cardsPane);
        return section;
    }

    private VBox createCompactEventCard(Event event) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-padding: 12;");
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setOnMouseClicked(e -> openSeatSelection(event));

        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);
        Label dateLoc = new Label(event.getEventDate().format(dateFormatter) + " | " + event.getLocation());
        dateLoc.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        Label price = new Label(event.getPrice() + " DT");
        price.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #D4AF37;");

        card.getChildren().addAll(title, dateLoc, price);
        return card;
    }

    private void filterAndDisplay() {
        String search = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();
        String sortKey = sortCombo.getValue();

        ObservableList<Event> filtered = FXCollections.observableArrayList();

        for (Event e : allEvents) {
            boolean matchSearch = search.isEmpty() ||
                    e.getTitle().toLowerCase().contains(search) ||
                    (e.getDescription() != null && e.getDescription().toLowerCase().contains(search)) ||
                    (e.getLocation() != null && e.getLocation().toLowerCase().contains(search));
            boolean matchCategory = category.equals("Toutes") || e.getCategory().equals(category);
            if (matchSearch && matchCategory) filtered.add(e);
        }

        if (sortKey != null) {
            switch (sortKey) {
                case "Date (croissante)" -> filtered.sort(Comparator.comparing(Event::getEventDate));
                case "Date (décroissante)" -> filtered.sort(Comparator.comparing(Event::getEventDate).reversed());
                case "Prix (croissant)" -> filtered.sort(Comparator.comparing(Event::getPrice));
                case "Prix (décroissant)" -> filtered.sort(Comparator.comparing(Event::getPrice).reversed());
            }
        }

        displayEvents(filtered);
    }

    private void displayEvents(ObservableList<Event> events) {
        VBox recoSection = null;
        if (!eventsContainer.getChildren().isEmpty()
                && eventsContainer.getChildren().get(0) instanceof VBox
                && ((VBox) eventsContainer.getChildren().get(0)).getId() != null
                && "recoSection".equals(((VBox) eventsContainer.getChildren().get(0)).getId())) {
            recoSection = (VBox) eventsContainer.getChildren().remove(0);
        }
        eventsContainer.getChildren().clear();
        if (recoSection != null) {
            eventsContainer.getChildren().add(recoSection);
        }

        if (events.isEmpty()) {
            Label empty = new Label("Aucun événement ne correspond à vos critères.");
            empty.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
            eventsContainer.getChildren().add(empty);
            return;
        }

        Map<String, List<Event>> grouped = events.stream()
                .collect(Collectors.groupingBy(Event::getCategory));

        for (Map.Entry<String, List<Event>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<Event> catEvents = entry.getValue();

            Label categoryTitle = new Label(category);
            categoryTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e; -fx-padding: 20 0 10 20; -fx-border-color: #D4AF37; -fx-border-width: 0 0 2 0;");
            eventsContainer.getChildren().add(categoryTitle);

            TilePane cardsPane = new TilePane();
            cardsPane.setHgap(20);
            cardsPane.setVgap(20);
            cardsPane.setPrefColumns(3);
            cardsPane.setAlignment(Pos.TOP_LEFT);
            cardsPane.setPadding(new Insets(10, 0, 20, 0));

            for (Event ev : catEvents) {
                cardsPane.getChildren().add(createEventCard(ev));
            }
            eventsContainer.getChildren().add(cardsPane);
        }
    }

    // ==================== VRAIE MÉTHODE createEventCard ====================
    private VBox createEventCard(Event event) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 8); -fx-padding: 0 0 15 0; -fx-min-height: 480px; -fx-pref-width: 280px; -fx-min-width: 280px; -fx-max-width: 280px;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 25, 0, 0, 12); -fx-padding: 0 0 15 0; -fx-min-height: 480px; -fx-pref-width: 280px; -fx-min-width: 280px; -fx-max-width: 280px;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 8); -fx-padding: 0 0 15 0; -fx-min-height: 480px; -fx-pref-width: 280px; -fx-min-width: 280px; -fx-max-width: 280px;"));

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);
        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            try { imageView.setImage(new Image(event.getImageUrl())); } catch (Exception e) {}
        }
        if (imageView.getImage() == null) {
            try { imageView.setImage(new Image(getClass().getResourceAsStream("/images/default.png"))); } catch (Exception e) {}
        }

        Region overlay = new Region();
        overlay.setPrefHeight(60);
        overlay.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.7), transparent);");
        overlay.setMouseTransparent(true);

        Label categoryBadge = new Label(event.getCategory());
        categoryBadge.setStyle("-fx-background-color: #D4AF37; -fx-text-fill: #1a1a2e; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 20;");
        StackPane.setAlignment(categoryBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(categoryBadge, new Insets(10, 10, 0, 0));

        Label dateBadge = new Label(event.getEventDate().format(dateFormatter));
        dateBadge.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 15;");
        StackPane.setAlignment(dateBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(dateBadge, new Insets(0, 0, 10, 10));

        imageContainer.getChildren().addAll(imageView, overlay, categoryBadge, dateBadge);

        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(12, 15, 15, 15));

        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        Label location = new Label("📍 " + event.getLocation());
        location.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        location.setWrapText(true);

        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(event.getPrice() + " DT");
        price.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #D4AF37;");

        Button listenBtn = new Button("🔊");
        listenBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand;");
        listenBtn.setOnAction(e -> {
            int available = event.getMaxCapacity() - event.getCurrentCapacity();
            String message = String.format(
                    "Événement : %s. Date : %s. Lieu : %s. Prix : %.2f DT. %d places disponibles.",
                    event.getTitle(), event.getEventDate().format(dateFormatter),
                    event.getLocation(), event.getPrice(), available);
            TextToSpeechUtil.speak(message);
        });
        priceBox.getChildren().addAll(price, listenBtn);

        int available = event.getMaxCapacity() - event.getCurrentCapacity();
        Label availability = new Label(available + " places disponibles");
        if (available > 20) availability.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px; -fx-font-weight: bold;");
        else if (available > 0) availability.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 12px; -fx-font-weight: bold;");
        else availability.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button reserveBtn = new Button("Réserver");
        reserveBtn.setStyle("-fx-background-color: linear-gradient(to right, #D4AF37, #c49b29); -fx-text-fill: #1a1a2e; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 8 20; -fx-cursor: hand;");
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setOnAction(e -> openSeatSelection(event));

        Button sponsorBtn = new Button("Sponsoriser");
        sponsorBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 8 20; -fx-cursor: hand;");
        sponsorBtn.setMaxWidth(Double.MAX_VALUE);
        sponsorBtn.setOnAction(e -> showAlert("Sponsoring", "Fonctionnalité à venir – intégration avec le module sponsor."));

        contentBox.getChildren().addAll(title, location, priceBox, availability, reserveBtn, sponsorBtn);
        card.getChildren().addAll(imageContainer, contentBox);
        return card;
    }

    // ==================== MÉTHODES DE NAVIGATION (PLEIN ÉCRAN) ====================
    private void openSeatSelection(Event event) {
        if (event.getMaxCapacity() - event.getCurrentCapacity() <= 0) {
            showAlert("Complet", "Cet événement n'a plus de places disponibles.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/seat_selection.fxml"));
            Parent root = loader.load();
            SeatSelectionController controller = loader.getController();
            controller.setEvent(event);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Choisissez vos places - " + event.getTitle());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(true);  // redimensionnable
            // ne pas maximiser cette fenêtre secondaire
            stage.showAndWait();

            loadEvents();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur ouverture sélection sièges", e);
            showAlert("Erreur", "Impossible d'ouvrir la sélection des places.");
        }
    }

    @FXML
    private void handleMyReservations() {
        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            showAlert("Non connecté", "Veuillez vous connecter pour voir vos réservations.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/my_reservations.fxml"));
            Parent root = loader.load();
            MyReservationsController controller = loader.getController();
            controller.setUserEmail(currentUserEmail);
            Stage stage = (Stage) eventsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Mes réservations - Museum Digital");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur ouverture historique", e);
            showAlert("Erreur", "Impossible d'ouvrir vos réservations.");
        }
    }

    @FXML
    private void toggleChatbot() {
        openChatbot();
    }

    private void openChatbot() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chatbot.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Assistant Museum Digital");
            stage.initModality(javafx.stage.Modality.NONE);
            stage.setResizable(false);  // fenêtre du chatbot non redimensionnable
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur ouverture chatbot", e);
            showAlert("Erreur", "Impossible d'ouvrir l'assistant.");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) eventsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Museum Digital — Connexion");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur déconnexion", e);
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