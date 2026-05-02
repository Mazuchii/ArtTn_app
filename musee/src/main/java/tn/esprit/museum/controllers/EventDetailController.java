package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.ReservationService;

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
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setEvent(Event event) {
        this.currentEvent = event;
        displayEventDetails();
        configureSpinner();
    }

    @FXML
    public void initialize() {
        reservationService = new ReservationService();
    }

    private void displayEventDetails() {
        if (currentEvent == null) return;

        eventTitle.setText(currentEvent.getTitle());
        eventDate.setText(currentEvent.getEventDate().format(dateFormatter) + " à " + currentEvent.getEventTime());
        eventLocation.setText(currentEvent.getLocation());
        eventCategory.setText(currentEvent.getCategory());
        eventPrice.setText(currentEvent.getPrice() + " DT");
        int available = currentEvent.getMaxCapacity() - currentEvent.getCurrentCapacity();
        eventAvailability.setText(available + " places disponibles");
        eventDescription.setText(currentEvent.getDescription());

        if (currentEvent.getImageUrl() != null && !currentEvent.getImageUrl().isEmpty()) {
            try {
                Image img = new Image(currentEvent.getImageUrl());
                eventImageView.setImage(img);
            } catch (Exception e) {
                eventImageView.setImage(null);
            }
        }
    }

    private void configureSpinner() {
        int max = currentEvent.getMaxCapacity() - currentEvent.getCurrentCapacity();
        if (max <= 0) max = 0;
        SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max, 1);
        ticketsSpinner.setValueFactory(factory);
    }

    @FXML
    private void handleReserve() {
        String name = clientNameField.getText().trim();
        String email = clientEmailField.getText().trim();
        String phone = clientPhoneField.getText().trim();
        int tickets = ticketsSpinner.getValue();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert("Erreur", "Veuillez saisir votre nom et votre email.");
            return;
        }
        if (!email.contains("@")) {
            showAlert("Erreur", "Email invalide.");
            return;
        }
        if (tickets <= 0) {
            showAlert("Erreur", "Nombre de billets invalide.");
            return;
        }

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

    @FXML
    private void handleClose() {
        closeWindow();
    }

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