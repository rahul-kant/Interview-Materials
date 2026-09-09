import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inventory Class - Manages product stock
 * 
 * Interview Key Points:
 * - Thread-safe using ConcurrentHashMap
 * - Handles stock management
 * - Prevents over-selling
 * - Low stock notifications
 */
public class Inventory {
    // Thread-safe map for concurrent access
    private final Map<Product, Integer> stock;
    private static final int LOW_STOCK_THRESHOLD = 3;
    
    public Inventory() {
        this.stock = new ConcurrentHashMap<>();
    }
    
    /**
     * Add product to inventory
     */
    public synchronized void addProduct(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        stock.merge(product, quantity, Integer::sum);
        System.out.println("Added " + quantity + " units of " + product.getName());
    }
    
    /**
     * Check if product is available
     */
    public boolean isAvailable(Product product) {
        return stock.getOrDefault(product, 0) > 0;
    }
    
    /**
     * Get quantity of product
     */
    public int getQuantity(Product product) {
        return stock.getOrDefault(product, 0);
    }
    
    /**
     * Deduct product from inventory
     * Thread-safe operation
     */
    public synchronized boolean deductProduct(Product product) {
        int currentStock = stock.getOrDefault(product, 0);
        
        if (currentStock <= 0) {
            System.out.println("Product " + product.getName() + " is out of stock!");
            return false;
        }
        
        stock.put(product, currentStock - 1);
        
        // Check for low stock warning
        if (currentStock - 1 <= LOW_STOCK_THRESHOLD) {
            System.out.println("⚠️  LOW STOCK WARNING: " + product.getName() + 
                " - Only " + (currentStock - 1) + " left!");
        }
        
        return true;
    }
    
    /**
     * Get all available products
     */
    public List<Product> getAvailableProducts() {
        List<Product> available = new ArrayList<>();
        
        for (Map.Entry<Product, Integer> entry : stock.entrySet()) {
            if (entry.getValue() > 0) {
                available.add(entry.getKey());
            }
        }
        
        return available;
    }
    
    /**
     * Display inventory status
     */
    public void displayInventory() {
        System.out.println("\n=== INVENTORY STATUS ===");
        
        for (Map.Entry<Product, Integer> entry : stock.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            String status = quantity > LOW_STOCK_THRESHOLD ? "✓" : "⚠️";
            
            System.out.printf("%s %s - Stock: %d%n", 
                status, product, quantity);
        }
        
        System.out.println("========================\n");
    }
    
    /**
     * Restock all products to maximum
     */
    public synchronized void restock(int maxQuantity) {
        for (Product product : stock.keySet()) {
            stock.put(product, maxQuantity);
        }
        System.out.println("Inventory restocked to " + maxQuantity + " units each");
    }
}
