package tn.esprit.produit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.produit.entities.Order;
import tn.esprit.produit.entities.OrderItem;
import tn.esprit.produit.entities.Product;
import tn.esprit.produit.services.OrderService;
import tn.esprit.produit.services.ProductService;
import tn.esprit.produit.utils.SessionManager;

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
    private ProductsViewController parentController; // Pour mettre à jour le badge

    @FXML
    public void initialize() {
        productService = new ProductService();
        orderService = new OrderService();
        cart = new ArrayList<>();
        cartQuantities = new ArrayList<>();

        paymentMethodCombo.getItems().addAll("Carte Bancaire", "PayPal", "Espèce à la livraison");
        paymentMethodCombo.setValue("Carte Bancaire");

        // Désactiver le bouton si pas d'utilisateur connecté
        if (!SessionManager.isLoggedIn()) {
            validateButton.setDisable(true);
            errorLabel.setText("⚠️ Veuillez vous connecter pour passer commande");
            errorLabel.setVisible(true);
        }
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

            // Mettre à jour le badge dans la fenêtre principale
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

        // Mettre à jour le badge dans la fenêtre principale
        if (parentController != null) {
            parentController.updateCartBadge();
        }

        showMessage("Panier vidé", "success");
    }

    @FXML
    private void handleValidateOrder() {
        // Vérification 1 : Utilisateur connecté
        if (!SessionManager.isLoggedIn()) {
            showMessage("Veuillez vous connecter pour passer commande", "error");
            return;
        }

        // Vérification 2 : Panier non vide
        if (cart.isEmpty()) {
            showMessage("Votre panier est vide", "error");
            return;
        }

        // Vérification 3 : Adresse de livraison
        String address = shippingAddressField.getText().trim();
        if (address.isEmpty()) {
            showMessage("Veuillez entrer une adresse de livraison", "error");
            return;
        }

        // Vérification 4 : Mode de paiement
        String paymentMethod = paymentMethodCombo.getValue();
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            showMessage("Veuillez sélectionner un mode de paiement", "error");
            return;
        }

        try {
            // Calculer le total et préparer les items
            double total = 0;
            List<OrderItem> items = new ArrayList<>();

            for (int i = 0; i < cart.size(); i++) {
                Product p = cart.get(i);
                int qty = cartQuantities.get(i);

                // Vérifier le stock avant de commander
                Product currentProduct = productService.getById(p.getId());
                if (currentProduct.getStock() < qty) {
                    showMessage("Stock insuffisant pour: " + p.getName(), "error");
                    return;
                }

                double subtotal = p.getPrice() * qty;
                total += subtotal;
                items.add(new OrderItem(p.getId(), qty, p.getPrice()));
            }

            // Créer la commande
            Order order = new Order();
            order.setUserId(SessionManager.getCurrentUserId());
            order.setTotalAmount(total);
            order.setShippingAddress(address);
            order.setPaymentMethod(paymentMethod);
            order.setStatus("PENDING");
            order.setItems(items);

            // Sauvegarder en base
            int orderId = orderService.createOrder(order);

            if (orderId > 0) {
                // Vider le panier
                cart.clear();
                cartQuantities.clear();
                updateCartDisplay();
                shippingAddressField.clear();

                // Mettre à jour le badge dans la fenêtre principale
                if (parentController != null) {
                    parentController.updateCartBadge();
                }

                showMessage("✅ Commande #" + orderId + " validée avec succès !", "success");

                // Fermer la fenêtre après 2 secondes
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
        if (type.equals("error")) {
            errorLabel.setStyle("-fx-text-fill: red;");
        } else {
            errorLabel.setStyle("-fx-text-fill: green;");
        }
        errorLabel.setVisible(true);

        // Masquer le message après 3 secondes
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            Platform.runLater(() -> errorLabel.setVisible(false));
        }).start();
    }
}