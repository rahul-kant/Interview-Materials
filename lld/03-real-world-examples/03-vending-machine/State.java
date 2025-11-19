/**
 * State Interface - State Pattern Implementation
 * 
 * Interview Key Points:
 * - State pattern allows object to change behavior based on state
 * - Each state implements these operations differently
 * - Makes state transitions clear and maintainable
 * - Follows Open-Closed Principle (easy to add new states)
 */
public interface State {
    
    /**
     * Insert coin into machine
     */
    void insertCoin(VendingMachine machine, Coin coin);
    
    /**
     * Select product by code
     */
    void selectProduct(VendingMachine machine, String productCode);
    
    /**
     * Dispense the selected product
     */
    void dispenseProduct(VendingMachine machine);
    
    /**
     * Cancel transaction and return money
     */
    void cancelTransaction(VendingMachine machine);
    
    /**
     * Get current state name
     */
    String getStateName();
}
