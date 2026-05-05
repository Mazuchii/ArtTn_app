package tn.esprit.museum.entities;

public class OrderItem {
    private int id;
    private int orderId;
    private int productId;
    private int quantity;
    private double unitPrice;
    private Product product;

    public OrderItem() {}

    public OrderItem(int productId, int quantity, double unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters
    public int getId() { return id; }
    public int getOrderId() { return orderId; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public Product getProduct() { return product; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setProductId(int productId) { this.productId = productId; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public void setProduct(Product product) { this.product = product; }

    public double getSubtotal() {
        return quantity * unitPrice;
    }
}
