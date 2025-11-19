/**
 * VendingMachineDemo - Demonstrates the Vending Machine System
 * 
 * Interview Key Points:
 * - Shows complete workflow
 * - Demonstrates state transitions
 * - Tests various scenarios
 * - Error handling examples
 */
public class VendingMachineDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   VENDING MACHINE SYSTEM DEMO            ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        // Get vending machine instance (Singleton)
        VendingMachine machine = VendingMachine.getInstance();
        
        // Setup - Add products
        setupMachine(machine);
        
        // Test Scenarios
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SCENARIO 1: Successful Purchase");
        System.out.println("=".repeat(50));
        testSuccessfulPurchase(machine);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SCENARIO 2: Insufficient Funds");
        System.out.println("=".repeat(50));
        testInsufficientFunds(machine);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SCENARIO 3: Cancel Transaction");
        System.out.println("=".repeat(50));
        testCancelTransaction(machine);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SCENARIO 4: Out of Stock");
        System.out.println("=".repeat(50));
        testOutOfStock(machine);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SCENARIO 5: Exact Change");
        System.out.println("=".repeat(50));
        testExactChange(machine);
        
        // Show final inventory
        System.out.println("\n" + "=".repeat(50));
        System.out.println("FINAL INVENTORY STATUS");
        System.out.println("=".repeat(50));
        machine.showInventory();
    }
    
    /**
     * Setup machine with initial inventory
     */
    private static void setupMachine(VendingMachine machine) {
        System.out.println("⚙️  Setting up vending machine...\n");
        
        // Add snacks
        machine.addProduct(new Product("A1", "Coca Cola", 150, "Beverages"), 10);
        machine.addProduct(new Product("A2", "Pepsi", 150, "Beverages"), 8);
        machine.addProduct(new Product("B1", "Lays Chips", 200, "Snacks"), 5);
        machine.addProduct(new Product("B2", "Doritos", 225, "Snacks"), 3);
        machine.addProduct(new Product("C1", "Snickers", 175, "Chocolate"), 6);
        machine.addProduct(new Product("C2", "KitKat", 150, "Chocolate"), 7);
        machine.addProduct(new Product("D1", "Water", 100, "Beverages"), 15);
        
        System.out.println("✓ Machine setup complete!\n");
        machine.displayProducts();
    }
    
    /**
     * Test 1: Successful purchase with change
     */
    private static void testSuccessfulPurchase(VendingMachine machine) {
        System.out.println("Customer wants to buy Coca Cola ($1.50)");
        System.out.println("Inserting $2.00...\n");
        
        machine.insertCoin(Coin.DOLLAR);
        machine.insertCoin(Coin.DOLLAR);
        
        System.out.println("\nSelecting product A1 (Coca Cola)...");
        machine.selectProduct("A1");
    }
    
    /**
     * Test 2: Insufficient funds
     */
    private static void testInsufficientFunds(VendingMachine machine) {
        System.out.println("Customer wants to buy Doritos ($2.25)");
        System.out.println("Inserting only $1.50...\n");
        
        machine.insertCoin(Coin.DOLLAR);
        machine.insertCoin(Coin.QUARTER);
        machine.insertCoin(Coin.QUARTER);
        
        System.out.println("\nAttempting to select B2 (Doritos)...");
        machine.selectProduct("B2");
        
        System.out.println("\nAdding more money...");
        machine.insertCoin(Coin.DOLLAR);
        
        System.out.println("\nRetrying product selection...");
        machine.selectProduct("B2");
    }
    
    /**
     * Test 3: Cancel transaction
     */
    private static void testCancelTransaction(VendingMachine machine) {
        System.out.println("Customer inserts money but changes mind...\n");
        
        machine.insertCoin(Coin.DOLLAR);
        machine.insertCoin(Coin.QUARTER);
        
        System.out.println("\nCustomer presses cancel button...");
        machine.cancelTransaction();
    }
    
    /**
     * Test 4: Product out of stock
     */
    private static void testOutOfStock(VendingMachine machine) {
        System.out.println("Depleting Doritos stock first...\n");
        
        // Buy remaining Doritos
        for (int i = 0; i < 3; i++) {
            machine.insertCoin(Coin.DOLLAR);
            machine.insertCoin(Coin.DOLLAR);
            machine.insertCoin(Coin.QUARTER);
            machine.selectProduct("B2");
            System.out.println();
        }
        
        System.out.println("Now trying to buy Doritos when out of stock...\n");
        machine.insertCoin(Coin.DOLLAR);
        machine.insertCoin(Coin.DOLLAR);
        machine.insertCoin(Coin.QUARTER);
        machine.selectProduct("B2");
        machine.cancelTransaction();
    }
    
    /**
     * Test 5: Exact change scenario
     */
    private static void testExactChange(VendingMachine machine) {
        System.out.println("Customer buying Water ($1.00) with exact change...\n");
        
        machine.insertCoin(Coin.DOLLAR);
        machine.selectProduct("D1");
    }
}
