/**
 * BAD EXAMPLE - Violates Dependency Inversion Principle
 * 
 * High-level module (NotificationService) depends directly on
 * low-level module (EmailService) - concrete implementation!
 * 
 * Problems:
 * - Tightly coupled to EmailService
 * - Can't switch to SMS without code changes
 * - Hard to test (need real EmailService)
 * - Violates DIP!
 */
public class NotificationService {
    
    // PROBLEM: Direct dependency on concrete class!
    private EmailService emailService;
    
    /**
     * PROBLEM: Creating dependency inside the class!
     * This is tight coupling - can't inject different implementation
     */
    public NotificationService() {
        this.emailService = new EmailService(); // Creating concrete dependency!
    }
    
    /**
     * PROBLEM: Tightly coupled to email
     * What if we want SMS or Push notifications?
     * Must modify this class!
     */
    public void sendNotification(String message) {
        emailService.sendEmail(message); // Locked into email!
    }
}
