package tn.esprit.museum.services;

import tn.esprit.museum.entities.OrderItem;
import tn.esprit.museum.entities.Product;
import tn.esprit.museum.interfaces.IService;
import tn.esprit.museum.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderItemService implements IService<OrderItem> {

    private ProductService productService = new ProductService();

    // ============ INSERT (pour transaction) ============
    public void insert(OrderItem item, Connection conn) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, item.getOrderId());
            pstmt.setInt(2, item.getProductId());
            pstmt.setInt(3, item.getQuantity());
            pstmt.setDouble(4, item.getUnitPrice());
            pstmt.executeUpdate();
        }
    }

    // ============ INSERT STANDARD ============
    @Override
    public void insert(OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, item.getOrderId());
            pstmt.setInt(2, item.getProductId());
            pstmt.setInt(3, item.getQuantity());
            pstmt.setDouble(4, item.getUnitPrice());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
        }
    }

    // ============ RÉCUPÉRER TOUS LES ITEMS ============
    @Override
    public List<OrderItem> getAll() throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapResultSetToOrderItem(rs));
            }
        }
        return items;
    }

    // ============ RÉCUPÉRER PAR ID ============
    @Override
    public OrderItem getById(int id) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOrderItem(rs);
                }
            }
        }
        return null;
    }

    // ============ RÉCUPÉRER LES ITEMS D'UNE COMMANDE ============
    public List<OrderItem> getItemsByOrderId(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";

        System.out.println("🔍 Recherche des items pour order_id: " + orderId);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = mapResultSetToOrderItem(rs);

                    // Charger le produit associé
                    try {
                        Product product = productService.getById(item.getProductId());
                        item.setProduct(product);
                    } catch (SQLException e) {
                        System.err.println("⚠️ Impossible de charger le produit ID: " + item.getProductId());
                    }

                    items.add(item);
                    System.out.println("   ✅ Item trouvé: product_id=" + item.getProductId() + ", quantity=" + item.getQuantity());
                }
            }
        }

        System.out.println("📊 Total items trouvés: " + items.size());
        return items;
    }

    // ============ METTRE À JOUR ============
    @Override
    public void update(OrderItem item) throws SQLException {
        String sql = "UPDATE order_items SET quantity = ?, unit_price = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, item.getQuantity());
            pstmt.setDouble(2, item.getUnitPrice());
            pstmt.setInt(3, item.getId());
            pstmt.executeUpdate();
        }
    }

    // ============ SUPPRIMER ============
    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM order_items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // ============ SUPPRIMER TOUS LES ITEMS D'UNE COMMANDE ============
    public void deleteByOrderId(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, orderId);
            pstmt.executeUpdate();
            System.out.println("✅ Items de la commande #" + orderId + " supprimés");
        }
    }

    // ============ MAPPING ============
    private OrderItem mapResultSetToOrderItem(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setId(rs.getInt("id"));
        item.setOrderId(rs.getInt("order_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        return item;
    }
}

