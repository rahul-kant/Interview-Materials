/**
 * GOOD EXAMPLE - Follows Dependency Inversion Principle!
 * 
 * NotificationService (high-level) depends on MessageService (abstraction)
 * NOT on concrete implementations!
 * 
 * Benefits:
 * - Can switch between Email/SMS/Push without code changes
 * - Easy to test (mock MessageService)
 * - Flexible and maintainable
 * - Follows DIP!
 */
public class NotificationService {
    
    // Depends on ABSTRACTION (interface), not concrete class!
    private final MessageService messageService;
    
    /**
     * DEPENDENCY INJECTION via constructor!
     * No longer creating dependency internally
     * Receives dependency from outside (Inversion of Control)
     */
    public NotificationService(MessageService messageService) {
        this.messageService = messageService;
    }
    
    /**
     * Works with ANY MessageService implementation!
     * Email, SMS, Push - doesn't matter!
     */
    public void sendNotification(String message) {
        messageService.sendMessage(message);
    }
}
