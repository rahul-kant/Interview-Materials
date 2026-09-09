/**
 * Customer class represents a hotel guest
 * 
 * EXPLANATION:
 * - Stores customer information
 * - Used for booking identification and contact
 * - Can be extended with loyalty programs, preferences, etc.
 * 
 * SOLID PRINCIPLES:
 * - SRP: Customer only manages guest information
 * - OCP: Can be extended with VIP, Corporate, etc. types
 */
public class Customer {
    private String customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    
    /**
     * Constructor
     * 
     * @param customerId Unique identifier
     * @param name Customer name
     * @param email Email address
     * @param phone Phone number
     */
    public Customer(String customerId, String name, String email, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
    
    // Getters and setters
    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    
    public void setAddress(String address) { this.address = address; }
    
    @Override
    public String toString() {
        return "Customer: " + name + " (" + email + ")";
    }
}
