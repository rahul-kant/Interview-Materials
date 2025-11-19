/**
 * GOOD EXAMPLE - Follows Open/Closed Principle
 * 
 * PaymentMethod interface - This is the KEY to OCP!
 * 
 * By using an interface, we make the system:
 * - OPEN for extension (add new payment types)
 * - CLOSED for modification (no changes to existing code)
 * 
 * Each payment method implements this interface with its own logic.
 */
public interface PaymentMethod {
    
    /**
     * Processes a payment
     * @param amount The amount to charge
     * @param accountInfo Account/card/wallet information
     * @return true if payment successful
     */
    boolean processPayment(double amount, String accountInfo);
    
    /**
     * Processes a refund
     * @param amount The amount to refund
     * @param accountInfo Account/card/wallet information
     * @return true if refund successful
     */
    boolean refund(double amount, String accountInfo);
    
    /**
     * Gets the payment method name
     * @return Name of the payment method
     */
    String getPaymentMethodName();
    
    /**
     * Gets details about this payment method (fees, processing time, etc.)
     * @return Payment method details
     */
    String getDetails();
}
