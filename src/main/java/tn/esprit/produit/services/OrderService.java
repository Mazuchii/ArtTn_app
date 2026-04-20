package tn.esprit.produit.services;

import tn.esprit.produit.entities.Order;
import tn.esprit.produit.entities.OrderItem;
import tn.esprit.produit.entities.Product;
import tn.esprit.produit.interfaces.Iservice;
import tn.esprit.produit.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderService implements Iservice<Order> {

    private ProductService productService = new ProductService();
    private OrderItemService orderItemService = new OrderItemService();

    @Override
    public void insert(Order order) throws SQLException {
        createOrder(order);
    }

    @Override
    public List<Order> getAll() throws SQLException {
        return getAllOrders();
    }

    @Override
    public Order getById(int id) throws SQLException {
        return getOrderById(id);
    }

    @Override
    public void update(Order order) throws SQLException {
        updateOrderStatus(order.getId(), order.getStatus());
    }

    @Override
    public void delete(int id) throws SQLException {
        deleteOrder(id);
    }

    // ============ CRÉER UNE COMMANDE ============
    public int createOrder(Order order) throws SQLException {
        String sql = "INSERT INTO orders (user_id, total_amount, status, shipping_address, payment_method) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setInt(1, order.getUserId());
            pstmt.setDouble(2, order.getTotalAmount());
            pstmt.setString(3, order.getStatus());
            pstmt.setString(4, order.getShippingAddress());
            pstmt.setString(5, order.getPaymentMethod());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Échec création commande, aucune ligne affectée");
            }

            rs = pstmt.getGeneratedKeys();
            int orderId;
            if (rs.next()) {
                orderId = rs.getInt(1);
                order.setId(orderId);
            } else {
                throw new SQLException("Échec création commande, aucun ID généré");
            }

            System.out.println("📝 Commande #" + orderId + " créée, ajout de " + order.getItems().size() + " item(s)");

            // Sauvegarder les items
            for (OrderItem item : order.getItems()) {
                item.setOrderId(orderId);
                String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

                try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                    itemStmt.setInt(1, item.getOrderId());
                    itemStmt.setInt(2, item.getProductId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getUnitPrice());
                    itemStmt.executeUpdate();
                    System.out.println("   ✅ Item ajouté: product_id=" + item.getProductId() + ", quantity=" + item.getQuantity());
                }

                // Mettre à jour le stock
                Product product = productService.getById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() - item.getQuantity());
                    productService.updateStock(product.getId(), product.getStock());
                }
            }

            conn.commit();
            System.out.println("✅ Commande #" + orderId + " créée avec succès !");
            return orderId;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("❌ Erreur création commande: " + e.getMessage());
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    // ============ COMMANDES PAR UTILISATEUR ============
    public List<Order> getOrdersByUser(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC";

        System.out.println("🔍 Recherche des commandes pour user_id: " + userId);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    // Récupérer les items
                    List<OrderItem> items = orderItemService.getItemsByOrderId(order.getId());
                    order.setItems(items);
                    orders.add(order);
                    System.out.println("   📦 Commande #" + order.getId() + " trouvée avec " + items.size() + " item(s)");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getOrdersByUser: " + e.getMessage());
            throw e;
        }

        System.out.println("📊 Total commandes trouvées: " + orders.size());
        return orders;
    }

    // ============ TOUTES LES COMMANDES ============
    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                List<OrderItem> items = orderItemService.getItemsByOrderId(order.getId());
                order.setItems(items);
                orders.add(order);
            }
        }
        return orders;
    }

    // ============ COMMANDE PAR ID ============
    public Order getOrderById(int orderId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    List<OrderItem> items = orderItemService.getItemsByOrderId(order.getId());
                    order.setItems(items);
                    return order;
                }
            }
        }
        return null;
    }

    // ============ METTRE À JOUR LE STATUT ============
    public void updateOrderStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, orderId);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println(" Statut commande #" + orderId + " mis à jour : " + status);
            } else {
                System.out.println("Commande #" + orderId + " non trouvée");
            }
        }
    }

    // ============ SUPPRIMER UNE COMMANDE ============
    public void deleteOrder(int orderId) throws SQLException {
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Récupérer les items pour restaurer les stocks
            List<OrderItem> items = orderItemService.getItemsByOrderId(orderId);

            for (OrderItem item : items) {
                Product product = productService.getById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productService.updateStock(product.getId(), product.getStock());
                }
            }

            // Supprimer les items
            String deleteItemsSql = "DELETE FROM order_items WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteItemsSql)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
            }

            // Supprimer la commande
            String deleteOrderSql = "DELETE FROM orders WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteOrderSql)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
            }

            conn.commit();
            System.out.println(" Commande #" + orderId + " supprimée !");

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // ============ ANNULER UNE COMMANDE ============
    public void cancelOrder(int orderId) throws SQLException {
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Order order = getOrderById(orderId);
            if (order == null) {
                throw new SQLException("Commande non trouvée");
            }
            if (!order.getStatus().equals("PENDING")) {
                throw new SQLException("Seules les commandes en attente peuvent être annulées");
            }

            // Restaurer les stocks
            for (OrderItem item : order.getItems()) {
                Product product = productService.getById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productService.updateStock(product.getId(), product.getStock());
                }
            }

            // Changer le statut
            String sql = "UPDATE orders SET status = 'CANCELLED' WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, orderId);
                pstmt.executeUpdate();
            }

            conn.commit();
            System.out.println(" Commande #" + orderId + " annulée !");

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // ============ MAPPING ============
    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setUserId(rs.getInt("user_id"));
        order.setOrderDate(rs.getTimestamp("order_date"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setShippingAddress(rs.getString("shipping_address"));
        order.setPaymentMethod(rs.getString("payment_method"));
        return order;
    }
}