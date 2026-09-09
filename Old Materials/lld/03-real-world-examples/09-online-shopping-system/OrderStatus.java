/**
 * Enum representing order status
 * 
 * EXPLANATION:
 * Tracks the lifecycle of an order from creation to delivery/cancellation
 * 
 * Used in State pattern for order management
 */
public enum OrderStatus {
    PENDING,        // Order created, payment not confirmed
    CONFIRMED,      // Payment successful
    PROCESSING,     // Order being prepared
    SHIPPED,        // Order dispatched
    DELIVERED,      // Order delivered to customer
    CANCELLED,      // Order cancelled
    RETURNED        // Order returned by customer
}
