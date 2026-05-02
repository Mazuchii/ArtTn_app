import tn.esprit.museum.entities.Order;
import tn.esprit.museum.entities.OrderItem;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.entities.User;
import tn.esprit.museum.services.OrderService;
import tn.esprit.museum.services.ProductService;
import tn.esprit.museum.utils.SessionManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderServiceTest {

    static int testOrderId;
    static int testUserId = 1;
    static OrderService orderService;
    static ProductService productService;
    static List<Product> testProducts;

    @BeforeAll
    static void init() {
        orderService = new OrderService();
        productService = new ProductService();

        User testUser = new User(testUserId, "admin", "admin@museum.com", "Administrateur", "ADMIN");
        SessionManager.login(testUser);

        System.out.println("✅ Initialisation des tests commandes terminée");
    }

    @AfterAll
    static void cleanup() {
        try {
            if (testOrderId != 0) {
                Order order = orderService.getOrderById(testOrderId);
                if (order != null && !order.getStatus().equals("CANCELLED")) {
                    orderService.deleteOrder(testOrderId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur nettoyage: " + e.getMessage());
        }
        System.out.println("🧹 Nettoyage terminé");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    public void testCreerCommande() {
        try {
            testProducts = productService.getAll();
            if (testProducts == null || testProducts.size() < 2) {
                System.out.println("⚠️ Pas assez de produits pour tester");
                return;
            }

            List<OrderItem> items = new ArrayList<>();
            double total = 0;

            OrderItem item1 = new OrderItem(testProducts.get(0).getId(), 2, testProducts.get(0).getPrice());
            OrderItem item2 = new OrderItem(testProducts.get(1).getId(), 1, testProducts.get(1).getPrice());
            items.add(item1);
            items.add(item2);

            total = (testProducts.get(0).getPrice() * 2) + testProducts.get(1).getPrice();

            Order order = new Order();
            order.setUserId(testUserId);
            order.setTotalAmount(total);
            order.setShippingAddress("123 Rue Test, Tunis");
            order.setPaymentMethod("Carte Bancaire");
            order.setItems(items);
            order.setStatus("PENDING");

            int orderId = orderService.createOrder(order);
            testOrderId = orderId;

            assertTrue(orderId > 0);
            System.out.println("✅ Test création commande réussi - ID: " + testOrderId);

        } catch (SQLException e) {
            fail("Erreur SQL: " + e.getMessage());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void testGetCommandesByUser() {
        try {
            List<Order> orders = orderService.getOrdersByUser(testUserId);

            assertNotNull(orders);
            System.out.println("✅ Test récupération commandes réussi - " + orders.size() + " commandes trouvées");

        } catch (SQLException e) {
            fail("Erreur SQL: " + e.getMessage());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void testGetCommandeById() {
        try {
            if (testOrderId == 0) {
                System.out.println("⚠️ Pas de commande de test disponible");
                return;
            }

            Order order = orderService.getOrderById(testOrderId);

            assertNotNull(order);
            assertEquals(testOrderId, order.getId());

            System.out.println("✅ Test récupération par ID réussi");

        } catch (SQLException e) {
            fail("Erreur SQL: " + e.getMessage());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void testUpdateStatut() {
        try {
            if (testOrderId == 0) {
                System.out.println("⚠️ Pas de commande de test disponible");
                return;
            }

            orderService.updateOrderStatus(testOrderId, "CONFIRMED");

            Order order = orderService.getOrderById(testOrderId);
            assertEquals("CONFIRMED", order.getStatus());

            System.out.println("✅ Test mise à jour statut réussi");

        } catch (SQLException e) {
            fail("Erreur SQL: " + e.getMessage());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    public void testGetAllCommandes() {
        try {
            List<Order> allOrders = orderService.getAllOrders();

            assertNotNull(allOrders);
            System.out.println("✅ Test récupération toutes commandes réussi - " + allOrders.size() + " commandes");

        } catch (SQLException e) {
            fail("Erreur SQL: " + e.getMessage());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    public void testAnnulerCommande() {
        try {
            if (testProducts == null || testProducts.isEmpty()) {
                System.out.println("⚠️ Pas de produits disponibles");
                return;
            }

            List<OrderItem> items = new ArrayList<>();
            OrderItem item = new OrderItem(testProducts.get(0).getId(), 1, testProducts.get(0).getPrice());
            items.add(item);

            Order order = new Order();
            order.setUserId(testUserId);
            order.setTotalAmount(testProducts.get(0).getPrice());
            order.setShippingAddress("Test Annulation");
            order.setPaymentMethod("Test");
            order.setItems(items);
            order.setStatus("PENDING");

            int orderId = orderService.createOrder(order);

            orderService.cancelOrder(orderId);

            Order cancelledOrder = orderService.getOrderById(orderId);
            assertEquals("CANCELLED", cancelledOrder.getStatus());

            orderService.deleteOrder(orderId);

            System.out.println("✅ Test annulation commande réussi");

        } catch (SQLException e) {
            fail("Erreur SQL: " + e.getMessage());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    public void testSupprimerCommande() {
        try {
            if (testProducts == null || testProducts.isEmpty()) {
                System.out.println("⚠️ Pas de produits disponibles");
                return;
            }

            List<OrderItem> items = new ArrayList<>();
            OrderItem item = new OrderItem(testProducts.get(0).getId(), 1, testProducts.get(0).getPrice());
            items.add(item);

            Order order = new Order();
            order.setUserId(testUserId);
            order.setTotalAmount(testProducts.get(0).getPrice());
            order.setShippingAddress("Test Suppression");
            order.setPaymentMethod("Test");
            order.setItems(items);
            order.setStatus("PENDING");

            int orderId = orderService.createOrder(order);

            orderService.deleteOrder(orderId);

            Order deletedOrder = orderService.getOrderById(orderId);
            assertNull(deletedOrder);

            System.out.println("✅ Test suppression commande réussi");

        } catch (SQLException e) {
            fail("Erreur SQL: " + e.getMessage());
        }
    }
}