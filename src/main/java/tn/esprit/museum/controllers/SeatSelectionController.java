package tn.esprit.museum.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.ReservationService;
import tn.esprit.museum.utils.SessionManager;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SeatSelectionController {

    private static final Logger LOGGER = Logger.getLogger(SeatSelectionController.class.getName());

    @FXML private Label eventTitleLabel;
    @FXML private ImageView eventImageView;
    @FXML private Label eventTitle;
    @FXML private Label eventLocation;
    @FXML private Label eventDateTime;
    @FXML private Label eventPricePerTicket;
    @FXML private Label availableSeatsLabel;
    @FXML private GridPane seatsGrid;
    @FXML private TextField promoCodeField;
    @FXML private Label discountLabel;
    @FXML private Label selectedSeatsLabel;
    @FXML private Label totalPriceLabel;
    @FXML private TextField clientNameField;
    @FXML private TextField clientEmailField;
    @FXML private TextField clientPhoneField;

    private Event currentEvent;
    private ReservationService reservationService;
    private EventService eventService;

    private final int ROWS = 8;
    private final int COLS = 10;
    private Button[][] seatButtons;
    private boolean[][] selected;
    private boolean[][] isVip;
    private boolean[][] isReserved;
    private final List<String> selectedSeats = new ArrayList<>();
    private double discountPercent = 0;
    private double currentTotal = 0;

    @FXML
    public void initialize() {
        reservationService = new ReservationService();
        eventService = new EventService();
    }

    public void setEvent(Event event) {
        this.currentEvent = event;
        displayEventInfo();
        generateSeatMap();

        if (SessionManager.getCurrentUser() != null) {
            if (clientNameField != null) clientNameField.setText(SessionManager.getCurrentUser().getFullName());
            if (clientEmailField != null) clientEmailField.setText(SessionManager.getCurrentUser().getEmail());
        }

        promoCodeField.textProperty().addListener((obs, old, val) -> {
            if ("ArtTn".equalsIgnoreCase(val.trim())) {
                discountPercent = 0.10;
                discountLabel.setText("🎉 Code promo ArtTn appliqué : -10%");
            } else {
                discountPercent = 0;
                discountLabel.setText("");
            }
            updateSummary();
        });
    }

    public void setUserEmail(String email) {
        if (clientEmailField != null && email != null) clientEmailField.setText(email);
    }

    private void displayEventInfo() {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        if (eventTitleLabel != null) eventTitleLabel.setText("📅 " + currentEvent.getTitle());
        if (eventTitle != null) eventTitle.setText(currentEvent.getTitle());
        if (eventLocation != null) eventLocation.setText("📍 " + currentEvent.getLocation());

        String dateStr = currentEvent.getEventDate() != null ? currentEvent.getEventDate().format(dateFmt) : "N/A";
        String timeStr = currentEvent.getEventTime() != null ? currentEvent.getEventTime().format(timeFmt) : "N/A";
        if (eventDateTime != null) eventDateTime.setText("🕐 " + dateStr + " à " + timeStr);
        if (eventPricePerTicket != null) eventPricePerTicket.setText("💰 " + currentEvent.getPrice() + " DT standard | VIP +10 DT");

        int available = currentEvent.getMaxCapacity() - currentEvent.getCurrentCapacity();
        if (availableSeatsLabel != null) availableSeatsLabel.setText("✅ " + available + " places disponibles");

        if (eventImageView != null && currentEvent.getImageUrl() != null && !currentEvent.getImageUrl().isEmpty()) {
            try { eventImageView.setImage(new Image(currentEvent.getImageUrl())); } catch (Exception ignored) {}
        }
    }

    private void generateSeatMap() {
        selected = new boolean[ROWS][COLS];
        isVip = new boolean[ROWS][COLS];
        isReserved = new boolean[ROWS][COLS];
        seatButtons = new Button[ROWS][COLS];
        seatsGrid.getChildren().clear();

        // Charger les sièges déjà réservés depuis la DB
        List<String> reservedSeats = new ArrayList<>();
        try {
            reservedSeats = reservationService.getReservedSeatsByEventId(currentEvent.getId());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Impossible de charger les sièges réservés", e);
        }

        // Construire un Set pour lookup O(1)
        java.util.Set<String> reservedSet = new java.util.HashSet<>(reservedSeats);

        for (int c = 0; c < COLS; c++) {
            Label lbl = new Label(String.valueOf(c + 1));
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
            seatsGrid.add(lbl, c + 1, 0);
        }
        for (int r = 0; r < ROWS; r++) {
            Label lbl = new Label(String.valueOf((char)('A' + r)));
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
            seatsGrid.add(lbl, 0, r + 1);
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                boolean vip = (c == 0 || c == COLS - 1);
                isVip[r][c] = vip;
                String label = getSeatLabel(r, c);
                boolean reserved = reservedSet.contains(label);
                isReserved[r][c] = reserved;

                Button btn = new Button(label);
                btn.setPrefSize(52, 38);
                btn.setMinSize(52, 38);
                btn.setStyle(getSeatStyle(vip, false, reserved));

                if (reserved) {
                    btn.setDisable(true);
                    // Tooltip pour indiquer que le siège est pris
                    Tooltip tip = new Tooltip("Siège " + label + " — déjà réservé");
                    tip.setStyle("-fx-font-size: 11px;");
                    Tooltip.install(btn, tip);
                } else {
                    final int row = r, col = c;
                    btn.setOnAction(e -> toggleSeat(row, col));
                }
                seatButtons[r][c] = btn;
                seatsGrid.add(btn, c + 1, r + 1);
            }
        }
    }

    private String getSeatLabel(int r, int c) { return String.valueOf((char)('A' + r)) + (c + 1); }

    private String getSeatStyle(boolean vip, boolean sel, boolean reserved) {
        if (reserved) return "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-opacity: 0.85;";
        if (sel)      return "-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;";
        if (vip)      return "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;";
        return "-fx-background-color: #D4AF37; -fx-text-fill: #1a1a2e; -fx-font-weight: bold; -fx-background-radius: 6;";
    }

    private double getSeatPrice(boolean vip) { return currentEvent.getPrice().doubleValue() + (vip ? 10 : 0); }

    private void toggleSeat(int r, int c) {
        if (isReserved[r][c]) return; // sécurité supplémentaire
        selected[r][c] = !selected[r][c];
        String label = getSeatLabel(r, c);
        if (selected[r][c]) selectedSeats.add(label);
        else selectedSeats.remove(label);
        seatButtons[r][c].setStyle(getSeatStyle(isVip[r][c], selected[r][c], false));
        updateSummary();
    }

    private void updateSummary() {
        if (selectedSeatsLabel != null)
            selectedSeatsLabel.setText(selectedSeats.isEmpty() ? "Aucun siège sélectionné" : "Sièges : " + String.join(", ", selectedSeats));
        double subtotal = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (selected[r][c]) subtotal += getSeatPrice(isVip[r][c]);
        currentTotal = subtotal * (1 - discountPercent);
        if (totalPriceLabel != null) totalPriceLabel.setText(String.format("Total : %.2f DT", currentTotal));
    }

    @FXML private void handleConfirm() {
        if (selectedSeats.isEmpty()) { showAlert("Erreur", "Veuillez sélectionner au moins un siège."); return; }

        String name  = clientNameField  != null ? clientNameField.getText().trim()  : "";
        String email = clientEmailField != null ? clientEmailField.getText().trim() : "";
        String phone = clientPhoneField != null ? clientPhoneField.getText().trim() : "";

        if ((name.isEmpty() || email.isEmpty()) && SessionManager.getCurrentUser() != null) {
            if (name.isEmpty())  name  = SessionManager.getCurrentUser().getFullName();
            if (email.isEmpty()) email = SessionManager.getCurrentUser().getEmail();
        }

        if (name.isEmpty()) { showAlert("Erreur", "Le nom est obligatoire."); return; }
        if (email.isEmpty() || !email.contains("@")) { showAlert("Erreur", "Email invalide."); return; }

        try {
            Reservation r = new Reservation();
            r.setEventId(currentEvent.getId());
            r.setClientName(name);
            r.setClientEmail(email);
            r.setClientPhone(phone);
            r.setNumberOfTickets(selectedSeats.size());
            r.setTotalPrice(BigDecimal.valueOf(currentTotal));
            r.setStatus("EN_ATTENTE");
            r.setPaymentMethod("CARTE");
            r.setReservationDate(LocalDateTime.now());
            r.setSpecialRequests("Sièges: " + String.join(", ", selectedSeats)
                    + (promoCodeField.getText().isEmpty() ? "" : " | Promo: " + promoCodeField.getText()));
            r.setSelectedSeats(String.join(",", selectedSeats));

            reservationService.insert(r);
            showAlert("✅ Réservation confirmée !",
                    "Client : " + name + "\nEmail : " + email +
                    "\nSièges : " + String.join(", ", selectedSeats) +
                    "\nTotal : " + String.format("%.2f", currentTotal) + " DT");
            closeWindow();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur réservation", e);
            showAlert("Erreur DB", e.getMessage());
        }
    }

    @FXML private void handleClose() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) seatsGrid.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

