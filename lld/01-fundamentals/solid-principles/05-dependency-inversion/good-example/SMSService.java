/**
 * GOOD EXAMPLE - Another low-level module
 * SMSService also implements MessageService interface
 * Can be swapped with EmailService without changing NotificationService!
 */
public class SMSService implements MessageService {
    
    @Override
    public void sendMessage(String message) {
        System.out.println("Connecting to SMS gateway...");
        System.out.println("Sending SMS: " + message);
        System.out.println("SMS sent successfully!");
    }
}
