/**
 * IdleState - Machine waiting for customer action
 * 
 * Interview Key Points:
 * - Initial state of vending machine
 * - Only accepts coin insertion
 * - Other operations are invalid in this state
 * - Transitions to AcceptingMoneyState when coin inserted
 */
public class IdleState implements State {
    
    @Override
    public void insertCoin(VendingMachine machine, Coin coin) {
        System.out.println("Coin inserted: " + coin.name() + 
            " (" + coin.getValue() + " cents)");
        
        machine.addMoney(coin.getValue());
        machine.setState(new AcceptingMoneyState());
        
        System.out.println("Current balance: " + 
            String.format("$%.2f", machine.getCurrentBalance() / 100.0));
    }
    
    @Override
    public void selectProduct(VendingMachine machine, String productCode) {
        System.out.println("❌ Please insert money first!");
    }
    
    @Override
    public void dispenseProduct(VendingMachine machine) {
        System.out.println("❌ No product selected!");
    }
    
    @Override
    public void cancelTransaction(VendingMachine machine) {
        System.out.println("No transaction to cancel.");
    }
    
    @Override
    public String getStateName() {
        return "IDLE";
    }
}
