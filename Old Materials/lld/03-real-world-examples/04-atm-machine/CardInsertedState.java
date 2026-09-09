/**
 * CardInsertedState - Card inserted, waiting for PIN
 * 
 * Interview Key Points:
 * - Validates PIN
 * - Tracks failed attempts
 * - Transitions to AuthenticatedState or back to Idle
 */
public class CardInsertedState implements ATMState {
    
    @Override
    public void insertCard(ATM atm, Card card) {
        System.out.println("❌ Card already inserted");
    }
    
    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("\n🔐 Verifying PIN...");
        
        Account account = atm.getAccountForCard(atm.getCurrentCard());
        
        if (account == null) {
            System.out.println("❌ Account not found");
            ejectCard(atm);
            return;
        }
        
        if (account.verifyPin(pin)) {
            System.out.println("✓ PIN verified successfully");
            atm.setCurrentAccount(account);
            atm.setState(new AuthenticatedState());
            System.out.println("→ Please select transaction type");
        } else {
            if (account.isLocked()) {
                ejectCard(atm);
            }
        }
    }
    
    @Override
    public void selectTransaction(ATM atm, TransactionType type) {
        System.out.println("❌ Please enter PIN first");
    }
    
    @Override
    public void executeTransaction(ATM atm, double amount) {
        System.out.println("❌ Please enter PIN first");
    }
    
    @Override
    public void ejectCard(ATM atm) {
        System.out.println("\n💳 Card ejected");
        atm.reset();
        atm.setState(new IdleState());
    }
    
    @Override
    public String getStateName() {
        return "CARD_INSERTED";
    }
}
