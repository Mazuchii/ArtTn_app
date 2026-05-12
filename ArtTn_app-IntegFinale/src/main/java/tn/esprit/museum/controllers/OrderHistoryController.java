package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.museum.entities.Order;
import tn.esprit.museum.entities.OrderItem;
import tn.esprit.museum.services.OrderService;
import tn.esprit.museum.utils.SessionManager;

import java.sql.SQLException;
import java.util.List;

public class OrderHistoryController {

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> idColumn;
    @FXML private TableColumn<Order, String> dateColumn;
    @FXML private TableColumn<Order, Double> totalColumn;
    @FXML private TableColumn<Order, String> statusColumn;
    @FXML private ListView<String> itemsListView;
    @FXML private Label orderDetailsLabel;
    @FXML private Button cancelButton;
    @FXML private Button deleteButton;
    @FXML private Label messageLabel;

    private OrderService orderService;
    private ObservableList<Order> orderList;

    @FXML
    public void initialize() {
        System.out.println("=== OrderHistoryController INITIALISÉ ===");

        orderService = new OrderService();
        orderList = FXCollections.observableArrayList();

        // Configurer les colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Écouter la sélection
        ordersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null) {
                        showOrderDetails(selected);
                    }
                });

        // Charger les commandes
        loadOrders();
    }

    private void loadOrders() {
        try {
            System.out.println("=== Chargement des commandes ===");

            int userId = SessionManager.getCurrentUserId();
            System.out.println("User ID: " + userId);

            List<Order> orders = orderService.getOrdersByUser(userId);
            System.out.println("Commandes trouvées: " + orders.size());

            // Afficher dans la console pour déboguer
            for (Order o : orders) {
                System.out.println("  - Commande #" + o.getId() + " | " + o.getTotalAmount() + "D | " + o.getStatus());
            }

            // Mettre à jour la liste
            orderList.clear();
            orderList.addAll(orders);
            ordersTable.setItems(orderList);

            if (orders.isEmpty()) {
                messageLabel.setText("Aucune commande trouvée");
                messageLabel.setVisible(true);
            } else {
                messageLabel.setVisible(false);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            messageLabel.setText("Erreur: " + e.getMessage());
            messageLabel.setVisible(true);
        }
    }

    private void showOrderDetails(Order order) {
        if (order == null) {
            itemsListView.getItems().clear();
            orderDetailsLabel.setText("");
            cancelButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }

        String dateStr = order.getOrderDate() != null ?
                order.getOrderDate().toString().substring(0, 19) : "N/A";

        orderDetailsLabel.setText(String.format("Commande #%d - %s - Total: %.2f D",
                order.getId(), dateStr, order.getTotalAmount()));

        itemsListView.getItems().clear();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                String productName = item.getProduct() != null ? item.getProduct().getName() : "Produit #" + item.getProductId();
                itemsListView.getItems().add(
                        String.format("• %s x%d = %.2f D",
                                productName, item.getQuantity(), item.getSubtotal()));
            }
        } else {
            itemsListView.getItems().add("Aucun détail disponible");
        }

        boolean isPending = order.getStatus().equals("PENDING");
        cancelButton.setDisable(!isPending);
        deleteButton.setDisable(!isPending);
    }

    @FXML
    private void handleCancelOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Annuler la commande #" + selected.getId() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                orderService.cancelOrder(selected.getId());
                showMessage("✅ Commande annulée !", "success");
                loadOrders();
            } catch (SQLException e) {
                showMessage("Erreur: " + e.getMessage(), "error");
            }
        }
    }

    @FXML
    private void handleDeleteOrder() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer la commande #" + selected.getId() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                orderService.deleteOrder(selected.getId());
                showMessage("✅ Commande supprimée !", "success");
                loadOrders();
            } catch (SQLException e) {
                showMessage("Erreur: " + e.getMessage(), "error");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadOrders();
        showMessage("Liste actualisée", "success");
    }

    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        if (type.equals("error")) {
            messageLabel.setStyle("-fx-text-fill: red;");
        } else {
            messageLabel.setStyle("-fx-text-fill: green;");
        }
        messageLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            Platform.runLater(() -> messageLabel.setVisible(false));
        }).start();
    }
}
