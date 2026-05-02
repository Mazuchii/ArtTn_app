package tn.esprit.museum.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.ReservationService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
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

    private ReservationService reservationService;
    private EventService eventService;
    private String userEmail;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setUserEmail(String email) {
        this.userEmail = email;
        loadReservations();
    }

    @FXML
    public void initialize() {
        reservationService = new ReservationService();
        eventService = new EventService();

        eventColumn.setCellValueFactory(new PropertyValueFactory<>("eventTitle"));
        ticketsColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfTickets"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Date de l'événement
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

        statusColumn.setCellFactory(col -> new TableCell<Reservation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "CONFIRMEE" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "EN_ATTENTE" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    case "ANNULEE"    -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    case "REFUSEE"    -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            }
        });

        // Colonne calendrier
        calendarColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("📅");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14px;");
                btn.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    openGoogleCalendar(reservation);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void loadReservations() {
        if (userEmail == null || userEmail.isEmpty()) return;
        try {
            ObservableList<Reservation> list = FXCollections.observableArrayList(
                    reservationService.getByClientEmail(userEmail)
            );
            reservationsTable.setItems(list);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement réservations", e);
            showAlert("Erreur", "Impossible de charger vos réservations.");
        }
    }

    // Ajout à Google Calendar
    private void openGoogleCalendar(Reservation reservation) {
        try {
            Event event = eventService.getById(reservation.getEventId());
            if (event == null) {
                showAlert("Erreur", "Événement non trouvé.");
                return;
            }

            LocalDateTime start = event.getEventDate().atTime(event.getEventTime());
            LocalDateTime end = start.plusHours(2);
            DateTimeFormatter gcalFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
            String startStr = start.format(gcalFormat);
            String endStr = end.format(gcalFormat);

            String title = "🎟️ " + event.getTitle() + " (" + reservation.getNumberOfTickets() + " place(s))";
            String description = "Réservation Museum Digital\n\n"
                    + "Événement : " + event.getTitle() + "\n"
                    + "Lieu : " + event.getLocation() + "\n"
                    + "Billets : " + reservation.getNumberOfTickets() + "\n"
                    + "Sièges : " + (reservation.getSelectedSeats() != null ? reservation.getSelectedSeats() : "Non spécifiés");

            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8);
            String encodedLocation = URLEncoder.encode(event.getLocation(), StandardCharsets.UTF_8);

            String url = "https://calendar.google.com/calendar/render?action=TEMPLATE"
                    + "&text=" + encodedTitle
                    + "&dates=" + startStr + "/" + endStr
                    + "&details=" + encodedDesc
                    + "&location=" + encodedLocation
                    + "&sf=true&output=xml"
                    + "&ctz=Africa/Tunis";

            Desktop.getDesktop().browse(URI.create(url));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur Google Calendar", e);
            showAlert("Erreur", "Impossible d'ouvrir Google Calendar.");
        }
    }

    // ==================== RETOUR AU CATALOGUE (PLEIN ÉCRAN) ====================
    @FXML
    private void handleBack() {
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