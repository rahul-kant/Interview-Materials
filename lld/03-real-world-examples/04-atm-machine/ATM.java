import java.util.*;

/**
 * ATM - Main ATM controller (Singleton)
 * 
 * Interview Key Points:
 * - Singleton pattern for single ATM instance
 * - State pattern for behavior management
 * - Thread-safe operations
 * - Cash dispensing with denomination optimization
 * - Account management integration
 */
public class ATM {
    private static ATM instance;
    
    private ATMState currentState;
    private Card currentCard;
    private Account currentAccount;
    private TransactionType selectedTransaction;
    private CashDispenser cashDispenser;
    private Map<String, Account> accounts; // cardNumber -> Account
    
    private ATM() {
        this.currentState = new IdleState();
        this.cashDispenser = new CashDispenser();
        this.accounts = new HashMap<>();
        initializeTestAccounts();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ATM getInstance() {
        if (instance == null) {
            instance = new ATM();
        }
        return instance;
    }
    
    /**
     * Initialize with some test accounts
     */
    private void initializeTestAccounts() {
        Account acc1 = new Account("ACC001", "John Doe", 1000.00, "1234");
        Account acc2 = new Account("ACC002", "Jane Smith", 2500.00, "5678");
        Account acc3 = new Account("ACC003", "Bob Johnson", 500.00, "9999");
        
        accounts.put("1111222233334444", acc1);
        accounts.put("5555666677778888", acc2);
        accounts.put("9999888877776666", acc3);
    }
    
    // ========== Public ATM Operations ==========
    
    /**
     * Insert card
     */
    public void insertCard(Card card) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("State: " + currentState.getStateName());
        currentState.insertCard(this, card);
    }
    
    /**
     * Enter PIN
     */
    public void enterPin(String pin) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("State: " + currentState.getStateName());
        currentState.enterPin(this, pin);
    }
    
    /**
     * Select transaction
     */
    public void selectTransaction(TransactionType type) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("State: " + currentState.getStateName());
        currentState.selectTransaction(this, type);
    }
    
    /**
     * Execute transaction with amount
     */
    public void executeTransaction(double amount) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("State: " + currentState.getStateName());
        currentState.executeTransaction(this, amount);
    }
    
    /**
     * Eject card
     */
    public void ejectCard() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("State: " + currentState.getStateName());
        currentState.ejectCard(this);
    }
    
    // ========== State Management ==========
    
    protected void setState(ATMState state) {
        this.currentState = state;
    }
    
    protected ATMState getCurrentState() {
        return currentState;
    }
    
    // ========== Session Data ==========
    
    protected void setCurrentCard(Card card) {
        this.currentCard = card;
    }
    
    protected Card getCurrentCard() {
        return currentCard;
    }
    
    protected void setCurrentAccount(Account account) {
        this.currentAccount = account;
    }
    
    protected Account getCurrentAccount() {
        return currentAccount;
    }
    
    protected void setSelectedTransaction(TransactionType type) {
        this.selectedTransaction = type;
    }
    
    protected TransactionType getSelectedTransaction() {
        return selectedTransaction;
    }
    
    protected void reset() {
        this.currentCard = null;
        this.currentAccount = null;
        this.selectedTransaction = null;
    }
    
    // ========== Account Management ==========
    
    protected Account getAccountForCard(Card card) {
        return accounts.get(card.getCardNumber());
    }
    
    public void addAccount(String cardNumber, Account account) {
        accounts.put(cardNumber, account);
    }
    
    // ========== Cash Management ==========
    
    protected boolean hasSufficientCash(double amount) {
        return cashDispenser.hasSufficientCash(amount);
    }
    
    protected void dispenseCash(double amount) {
        Map<Integer, Integer> dispensed = cashDispenser.dispenseCash(amount);
        
        if (dispensed != null) {
            System.out.println("\n💵 Cash dispensed:");
            for (Map.Entry<Integer, Integer> entry : dispensed.entrySet()) {
                System.out.println("   $" + entry.getKey() + " x " + 
                    entry.getValue() + " = $" + (entry.getKey() * entry.getValue()));
            }
        }
    }
    
    // ========== Admin Functions ==========
    
    public void displayCashInventory() {
        cashDispenser.displayInventory();
    }
    
    public void refillCash(int denomination, int count) {
        cashDispenser.refill(denomination, count);
    }
    
    public String getStatus() {
        return String.format("ATM Status: %s | Cash Available: $%.2f",
            currentState.getStateName(), cashDispenser.getTotalCash());
    }
}
