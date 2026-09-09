package ride_sharing;

// package declaration omitted
// No import needed for same-package class
/**
 * Rider - Represents a user who can request rides
 * 
 * Interview Key Points:
 * - Rider profile
 * - Payment information
 * - Trip history
 */
public class Rider extends User {
    private String paymentMethod;
    private boolean priorityCustomer;  // For surge pricing exceptions
    private int tripsCompleted;
    
    public Rider(String userId, String name, String email, String phone) {
        super(userId, name, email, phone);
        this.paymentMethod = null;
        this.priorityCustomer = false;
        this.tripsCompleted = 0;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public boolean hasPaymentMethod() {
        return paymentMethod != null;
    }
    
    public boolean isPriorityCustomer() {
        return priorityCustomer;
    }
    
    public void setPriorityCustomer(boolean priorityCustomer) {
        this.priorityCustomer = priorityCustomer;
    }
    
    public int getTripsCompleted() {
        return tripsCompleted;
    }
    
    public void incrementTrips() {
        this.tripsCompleted++;
        // Check for priority status
        if (tripsCompleted >= 50 && getRating() >= 4.5) {
            priorityCustomer = true;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Rider: %s (Trips: %d)%s", 
            super.toString(), 
            tripsCompleted,
            priorityCustomer ? " [PRIORITY]" : "");
    }
    
    public double getRating() {
        return super.getRating();
    }
}