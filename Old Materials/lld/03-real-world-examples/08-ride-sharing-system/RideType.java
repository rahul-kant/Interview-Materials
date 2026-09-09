package ride_sharing;

/**
 * RideType - Different types of rides with pricing models
 * 
 * Interview Key Points:
 * - Different vehicle types
 * - Custom pricing models
 * - Extensible design
 */
public enum RideType {
    UBER_X("UberX", 2.0, 0.3, 5.0),      // Base sedan
    UBER_XL("UberXL", 3.0, 0.5, 7.0),    // SUV/Van
    UBER_BLACK("UberBlack", 5.0, 0.7, 10.0), // Luxury
    UBER_POOL("UberPool", 1.5, 0.2, 3.0); // Shared
    
    private final String displayName;
    private final double basePrice;     // Base fare in dollars
    private final double perKmRate;     // Rate per kilometer
    private final double perMinRate;    // Rate per minute
    
    RideType(String displayName, double basePrice, double perKmRate, double perMinRate) {
        this.displayName = displayName;
        this.basePrice = basePrice;
        this.perKmRate = perKmRate;
        this.perMinRate = perMinRate;
    }
    
    /**
     * Calculate fare based on distance and time
     */
    public double calculateFare(double distanceKm, long durationMinutes, double surgeFactor) {
        double baseFare = basePrice;
        double distanceFare = distanceKm * perKmRate;
        double timeFare = durationMinutes * perMinRate;
        
        double totalFare = (baseFare + distanceFare + timeFare) * surgeFactor;
        return Math.max(totalFare, getMinimumFare());
    }
    
    /**
     * Get minimum fare for this ride type
     */
    public double getMinimumFare() {
        return basePrice * 2;  // Minimum is 2x base price
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getBasePrice() {
        return basePrice;
    }
    
    public double getPerKmRate() {
        return perKmRate;
    }
    
    public double getPerMinRate() {
        return perMinRate;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}