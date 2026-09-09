/**
 * CartItem represents a product in shopping cart with quantity
 * 
 * EXPLANATION:
 * - Links product with quantity
 * - Calculates subtotal
 * - Part of Composite pattern (Cart contains CartItems)
 */
public class CartItem {
    private Product product;
    private int quantity;
    
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
    
    /**
     * Calculate subtotal for this item
     */
    public double getSubtotal() {
        return product.getPrice() * quantity;
    }
    
    // Getters and setters
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    @Override
    public String toString() {
        return product.getName() + " x " + quantity + 
               " = $" + String.format("%.2f", getSubtotal());
    }
}
