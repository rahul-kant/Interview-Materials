import java.util.*;

/**
 * Main Shopping System class
 * 
 * EXPLANATION:
 * - Coordinates all e-commerce operations
 * - Manages products, carts, orders
 * - Integrates with inventory
 * 
 * DESIGN PATTERNS:
 * - Singleton: Only one shopping system
 * - Facade: Simplifies complex operations
 * - Factory: Creates carts and orders
 */
public class ShoppingSystem {
    private static ShoppingSystem instance;
    private Map<String, Product> products;
    private Map<String, Cart> carts;
    private Map<String, Order> orders;
    private Inventory inventory;
    private int orderIdCounter;
    
    private ShoppingSystem() {
        this.products = new HashMap<>();
        this.carts = new HashMap<>();
        this.orders = new HashMap<>();
        this.inventory = Inventory.getInstance();
        this.orderIdCounter = 1;
    }
    
    public static synchronized ShoppingSystem getInstance() {
        if (instance == null) {
            instance = new ShoppingSystem();
        }
        return instance;
    }
    
    /**
     * Add product to catalog
     */
    public void addProduct(Product product, int initialStock) {
        products.put(product.getProductId(), product);
        inventory.addStock(product.getProductId(), initialStock);
        System.out.println("Product added: " + product.getName());
    }
    
    /**
     * Search products by name
     */
    public List<Product> searchProducts(String keyword) {
        List<Product> results = new ArrayList<>();
        for (Product product : products.values()) {
            if (product.getName().toLowerCase().contains(keyword.toLowerCase())) {
                results.add(product);
            }
        }
        return results;
    }
    
    /**
     * Get products by category
     */
    public List<Product> getProductsByCategory(String category) {
        List<Product> results = new ArrayList<>();
        for (Product product : products.values()) {
            if (product.getCategory().equalsIgnoreCase(category)) {
                results.add(product);
            }
        }
        return results;
    }
    
    /**
     * Get or create cart for user
     */
    public Cart getCart(User user) {
        return carts.computeIfAbsent(user.getUserId(), 
            k -> new Cart("CART-" + user.getUserId(), user));
    }
    
    /**
     * Checkout cart and create order
     * 
     * EXPLANATION:
     * - Validates inventory
     * - Reserves stock
     * - Creates order
     * - Clears cart on success
     * 
     * INTERVIEW POINT: Transaction handling
     * - What if inventory reservation fails midway?
     * - Need rollback mechanism
     */
    public Order checkout(Cart cart, PaymentMethod paymentMethod) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty!");
            return null;
        }
        
        // Check and reserve inventory
        List<CartItem> items = new ArrayList<>(cart.getItems());
        List<String> reservedProducts = new ArrayList<>();
        
        try {
            // Reserve all items
            for (CartItem item : items) {
                String productId = item.getProduct().getProductId();
                int quantity = item.getQuantity();
                
                if (!inventory.reserveStock(productId, quantity)) {
                    throw new RuntimeException("Insufficient stock for " + 
                        item.getProduct().getName());
                }
                
                reservedProducts.add(productId);
            }
            
            // Create order
            String orderId = "ORD" + String.format("%06d", orderIdCounter++);
            Order order = new Order(orderId, cart.getUser(), items, paymentMethod);
            order.setShippingAddress(cart.getUser().getAddress());
            
            // Process payment (simulation)
            System.out.println("Processing payment via " + paymentMethod + "...");
            order.confirm();
            
            // Store order
            orders.put(orderId, order);
            
            // Clear cart
            cart.clear();
            
            System.out.println("Order placed successfully! Order ID: " + orderId);
            return order;
            
        } catch (Exception e) {
            // Rollback inventory reservations
            System.out.println("Checkout failed: " + e.getMessage());
            for (CartItem item : items) {
                if (reservedProducts.contains(item.getProduct().getProductId())) {
                    inventory.releaseStock(
                        item.getProduct().getProductId(),
                        item.getQuantity()
                    );
                }
            }
            return null;
        }
    }
    
    /**
     * Get order by ID
     */
    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }
    
    /**
     * Get all orders for a user
     */
    public List<Order> getUserOrders(User user) {
        List<Order> userOrders = new ArrayList<>();
        for (Order order : orders.values()) {
            if (order.getUser().getUserId().equals(user.getUserId())) {
                userOrders.add(order);
            }
        }
        return userOrders;
    }
    
    /**
     * Get product by ID
     */
    public Product getProduct(String productId) {
        return products.get(productId);
    }
    
    /**
     * Display all products
     */
    public void displayProducts() {
        System.out.println("\n=== Product Catalog ===");
        for (Product product : products.values()) {
            int stock = inventory.getStock(product.getProductId());
            System.out.println(product + " | Stock: " + stock);
        }
    }
}
