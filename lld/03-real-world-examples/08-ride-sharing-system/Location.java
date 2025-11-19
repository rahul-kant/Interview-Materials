package ride_sharing;

/**
 * Location - Represents a GPS location
 * 
 * Interview Key Points:
 * - Location tracking
 * - Distance calculation
 * - Validation of coordinates
 */
public class Location {
    private final double latitude;
    private final double longitude;
    
    public Location(double latitude, double longitude) {
        validate(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    private void validate(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude: " + longitude);
        }
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    /**
     * Calculate distance to another location in kilometers
     * Uses Haversine formula
     */
    public double distanceTo(Location other) {
        final int R = 6371; // Earth's radius in kilometers
        
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
                
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
    
    @Override
    public String toString() {
        return String.format("(%.6f, %.6f)", latitude, longitude);
    }
}