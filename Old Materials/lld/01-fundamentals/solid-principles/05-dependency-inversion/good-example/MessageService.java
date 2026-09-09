/**
 * GOOD EXAMPLE - Follows Dependency Inversion Principle
 * 
 * MessageService interface - THE ABSTRACTION!
 * Both high-level and low-level modules depend on this.
 * 
 * This is the key to DIP - depend on abstraction, not concrete classes.
 */
public interface MessageService {
    
    /**
     * Send a message
     * @param message The message to send
     */
    void sendMessage(String message);
}
