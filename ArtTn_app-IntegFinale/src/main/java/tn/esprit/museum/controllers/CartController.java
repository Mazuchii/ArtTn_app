package tn.esprit.museum.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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

        // Vérification du stock (rapide, peut rester sur le thread UI)
        try {
            for (int i = 0; i < cart.size(); i++) {
                Product p = cart.get(i);
                int qty = cartQuantities.get(i);
                Product current = productService.getById(p.getId());
                if (current == null || current.getStock() < qty) {
                    showMessage("Stock insuffisant pour : " + p.getName(), "error");
                    return;
                }
            }
        } catch (SQLException e) {
            showMessage("Erreur lors de la vérification du stock", "error");
            return;
        }

        // Désactiver le bouton et afficher un indicateur pendant le traitement
        validateButton.setDisable(true);
        validateButton.setText("⏳ Traitement...");
        showMessage("Création de la commande en cours...", "success");

        // Capturer les données nécessaires avant de quitter le thread UI
        final String userEmail    = currentUser.getEmail();
        final String userName     = currentUser.getFullName();
        final int    userId       = currentUser.getId();
        final String finalAddress = address;
        final String finalPayment = paymentMethod;

        // Snapshot du panier pour le thread background
        final List<Product>  cartSnapshot = new ArrayList<>(cart);
        final List<Integer>  qtySnapshot  = new ArrayList<>(cartQuantities);

        // Tout le travail lourd (DB + Stripe + Email) dans un thread background
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                // 1. Créer la commande en DB
                updateMessage("Enregistrement de la commande...");
                double total = 0;
                List<OrderItem> items = new ArrayList<>();
                for (int i = 0; i < cartSnapshot.size(); i++) {
                    Product p = cartSnapshot.get(i);
                    int qty = qtySnapshot.get(i);
                    total += p.getPrice() * qty;
                    items.add(new OrderItem(p.getId(), qty, p.getPrice()));
                }

                Order order = new Order();
                order.setUserId(userId);
                order.setTotalAmount(total);
                order.setShippingAddress(finalAddress);
                order.setPaymentMethod(finalPayment);
                order.setStatus("PENDING");
                order.setItems(items);

                int orderId = orderService.createOrder(order);
                if (orderId <= 0) throw new Exception("Échec de la création de la commande");

                System.out.println("✅ Commande #" + orderId + " créée !");

                // 2. Générer le lien Stripe (appel réseau)
                updateMessage("Génération du lien de paiement...");
                String paymentLink = StripeService.createPaymentLink(
                        total, "eur", orderId,
                        "https://www.google.com", "https://www.google.com"
                );
                System.out.println("🔗 Lien Stripe : " + paymentLink);

                // 3. Envoyer l'email (connexion SMTP)
                updateMessage("Envoi de l'email de confirmation...");
                boolean emailSent = EmailService.sendPaymentLinkEmail(
                        userEmail, userName, orderId, total, paymentLink
                );
                System.out.println(emailSent ? "✅ Email envoyé à " + userEmail : "⚠️ Email non envoyé");

                // Retourner un message de résultat
                return "Commande #" + orderId + " validée !" +
                        (emailSent ? " Email envoyé à " + userEmail : " (email non envoyé)");
            }
        };

        // Mettre à jour le label de statut depuis le thread background
        task.messageProperty().addListener((obs, old, msg) ->
                Platform.runLater(() -> showMessage(msg, "success"))
        );

        // Succès
        task.setOnSucceeded(e -> {
            String result = task.getValue();
            validateButton.setDisable(false);
            validateButton.setText("✅ Valider la commande");

            // Vider le panier
            cart.clear();
            cartQuantities.clear();
            updateCartDisplay();
            shippingAddressField.clear();

            if (parentController != null) {
                parentController.updateCartBadge();
            }

            // Alerte de confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Commande confirmée");
            alert.setHeaderText("✅ " + result);
            alert.setContentText("Vérifiez votre boîte mail pour le lien de paiement.");
            alert.showAndWait();

            // Fermer la fenêtre du panier
            Stage stage = (Stage) validateButton.getScene().getWindow();
            stage.close();
        });

        // Échec
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Erreur inconnue";
            System.err.println("❌ Erreur validation commande : " + msg);
            validateButton.setDisable(false);
            validateButton.setText("✅ Valider la commande");
            showMessage("Erreur : " + msg, "error");
        });

        // Lancer le thread background
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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
