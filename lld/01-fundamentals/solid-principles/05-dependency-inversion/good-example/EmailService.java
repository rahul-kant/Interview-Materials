/**
 * GOOD EXAMPLE - Low-level module implements abstraction
 * EmailService implements MessageService interface
 */
public class EmailService implements MessageService {
    
    @Override
    public void sendMessage(String message) {
        System.out.println("Connecting to email server...");
        System.out.println("Sending email: " + message);
        System.out.println("Email sent successfully!");
    }
}
