/**
 * Product class represents an item in the catalog
 * 
 * EXPLANATION:
 * - Core entity of e-commerce system
 * - Contains product details and pricing
 * - Can have reviews and ratings
 * 
 * SOLID PRINCIPLES:
 * - SRP: Product only manages product information
 * - OCP: Can be extended for special product types
 */
public class Product {
    private String productId;
    private String name;
    private String description;
    private String category;
    private double price;
    private String seller;
    private double rating;      // Average rating (0-5)
    private int reviewCount;
    
    /**
     * Constructor
     * 
     * @param productId Unique identifier
     * @param name Product name
     * @param description Product description
     * @param category Product category
     * @param price Product price
     * @param seller Seller name
     */
    public Product(String productId, String name, String description, 
                  String category, double price, String seller) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.seller = seller;
        this.rating = 0.0;
        this.reviewCount = 0;
    }
    
    /**
     * Add review and update rating
     * 
     * EXPLANATION:
     * - Calculates new average rating
     * - In real system, would store review separately
     */
    public void addReview(double newRating) {
        double totalRating = rating * reviewCount + newRating;
        reviewCount++;
        rating = totalRating / reviewCount;
    }
    
    // Getters
    public String getProductId() { return productId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getSeller() { return seller; }
    public double getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    
    // Setters
    public void setPrice(double price) { this.price = price; }
    
    @Override
    public String toString() {
        return name + " - $" + price + " (" + rating + "★, " + 
               reviewCount + " reviews) by " + seller;
    }
}
