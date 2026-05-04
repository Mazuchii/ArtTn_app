package tn.esprit.museum.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;
public class Order {
    private int id;
    private int userId;
    private Timestamp orderDate;
    private double totalAmount;
    private String status;
    private String shippingAddress;
    private String paymentMethod;
    private List<OrderItem> items;

    public Order() {
        this.items = new ArrayList<>();
        this.status = "PENDING";
    }

    public Order(int userId, String shippingAddress, String paymentMethod) {
        this();
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public Timestamp getOrderDate() { return orderDate; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getShippingAddress() { return shippingAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public List<OrderItem> getItems() { return items; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getStatusFrench() {
        switch (status) {
            case "PENDING": return "⏳ En attente";
            case "CONFIRMED": return "✅ Confirmée";
            case "SHIPPED": return "📦 Expédiée";
            case "DELIVERED": return "🚚 Livrée";
            case "CANCELLED": return "❌ Annulée";
            default: return status;
        }
    }

    @Override
    public String toString() {
        return "Commande #" + id + " - " + getStatusFrench() + " - " + totalAmount + "D";
    }
}
