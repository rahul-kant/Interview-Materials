/**
 * BAD EXAMPLE - Violates Open/Closed Principle
 * 
 * This PaymentProcessor class violates OCP because:
 * - Every new payment method requires modifying this class
 * - Uses if-else statements based on payment type
 * - Not "closed" for modification
 * - Risk of breaking existing functionality with each change
 * 
 * Problems:
 * - Add Bitcoin payment? Must modify this class
 * - Add Google Pay? Must modify this class
 * - Each modification risks breaking existing payment methods
 * - Hard to test individual payment methods
 * - Code becomes messy with more payment types
 */
public class PaymentProcessor {
    
    /**
     * Processes a payment based on the payment type
     * 
     * PROBLEM: Must modify this method for every new payment type!
     */
    public boolean processPayment(String paymentType, double amount, String accountInfo) {
        System.out.println("\n[PAYMENT PROCESSOR] Processing " + paymentType + " payment...");
        System.out.println("Amount: $" + amount);
        
        // BAD: Using if-else chain based on type
        // Every new payment type requires adding another if-else block here
        
        if (paymentType.equals("CREDIT_CARD")) {
            // Credit card specific logic
            System.out.println("Payment Type: Credit Card");
            System.out.println("Card Number: " + accountInfo);
            System.out.println("Validating card number...");
            System.out.println("Checking credit limit...");
            System.out.println("Processing with payment gateway...");
            System.out.println("Sending confirmation email...");
            System.out.println("✓ Credit Card payment successful!");
            return true;
            
        } else if (paymentType.equals("PAYPAL")) {
            // PayPal specific logic
            System.out.println("Payment Type: PayPal");
            System.out.println("PayPal Email: " + accountInfo);
            System.out.println("Redirecting to PayPal...");
            System.out.println("Authenticating with PayPal...");
            System.out.println("Processing payment...");
            System.out.println("Returning from PayPal...");
            System.out.println("✓ PayPal payment successful!");
            return true;
            
        } else if (paymentType.equals("BANK_TRANSFER")) {
            // Bank transfer specific logic
            System.out.println("Payment Type: Bank Transfer");
            System.out.println("Account Number: " + accountInfo);
            System.out.println("Validating account...");
            System.out.println("Initiating transfer...");
            System.out.println("Waiting for confirmation...");
            System.out.println("✓ Bank Transfer successful!");
            return true;
            
        } else if (paymentType.equals("BITCOIN")) {
            // Bitcoin specific logic (NEW - Had to modify this class!)
            System.out.println("Payment Type: Bitcoin");
            System.out.println("Wallet Address: " + accountInfo);
            System.out.println("Generating payment request...");
            System.out.println("Waiting for blockchain confirmation...");
            System.out.println("Payment confirmed on blockchain...");
            System.out.println("✓ Bitcoin payment successful!");
            return true;
            
        } else {
            System.out.println("✗ Error: Unknown payment type: " + paymentType);
            return false;
        }
        
        // To add Google Pay, Apple Pay, etc., we must keep modifying this method!
        // This violates OCP - not "closed" for modification
    }
    
    /**
     * Refunds a payment
     * SAME PROBLEM: Must modify for every new payment type
     */
    public boolean refundPayment(String paymentType, double amount, String accountInfo) {
        System.out.println("\n[REFUND] Processing refund for " + paymentType + "...");
        
        // Another if-else chain that must be modified for each new type
        if (paymentType.equals("CREDIT_CARD")) {
            System.out.println("Refunding to credit card: " + accountInfo);
            System.out.println("✓ Credit card refund successful!");
            return true;
        } else if (paymentType.equals("PAYPAL")) {
            System.out.println("Refunding to PayPal: " + accountInfo);
            System.out.println("✓ PayPal refund successful!");
            return true;
        } else if (paymentType.equals("BANK_TRANSFER")) {
            System.out.println("Refunding to bank account: " + accountInfo);
            System.out.println("✓ Bank transfer refund successful!");
            return true;
        } else if (paymentType.equals("BITCOIN")) {
            System.out.println("Refunding to Bitcoin wallet: " + accountInfo);
            System.out.println("✓ Bitcoin refund successful!");
            return true;
        } else {
            System.out.println("✗ Error: Cannot refund unknown payment type");
            return false;
        }
    }
    
    /**
     * Gets payment method details
     * YET ANOTHER method that must be modified for each new type
     */
    public String getPaymentMethodDetails(String paymentType) {
        if (paymentType.equals("CREDIT_CARD")) {
            return "Credit Card - 2-3% transaction fee, instant processing";
        } else if (paymentType.equals("PAYPAL")) {
            return "PayPal - 2.9% + $0.30 per transaction";
        } else if (paymentType.equals("BANK_TRANSFER")) {
            return "Bank Transfer - No fee, 1-3 days processing";
        } else if (paymentType.equals("BITCOIN")) {
            return "Bitcoin - Variable network fee, 10-60 mins confirmation";
        } else {
            return "Unknown payment method";
        }
    }
}
