/**
 * ATM Demo - Demonstrates ATM System functionality
 * 
 * Interview Key Points:
 * - Complete workflow demonstration
 * - State transitions
 * - Security features
 * - Error handling
 * - Multiple scenarios
 */
public class ATMDemo {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║        ATM MACHINE SYSTEM DEMO         ║");
        System.out.println("╚════════════════════════════════════════╝");
        
        ATM atm = ATM.getInstance();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 1: Successful Withdrawal");
        System.out.println("=".repeat(50));
        testSuccessfulWithdrawal(atm);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 2: Balance Inquiry");
        System.out.println("=".repeat(50));
        testBalanceInquiry(atm);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 3: Incorrect PIN");
        System.out.println("=".repeat(50));
        testIncorrectPin(atm);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 4: Insufficient Funds");
        System.out.println("=".repeat(50));
        testInsufficientFunds(atm);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCENARIO 5: Deposit");
        System.out.println("=".repeat(50));
        testDeposit(atm);
        
        // Show final ATM status
        System.out.println("\n" + "=".repeat(50));
        System.out.println("FINAL ATM STATUS");
        System.out.println("=".repeat(50));
        System.out.println(atm.getStatus());
        atm.displayCashInventory();
    }
    
    /**
     * Scenario 1: Successful withdrawal
     */
    private static void testSuccessfulWithdrawal(ATM atm) {
        Card card = new Card("1111222233334444", "John Doe", 
            "12/26", "123", Card.CardType.DEBIT);
        
        atm.insertCard(card);
        atm.enterPin("1234");
        atm.selectTransaction(TransactionType.WITHDRAW);
        atm.executeTransaction(200.00);
        atm.ejectCard();
    }
    
    /**
     * Scenario 2: Balance inquiry
     */
    private static void testBalanceInquiry(ATM atm) {
        Card card = new Card("5555666677778888", "Jane Smith", 
            "03/27", "456", Card.CardType.CREDIT);
        
        atm.insertCard(card);
        atm.enterPin("5678");
        atm.selectTransaction(TransactionType.BALANCE_INQUIRY);
        atm.ejectCard();
    }
    
    /**
     * Scenario 3: Incorrect PIN (demonstrates security)
     */
    private static void testIncorrectPin(ATM atm) {
        Card card = new Card("1111222233334444", "John Doe", 
            "12/26", "123", Card.CardType.DEBIT);
        
        atm.insertCard(card);
        atm.enterPin("0000"); // Wrong PIN
        atm.enterPin("1111"); // Wrong PIN
        atm.enterPin("1234"); // Correct PIN
        atm.ejectCard();
    }
    
    /**
     * Scenario 4: Insufficient funds
     */
    private static void testInsufficientFunds(ATM atm) {
        Card card = new Card("9999888877776666", "Bob Johnson", 
            "06/25", "789", Card.CardType.DEBIT);
        
        atm.insertCard(card);
        atm.enterPin("9999");
        atm.selectTransaction(TransactionType.WITHDRAW);
        atm.executeTransaction(1000.00); // Account only has $500
        atm.ejectCard();
    }
    
    /**
     * Scenario 5: Deposit
     */
    private static void testDeposit(ATM atm) {
        Card card = new Card("9999888877776666", "Bob Johnson", 
            "06/25", "789", Card.CardType.DEBIT);
        
        atm.insertCard(card);
        atm.enterPin("9999");
        atm.selectTransaction(TransactionType.DEPOSIT);
        atm.executeTransaction(300.00);
        
        // Check balance after deposit
        atm.selectTransaction(TransactionType.BALANCE_INQUIRY);
        atm.ejectCard();
    }
}
