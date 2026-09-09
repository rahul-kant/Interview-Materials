/**
 * Online Shopping System Demo
 * 
 * This demo demonstrates a complete e-commerce workflow:
 * 1. Setup products and inventory
 * 2. Browse and search products
 * 3. Add items to cart
 * 4. Checkout and place order
 * 5. Track order status
 * 6. Handle concurrent orders
 * 
 * LEARNING OBJECTIVES:
 * - Understand e-commerce system architecture
 * - Learn inventory management with concurrency
 * - See cart and order lifecycle
 * - Transaction handling with rollback
 */
public class OnlineShoppingDemo {
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  ONLINE SHOPPING SYSTEM - DEMO");
        System.out.println("==========================================\n");
        
        ShoppingSystem system = ShoppingSystem.getInstance();
        
        // Demo 1: Setup products
        System.out.println("=== DEMO 1: Setup Products ===");
        setupProducts(system);
        
        // Demo 2: Browse products
        System.out.println("\n\n=== DEMO 2: Browse Products ===");
        browseProducts(system);
        
        // Demo 3: Add to cart and checkout
        System.out.println("\n\n=== DEMO 3: Shopping Flow ===");
        shoppingFlow(system);
        
        // Demo 4: Order tracking
        System.out.println("\n\n=== DEMO 4: Order Tracking ===");
        orderTracking(system);
        
        // Demo 5: Handle insufficient stock
        System.out.println("\n\n=== DEMO 5: Handle Insufficient Stock ===");
        handleInsufficientStock(system);
        
        System.out.println("\n\n==========================================");
        System.out.println("  DEMO COMPLETE");
        System.out.println("==========================================");
    }
    
    /**
     * Demo 1: Setup products and inventory
     */
    private static void setupProducts(ShoppingSystem system) {
        System.out.println("Setting up product catalog...\n");
        
        // Electronics
        Product laptop = new Product("P001", "Gaming Laptop", 
            "High-performance laptop", "Electronics", 1299.99, "TechCorp");
        system.addProduct(laptop, 10);
        
        Product phone = new Product("P002", "Smartphone X", 
            "Latest flagship phone", "Electronics", 899.99, "MobileCo");
        system.addProduct(phone, 25);
        
        Product headphones = new Product("P003", "Wireless Headphones",
            "Noise cancelling", "Electronics", 199.99, "AudioBrand");
        system.addProduct(headphones, 50);
        
        // Books
        Product book1 = new Product("P004", "Design Patterns",
            "Software design book", "Books", 49.99, "TechBooks");
        system.addProduct(book1, 100);
        
        Product book2 = new Product("P005", "Clean Code",
            "Programming best practices", "Books", 44.99, "TechBooks");
        system.addProduct(book2, 100);
        
        // Add reviews
        laptop.addReview(4.5);
        laptop.addReview(5.0);
        phone.addReview(4.8);
        headphones.addReview(4.2);
        book1.addReview(5.0);
        book2.addReview(4.9);
        
        System.out.println("\n✓ Product catalog setup complete!");
    }
    
    /**
     * Demo 2: Browse and search products
     */
    private static void browseProducts(ShoppingSystem system) {
        // Display all products
        system.displayProducts();
        
        // Search by keyword
        System.out.println("\n\nSearching for 'laptop'...");
        var results = system.searchProducts("laptop");
        System.out.println("Found " + results.size() + " product(s):");
        for (Product p : results) {
            System.out.println("  " + p);
        }
        
        // Browse by category
        System.out.println("\n\nBrowsing Electronics category...");
        results = system.getProductsByCategory("Electronics");
        System.out.println("Found " + results.size() + " product(s):");
        for (Product p : results) {
            System.out.println("  " + p);
        }
    }
    
    /**
     * Demo 3: Complete shopping flow
     */
    private static void shoppingFlow(ShoppingSystem system) {
        // Create user
        User user = new User("U001", "John Doe", "john@email.com", "555-0100");
        user.setAddress("123 Main St, City, State 12345");
        System.out.println("User: " + user);
        
        // Get cart
        Cart cart = system.getCart(user);
        
        // Add items to cart
        System.out.println("\n--- Adding items to cart ---");
        Product laptop = system.getProduct("P001");
        Product headphones = system.getProduct("P003");
        Product book = system.getProduct("P004");
        
        cart.addItem(laptop, 1);
        cart.addItem(headphones, 2);
        cart.addItem(book, 3);
        
        // Display cart
        System.out.println("\n" + cart);
        System.out.println("Items in cart: " + cart.getItemCount());
        
        // Checkout
        System.out.println("\n--- Checkout ---");
        Order order = system.checkout(cart, PaymentMethod.CREDIT_CARD);
        
        if (order != null) {
            System.out.println("\n✓ Order placed successfully!");
            System.out.println(order);
        }
    }
    
    /**
     * Demo 4: Order tracking and status updates
     */
    private static void orderTracking(ShoppingSystem system) {
        // Get user's orders
        User user = new User("U001", "John Doe", "john@email.com", "555-0100");
        var orders = system.getUserOrders(user);
        
        if (!orders.isEmpty()) {
            Order order = orders.get(0);
            System.out.println("Tracking order: " + order.getOrderId());
            System.out.println("Current status: " + order.getStatus());
            
            // Simulate order lifecycle
            System.out.println("\n--- Order Lifecycle ---");
            order.process();
            System.out.println("Status: " + order.getStatus());
            
            order.ship();
            System.out.println("Status: " + order.getStatus());
            
            order.deliver();
            System.out.println("Status: " + order.getStatus());
            
            System.out.println("\n✓ Order delivered successfully!");
        }
    }
    
    /**
     * Demo 5: Handle insufficient stock scenario
     */
    private static void handleInsufficientStock(ShoppingSystem system) {
        System.out.println("Attempting to order out-of-stock item...\n");
        
        User user = new User("U002", "Jane Smith", "jane@email.com", "555-0200");
        user.setAddress("456 Oak Ave, City, State 54321");
        
        Cart cart = system.getCart(user);
        
        // Try to add more items than available
        Product laptop = system.getProduct("P001");
        Inventory inventory = Inventory.getInstance();
        
        System.out.println("Current stock: " + inventory.getStock("P001"));
        System.out.println("Attempting to add 50 units to cart...");
        
        cart.addItem(laptop, 50);
        
        // Try to checkout
        System.out.println("\nAttempting checkout...");
        Order order = system.checkout(cart, PaymentMethod.UPI);
        
        if (order == null) {
            System.out.println("✗ Checkout failed due to insufficient stock");
            System.out.println("Stock properly protected from overselling!");
        }
    }
}
