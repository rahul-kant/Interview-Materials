/**
 * Card Class - Represents ATM/Debit Card
 * 
 * Interview Key Points:
 * - Immutable design for security
 * - Card validation
 * - Masked number display
 * - Security considerations
 */
public class Card {
    private final String cardNumber;
    private final String cardHolderName;
    private final String expiryDate;
    private final String cvv;
    private final CardType cardType;
    
    public enum CardType {
        DEBIT,
        CREDIT
    }
    
    public Card(String cardNumber, String cardHolderName, String expiryDate, 
                String cvv, CardType cardType) {
        validateCard(cardNumber, expiryDate, cvv);
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.cardType = cardType;
    }
    
    private void validateCard(String cardNumber, String expiryDate, String cvv) {
        if (cardNumber == null || cardNumber.length() != 16) {
            throw new IllegalArgumentException("Invalid card number");
        }
        if (cvv == null || cvv.length() != 3) {
            throw new IllegalArgumentException("Invalid CVV");
        }
        // Additional validation could include Luhn algorithm
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public String getCardHolderName() {
        return cardHolderName;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public CardType getCardType() {
        return cardType;
    }
    
    /**
     * Get masked card number for display
     * Shows only last 4 digits
     */
    public String getMaskedCardNumber() {
        return "**** **** **** " + cardNumber.substring(12);
    }
    
    /**
     * Verify CVV (in real system, this would be encrypted)
     */
    public boolean verifyCVV(String inputCvv) {
        return this.cvv.equals(inputCvv);
    }
    
    @Override
    public String toString() {
        return String.format("%s Card - %s (%s)", 
            cardType, getMaskedCardNumber(), cardHolderName);
    }
}
