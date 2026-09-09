/**
 * TransactionState - Executing transaction
 * 
 * Interview Key Points:
 * - Processes withdrawals and deposits
 * - Validates amount and availability
 * - Returns to AuthenticatedState after completion
 */
public class TransactionState implements ATMState {
    
    @Override
    public void insertCard(ATM atm, Card card) {
        System.out.println("❌ Transaction in progress");
    }
    
    @Override
    public void enterPin(ATM atm, String pin) {
        System.out.println("❌ Transaction in progress");
    }
    
    @Override
    public void selectTransaction(ATM atm, TransactionType type) {
        System.out.println("❌ Transaction already selected");
    }
    
    @Override
    public void executeTransaction(ATM atm, double amount) {
        System.out.println("\n💵 Processing transaction...");
        
        TransactionType type = atm.getSelectedTransaction();
        Account account = atm.getCurrentAccount();
        boolean success = false;
        
        switch (type) {
            case WITHDRAW:
                if (atm.hasSufficientCash(amount)) {
                    success = account.withdraw(amount);
                    if (success) {
                        atm.dispenseCash(amount);
                    }
                } else {
                    System.out.println("❌ ATM has insufficient cash");
                }
                break;
                
            case DEPOSIT:
                success = account.deposit(amount);
                break;
                
            case TRANSFER:
                System.out.println("Transfer not implemented in this demo");
                break;
                
            default:
                System.out.println("❌ Invalid transaction type");
        }
        
        if (success) {
            System.out.println("✅ Transaction completed successfully!");
        }
        
        // Return to authenticated state for more transactions
        atm.setSelectedTransaction(null);
        atm.setState(new AuthenticatedState());
        System.out.println("\n→ Select another transaction or eject card");
    }
    
    @Override
    public void ejectCard(ATM atm) {
        System.out.println("❌ Please complete transaction first");
    }
    
    @Override
    public String getStateName() {
        return "TRANSACTION";
    }
}
