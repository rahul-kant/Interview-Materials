import java.util.*;

/**
 * Shopping Cart class
 * 
 * EXPLANATION:
 * - Manages items before checkout
 * - Calculates total price
 * - Can apply discounts
 * 
 * DESIGN PATTERNS:
 * - Composite: Cart contains CartItems
 * - SRP: Cart only manages cart operations
 */
public class Cart {
    private String cartId;
    private User user;
    private Map<String, CartItem> items; // productId -> CartItem
    
    public Cart(String cartId, User user) {
        this.cartId = cartId;
        this.user = user;
        this.items = new HashMap<>();
    }
    
    /**
     * Add product to cart
     * 
     * EXPLANATION:
     * - If product exists, update quantity
     * - Otherwise, create new cart item
     */
    public void addItem(Product product, int quantity) {
        String productId = product.getProductId();
        
        if (items.containsKey(productId)) {
            CartItem item = items.get(productId);
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            items.put(productId, new CartItem(product, quantity));
        }
        
        System.out.println("Added " + quantity + "x " + product.getName() + " to cart");
    }
    
    /**
     * Remove product from cart
     */
    public void removeItem(String productId) {
        if (items.remove(productId) != null) {
            System.out.println("Item removed from cart");
        }
    }
    
    /**
     * Update quantity of product
     */
    public void updateQuantity(String productId, int quantity) {
        if (items.containsKey(productId)) {
            if (quantity <= 0) {
                removeItem(productId);
            } else {
                items.get(productId).setQuantity(quantity);
            }
        }
    }
    
    /**
     * Calculate total cart value
     */
    public double getTotal() {
        return items.values().stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }
    
    /**
     * Get item count
     */
    public int getItemCount() {
        return items.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    
    /**
     * Clear all items
     */
    public void clear() {
        items.clear();
    }
    
    /**
     * Check if cart is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    // Getters
    public String getCartId() { return cartId; }
    public User getUser() { return user; }
    public Collection<CartItem> getItems() { return items.values(); }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cart for ").append(user.getName()).append(":\n");
        for (CartItem item : items.values()) {
            sb.append("  ").append(item).append("\n");
        }
        sb.append("Total: $").append(String.format("%.2f", getTotal()));
        return sb.toString();
    }
}
