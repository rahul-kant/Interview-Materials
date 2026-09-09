import java.util.*;

/**
 * VendingMachine - Main controller class (Singleton)
 * 
 * Interview Key Points:
 * - Singleton pattern ensures single instance
 * - State pattern for behavior changes
 * - Thread-safe operations
 * - Maintains inventory and current transaction
 * - Handles all vending operations
 * 
 * Design Decisions:
 * - Why Singleton? Only one physical machine
 * - Why State Pattern? Clean state management
 * - Thread-safety for concurrent operations
 */
public class VendingMachine {
    // Singleton instance
    private static VendingMachine instance;
    
    // Machine components
    private final Inventory inventory;
    private final Map<String, Product> productCatalog;
    
    // Current transaction state
    private State currentState;
    private int currentBalance;     // Current money inserted (in cents)
    private Product selectedProduct;
    
    /**
     * Private constructor for Singleton
     */
    private VendingMachine() {
        this.inventory = new Inventory();
        this.productCatalog = new HashMap<>();
        this.currentState = new IdleState();
        this.currentBalance = 0;
        this.selectedProduct = null;
    }
    
    /**
     * Get singleton instance
     * Thread-safe implementation
     */
    public static synchronized VendingMachine getInstance() {
        if (instance == null) {
            instance = new VendingMachine();
        }
        return instance;
    }
    
    /**
     * Initialize machine with products
     */
    public void addProduct(Product product, int quantity) {
        productCatalog.put(product.getCode(), product);
        inventory.addProduct(product, quantity);
    }
    
    /**
     * Display all available products
     */
    public void displayProducts() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║     AVAILABLE PRODUCTS                ║");
        System.out.println("╠═══════════════════════════════════════╣");
        
        List<Product> available = inventory.getAvailableProducts();
        
        if (available.isEmpty()) {
            System.out.println("║  No products available                ║");
        } else {
            for (Product product : available) {
                int stock = inventory.getQuantity(product);
                System.out.printf("║ %-37s ║%n", product.toString() + 
                    " [Stock: " + stock + "]");
            }
        }
        
        System.out.println("╚═══════════════════════════════════════╝\n");
    }
    
    /**
     * Insert coin - delegates to current state
     */
    public void insertCoin(Coin coin) {
        System.out.println("\n→ Current State: " + currentState.getStateName());
        currentState.insertCoin(this, coin);
    }
    
    /**
     * Select product - delegates to current state
     */
    public void selectProduct(String productCode) {
        System.out.println("\n→ Current State: " + currentState.getStateName());
        currentState.selectProduct(this, productCode);
    }
    
    /**
     * Cancel transaction - delegates to current state
     */
    public void cancelTransaction() {
        System.out.println("\n→ Current State: " + currentState.getStateName());
        currentState.cancelTransaction(this);
    }
    
    // ========== State Management Methods ==========
    
    protected void setState(State state) {
        this.currentState = state;
        System.out.println("→ State changed to: " + state.getStateName());
    }
    
    protected State getCurrentState() {
        return currentState;
    }
    
    // ========== Transaction Management ==========
    
    protected void addMoney(int amount) {
        this.currentBalance += amount;
    }
    
    protected int getCurrentBalance() {
        return currentBalance;
    }
    
    protected void setSelectedProduct(Product product) {
        this.selectedProduct = product;
    }
    
    protected Product getSelectedProduct() {
        return selectedProduct;
    }
    
    protected void resetTransaction() {
        this.currentBalance = 0;
        this.selectedProduct = null;
    }
    
    // ========== Product Management ==========
    
    protected Product getProductByCode(String code) {
        return productCatalog.get(code);
    }
    
    protected boolean isProductAvailable(Product product) {
        return inventory.isAvailable(product);
    }
    
    protected boolean deductFromInventory(Product product) {
        return inventory.deductProduct(product);
    }
    
    /**
     * Return change to customer
     * In real system, this would interface with hardware
     */
    protected void returnChange(int amount) {
        System.out.println("\n💵 Change breakdown:");
        
        int remaining = amount;
        
        // Calculate optimal coin combination
        Coin[] coins = {Coin.DOLLAR, Coin.QUARTER, Coin.DIME, Coin.NICKEL, Coin.PENNY};
        
        for (Coin coin : coins) {
            int count = remaining / coin.getValue();
            if (count > 0) {
                System.out.println("   " + coin.name() + " × " + count + 
                    " = " + String.format("$%.2f", (count * coin.getValue()) / 100.0));
                remaining -= count * coin.getValue();
            }
        }
    }
    
    // ========== Admin Methods ==========
    
    public void showInventory() {
        inventory.displayInventory();
    }
    
    public void restockAll(int quantity) {
        inventory.restock(quantity);
    }
    
    public String getCurrentStateInfo() {
        return String.format("State: %s | Balance: $%.2f | Product: %s",
            currentState.getStateName(),
            currentBalance / 100.0,
            selectedProduct != null ? selectedProduct.getName() : "None");
    }
}
