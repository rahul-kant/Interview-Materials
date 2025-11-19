/**
 * IdleState - ATM waiting for card
 * 
 * Interview Key Points:
 * - Initial state
 * - Only accepts card insertion
 * - Transitions to CardInsertedState
 */
public class IdleState implements ATMState {
    
    @Override
    public void insertCard(ATM atm, Card card) {
        System.out.println("\n💳 Card inserted: " + card);
        atm.setCurrentCard(card);
        atm.setState(new CardInsertedState());
        System.out.println("→ Please enter your PIN");
    }
    
    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("❌ Please insert card first");
    }
    
    @Override
    public void selectTransaction(ATM atm, TransactionType type) {
        System.out.println("❌ Please insert card first");
    }
    
    @Override
    public void executeTransaction(ATM atm, double amount) {
        System.out.println("❌ Please insert card first");
    }
    
    @Override
    public void ejectCard(ATM atm) {
        System.out.println("No card inserted");
    }
    
    @Override
    public String getStateName() {
        return "IDLE";
    }
}
