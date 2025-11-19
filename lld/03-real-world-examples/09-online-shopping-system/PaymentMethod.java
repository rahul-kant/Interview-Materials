/**
 * Enum representing different payment methods
 * 
 * EXPLANATION:
 * Supports multiple payment options for checkout
 * Uses Strategy pattern for different payment processing
 */
public enum PaymentMethod {
    CREDIT_CARD,    // Credit card payment
    DEBIT_CARD,     // Debit card payment
    UPI,            // UPI payment (PhonePe, GPay, etc.)
    NET_BANKING,    // Online banking
    CASH_ON_DELIVERY, // COD
    WALLET          // Digital wallet (Paytm, etc.)
}
