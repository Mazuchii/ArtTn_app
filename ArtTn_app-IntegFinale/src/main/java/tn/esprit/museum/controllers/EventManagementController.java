package tn.esprit.museum.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Event;
import tn.esprit.museum.entities.Reservation;
import tn.esprit.museum.services.EventService;
import tn.esprit.museum.services.PriceOptimizationService;
import tn.esprit.museum.services.ReservationService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventManagementController {

    private static final Logger LOGGER = Logger.getLogger(EventManagementController.class.getName());

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, Integer> idColumn;
    @FXML private TableColumn<Event, String> titleColumn;
    @FXML private TableColumn<Event, LocalDate> dateColumn;
    @FXML private TableColumn<Event, String> locationColumn;
    @FXML private TableColumn<Event, Integer> capacityColumn;
    @FXML private TableColumn<Event, BigDecimal> priceColumn;
    @FXML private TableColumn<Event, String> statusColumn;
    @FXML private TableColumn<Event, String> imageColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Label statsTotalEvents;
    @FXML private Label statsUpcomingEvents;
    @FXML private Label statsTotalCapacity;

    private EventService eventService;
    private ReservationService reservationService;
    private ObservableList<Event> masterEventList;
    private ObservableList<Event> filteredList;

    @FXML
    public void initialize() {
        try {
            eventService = new EventService();
            reservationService = new ReservationService();
            masterEventList = FXCollections.observableArrayList();
            filteredList = FXCollections.observableArrayList();
            setupTable();
            setupFilters();
            loadEvents();
            updateStats();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur init: " + e.getMessage(), e);
        }
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("eventDate"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        // Affiche les places RESTANTES (current_capacity = max - réservées)
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("currentCapacity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));

        capacityColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                Event ev = getTableView().getItems().get(getIndex());
                setText(item + " / " + ev.getMaxCapacity());
                if (item == 0)       setStyle("-fx-text-fill:#e74c3c;-fx-font-weight:bold;");
                else if (item <= 10) setStyle("-fx-text-fill:#e67e22;-fx-font-weight:bold;");
                else                 setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;");
            }
        });

        dateColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });
        priceColumn.setCellFactory(col -> new TableCell<>() {
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
                    case "A_VENIR"  -> setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;");
                    case "EN_COURS" -> setStyle("-fx-text-fill:#f39c12;-fx-font-weight:bold;");
                    case "TERMINE"  -> setStyle("-fx-text-fill:#7f8c8d;-fx-font-weight:bold;");
                    case "ANNULE"   -> setStyle("-fx-text-fill:#e74c3c;-fx-font-weight:bold;");
                    default         -> setStyle("");
                }
            }
        });
        imageColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) { setGraphic(null); setText(null); return; }
                ImageView iv = new ImageView();
                iv.setFitHeight(45); iv.setFitWidth(45); iv.setPreserveRatio(true);
                try { iv.setImage(new Image(item)); setGraphic(iv); setText(null); }
                catch (Exception e) { setText("📷"); setGraphic(null); }
            }
        });
    }

    private void setupFilters() {
        categoryFilterCombo.getItems().addAll("Tous","EXPOSITION","CONFERENCE","ATELIER","VISITE_GUIDEE","SPECTACLE");
        categoryFilterCombo.setValue("Tous");
        statusFilterCombo.getItems().addAll("Tous","A_VENIR","EN_COURS","TERMINE","ANNULE");
        statusFilterCombo.setValue("Tous");
        searchField.textProperty().addListener((o,v,n) -> applyFilters());
        categoryFilterCombo.valueProperty().addListener((o,v,n) -> applyFilters());
        statusFilterCombo.valueProperty().addListener((o,v,n) -> applyFilters());
    }

    private void loadEvents() {
        try {
            List<Event> events = eventService.getAll();
            // Synchroniser current_capacity (places restantes) depuis la DB
            for (Event ev : events) {
                try {
                    ev.setCurrentCapacity(eventService.getAvailableSeatsFromDB(ev.getId()));
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, "Sync capacité event id=" + ev.getId(), ex);
                }
            }
            masterEventList.setAll(events);
            applyFilters();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger: " + e.getMessage());
        }
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();
        String cat  = categoryFilterCombo.getValue();
        String stat = statusFilterCombo.getValue();
        filteredList.clear();
        for (Event ev : masterEventList) {
            boolean ms = search.isEmpty()
                    || ev.getTitle().toLowerCase().contains(search)
                    || (ev.getLocation() != null && ev.getLocation().toLowerCase().contains(search));
            boolean mc = "Tous".equals(cat) || cat.equals(ev.getCategory());
            boolean mst = "Tous".equals(stat) || stat.equals(ev.getStatus());
            if (ms && mc && mst) filteredList.add(ev);
        }
        eventsTable.setItems(filteredList);
    }

    private void updateStats() {
        try {
            int total    = eventService.getAll().size();
            int upcoming = eventService.getUpcomingEventsCount();
            int capacity = eventService.getAll().stream().mapToInt(Event::getMaxCapacity).sum();
            if (statsTotalEvents != null) statsTotalEvents.setText(String.valueOf(total));
            if (statsUpcomingEvents != null) statsUpcomingEvents.setText(String.valueOf(upcoming));
            if (statsTotalCapacity != null) statsTotalCapacity.setText(String.valueOf(capacity));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur stats", e);
        }
    }

    // ==================== NAVIGATION ====================
    @FXML private void handleShowReservations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController dashboardController = loader.getController();
            Stage stage = (Stage) eventsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Dashboard Admin");
            dashboardController.handleShowReservations();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir les réservations");
        }
    }

    @FXML private void handleViewReservations() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Aucune sélection", "Sélectionnez un événement."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController dashboardController = loader.getController();
            Stage stage = (Stage) eventsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Dashboard Admin");
            dashboardController.handleShowReservationsForEvent(selected.getId(), selected.getTitle());
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir les réservations");
        }
    }

    @FXML private void handleOpenFront() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/events_front.fxml"));
            Stage stage = (Stage) eventsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Catalogue - Museum Digital");
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le catalogue: " + e.getMessage());
        }
    }

    @FXML private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment vous déconnecter ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                Stage stage = (Stage) eventsTable.getScene().getWindow();
                stage.getScene().setRoot(root);
                stage.setTitle("Museum Digital — Connexion");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erreur déconnexion", e);
            }
        }
    }

    // ==================== CRUD ====================
    @FXML private void handleAddEvent()  { showEventDialog(null); }

    @FXML private void handleEditEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Info","Sélectionnez un événement."); return; }
        showEventDialog(selected);
    }

    @FXML private void handleDeleteEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Info","Sélectionnez un événement."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setContentText("Supprimer \"" + selected.getTitle() + "\" ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                eventService.delete(selected.getId());
                loadEvents(); updateStats();
                showAlert("Succès","✅ Événement supprimé !");
            } catch (SQLException e) {
                showAlert("Erreur","Impossible de supprimer: " + e.getMessage());
            }
        }
    }

    // ==================== EXPORT PDF ====================
    @FXML private void handleExportPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter la liste des événements");
        fc.setInitialFileName("evenements_" + LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(eventsTable.getScene().getWindow());
        if (file == null) return;
        try {
            generateEventsPdf(file.getAbsolutePath());
            showAlert("✅ Export réussi", "PDF exporté :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showAlert("Erreur PDF", "Impossible de générer :\n" + e.getMessage());
        }
    }

    // ==================== OPTIMISATION PRIX IA ====================
    @FXML private void handleOptimizePrices() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/price_optimization.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("🤖 Optimisation des Prix — Gemini IA");
            stage.setScene(new Scene(root, 750, 650));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(650); stage.setMinHeight(550);
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir l'optimiseur :\n" + e.getMessage());
        }
    }

    @FXML private void handleOptimizeSingleEvent() {
        Event selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Aucune sélection", "Veuillez sélectionner un événement."); return; }

        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        ProgressIndicator progress = new ProgressIndicator();
        Label loadingLabel = new Label("Analyse par IA en cours...\nVeuillez patienter.");
        VBox vbox = new VBox(15, loadingLabel, progress);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(30));
        progressStage.setScene(new Scene(vbox, 350, 200));
        progressStage.setTitle("Optimisation IA");
        progressStage.show();

        Task<PriceOptimizationService.PriceSuggestion> task = new Task<>() {
            @Override
            protected PriceOptimizationService.PriceSuggestion call() throws Exception {
                return new PriceOptimizationService().suggestPriceForEvent(selected, reservationService.getAll());
            }
        };
        task.setOnSucceeded(e -> {
            progressStage.close();
            PriceOptimizationService.PriceSuggestion suggestion = task.getValue();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Proposition de prix");
            confirm.setHeaderText("Événement : " + selected.getTitle());
            confirm.setContentText("Prix actuel : " + selected.getPrice() + " DT\n" +
                    "Prix suggéré par IA : " + String.format("%.2f", suggestion.getSuggestedPrice()) + " DT\n\n" +
                    "Justification : " + suggestion.getReason() + "\n\nVoulez-vous appliquer ce nouveau prix ?");
            ButtonType yesBtn = new ButtonType("Oui, appliquer", ButtonBar.ButtonData.OK_DONE);
            ButtonType noBtn  = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(yesBtn, noBtn);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == yesBtn) {
                    try {
                        selected.setPrice(BigDecimal.valueOf(suggestion.getSuggestedPrice()));
                        eventService.update(selected);
                        loadEvents(); updateStats();
                        showAlert("Succès", "Le prix a été mis à jour avec succès !");
                    } catch (SQLException ex) {
                        showAlert("Erreur", "Impossible de mettre à jour : " + ex.getMessage());
                    }
                }
            });
        });
        task.setOnFailed(e -> {
            progressStage.close();
            showAlert("Erreur IA", "L'analyse a échoué : " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    // ==================== IMAGE ====================
    private String copyImageToProject(File sourceFile) {
        try {
            File dir = new File(System.getProperty("user.dir") + "/src/main/resources/images/");
            if (!dir.exists()) dir.mkdirs();
            String fileName = System.currentTimeMillis() + "_" + sourceFile.getName();
            File dest = new File(dir, fileName);
            Files.copy(sourceFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return "/images/" + fileName;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur image", e);
            return null;
        }
    }

    // ==================== DIALOGUE AJOUT/MODIFICATION ====================
    private void showEventDialog(Event event) {
        boolean isEdit = (event != null);
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "✏️ Modifier l'événement" : "➕ Nouvel événement");
        dialog.getDialogPane().setPrefWidth(520);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField titleField       = new TextField(isEdit ? event.getTitle() : "");
        TextArea  descField        = new TextArea(isEdit ? event.getDescription() : "");
        descField.setPrefRowCount(3);
        DatePicker datePicker      = new DatePicker(isEdit ? event.getEventDate() : LocalDate.now().plusDays(7));
        TextField timeField        = new TextField(isEdit && event.getEventTime() != null ? event.getEventTime().toString() : "18:00");
        TextField locationField    = new TextField(isEdit ? event.getLocation() : "");
        TextField capacityField    = new TextField(isEdit ? String.valueOf(event.getMaxCapacity()) : "100");
        TextField priceField       = new TextField(isEdit && event.getPrice() != null ? event.getPrice().toString() : "0");
        ComboBox<String> catCombo  = new ComboBox<>();
        catCombo.getItems().addAll("EXPOSITION","CONFERENCE","ATELIER","VISITE_GUIDEE","SPECTACLE");
        catCombo.setValue(isEdit ? event.getCategory() : "EXPOSITION");
        ComboBox<String> statCombo = new ComboBox<>();
        statCombo.getItems().addAll("A_VENIR","EN_COURS","TERMINE","ANNULE");
        statCombo.setValue(isEdit ? event.getStatus() : "A_VENIR");

        // Champ image
        TextField imageUrlField = new TextField(isEdit && event.getImageUrl() != null ? event.getImageUrl() : "");
        Button browseBtn = new Button("📁 Parcourir");
        browseBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif"));
            File f = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) {
                String copied = copyImageToProject(f);
                if (copied != null) imageUrlField.setText("file:" + new File(System.getProperty("user.dir") + "/src/main/resources" + copied).getAbsolutePath());
            }
        });
        HBox imageBox = new HBox(8, imageUrlField, browseBtn);
        HBox.setHgrow(imageUrlField, Priority.ALWAYS);

        int row = 0;
        grid.add(new Label("Titre *"),        0, row); grid.add(titleField,    1, row++);
        grid.add(new Label("Description"),    0, row); grid.add(descField,     1, row++);
        grid.add(new Label("Date *"),         0, row); grid.add(datePicker,    1, row++);
        grid.add(new Label("Heure (HH:mm)"),  0, row); grid.add(timeField,     1, row++);
        grid.add(new Label("Lieu *"),         0, row); grid.add(locationField, 1, row++);
        grid.add(new Label("Capacité max *"), 0, row); grid.add(capacityField, 1, row++);
        grid.add(new Label("Prix (DT) *"),    0, row); grid.add(priceField,    1, row++);
        grid.add(new Label("Catégorie *"),    0, row); grid.add(catCombo,      1, row++);
        grid.add(new Label("Statut *"),       0, row); grid.add(statCombo,     1, row++);
        grid.add(new Label("Image URL"),      0, row); grid.add(imageBox,      1, row);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            try {
                Event e = isEdit ? event : new Event();
                e.setTitle(titleField.getText().trim());
                e.setDescription(descField.getText().trim());
                e.setEventDate(datePicker.getValue());
                e.setEventTime(LocalTime.parse(timeField.getText().trim()));
                e.setLocation(locationField.getText().trim());
                e.setMaxCapacity(Integer.parseInt(capacityField.getText().trim()));
                e.setPrice(new BigDecimal(priceField.getText().trim()));
                e.setCategory(catCombo.getValue());
                e.setStatus(statCombo.getValue());
                e.setImageUrl(imageUrlField.getText().trim());
                return e;
            } catch (Exception ex) {
                showAlert("Erreur de saisie", "Vérifiez les champs : " + ex.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(e -> {
            if (e == null) return;
            try {
                if (isEdit) {
                    eventService.update(e);
                    showAlert("Succès", "✅ Événement modifié !");
                } else {
                    eventService.insert(e);
                    showAlert("Succès", "✅ Événement ajouté !");
                }
                loadEvents(); updateStats();
            } catch (SQLException ex) {
                showAlert("Erreur DB", ex.getMessage());
            }
        });
    }

    private void generateEventsPdf(String filePath) throws Exception {
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(filePath);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf);

        doc.add(new com.itextpdf.layout.element.Paragraph("📅 Liste des Événements - Museum Digital")
                .setFontSize(18).setBold());
        doc.add(new com.itextpdf.layout.element.Paragraph("Généré le : " + LocalDate.now())
                .setFontSize(10));
        doc.add(new com.itextpdf.layout.element.Paragraph(" "));

        List<Event> events = eventService.getAll();
        for (Event ev : events) {
            doc.add(new com.itextpdf.layout.element.Paragraph(
                    "• " + ev.getTitle() + " | " + ev.getCategory() + " | " +
                    (ev.getEventDate() != null ? ev.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A") +
                    " | " + ev.getLocation() + " | " + ev.getPrice() + " DT | " + ev.getStatus())
                    .setFontSize(11));
        }
        doc.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

