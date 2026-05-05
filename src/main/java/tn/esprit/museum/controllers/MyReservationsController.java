package tn.esprit.museum.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.ReservationService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyReservationsController {

    private static final Logger LOGGER = Logger.getLogger(MyReservationsController.class.getName());

    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, String> eventColumn;
    @FXML private TableColumn<Reservation, String> dateColumn;
    @FXML private TableColumn<Reservation, Integer> ticketsColumn;
    @FXML private TableColumn<Reservation, String> totalColumn;
    @FXML private TableColumn<Reservation, String> statusColumn;
    @FXML private TableColumn<Reservation, String> seatsColumn;
    @FXML private TableColumn<Reservation, Void> calendarColumn;
    @FXML private Label userWelcomeLabel;

    private ReservationService reservationService;
    private String userEmail;
    private UserHomeController parentController;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setParentController(UserHomeController controller) {
        this.parentController = controller;
    }

    public void setUserEmail(String email) {
        this.userEmail = email;
        if (userWelcomeLabel != null && email != null && !email.isEmpty()) {
            userWelcomeLabel.setText("Bienvenue, " + email);
        }
        loadReservations();
    }

    @FXML
    public void initialize() {
        reservationService = new ReservationService();

        eventColumn.setCellValueFactory(new PropertyValueFactory<>("eventTitle"));
        ticketsColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfTickets"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        dateColumn.setCellValueFactory(cellData -> {
            var eventDate = cellData.getValue().getEventDate();
            return new SimpleStringProperty(eventDate != null ? eventDate.format(dateFormatter) : "");
        });
        totalColumn.setCellValueFactory(cellData -> {
            var price = cellData.getValue().getTotalPrice();
            return new SimpleStringProperty(price != null ? price + " DT" : "0 DT");
        });
        seatsColumn.setCellValueFactory(cellData -> {
            String seats = cellData.getValue().getSpecialRequests();
            if (seats != null && seats.startsWith("Sièges: ")) seats = seats.substring(8);
            return new SimpleStringProperty(seats != null ? seats : "");
        });

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "CONFIRMEE"  -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "EN_ATTENTE" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    case "ANNULEE"    -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    case "REFUSEE"    -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });

        calendarColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("📅");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14px;");
                btn.setOnAction(e -> openGoogleCalendar(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) { setGraphic(empty ? null : btn); }
        });
    }

    private void loadReservations() {
        if (userEmail == null || userEmail.isEmpty()) return;
        try {
            ObservableList<Reservation> list = FXCollections.observableArrayList(
                    reservationService.getByClientEmail(userEmail));
            reservationsTable.setItems(list);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement réservations", e);
            showAlert("Erreur", "Impossible de charger vos réservations.");
        }
    }

    private void openGoogleCalendar(Reservation reservation) {
        try {
            // Utilise les données déjà chargées dans la réservation
            LocalDate eventDate = reservation.getEventDate();
            if (eventDate == null) {
                showAlert("Erreur", "La date de l'événement est introuvable.");
                return;
            }

            java.time.LocalTime eventTime = reservation.getEventTime() != null
                    ? reservation.getEventTime()
                    : java.time.LocalTime.of(10, 0); // heure par défaut si absente

            LocalDateTime start = eventDate.atTime(eventTime);
            LocalDateTime end = start.plusHours(2);
            DateTimeFormatter gcalFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

            String title = "🎟️ " + reservation.getEventTitle() + " (" + reservation.getNumberOfTickets() + " place(s))";
            String description = "Réservation Museum Digital\nÉvénement : " + reservation.getEventTitle() +
                    "\nBillets : " + reservation.getNumberOfTickets();

            String url = "https://calendar.google.com/calendar/render?action=TEMPLATE"
                    + "&text=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                    + "&dates=" + start.format(gcalFormat) + "/" + end.format(gcalFormat)
                    + "&details=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                    + "&sf=true&output=xml&ctz=Africa/Tunis";

            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur Google Calendar", e);
            showAlert("Erreur", "Impossible d'ouvrir Google Calendar.");
        }
    }

    /** Bouton "← Retour au catalogue" en bas de page */
    @FXML private void handleBack() {
        navigateToEvents();
    }

    /** Bouton "📅 Événements" dans la navbar du haut */
    @FXML private void handleBackToEvents() {
        navigateToEvents();
    }

    private void navigateToEvents() {
        if (parentController != null) {
            parentController.handleShowEventsFrontPublic(this.userEmail);
            return;
        }
        // Fallback standalone
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/events_front.fxml"));
            Parent root = loader.load();
            EventFrontController controller = loader.getController();
            controller.setCurrentUserEmail(this.userEmail);
            Stage stage = (Stage) reservationsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Catalogue - Museum Digital");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur retour au catalogue", e);
            showAlert("Erreur", "Impossible de retourner au catalogue.");
        }
    }

    @FXML private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) reservationsTable.getScene().getWindow();
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

