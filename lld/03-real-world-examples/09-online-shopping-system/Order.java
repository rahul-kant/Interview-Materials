import java.time.LocalDateTime;
import java.util.*;

/**
 * Order class represents a completed purchase
 * 
 * EXPLANATION:
 * - Created when user checks out cart
 * - Tracks order status lifecycle
 * - Manages order items and payment
 * 
 * DESIGN PATTERNS:
 * - State pattern for order status
 * - Builder pattern could be used for complex orders
 */
public class Order {
    private String orderId;
    private User user;
    private List<CartItem> items;
    private double totalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private LocalDateTime orderDate;
    private String shippingAddress;
    
    public Order(String orderId, User user, Collection<CartItem> cartItems,
                PaymentMethod paymentMethod) {
        this.orderId = orderId;
        this.user = user;
        this.items = new ArrayList<>(cartItems);
        this.paymentMethod = paymentMethod;
        this.status = OrderStatus.PENDING;
        this.orderDate = LocalDateTime.now();
        this.totalAmount = calculateTotal();
    }
    
    private double calculateTotal() {
        return items.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }
    
    /**
     * Confirm order after payment
     */
    public void confirm() {
        if (status == OrderStatus.PENDING) {
            status = OrderStatus.CONFIRMED;
            System.out.println("Order " + orderId + " confirmed!");
        }
    }
    
    /**
     * Mark order as processing
     */
    public void process() {
        if (status == OrderStatus.CONFIRMED) {
            status = OrderStatus.PROCESSING;
            System.out.println("Order " + orderId + " is being processed");
        }
    }
    
    /**
     * Mark order as shipped
     */
    public void ship() {
        if (status == OrderStatus.PROCESSING) {
            status = OrderStatus.SHIPPED;
            System.out.println("Order " + orderId + " has been shipped!");
        }
    }
    
    /**
     * Mark order as delivered
     */
    public void deliver() {
        if (status == OrderStatus.SHIPPED) {
            status = OrderStatus.DELIVERED;
            System.out.println("Order " + orderId + " delivered successfully!");
        }
    }
    
    /**
     * Cancel order
     */
    public void cancel() {
        if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
            status = OrderStatus.CANCELLED;
            System.out.println("Order " + orderId + " cancelled");
        }
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public User getUser() { return user; }
    public List<CartItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public String getShippingAddress() { return shippingAddress; }
    
    public void setShippingAddress(String address) {
        this.shippingAddress = address;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(orderId).append("\n");
        sb.append("Customer: ").append(user.getName()).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Items:\n");
        for (CartItem item : items) {
            sb.append("  ").append(item).append("\n");
        }
        sb.append("Total: $").append(String.format("%.2f", totalAmount)).append("\n");
        sb.append("Payment: ").append(paymentMethod);
        return sb.toString();
    }
}
