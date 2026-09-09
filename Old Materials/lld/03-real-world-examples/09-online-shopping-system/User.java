/**
 * User class represents a customer in the shopping system
 * 
 * EXPLANATION:
 * - Manages user information
 * - Can have address, cart, and order history
 * - Can be extended for sellers, admins
 * 
 * SOLID PRINCIPLES:
 * - SRP: User only manages user data
 * - OCP: Can extend for different user types
 */
public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String address;
    
    public User(String userId, String name, String email, String phone) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
    
    // Getters and setters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
}
