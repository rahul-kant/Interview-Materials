/**
 * DispensingState - Machine dispensing product
 * 
 * Interview Key Points:
 * - Handles product dispensing
 * - Calculates and returns change
 * - Updates inventory
 * - Transitions back to IdleState
 * - No operations allowed during dispensing
 */
public class DispensingState implements State {
    
    @Override
    public void insertCoin(VendingMachine machine, Coin coin) {
        System.out.println("❌ Please wait, dispensing in progress...");
    }
    
    @Override
    public void selectProduct(VendingMachine machine, String productCode) {
        System.out.println("❌ Please wait, dispensing in progress...");
    }
    
    @Override
    public void dispenseProduct(VendingMachine machine) {
        Product product = machine.getSelectedProduct();
        
        if (product == null) {
            System.out.println("❌ Error: No product selected!");
            machine.setState(new IdleState());
            return;
        }
        
        System.out.println("\n🎁 Dispensing: " + product.getName());
        System.out.println("════════════════════════════");
        
        // Deduct from inventory
        boolean success = machine.deductFromInventory(product);
        
        if (!success) {
            System.out.println("❌ Failed to dispense product!");
            System.out.println("Refunding your money...");
            int refund = machine.getCurrentBalance();
            machine.returnChange(refund);
            machine.resetTransaction();
            machine.setState(new IdleState());
            return;
        }
        
        // Calculate change
        int change = machine.getCurrentBalance() - product.getPrice();
        
        if (change > 0) {
            System.out.println("💰 Returning change: " + 
                String.format("$%.2f", change / 100.0));
            machine.returnChange(change);
        } else {
            System.out.println("💰 Exact change - No change to return");
        }
        
        System.out.println("✅ Enjoy your " + product.getName() + "!");
        System.out.println("════════════════════════════\n");
        
        // Reset and go back to idle
        machine.resetTransaction();
        machine.setState(new IdleState());
    }
    
    @Override
    public void cancelTransaction(VendingMachine machine) {
        System.out.println("❌ Cannot cancel while dispensing!");
    }
    
    @Override
    public String getStateName() {
        return "DISPENSING";
    }
}
