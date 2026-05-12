package tn.esprit.museum.services;

import tn.esprit.museum.entities.Order;
import tn.esprit.museum.entities.OrderItem;
import tn.esprit.museum.interfaces.IService;
import tn.esprit.museum.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderService implements IService<Order> {

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

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int orderId;

                // 1. Insérer la commande
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, order.getUserId());
                    pstmt.setDouble(2, order.getTotalAmount());
                    pstmt.setString(3, order.getStatus());
                    pstmt.setString(4, order.getShippingAddress());
                    pstmt.setString(5, order.getPaymentMethod());
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            orderId = rs.getInt(1);
                            order.setId(orderId);
                        } else {
                            throw new SQLException("Échec création commande, aucun ID généré");
                        }
                    }
                }

                System.out.println("📝 Commande #" + orderId + " créée, ajout de " + order.getItems().size() + " item(s)");

                // 2. Insérer les items ET mettre à jour le stock — tout dans la même connexion/transaction
                String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
                String stockSql = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";

                for (OrderItem item : order.getItems()) {
                    item.setOrderId(orderId);

                    // Insérer l'item
                    try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                        itemStmt.setInt(1, orderId);
                        itemStmt.setInt(2, item.getProductId());
                        itemStmt.setInt(3, item.getQuantity());
                        itemStmt.setDouble(4, item.getUnitPrice());
                        itemStmt.executeUpdate();
                        System.out.println("   ✅ Item ajouté: product_id=" + item.getProductId() + ", qty=" + item.getQuantity());
                    }

                    // Décrémenter le stock dans la même transaction (évite le lock inter-connexions)
                    try (PreparedStatement stockStmt = conn.prepareStatement(stockSql)) {
                        stockStmt.setInt(1, item.getQuantity());
                        stockStmt.setInt(2, item.getProductId());
                        stockStmt.setInt(3, item.getQuantity()); // vérifie stock >= qty
                        int updated = stockStmt.executeUpdate();
                        if (updated == 0) {
                            throw new SQLException("Stock insuffisant pour le produit ID " + item.getProductId());
                        }
                    }
                }

                conn.commit();
                System.out.println("✅ Commande #" + orderId + " créée avec succès !");
                return orderId;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("❌ Erreur création commande (rollback effectué): " + e.getMessage());
                throw e;
            }
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
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Restaurer les stocks dans la même connexion
                String selectItems = "SELECT product_id, quantity FROM order_items WHERE order_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectItems)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        String restoreStock = "UPDATE products SET stock = stock + ? WHERE id = ?";
                        while (rs.next()) {
                            try (PreparedStatement rs2 = conn.prepareStatement(restoreStock)) {
                                rs2.setInt(1, rs.getInt("quantity"));
                                rs2.setInt(2, rs.getInt("product_id"));
                                rs2.executeUpdate();
                            }
                        }
                    }
                }

                // Supprimer les items
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }

                // Supprimer la commande
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }

                conn.commit();
                System.out.println("✅ Commande #" + orderId + " supprimée !");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ============ ANNULER UNE COMMANDE ============
    public void cancelOrder(int orderId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Vérifier le statut
                String checkSql = "SELECT status FROM orders WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Commande non trouvée");
                        String status = rs.getString("status");
                        if (!"PENDING".equals(status))
                            throw new SQLException("Seules les commandes en attente peuvent être annulées");
                    }
                }

                // Restaurer les stocks dans la même connexion
                String selectItems = "SELECT product_id, quantity FROM order_items WHERE order_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectItems)) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        String restoreStock = "UPDATE products SET stock = stock + ? WHERE id = ?";
                        while (rs.next()) {
                            try (PreparedStatement rs2 = conn.prepareStatement(restoreStock)) {
                                rs2.setInt(1, rs.getInt("quantity"));
                                rs2.setInt(2, rs.getInt("product_id"));
                                rs2.executeUpdate();
                            }
                        }
                    }
                }

                // Changer le statut
                try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE id = ?")) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }

                conn.commit();
                System.out.println("✅ Commande #" + orderId + " annulée !");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
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

