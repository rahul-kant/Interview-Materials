/**
 * TransactionType Enum - Types of ATM transactions
 * 
 * Interview Key Points:
 * - Enum for type safety
 * - Easy to extend with new transaction types
 */
public enum TransactionType {
    WITHDRAW("Withdraw Cash"),
    DEPOSIT("Deposit Cash"),
    BALANCE_INQUIRY("Check Balance"),
    TRANSFER("Transfer Funds");
    
    private final String description;
    
    TransactionType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
