/**
 * Demo showing Dependency Inversion Principle SOLUTION
 * 
 * NotificationService depends on MessageService interface,
 * not concrete implementations. This follows DIP!
 */
public class DIPDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  DIP SOLUTION - Dependency Injection");
        System.out.println("========================================\n");
        
        // Create message services (low-level modules)
        MessageService email = new EmailService();
        MessageService sms = new SMSService();
        
        // Test with Email
        System.out.println("TEST 1: Using EmailService");
        System.out.println("---------------------------");
        NotificationService emailNotifier = new NotificationService(email);
        emailNotifier.sendNotification("Hello via Email!");
        
        // Test with SMS - NO CODE CHANGES to NotificationService!
        System.out.println("\n\nTEST 2: Using SMSService");
        System.out.println("------------------------");
        NotificationService smsNotifier = new NotificationService(sms);
        smsNotifier.sendNotification("Hello via SMS!");
        
        // Benefits
        System.out.println("\n\n========================================");
        System.out.println("           BENEFITS OF DIP");
        System.out.println("========================================\n");
        
        System.out.println("✅ DEPENDS ON ABSTRACTION:");
        System.out.println("   • NotificationService → MessageService (interface)");
        System.out.println("   • EmailService → MessageService (interface)");
        System.out.println("   • SMSService → MessageService (interface)");
        System.out.println("   • All depend on abstraction, not concrete!");
        
        System.out.println("\n✅ DEPENDENCY INJECTION:");
        System.out.println("   • NotificationService receives dependency via constructor");
        System.out.println("   • Doesn't create EmailService/SMSService itself");
        System.out.println("   • Inversion of Control - caller provides dependency");
        
        System.out.println("\n✅ EASY TO SWITCH:");
        System.out.println("   • Want Email? Pass EmailService");
        System.out.println("   • Want SMS? Pass SMSService");
        System.out.println("   • Want Push? Create PushService, pass it in");
        System.out.println("   • NO changes to NotificationService!");
        
        System.out.println("\n✅ EASY TO TEST:");
        System.out.println("   • Create MockMessageService for testing");
        System.out.println("   • Inject mock instead of real service");
        System.out.println("   • No need for real email/SMS in tests");
        System.out.println("   • Fast, reliable unit tests");
        
        System.out.println("\n✅ LOW COUPLING:");
        System.out.println("   • NotificationService doesn't depend on EmailService");
        System.out.println("   • Changes in EmailService don't affect NotificationService");
        System.out.println("   • High-level and low-level are independent");
        
        System.out.println("\n✅ FOLLOWS SOLID:");
        System.out.println("   • SRP: Each class has one responsibility");
        System.out.println("   • OCP: Open for extension (new MessageService types)");
        System.out.println("   • LSP: All MessageService implementations substitutable");
        System.out.println("   • ISP: Focused interface (just sendMessage)");
        System.out.println("   • DIP: Depend on abstractions!");
        
        System.out.println("\n\n========================================");
        System.out.println("  DIP COMPARISON");
        System.out.println("========================================");
        
        System.out.println("\nBad (Violates DIP):");
        System.out.println("  • NotificationService → EmailService (concrete)");
        System.out.println("  • Creates dependency internally");
        System.out.println("  • Tightly coupled");
        System.out.println("  • Hard to test and change");
        
        System.out.println("\nGood (Follows DIP):");
        System.out.println("  • NotificationService → MessageService (interface)");
        System.out.println("  • Receives dependency via constructor");
        System.out.println("  • Loosely coupled");
        System.out.println("  • Easy to test and change");
        
        System.out.println("\n\n========================================");
        System.out.println("  KEY LESSON");
        System.out.println("========================================");
        System.out.println("\n\"Depend on abstractions,");
        System.out.println(" not on concretions.\"");
        System.out.println("\nHigh-level modules should NOT depend on");
        System.out.println("low-level modules. Both should depend on");
        System.out.println("abstractions!");
        System.out.println("\n========================================\n");
    }
}
