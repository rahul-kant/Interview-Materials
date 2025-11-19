package ride_sharing;

// package ...
// package declaration omitted
import java.time.LocalDateTime;
import java.time.LocalDateTime;

/**
 * Ride - Represents a ride/trip
 * 
 * Interview Key Points:
 * - Ride state management
 * - Fare calculation
 * - Real-time tracking
 */
public class Ride {
    private final String rideId;
    private final Rider rider;
    private final Driver driver;
    private final Location pickup;
    private final Location dropoff;
    private final RideType rideType;
    
    private Location currentLocation;
    private RideStatus status;
    private double fare;
    private double distance;
    private LocalDateTime requestTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private PaymentStatus paymentStatus;
    
    public enum RideStatus {
        REQUESTED,    // Rider requested, waiting for driver
        ACCEPTED,     // Driver accepted, heading to pickup
        STARTED,      // Picked up rider, on the way
        COMPLETED,    // Reached destination
        CANCELLED     // Cancelled by rider or driver
    }
    
    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
    
    public Ride(String rideId, Rider rider, Driver driver, Location pickup, 
                Location dropoff, RideType rideType) {
        this.rideId = rideId;
        this.rider = rider;
        this.driver = driver;
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.rideType = rideType;
        
        this.currentLocation = driver.getCurrentLocation();
        this.status = RideStatus.REQUESTED;
        this.requestTime = LocalDateTime.now();
        this.paymentStatus = PaymentStatus.PENDING;
        
        // Calculate initial distance estimate
        this.distance = pickup.distanceTo(dropoff);
    }
    
    /**
     * Start ride when driver picks up rider
     */
    public void startRide() {
        if (status != RideStatus.ACCEPTED) {
            throw new IllegalStateException(
                "Ride must be accepted before starting");
        }
        
        this.status = RideStatus.STARTED;
        this.startTime = LocalDateTime.now();
        this.currentLocation = pickup;
    }
    
    /**
     * Complete ride when reaching destination
     */
    public void completeRide() {
        if (status != RideStatus.STARTED) {
            throw new IllegalStateException(
                "Ride must be started before completing");
        }
        
        this.status = RideStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.currentLocation = dropoff;
        
        // Update trip counts
        rider.incrementTrips();
        driver.incrementTrips();
    }
    
    /**
     * Cancel ride (by either rider or driver)
     */
    public void cancelRide() {
        if (status == RideStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed ride");
        }
        
        this.status = RideStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * Update current location during ride
     */
    public void updateLocation(Location location) {
        if (status != RideStatus.STARTED) {
            throw new IllegalStateException(
                "Can only update location for started rides");
        }
        this.currentLocation = location;
    }
    
    /**
     * Calculate ride duration in minutes
     */
    public long getDurationMinutes() {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return ChronoUnit.MINUTES.between(startTime, end);
    }
    
    /**
     * Set final fare for the ride
     */
    public void setFare(double fare) {
        if (fare < 0) {
            throw new IllegalArgumentException("Fare cannot be negative");
        }
        this.fare = fare;
    }
    
    /**
     * Update payment status
     */
    public void setPaymentStatus(PaymentStatus status) {
        this.paymentStatus = status;
        if (status == PaymentStatus.COMPLETED) {
            driver.addEarnings(fare * 0.8); // Driver gets 80%
        }
    }
    
    // Getters
    public String getRideId() { return rideId; }
    public Rider getRider() { return rider; }
    public Driver getDriver() { return driver; }
    public Location getPickup() { return pickup; }
    public Location getDropoff() { return dropoff; }
    public Location getCurrentLocation() { return currentLocation; }
    public RideStatus getStatus() { return status; }
    public RideType getRideType() { return rideType; }
    public double getFare() { return fare; }
    public double getDistance() { return distance; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    
    @Override
    public String toString() {
        return String.format("Ride %s: %s → %s (%.1f km) - %s [%s]",
            rideId,
            pickup,
            dropoff,
            distance,
            status,
            paymentStatus);
    }
}