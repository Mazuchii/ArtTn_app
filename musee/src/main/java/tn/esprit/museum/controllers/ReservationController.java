package tn.esprit.museum.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.ReservationService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReservationController {

    private static final Logger LOGGER = Logger.getLogger(ReservationController.class.getName());

    @FXML private Label titleLabel;
    @FXML private Label eventInfoLabel;
    @FXML private Label dashboardStatsReservations;
    @FXML private Label dashboardStatsTickets;
    @FXML private Label dashboardStatsRevenue;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, Integer> idColumn;
    @FXML private TableColumn<Reservation, String> clientNameColumn;
    @FXML private TableColumn<Reservation, String> clientEmailColumn;
    @FXML private TableColumn<Reservation, Integer> ticketsColumn;
    @FXML private TableColumn<Reservation, BigDecimal> totalColumn;
    @FXML private TableColumn<Reservation, String> statusColumn;
    @FXML private TableColumn<Reservation, String> dateColumn;

    private ReservationService reservationService;
    private EventService eventService;
    private ObservableList<Reservation> reservationList;
    private Integer eventFilterId = null;
    private String eventFilterTitle = null;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        try {
            reservationService = new ReservationService();
            eventService = new EventService();
            reservationList = FXCollections.observableArrayList();

            setupTable();
            setupFilters();
            loadReservations();
            updateStats();

            reservationsTable.setPlaceholder(new Label("📭 Aucune réservation pour le moment.\nLes réservations apparaîtront ici une fois effectuées."));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur init: " + e.getMessage(), e);
        }
    }

    public void setEventFilter(int eventId, String eventTitle) {
        this.eventFilterId = eventId;
        this.eventFilterTitle = eventTitle;
        if (eventInfoLabel != null)
            eventInfoLabel.setText("Filtré par événement : " + eventTitle);
        if (titleLabel != null)
            titleLabel.setText("🎟️ Réservations - " + eventTitle);
        loadReservations();
        updateStats();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        clientNameColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getClientName();
            return new SimpleStringProperty(name == null || name.isBlank() ? "Invité" : name);
        });
        clientEmailColumn.setCellValueFactory(cellData -> {
            String email = cellData.getValue().getClientEmail();
            return new SimpleStringProperty(email == null || email.isBlank() ? "non renseigné" : email);
        });
        ticketsColumn.setCellValueFactory(new PropertyValueFactory<>("numberOfTickets"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getReservationDate();
            return new SimpleStringProperty(date != null ? date.format(dateFormatter) : "");
        });

        totalColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item + " DT");
            }
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
                    default           -> setStyle("");
                }
            }
        });
    }

    private void setupFilters() {
        statusFilterCombo.getItems().addAll("Tous", "CONFIRMEE", "EN_ATTENTE", "ANNULEE", "REFUSEE");
        statusFilterCombo.setValue("Tous");
        statusFilterCombo.valueProperty().addListener((obs, o, n) -> filterReservations());
    }

    private void loadReservations() {
        try {
            List<Reservation> reservations;
            if (eventFilterId != null)
                reservations = reservationService.getByEventId(eventFilterId);
            else
                reservations = reservationService.getAll();
            reservationList.setAll(reservations);
            reservationsTable.setItems(reservationList);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur chargement: " + e.getMessage(), e);
            showAlert("Erreur", "Impossible de charger les réservations : " + e.getMessage());
        }
    }

    private void filterReservations() {
        String selected = statusFilterCombo.getValue();
        if (selected == null || selected.equals("Tous")) {
            reservationsTable.setItems(reservationList);
            return;
        }
        ObservableList<Reservation> filtered = FXCollections.observableArrayList();
        for (Reservation r : reservationList) {
            if (selected.equals(r.getStatus()))
                filtered.add(r);
        }
        reservationsTable.setItems(filtered);
    }

    private void updateStats() {
        try {
            if (dashboardStatsReservations == null) return;
            List<Reservation> reservations = (eventFilterId != null)
                    ? reservationService.getByEventId(eventFilterId)
                    : reservationService.getAll();
            int total = reservations.size();
            int totalTickets = reservations.stream().mapToInt(Reservation::getNumberOfTickets).sum();
            BigDecimal revenue = reservations.stream()
                    .filter(r -> "CONFIRMEE".equals(r.getStatus()))
                    .map(Reservation::getTotalPrice)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dashboardStatsReservations.setText(String.valueOf(total));
            dashboardStatsTickets.setText(String.valueOf(totalTickets));
            dashboardStatsRevenue.setText(revenue + " DT");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur stats: " + e.getMessage(), e);
        }
    }

    // ==================== NAVIGATION (PLEIN ÉCRAN ET REDIMENSIONNABLE) ====================
    @FXML
    private void handleShowEvents() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/events.fxml"));
            Stage stage = (Stage) reservationsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Gestion des Événements - Museum Digital");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur navigation: " + e.getMessage(), e);
            showAlert("Erreur", "Impossible d'ouvrir les événements : " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment vous déconnecter ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                Stage stage = (Stage) reservationsTable.getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.setTitle("Museum Digital — Connexion");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erreur déconnexion", e);
            }
        }
    }

    // ==================== ACTIONS SUR RÉSERVATIONS ====================
    @FXML
    private void handleRefresh() {
        loadReservations();
        updateStats();
    }

    @FXML
    private void handleAddReservation() {
        try {
            List<Event> events = eventService.getAll();
            if (events.isEmpty()) {
                showAlert("Aucun événement", "Aucun événement disponible pour réserver.");
                return;
            }

            Dialog<Reservation> dialog = new Dialog<>();
            dialog.setTitle("➕ Nouvelle réservation");
            dialog.getDialogPane().setPrefWidth(450);

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.setPadding(new Insets(20));

            ComboBox<Event> eventCombo = new ComboBox<>();
            eventCombo.getItems().addAll(events);
            eventCombo.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Event e, boolean empty) {
                    super.updateItem(e, empty);
                    setText(empty || e == null ? null : e.getTitle() + " - " + e.getEventDate());
                }
            });
            eventCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Event e, boolean empty) {
                    super.updateItem(e, empty);
                    setText(empty || e == null ? null : e.getTitle());
                }
            });
            if (eventFilterId != null) {
                events.stream().filter(e -> e.getId() == eventFilterId).findFirst().ifPresent(eventCombo::setValue);
                eventCombo.setDisable(true);
            } else {
                eventCombo.getSelectionModel().selectFirst();
            }

            TextField nameField = new TextField(); nameField.setPromptText("Nom complet");
            TextField emailField = new TextField(); emailField.setPromptText("Email");
            TextField ticketsField = new TextField("1"); ticketsField.setPromptText("Nombre de billets");
            ComboBox<String> statusCombo = new ComboBox<>();
            statusCombo.getItems().addAll("EN_ATTENTE", "CONFIRMEE", "ANNULEE");
            statusCombo.setValue("EN_ATTENTE");

            Label errName = new Label(); errName.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
            Label errEmail = new Label(); errEmail.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
            Label errTickets = new Label(); errTickets.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
            Label errEvent = new Label(); errEvent.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

            int row = 0;
            grid.add(new Label("Événement *"), 0, row); grid.add(eventCombo, 1, row++);
            grid.add(errEvent, 1, row++);
            grid.add(new Label("Nom du client *"), 0, row); grid.add(nameField, 1, row++);
            grid.add(errName, 1, row++);
            grid.add(new Label("Email *"), 0, row); grid.add(emailField, 1, row++);
            grid.add(errEmail, 1, row++);
            grid.add(new Label("Nombre de billets *"), 0, row); grid.add(ticketsField, 1, row++);
            grid.add(errTickets, 1, row++);
            grid.add(new Label("Statut *"), 0, row); grid.add(statusCombo, 1, row);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okBtn.setDisable(true);

            Runnable validate = () -> {
                boolean ok = true;
                if (eventCombo.getValue() == null) { errEvent.setText("❌ Sélectionnez un événement"); ok = false; }
                else errEvent.setText("");
                String n = nameField.getText();
                if (n == null || n.trim().isEmpty()) { errName.setText("❌ Nom obligatoire"); ok = false; }
                else errName.setText("");
                String e = emailField.getText();
                if (e == null || !e.matches("\\S+@\\S+\\.\\S+")) { errEmail.setText("❌ Email invalide"); ok = false; }
                else errEmail.setText("");
                try {
                    int t = Integer.parseInt(ticketsField.getText().trim());
                    if (t <= 0) throw new NumberFormatException();
                    errTickets.setText("");
                } catch (Exception ex) { errTickets.setText("❌ Nombre > 0 requis"); ok = false; }
                okBtn.setDisable(!ok);
            };

            nameField.textProperty().addListener((o,v,n) -> validate.run());
            emailField.textProperty().addListener((o,v,n) -> validate.run());
            ticketsField.textProperty().addListener((o,v,n) -> validate.run());
            eventCombo.valueProperty().addListener((o,v,n) -> validate.run());
            dialog.setOnShown(e -> validate.run());

            dialog.setResultConverter(btn -> {
                if (btn != ButtonType.OK) return null;
                Event selectedEvent = eventCombo.getValue();
                Reservation r = new Reservation();
                r.setEventId(selectedEvent.getId());
                r.setClientName(nameField.getText().trim());
                r.setClientEmail(emailField.getText().trim());
                r.setNumberOfTickets(Integer.parseInt(ticketsField.getText().trim()));
                r.setTotalPrice(selectedEvent.getPrice().multiply(BigDecimal.valueOf(r.getNumberOfTickets())));
                r.setStatus(statusCombo.getValue());
                r.setReservationDate(LocalDateTime.now());
                return r;
            });

            dialog.showAndWait().ifPresent(r -> {
                try {
                    reservationService.insert(r);
                    loadReservations();
                    updateStats();
                    showAlert("Succès", "Réservation ajoutée !");
                } catch (SQLException ex) {
                    showAlert("Erreur", "DB: " + ex.getMessage());
                }
            });

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les événements : " + e.getMessage());
        }
    }

    @FXML
    private void handleAcceptReservation() {
        Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une réservation.");
            return;
        }
        if (!"EN_ATTENTE".equals(selected.getStatus())) {
            showAlert("Info", "Seules les réservations en attente peuvent être acceptées.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Accepter la réservation");
        confirm.setHeaderText(null);
        confirm.setContentText("Accepter la réservation de " + selected.getClientName() + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reservationService.confirmReservation(selected.getId());
                loadReservations();
                updateStats();
                showAlert("Succès", "Réservation acceptée. Email envoyé au client.");
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void handleRejectReservation() {
        Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une réservation.");
            return;
        }
        if (!"EN_ATTENTE".equals(selected.getStatus())) {
            showAlert("Info", "Seules les réservations en attente peuvent être refusées.");
            return;
        }
        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Refuser la réservation");
        reasonDialog.setHeaderText("Motif du refus (optionnel)");
        reasonDialog.setContentText("Motif :");
        Optional<String> reasonResult = reasonDialog.showAndWait();
        String reason = reasonResult.orElse("");
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Refuser la réservation");
        confirm.setHeaderText(null);
        confirm.setContentText("Refuser la réservation de " + selected.getClientName() + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reservationService.rejectReservation(selected.getId(), reason);
                loadReservations();
                updateStats();
                showAlert("Succès", "Réservation refusée. Email envoyé au client.");
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancelReservation() {
        Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une réservation.");
            return;
        }
        String status = selected.getStatus();
        if ("CONFIRMEE".equals(status) || "REFUSEE".equals(status)) {
            showAlert("Info", "Cette réservation ne peut plus être annulée (elle a déjà été traitée).");
            return;
        }
        if ("ANNULEE".equals(status)) {
            showAlert("Info", "Cette réservation est déjà annulée.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la réservation");
        confirm.setHeaderText(null);
        confirm.setContentText("Annuler la réservation pour " + selected.getClientName() + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reservationService.cancelReservation(selected.getId());
                loadReservations();
                updateStats();
                showAlert("Succès", "Réservation annulée !");
            } catch (Exception e) {
                showAlert("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteReservation() {
        Reservation selected = reservationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Attention", "Veuillez sélectionner une réservation.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la réservation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer définitivement la réservation de " + selected.getClientName() + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                reservationService.delete(selected.getId());
                loadReservations();
                updateStats();
                showAlert("Succès", "Réservation supprimée !");
            } catch (Exception e) {
                showAlert("Erreur", e.getMessage());
            }
        }
    }

    // ==================== EXPORT PDF ====================
    @FXML
    private void handleExportPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les réservations");
        fc.setInitialFileName("reservations_" + LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(reservationsTable.getScene().getWindow());
        if (file == null) return;

        try {
            generateReservationsPdf(file.getAbsolutePath());
            showAlert("✅ Export réussi", "PDF exporté :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Erreur PDF", "Impossible de générer :\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateReservationsPdf(String filePath) throws Exception {
        // ... (votre code existant – inchangé) ...
        // Assurez-vous que cette méthode est présente (vous l'avez déjà).
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}