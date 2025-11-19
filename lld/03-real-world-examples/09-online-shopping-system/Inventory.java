import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inventory class manages product stock
 * 
 * EXPLANATION:
 * - Thread-safe inventory management
 * - Prevents overselling
 * - Tracks product quantities
 * 
 * DESIGN PATTERNS:
 * - Singleton: Only one inventory instance
 * - Thread-safe with ConcurrentHashMap and synchronized methods
 * 
 * INTERVIEW POINTS:
 * - How to handle concurrent inventory updates?
 * - Prevention of overselling
 * - Distributed inventory management
 */
public class Inventory {
    private static Inventory instance;
    private Map<String, Integer> stock; // productId -> quantity
    
    private Inventory() {
        this.stock = new ConcurrentHashMap<>();
    }
    
    /**
     * Get singleton instance (thread-safe)
     */
    public static synchronized Inventory getInstance() {
        if (instance == null) {
            instance = new Inventory();
        }
        return instance;
    }
    
    /**
     * Add product stock
     */
    public void addStock(String productId, int quantity) {
        stock.merge(productId, quantity, Integer::sum);
        System.out.println("Added " + quantity + " units of product " + productId);
    }
    
    /**
     * Check product availability
     */
    public boolean isAvailable(String productId, int quantity) {
        return stock.getOrDefault(productId, 0) >= quantity;
    }
    
    /**
     * Reserve stock for order (thread-safe)
     * 
     * EXPLANATION:
     * - synchronized prevents race conditions
     * - Atomic check-and-update operation
     * - Returns true only if stock reserved successfully
     */
    public synchronized boolean reserveStock(String productId, int quantity) {
        int currentStock = stock.getOrDefault(productId, 0);
        
        if (currentStock >= quantity) {
            stock.put(productId, currentStock - quantity);
            return true;
        }
        
        return false;
    }
    
    /**
     * Release reserved stock (e.g., if order cancelled)
     */
    public synchronized void releaseStock(String productId, int quantity) {
        stock.merge(productId, quantity, Integer::sum);
    }
    
    /**
     * Get current stock level
     */
    public int getStock(String productId) {
        return stock.getOrDefault(productId, 0);
    }
}
