import java.util.Objects;

/**
 * Member - Represents a library member
 * 
 * Interview Key Points:
 * - Unique member ID
 * - Member status tracking
 * - Fine calculation
 */
public class Member {
    private final String memberId;
    private final String name;
    private final String email;
    private final String phone;
    private boolean active;
    private double fineAmount;
    
    public Member(String memberId, String name, String email, String phone) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.active = true;
        this.fineAmount = 0.0;
    }
    
    // Getters
    public String getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
    public double getFineAmount() { return fineAmount; }
    
    // Member status management
    public void deactivate() {
        this.active = false;
    }
    
    public void activate() {
        this.active = true;
    }
    
    // Fine management
    public void addFine(double amount) {
        this.fineAmount += amount;
    }
    
    public void payFine(double amount) {
        if (amount > this.fineAmount) {
            throw new IllegalArgumentException("Payment amount exceeds fine balance");
        }
        this.fineAmount -= amount;
    }
    
    @Override
    public String toString() {
        return String.format("Member: %s (%s) - Status: %s", 
            name, memberId, active ? "Active" : "Inactive");
    }
}