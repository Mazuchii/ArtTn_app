import tn.esprit.museum.utils.DatabaseConnection;
import tn.esprit.museum.services.ProductService;
import tn.esprit.museum.services.OrderService;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.entities.Order;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TestConnection {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("🔍 TEST DE CONNEXION À LA BASE DE DONNÉES");
        System.out.println("=========================================\n");

        // TEST 1 : Connexion simple
        testSimpleConnection();

        // TEST 2 : Récupération des produits
        testGetProducts();

        // TEST 3 : Récupération des commandes
        testGetOrders();

        System.out.println("\n=========================================");
        System.out.println("✅ FIN DES TESTS");
        System.out.println("=========================================");
    }

    // TEST 1 : Vérifier la connexion simple
    private static void testSimpleConnection() {
        System.out.println("📌 TEST 1: Connexion simple à la base");
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("   ✅ Connexion établie avec succès !");
                System.out.println("   📍 Base de données: esprit_museum");
                System.out.println("   📍 URL: " + conn.getMetaData().getURL());
                System.out.println("   📍 Driver: " + conn.getMetaData().getDriverName());
                conn.close();
            } else {
                System.out.println("   ❌ Connexion échouée !");
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    // TEST 2 : Récupérer les produits
    private static void testGetProducts() {
        System.out.println("📌 TEST 2: Récupération des produits");
        try {
            ProductService productService = new ProductService();
            List<Product> products = productService.getAll();

            if (products != null) {
                System.out.println("   ✅ " + products.size() + " produit(s) trouvé(s) !");
                for (Product p : products) {
                    System.out.println("      - ID: " + p.getId() + " | Nom: " + p.getName() + " | Prix: " + p.getPrice() + " D");
                }
            } else {
                System.out.println("   ⚠️ Aucun produit trouvé !");
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    // TEST 3 : Récupérer les commandes
    private static void testGetOrders() {
        System.out.println("📌 TEST 3: Récupération des commandes");
        try {
            OrderService orderService = new OrderService();
            List<Order> orders = orderService.getAllOrders();

            if (orders != null) {
                System.out.println("   ✅ " + orders.size() + " commande(s) trouvée(s) !");
                for (Order o : orders) {
                    System.out.println("      - ID: " + o.getId() + " | User ID: " + o.getUserId() + " | Total: " + o.getTotalAmount() + " D | Statut: " + o.getStatus());
                }
            } else {
                System.out.println("   ⚠️ Aucune commande trouvée !");
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
}