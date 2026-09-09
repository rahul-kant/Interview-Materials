/**
 * AuthenticatedState - PIN verified, can perform transactions
 * 
 * Interview Key Points:
 * - User authenticated
 * - Can select and execute transactions
 * - Transitions to TransactionState or back to Idle
 */
public class AuthenticatedState implements ATMState {
    
    @Override
    public void insertCard(ATM atm, Card card) {
        System.out.println("❌ Card already inserted");
    }
    
    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("Already authenticated");
    }
    
    @Override
    public void selectTransaction(ATM atm, TransactionType type) {
        System.out.println("\n📋 Transaction selected: " + type);
        atm.setSelectedTransaction(type);
        
        if (type == TransactionType.BALANCE_INQUIRY) {
            // Execute immediately for balance inquiry
            executeTransaction(atm, 0);
        } else {
            atm.setState(new TransactionState());
            System.out.println("→ Please enter amount");
        }
    }
    
    @Override
    public void executeTransaction(ATM atm, double amount) {
        if (atm.getSelectedTransaction() == TransactionType.BALANCE_INQUIRY) {
            Account account = atm.getCurrentAccount();
            System.out.println("\n💰 Balance Inquiry:");
            System.out.println("   Account: " + account.getAccountNumber());
            System.out.println("   Balance: $" + String.format("%.2f", account.getBalance()));
            
            System.out.println("\n→ Transaction complete. Select another or eject card");
        } else {
            System.out.println("❌ Please select transaction type first");
        }
    }
    
    @Override
    public void ejectCard(ATM atm) {
        System.out.println("\n💳 Card ejected");
        System.out.println("Thank you for using our ATM!");
        atm.reset();
        atm.setState(new IdleState());
    }
    
    @Override
    public String getStateName() {
        return "AUTHENTICATED";
    }
}
