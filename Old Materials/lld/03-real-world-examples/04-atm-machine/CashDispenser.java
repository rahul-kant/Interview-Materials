import java.util.*;

/**
 * CashDispenser - Manages cash inventory and dispensing
 * 
 * Interview Key Points:
 * - Cash denomination optimization
 * - Inventory management
 * - Thread-safe operations
 * - Greedy algorithm for dispensing
 */
public class CashDispenser {
    private Map<Integer, Integer> cashInventory; // denomination -> count
    private static final int[] DENOMINATIONS = {100, 50, 20, 10, 5, 1};
    
    public CashDispenser() {
        this.cashInventory = new HashMap<>();
        initializeInventory();
    }
    
    /**
     * Initialize with default cash amounts
     */
    private void initializeInventory() {
        cashInventory.put(100, 50);  // 50 x $100 bills
        cashInventory.put(50, 50);   // 50 x $50 bills
        cashInventory.put(20, 100);  // 100 x $20 bills
        cashInventory.put(10, 100);  // 100 x $10 bills
        cashInventory.put(5, 50);    // 50 x $5 bills
        cashInventory.put(1, 100);   // 100 x $1 bills
    }
    
    /**
     * Check if sufficient cash available
     */
    public synchronized boolean hasSufficientCash(double amount) {
        return getTotalCash() >= amount;
    }
    
    /**
     * Get total cash in dispenser
     */
    public synchronized double getTotalCash() {
        double total = 0;
        for (Map.Entry<Integer, Integer> entry : cashInventory.entrySet()) {
            total += entry.getKey() * entry.getValue();
        }
        return total;
    }
    
    /**
     * Dispense cash using greedy algorithm
     * Returns map of denomination -> count
     */
    public synchronized Map<Integer, Integer> dispenseCash(double amount) {
        int amountInt = (int) amount;
        Map<Integer, Integer> dispensed = new HashMap<>();
        
        // Greedy algorithm - use largest denominations first
        for (int denomination : DENOMINATIONS) {
            int available = cashInventory.get(denomination);
            int needed = amountInt / denomination;
            int toDispense = Math.min(available, needed);
            
            if (toDispense > 0) {
                dispensed.put(denomination, toDispense);
                cashInventory.put(denomination, available - toDispense);
                amountInt -= denomination * toDispense;
            }
        }
        
        if (amountInt > 0) {
            // Couldn't dispense exact amount - rollback
            for (Map.Entry<Integer, Integer> entry : dispensed.entrySet()) {
                int denom = entry.getKey();
                int count = entry.getValue();
                cashInventory.put(denom, cashInventory.get(denom) + count);
            }
            return null;
        }
        
        return dispensed;
    }
    
    /**
     * Refill cash inventory
     */
    public synchronized void refill(int denomination, int count) {
        cashInventory.put(denomination, 
            cashInventory.getOrDefault(denomination, 0) + count);
        System.out.println("✓ Refilled: " + count + " x $" + denomination + " bills");
    }
    
    /**
     * Display current inventory
     */
    public void displayInventory() {
        System.out.println("\n=== Cash Inventory ===");
        for (int denomination : DENOMINATIONS) {
            int count = cashInventory.get(denomination);
            System.out.printf("$%3d: %3d bills = $%,6.2f%n", 
                denomination, count, (denomination * count));
        }
        System.out.printf("Total Cash: $%,.2f%n", getTotalCash());
        System.out.println("===================\n");
    }
}
