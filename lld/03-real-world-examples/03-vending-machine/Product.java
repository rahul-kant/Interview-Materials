/**
 * Product Class - Represents items in vending machine
 * 
 * Interview Key Points:
 * - Immutable design (final fields)
 * - Price stored in cents to avoid floating point issues
 * - Could add expiry date, nutritional info, etc.
 */
public class Product {
    private final String code;      // Product code (e.g., "A1", "B2")
    private final String name;      // Product name
    private final int price;        // Price in cents
    private final String category;  // e.g., "Snacks", "Beverages"
    
    public Product(String code, String name, int price, String category) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be null or empty");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        
        this.code = code;
        this.name = name;
        this.price = price;
        this.category = category;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public int getPrice() {
        return price;
    }
    
    public String getCategory() {
        return category;
    }
    
    /**
     * Display price in dollar format
     */
    public String getFormattedPrice() {
        return String.format("$%.2f", price / 100.0);
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s (%s)", 
            code, name, getFormattedPrice(), category);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return code.equals(product.code);
    }
    
    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
