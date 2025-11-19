/**
 * Enum representing different types of rooms
 * 
 * Used to categorize rooms and apply different pricing strategies
 */
public enum RoomType {
    STANDARD(100.0),    // Base price: $100/night
    DELUXE(200.0),      // Base price: $200/night
    SUITE(400.0),       // Base price: $400/night
    PRESIDENTIAL(1000.0); // Base price: $1000/night
    
    private final double basePrice;
    
    RoomType(double basePrice) {
        this.basePrice = basePrice;
    }
    
    public double getBasePrice() {
        return basePrice;
    }
}
