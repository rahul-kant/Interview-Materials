import java.util.*;

/**
 * Splitwise System Demo
 */
public class SplitwiseDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      SPLITWISE SYSTEM DEMO               ║");
        System.out.println("╚══════════════════════════════════════════╝\n");
        
        SplitwiseSystem system = SplitwiseSystem.getInstance();
        
        // Setup users
        System.out.println("=== Setting up users ===\n");
        User alice = new User("alice", "Alice", "alice@email.com");
        User bob = new User("bob", "Bob", "bob@email.com");
        User charlie = new User("charlie", "Charlie", "charlie@email.com");
        
        system.addUser(alice);
        system.addUser(bob);
        system.addUser(charlie);
        
        // Test 1: Equal split
        System.out.println("\n=== TEST 1: Equal Split ===\n");
        testEqualSplit(system);
        
        // Test 2: Custom split
        System.out.println("\n=== TEST 2: Custom Split ===\n");
        testCustomSplit(system);
        
        // Test 3: Show balances
        System.out.println("\n=== TEST 3: Show Balances ===");
        system.showBalances("alice");
        system.showBalances("bob");
        system.showBalances("charlie");
        
        // Test 4: Settle up
        System.out.println("\n=== TEST 4: Settle Up ===\n");
        testSettleUp(system);
        
        // Test 5: Real-world scenario
        System.out.println("\n=== REAL-WORLD: Trip Expenses ===\n");
        demonstrateTripExpenses(system);
    }
    
    private static void testEqualSplit(SplitwiseSystem system) {
        System.out.println("Alice pays $120 for dinner (split 3 ways)\n");
        
        system.addExpenseEqualSplit(
            "Dinner at Restaurant",
            120.0,
            "alice",
            Arrays.asList("alice", "bob", "charlie")
        );
    }
    
    private static void testCustomSplit(SplitwiseSystem system) {
        System.out.println("Bob pays $90 for groceries (custom split)\n");
        
        Map<String, Double> splits = new HashMap<>();
        splits.put("alice", 30.0);
        splits.put("bob", 40.0);
        splits.put("charlie", 20.0);
        
        system.addExpense("Groceries", 90.0, "bob", splits);
    }
    
    private static void testSettleUp(SplitwiseSystem system) {
        System.out.println("Charlie settles up with Alice\n");
        system.settleUp("charlie", "alice", 40.0);
        
        System.out.println("\nBalances after settlement:");
        system.showBalances("charlie");
    }
    
    private static void demonstrateTripExpenses(SplitwiseSystem system) {
        System.out.println("Vacation trip with multiple expenses:\n");
        
        // Hotel
        system.addExpenseEqualSplit("Hotel", 300.0, "alice", 
            Arrays.asList("alice", "bob", "charlie"));
        
        // Car rental
        system.addExpenseEqualSplit("Car Rental", 150.0, "bob",
            Arrays.asList("alice", "bob", "charlie"));
        
        // Gas
        system.addExpenseEqualSplit("Gas", 60.0, "charlie",
            Arrays.asList("alice", "bob", "charlie"));
        
        System.out.println("\n--- Final Balances ---");
        system.showBalances("alice");
        system.showBalances("bob");
        system.showBalances("charlie");
        
        System.out.println("\n💡 Production Features:");
        System.out.println("   - Debt simplification (minimize transactions)");
        System.out.println("   - Group management");
        System.out.println("   - Recurring expenses");
        System.out.println("   - Multiple currencies");
        System.out.println("   - Receipt scanning");
    }
}
