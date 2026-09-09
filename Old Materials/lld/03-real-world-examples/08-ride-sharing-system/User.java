package ride_sharing;

/**
 * User - Base class for Rider and Driver
 * 
 * Interview Key Points:
 * - Common user attributes
 * - Rating management
 * - User status tracking
 */
public abstract class User {
    private final String userId;
    private final String name;
    private final String email;
    private final String phone;
    private double rating;
    private int ratingCount;
    private boolean active;
    
    protected User(String userId, String name, String email, String phone) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.rating = 5.0;  // Initial rating
        this.ratingCount = 0;
        this.active = true;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public double getRating() {
        return rating;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Update rating with new review
     */
    public synchronized void addRating(double newRating) {
        if (newRating < 1 || newRating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        // Update cumulative rating
        double totalRating = rating * ratingCount + newRating;
        ratingCount++;
        this.rating = totalRating / ratingCount;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - Rating: %.1f", name, userId, rating);
    }
}