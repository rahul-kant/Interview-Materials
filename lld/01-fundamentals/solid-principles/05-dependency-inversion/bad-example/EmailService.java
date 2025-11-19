/**
 * BAD EXAMPLE - Low-level module (concrete implementation)
 * This is a concrete class that NotificationService will depend on directly
 */
public class EmailService {
    
    public void sendEmail(String message) {
        System.out.println("Connecting to email server...");
        System.out.println("Sending email: " + message);
        System.out.println("Email sent successfully!");
    }
}
