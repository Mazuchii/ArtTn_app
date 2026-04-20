import tn.esprit.produit.entities.Order;
import tn.esprit.produit.entities.OrderItem;
import tn.esprit.produit.entities.Product;
import tn.esprit.produit.entities.User;
import tn.esprit.produit.services.OrderService;
import tn.esprit.produit.services.ProductService;
import tn.esprit.produit.utils.SessionManager;

import java.sql.SQLException;
import java.util.List;

public class TestOrderHistory {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("🔍 TEST HISTORIQUE DES COMMANDES");
        System.out.println("=========================================\n");

        // 1. Vérifier l'utilisateur connecté
        testUtilisateurConnecte();

        // 2. Vérifier les commandes dans la base
        testCommandesDansBase();

        // 3. Vérifier getOrdersByUser()
        testGetOrdersByUser();

        // 4. Vérifier getOrderById()
        testGetOrderById();

        // 5. Vérifier les détails des commandes (items)
        testOrderDetails();

        // 6. Créer une commande de test si nécessaire
        testCreateOrderIfNeeded();

        System.out.println("\n=========================================");
        System.out.println("✅ FIN DU TEST");
        System.out.println("=========================================");
    }

    // 1. Vérifier l'utilisateur connecté
    private static void testUtilisateurConnecte() {
        System.out.println("📌 1. Vérification de l'utilisateur connecté");

        User user = new User(1, "admin", "admin@museum.com", "Administrateur", "ADMIN");
        SessionManager.login(user);

        int userId = SessionManager.getCurrentUserId();
        System.out.println("   ✅ ID Utilisateur: " + userId);
        System.out.println("   ✅ Nom: " + SessionManager.getCurrentUser().getFullName());
        System.out.println("   ✅ Connecté: " + SessionManager.isLoggedIn());
        System.out.println();
    }

    // 2. Vérifier les commandes directement dans la base
    private static void testCommandesDansBase() {
        System.out.println("📌 2. Vérification des commandes dans la base de données");

        try {
            OrderService orderService = new OrderService();
            List<Order> allOrders = orderService.getAllOrders();

            if (allOrders.isEmpty()) {
                System.out.println("   ⚠️ Aucune commande trouvée dans la base !");
                System.out.println("   💡 Solution: Créez une commande de test");
            } else {
                System.out.println("   ✅ " + allOrders.size() + " commande(s) trouvée(s) dans la base:");
                for (Order o : allOrders) {
                    System.out.println("      - Commande #" + o.getId() +
                            " | User ID: " + o.getUserId() +
                            " | Total: " + o.getTotalAmount() + "€" +
                            " | Statut: " + o.getStatus());
                }
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    // 3. Tester getOrdersByUser()
    private static void testGetOrdersByUser() {
        System.out.println("📌 3. Test de getOrdersByUser()");

        try {
            OrderService orderService = new OrderService();
            int userId = SessionManager.getCurrentUserId();

            List<Order> orders = orderService.getOrdersByUser(userId);

            if (orders == null) {
                System.out.println("   ❌ La liste est null");
            } else if (orders.isEmpty()) {
                System.out.println("   ⚠️ Aucune commande trouvée pour l'utilisateur ID: " + userId);
                System.out.println("   💡 Vérifiez que les commandes ont user_id = " + userId);
            } else {
                System.out.println("   ✅ " + orders.size() + " commande(s) trouvée(s) pour l'utilisateur:");
                for (Order o : orders) {
                    System.out.println("      - Commande #" + o.getId() +
                            " | Date: " + o.getOrderDate() +
                            " | Total: " + o.getTotalAmount() + "€");
                }
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    // 4. Tester getOrderById()
    private static void testGetOrderById() {
        System.out.println("📌 4. Test de getOrderById()");

        try {
            OrderService orderService = new OrderService();
            List<Order> orders = orderService.getAllOrders();

            if (!orders.isEmpty()) {
                Order firstOrder = orders.get(0);
                int orderId = firstOrder.getId();

                Order order = orderService.getOrderById(orderId);

                if (order != null) {
                    System.out.println("   ✅ Commande #" + orderId + " trouvée");
                    System.out.println("      - User ID: " + order.getUserId());
                    System.out.println("      - Total: " + order.getTotalAmount() + "€");
                    System.out.println("      - Statut: " + order.getStatus());
                    System.out.println("      - Adresse: " + order.getShippingAddress());
                    System.out.println("      - Paiement: " + order.getPaymentMethod());
                } else {
                    System.out.println("   ❌ Commande #" + orderId + " non trouvée");
                }
            } else {
                System.out.println("   ⚠️ Aucune commande à tester");
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
        }
        System.out.println();
    }

    // 5. Vérifier les détails des commandes (items)
    private static void testOrderDetails() {
        System.out.println("📌 5. Vérification des détails des commandes (items)");

        try {
            OrderService orderService = new OrderService();
            List<Order> orders = orderService.getAllOrders();

            if (!orders.isEmpty()) {
                for (Order order : orders) {
                    System.out.println("   📦 Commande #" + order.getId());
                    List<OrderItem> items = order.getItems();

                    if (items == null || items.isEmpty()) {
                        System.out.println("      ⚠️ Aucun item trouvé pour cette commande");
                        System.out.println("      💡 Vérifiez la table order_items");
                    } else {
                        System.out.println("      ✅ " + items.size() + " item(s):");
                        for (OrderItem item : items) {
                            String productName = item.getProduct() != null ? item.getProduct().getName() : "Produit #" + item.getProductId();
                            System.out.println("         - " + productName + " x" + item.getQuantity() + " = " + item.getSubtotal() + "€");
                        }
                    }
                }
            } else {
                System.out.println("   ⚠️ Aucune commande à analyser");
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
        }
        System.out.println();
    }

    // 6. Créer une commande de test si nécessaire
    private static void testCreateOrderIfNeeded() {
        System.out.println("📌 6. Création d'une commande de test (si nécessaire)");

        try {
            OrderService orderService = new OrderService();
            List<Order> orders = orderService.getAllOrders();

            if (orders.isEmpty()) {
                System.out.println("   📝 Création d'une commande de test...");

                ProductService productService = new ProductService();
                List<Product> products = productService.getAll();

                if (products.isEmpty()) {
                    System.out.println("   ❌ Aucun produit disponible pour créer une commande");
                    System.out.println("   💡 Ajoutez d'abord des produits");
                    return;
                }

                // Créer une commande de test
                Order testOrder = new Order();
                testOrder.setUserId(SessionManager.getCurrentUserId());
                testOrder.setTotalAmount(products.get(0).getPrice());
                testOrder.setShippingAddress("123 Rue de Test, Tunis");
                testOrder.setPaymentMethod("Carte Bancaire");
                testOrder.setStatus("PENDING");

                List<OrderItem> items = new java.util.ArrayList<>();
                OrderItem item = new OrderItem(products.get(0).getId(), 1, products.get(0).getPrice());
                items.add(item);
                testOrder.setItems(items);

                int orderId = orderService.createOrder(testOrder);
                System.out.println("   ✅ Commande de test créée avec succès !");
                System.out.println("      - ID: " + orderId);
                System.out.println("      - Rafraîchissez l'historique pour la voir");
            } else {
                System.out.println("   ✅ Des commandes existent déjà, pas besoin d'en créer");
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur lors de la création: " + e.getMessage());
        }
        System.out.println();
    }
}