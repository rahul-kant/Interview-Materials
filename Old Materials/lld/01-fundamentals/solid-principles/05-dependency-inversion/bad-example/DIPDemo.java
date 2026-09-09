/**
 * Demo showing Dependency Inversion Principle Violation
 * 
 * NotificationService depends directly on EmailService (concrete class)
 * This violates DIP and creates tight coupling
 */
public class DIPDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  DIP VIOLATION - Tight Coupling");
        System.out.println("========================================\n");
        
        // Create notification service
        // Problem: NotificationService creates EmailService internally
        NotificationService notifier = new NotificationService();
        
        System.out.println("Sending notification...\n");
        notifier.sendNotification("Hello from NotificationService!");
        
        // Analysis
        System.out.println("\n\n========================================");
        System.out.println("           PROBLEM ANALYSIS");
        System.out.println("========================================\n");
        
        System.out.println("THE DIP VIOLATION:");
        System.out.println("------------------");
        System.out.println("1. NotificationService (high-level) depends on");
        System.out.println("   EmailService (low-level concrete class)");
        System.out.println("2. NotificationService creates EmailService itself");
        System.out.println("3. Tightly coupled - can't change implementation");
        System.out.println("4. Violates DIP!");
        
        System.out.println("\n\nWHY IT'S BAD:");
        System.out.println("-------------");
        System.out.println("❌ Can't switch to SMS without modifying NotificationService");
        System.out.println("❌ Can't test without real EmailService");
        System.out.println("❌ Changes in EmailService affect NotificationService");
        System.out.println("❌ Not flexible or maintainable");
        
        System.out.println("\n\nWANT TO ADD SMS?");
        System.out.println("----------------");
        System.out.println("Must modify NotificationService class:");
        System.out.println("• Change EmailService to SMSService");
        System.out.println("• Or add if-else for type selection");
        System.out.println("• Breaks Open/Closed Principle too!");
        
        System.out.println("\n\nTHE LESSON:");
        System.out.println("-----------");
        System.out.println("High-level modules should NOT depend on");
        System.out.println("low-level concrete implementations!");
        System.out.println("");
        System.out.println("Solution: Both should depend on abstraction:");
        System.out.println("• Create MessageService interface");
        System.out.println("• NotificationService depends on interface");
        System.out.println("• EmailService implements interface");
        System.out.println("• Inject dependency via constructor");
        
        System.out.println("\n\n========================================");
        System.out.println("  Check good-example/ for the solution!");
        System.out.println("========================================\n");
    }
}
