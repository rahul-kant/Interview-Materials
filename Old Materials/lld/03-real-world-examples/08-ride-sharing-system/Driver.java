package ride_sharing;

/**
 * Driver - Represents a ride-share driver
 * 
 * Interview Key Points:
 * - Driver status tracking
 * - Vehicle assignment
 * - Location updates
 * - Earnings calculation
 */
public class Driver extends User {
    private final Vehicle vehicle;
    private Location currentLocation;
    private DriverStatus status;
    private double totalEarnings;
    private int tripsCompleted;
    
    public enum DriverStatus {
        OFFLINE,
        AVAILABLE,
        ON_TRIP,
        ON_BREAK
    }
    
    public Driver(String userId, String name, String email, String phone, Vehicle vehicle) {
        super(userId, name, email, phone);
        this.vehicle = vehicle;
        this.status = DriverStatus.OFFLINE;
        this.totalEarnings = 0.0;
        this.tripsCompleted = 0;
    }
    
    public Vehicle getVehicle() {
        return vehicle;
    }
    
    public synchronized Location getCurrentLocation() {
        return currentLocation;
    }
    
    public synchronized void updateLocation(Location location) {
        this.currentLocation = location;
    }
    
    public DriverStatus getStatus() {
        return status;
    }
    
    public void setStatus(DriverStatus status) {
        this.status = status;
    }
    
    public boolean isAvailable() {
        return status == DriverStatus.AVAILABLE && isActive();
    }
    
    public double getTotalEarnings() {
        return totalEarnings;
    }
    
    public void addEarnings(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.totalEarnings += amount;
    }
    
    public int getTripsCompleted() {
        return tripsCompleted;
    }
    
    public void incrementTrips() {
        this.tripsCompleted++;
    }
    
    /**
     * Calculate distance to pickup location
     */
    public double getDistanceToLocation(Location location) {
        if (currentLocation == null) {
            throw new IllegalStateException("Driver location not available");
        }
        return currentLocation.distanceTo(location);
    }
    
    @Override
    public String toString() {
        return String.format("Driver: %s - %s [%s] - Trips: %d, Earnings: $%.2f",
            super.toString(),
            vehicle.getModel(),
            status,
            tripsCompleted,
            totalEarnings);
    }
}