/**
 * AcceptingMoneyState - Machine accepting payment
 * 
 * Interview Key Points:
 * - Accepts additional coins
 * - Allows product selection
 * - Validates if enough money for product
 * - Can cancel and return money
 * - Transitions to DispensingState when product selected
 */
public class AcceptingMoneyState implements State {
    
    @Override
    public void insertCoin(VendingMachine machine, Coin coin) {
        System.out.println("Additional coin inserted: " + coin.name() + 
            " (" + coin.getValue() + " cents)");
        
        machine.addMoney(coin.getValue());
        
        System.out.println("Current balance: " + 
            String.format("$%.2f", machine.getCurrentBalance() / 100.0));
    }
    
    @Override
    public void selectProduct(VendingMachine machine, String productCode) {
        Product product = machine.getProductByCode(productCode);
        
        if (product == null) {
            System.out.println("❌ Invalid product code: " + productCode);
            return;
        }
        
        // Check if product is available
        if (!machine.isProductAvailable(product)) {
            System.out.println("❌ Product " + product.getName() + " is out of stock!");
            return;
        }
        
        // Check if enough money
        if (machine.getCurrentBalance() < product.getPrice()) {
            int needed = product.getPrice() - machine.getCurrentBalance();
            System.out.println("❌ Insufficient funds! Please insert " + 
                String.format("$%.2f", needed / 100.0) + " more.");
            return;
        }
        
        // Valid selection - proceed to dispense
        System.out.println("✓ Product selected: " + product.getName());
        machine.setSelectedProduct(product);
        machine.setState(new DispensingState());
        
        // Automatically dispense
        machine.getCurrentState().dispenseProduct(machine);
    }
    
    @Override
    public void dispenseProduct(VendingMachine machine) {
        System.out.println("❌ Please select a product first!");
    }
    
    @Override
    public void cancelTransaction(VendingMachine machine) {
        System.out.println("\n🔄 Transaction cancelled!");
        
        int refund = machine.getCurrentBalance();
        if (refund > 0) {
            System.out.println("Refunding: " + 
                String.format("$%.2f", refund / 100.0));
            machine.returnChange(refund);
        }
        
        machine.resetTransaction();
        machine.setState(new IdleState());
    }
    
    @Override
    public String getStateName() {
        return "ACCEPTING_MONEY";
    }
}
