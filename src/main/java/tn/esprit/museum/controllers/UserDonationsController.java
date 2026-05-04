package tn.esprit.museum.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import tn.esprit.museum.entities.UserDonation;
import tn.esprit.museum.services.SponsorService;
import tn.esprit.museum.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.List;

public class UserDonationsController {

    @FXML private TableView<UserDonation> donationsTable;
    @FXML private TableColumn<UserDonation, String> eventTitleColumn;
    @FXML private TableColumn<UserDonation, String> packNameColumn;
    @FXML private TableColumn<UserDonation, String> amountColumn;
    @FXML private TableColumn<UserDonation, String> statusColumn;
    @FXML private TableColumn<UserDonation, String> dateColumn;
    @FXML private Label emptyLabel;
    @FXML private Label userWelcomeLabel;

    private final SponsorService sponsorService = new SponsorService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private UserHomeController parentController;
    private int userId = -1;

    @FXML
    public void initialize() {
        eventTitleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventTitle()));
        packNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPackName()));
        amountColumn.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f DT", data.getValue().getAmount())));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(formatStatus(data.getValue().getStatus())));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDateCreation() == null ? "" : dateFormat.format(data.getValue().getDateCreation())
        ));
        donationsTable.setPlaceholder(new Label("Aucune donation trouvee."));
        if (SessionManager.getCurrentUser() != null && userWelcomeLabel != null) {
            userWelcomeLabel.setText("Bienvenue, " + SessionManager.getCurrentUser().getEmail());
        }
    }

    public void setParentController(UserHomeController parentController) {
        this.parentController = parentController;
    }

    public void setUserId(int userId) {
        this.userId = userId;
        loadDonations();
    }

    private void loadDonations() {
        if (userId <= 0) {
            showAlert("Non connecte", "Veuillez vous connecter pour voir vos donations.");
            return;
        }
        List<UserDonation> donations = sponsorService.getDonationsByUserId(userId);
        donationsTable.setItems(FXCollections.observableArrayList(donations));
        boolean empty = donations.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);
    }

    @FXML
    private void handleRefresh() {
        loadDonations();
    }

    @FXML
    private void handleBackToEvents() {
        String email = SessionManager.getCurrentUserEmail();
        if (parentController != null && email != null) {
            parentController.handleShowEventsFrontPublic(email);
        }
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "En attente";
        }
        return switch (status.toLowerCase()) {
            case "accepte", "accepté", "actif" -> "Active";
            case "refuse", "refusé" -> "Refusee";
            default -> "En attente";
        };
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
