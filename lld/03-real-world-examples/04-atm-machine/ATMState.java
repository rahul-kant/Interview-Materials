/**
 * ATMState Interface - State Pattern for ATM
 * 
 * Interview Key Points:
 * - State pattern for clean state management
 * - Each state handles operations differently
 * - Easy to add new states
 */
public interface ATMState {
    
    /**
     * Insert card into ATM
     */
    void insertCard(ATM atm, Card card);
    
    /**
     * Enter PIN
     */
    void enterPin(ATM atm, String pin);
    
    /**
     * Select transaction type
     */
    void selectTransaction(ATM atm, TransactionType type);
    
    /**
     * Execute transaction
     */
    void executeTransaction(ATM atm, double amount);
    
    /**
     * Eject card
     */
    void ejectCard(ATM atm);
    
    /**
     * Get state name
     */
    String getStateName();
}
