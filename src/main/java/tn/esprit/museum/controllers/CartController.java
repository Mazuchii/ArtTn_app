package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.museum.entities.Order;
import tn.esprit.museum.entities.OrderItem;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.OrderService;
import tn.esprit.museum.services.ProductService;
import tn.esprit.museum.utils.SessionManager;
import tn.esprit.museum.utils.EmailService;
import tn.esprit.museum.utils.StripeService;

import java.awt.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CartController {

    @FXML private ListView<String> cartListView;
    @FXML private Label totalPriceLabel;
    @FXML private TextArea shippingAddressField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private Button validateButton;
    @FXML private Label errorLabel;

    private List<Product> cart;
    private List<Integer> cartQuantities;
    private ProductService productService;
    private OrderService orderService;
    private ProductsViewController parentController;

    @FXML
    public void initialize() {
        productService = new ProductService();
        orderService = new OrderService();
        cart = new ArrayList<>();
        cartQuantities = new ArrayList<>();

        paymentMethodCombo.getItems().addAll("Carte Bancaire", "Espèce à la livraison");
        paymentMethodCombo.setValue("Carte Bancaire");

        // Vérification de la session
        System.out.println("=== CART CONTROLLER INIT ===");
        System.out.println("SessionManager.isLoggedIn(): " + SessionManager.isLoggedIn());
        if (SessionManager.isLoggedIn()) {
            User user = SessionManager.getCurrentUser();
            System.out.println("Utilisateur: " + user.getFullName());
            System.out.println("Email: " + user.getEmail());
            validateButton.setDisable(false);
            errorLabel.setVisible(false);
        } else {
            System.out.println("Aucun utilisateur connecté !");
            validateButton.setDisable(true);
            errorLabel.setText("⚠️ Veuillez vous connecter pour passer commande");
            errorLabel.setVisible(true);
        }
        System.out.println("============================");
    }

    public void setCart(List<Product> cart, List<Integer> quantities) {
        this.cart = cart;
        this.cartQuantities = quantities;
        updateCartDisplay();
    }

    public void setParentController(ProductsViewController parent) {
        this.parentController = parent;
    }

    private void updateCartDisplay() {
        cartListView.getItems().clear();
        double total = 0;

        for (int i = 0; i < cart.size(); i++) {
            Product p = cart.get(i);
            int qty = cartQuantities.get(i);
            double subtotal = p.getPrice() * qty;
            total += subtotal;
            cartListView.getItems().add(String.format("%s x%d = %.2f D", p.getName(), qty, subtotal));
        }

        totalPriceLabel.setText(String.format("%.2f D", total));
        validateButton.setDisable(cart.isEmpty());
    }

    @FXML
    private void handleRemoveSelected() {
        int selectedIndex = cartListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            cart.remove(selectedIndex);
            cartQuantities.remove(selectedIndex);
            updateCartDisplay();

            if (parentController != null) {
                parentController.updateCartBadge();
            }

            showMessage("Produit retiré du panier", "success");
        } else {
            showMessage("Sélectionnez un produit à retirer", "error");
        }
    }

    @FXML
    private void handleClearCart() {
        cart.clear();
        cartQuantities.clear();
        updateCartDisplay();

        if (parentController != null) {
            parentController.updateCartBadge();
        }

        showMessage("Panier vidé", "success");
    }

    @FXML
    private void handleValidateOrder() {
        // Vérification de l'utilisateur connecté
        if (!SessionManager.isLoggedIn()) {
            showMessage("Veuillez vous connecter pour passer commande", "error");
            return;
        }

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showMessage("Erreur: Aucun utilisateur connecté", "error");
            return;
        }

        String userEmail = currentUser.getEmail();
        String userName = currentUser.getFullName();
        int userId = currentUser.getId();

        System.out.println("=== CART CONTROLLER VALIDATION ===");
        System.out.println("👤 Utilisateur: " + userName);
        System.out.println("📧 Email: " + userEmail);
        System.out.println("🆔 ID: " + userId);
        System.out.println("===================================");

        if (cart.isEmpty()) {
            showMessage("Votre panier est vide", "error");
            return;
        }

        String address = shippingAddressField.getText().trim();
        if (address.isEmpty()) {
            showMessage("Veuillez entrer une adresse de livraison", "error");
            return;
        }

        String paymentMethod = paymentMethodCombo.getValue();
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            showMessage("Veuillez sélectionner un mode de paiement", "error");
            return;
        }

        // Vérification du stock
        try {
            for (int i = 0; i < cart.size(); i++) {
                Product p = cart.get(i);
                int qty = cartQuantities.get(i);
                Product currentProduct = productService.getById(p.getId());
                if (currentProduct.getStock() < qty) {
                    showMessage("Stock insuffisant pour: " + p.getName(), "error");
                    return;
                }
            }
        } catch (SQLException e) {
            showMessage("Erreur lors de la vérification du stock", "error");
            return;
        }

        // Création de la commande
        try {
            double total = 0;
            List<OrderItem> items = new ArrayList<>();

            for (int i = 0; i < cart.size(); i++) {
                Product p = cart.get(i);
                int qty = cartQuantities.get(i);
                total += p.getPrice() * qty;
                items.add(new OrderItem(p.getId(), qty, p.getPrice()));
            }

            Order order = new Order();
            order.setUserId(userId);
            order.setTotalAmount(total);
            order.setShippingAddress(address);
            order.setPaymentMethod(paymentMethod);
            order.setStatus("PENDING");
            order.setItems(items);

            int orderId = orderService.createOrder(order);

            if (orderId > 0) {
                System.out.println("✅ Commande #" + orderId + " créée !");

                // Stripe
                String paymentLink = StripeService.createPaymentLink(total, "eur", orderId, "https://www.google.com", "https://www.google.com");
                System.out.println("🔗 Lien de paiement: " + paymentLink);

                // Email
                boolean emailSent = EmailService.sendPaymentLinkEmail(userEmail, userName, orderId, total, paymentLink);

                if (emailSent) {
                    showMessage("✅ Commande #" + orderId + " validée ! Email envoyé à " + userEmail, "success");
                } else {
                    showMessage("✅ Commande #" + orderId + " validée !", "success");
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("✅ Commande validée !");
                alert.setContentText("Un email avec le lien de paiement a été envoyé à :\n" + userEmail);
                alert.showAndWait();

                // Vider le panier
                cart.clear();
                cartQuantities.clear();
                updateCartDisplay();
                shippingAddressField.clear();

                // Fermer la fenêtre
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException e) {}
                    Platform.runLater(() -> {
                        Stage stage = (Stage) validateButton.getScene().getWindow();
                        stage.close();
                    });
                }).start();
            }

        } catch (SQLException e) {
            showMessage("Erreur: " + e.getMessage(), "error");
            e.printStackTrace();
        }
    }



    private void showMessage(String message, String type) {
        errorLabel.setText(message);
        errorLabel.setStyle(type.equals("error") ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        errorLabel.setVisible(true);

        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }
}