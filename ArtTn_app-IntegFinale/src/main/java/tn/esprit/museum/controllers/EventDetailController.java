package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.ReservationService;
import tn.esprit.museum.utils.ImageLoader;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventDetailController {

    private static final Logger LOGGER = Logger.getLogger(EventDetailController.class.getName());

    @FXML private ImageView eventImageView;
    @FXML private Label eventTitle;
    @FXML private Label eventDate;
    @FXML private Label eventLocation;
    @FXML private Label eventCategory;
    @FXML private Label eventPrice;
    @FXML private Label eventAvailability;
    @FXML private TextArea eventDescription;
    @FXML private TextField clientNameField;
    @FXML private TextField clientEmailField;
    @FXML private TextField clientPhoneField;
    @FXML private Spinner<Integer> ticketsSpinner;

    private Event currentEvent;
    private ReservationService reservationService;
    private EventService eventService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setEvent(Event event) {
        this.currentEvent = event;
        displayEventDetails();
        configureSpinner();
    }

    @FXML
    public void initialize() {
        reservationService = new ReservationService();
        eventService = new EventService();
    }

    private void displayEventDetails() {
        if (currentEvent == null) return;
        if (eventTitle != null) eventTitle.setText(currentEvent.getTitle());
        if (eventDate != null) eventDate.setText(
                (currentEvent.getEventDate() != null ? currentEvent.getEventDate().format(dateFormatter) : "N/A")
                + " à " + (currentEvent.getEventTime() != null ? currentEvent.getEventTime() : "N/A"));
        if (eventLocation != null) eventLocation.setText(currentEvent.getLocation());
        if (eventCategory != null) eventCategory.setText(currentEvent.getCategory());
        if (eventPrice != null) eventPrice.setText(currentEvent.getPrice() + " DT");

        // current_capacity = places RESTANTES depuis la DB
        try {
            int available = eventService.getAvailableSeatsFromDB(currentEvent.getId());
            currentEvent.setCurrentCapacity(available);
            if (eventAvailability != null) eventAvailability.setText(available + " places disponibles");
        } catch (SQLException e) {
            int available = currentEvent.getCurrentCapacity();
            if (eventAvailability != null) eventAvailability.setText(available + " places disponibles");
            LOGGER.log(Level.WARNING, "Impossible de calculer les places depuis la DB", e);
        }

        if (eventDescription != null) eventDescription.setText(currentEvent.getDescription());
        if (eventImageView != null && currentEvent.getImageUrl() != null && !currentEvent.getImageUrl().isEmpty()) {
            eventImageView.setImage(ImageLoader.load(currentEvent.getImageUrl()));
        } else if (eventImageView != null) {
            eventImageView.setImage(ImageLoader.load(null));
        }
    }

    private void configureSpinner() {
        if (ticketsSpinner == null) return;
        // current_capacity = places restantes directement
        int max = currentEvent.getCurrentCapacity();
        if (max <= 0) max = 0;
        ticketsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, max), 1));
    }

    @FXML private void handleReserve() {
        String name  = clientNameField  != null ? clientNameField.getText().trim()  : "";
        String email = clientEmailField != null ? clientEmailField.getText().trim() : "";
        String phone = clientPhoneField != null ? clientPhoneField.getText().trim() : "";
        int tickets  = ticketsSpinner   != null ? ticketsSpinner.getValue() : 1;

        if (name.isEmpty() || email.isEmpty()) { showAlert("Erreur", "Veuillez saisir votre nom et votre email."); return; }
        if (!email.contains("@")) { showAlert("Erreur", "Email invalide."); return; }

        try {
            BigDecimal totalPrice = currentEvent.getPrice().multiply(BigDecimal.valueOf(tickets));
            Reservation r = new Reservation();
            r.setEventId(currentEvent.getId());
            r.setClientName(name);
            r.setClientEmail(email);
            r.setClientPhone(phone);
            r.setNumberOfTickets(tickets);
            r.setTotalPrice(totalPrice);
            r.setStatus("EN_ATTENTE");
            r.setPaymentStatus("EN_ATTENTE");
            r.setReservationDate(LocalDateTime.now());
            reservationService.insert(r);
            showAlert("Succès", "Réservation enregistrée !\nTotal : " + totalPrice + " DT");
            closeWindow();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur réservation", e);
            showAlert("Erreur", "Impossible de réserver : " + e.getMessage());
        }
    }

    @FXML private void handleClose() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) eventTitle.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

