import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Splitwise System - Expense Sharing
 * 
 * CORE FEATURES:
 * - Track shared expenses
 * - Calculate balances
 * - Simplify debts
 * - Settle payments
 */
public class SplitwiseSystem {
    private static SplitwiseSystem instance;
    private Map<String, User> users;
    private Map<String, Expense> expenses;
    private Map<String, Map<String, Double>> balances; // user1 -> (user2 -> amount)
    private AtomicInteger expenseIdCounter;
    
    private SplitwiseSystem() {
        this.users = new ConcurrentHashMap<>();
        this.expenses = new ConcurrentHashMap<>();
        this.balances = new ConcurrentHashMap<>();
        this.expenseIdCounter = new AtomicInteger(1);
    }
    
    public static synchronized SplitwiseSystem getInstance() {
        if (instance == null) {
            instance = new SplitwiseSystem();
        }
        return instance;
    }
    
    public void addUser(User user) {
        users.put(user.getUserId(), user);
        balances.putIfAbsent(user.getUserId(), new ConcurrentHashMap<>());
    }
    
    /**
     * Add expense with equal split
     */
    public Expense addExpenseEqualSplit(String description, double amount,
                                       String paidBy, List<String> participants) {
        double splitAmount = amount / participants.size();
        Map<String, Double> splits = new HashMap<>();
        
        for (String participant : participants) {
            splits.put(participant, splitAmount);
        }
        
        return addExpense(description, amount, paidBy, splits);
    }
    
    /**
     * Add expense with exact splits
     */
    public Expense addExpense(String description, double amount,
                             String paidBy, Map<String, Double> splits) {
        // Validate splits sum to amount
        double total = splits.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - amount) > 0.01) {
            System.out.println("❌ Splits don't add up to amount");
            return null;
        }
        
        String expenseId = "EXP" + expenseIdCounter.getAndIncrement();
        Expense expense = new Expense(expenseId, description, amount, paidBy, splits);
        expenses.put(expenseId, expense);
        
        // Update balances
        updateBalances(paidBy, splits);
        
        System.out.println("✓ Expense added: " + expenseId);
        return expense;
    }
    
    /**
     * Update balance sheet
     */
    private void updateBalances(String paidBy, Map<String, Double> splits) {
        for (Map.Entry<String, Double> entry : splits.entrySet()) {
            String participant = entry.getKey();
            double amount = entry.getValue();
            
            if (!participant.equals(paidBy)) {
                // participant owes paidBy
                addBalance(participant, paidBy, amount);
            }
        }
    }
    
    /**
     * Add to balance (from owes to)
     */
    private void addBalance(String from, String to, double amount) {
        balances.putIfAbsent(from, new ConcurrentHashMap<>());
        balances.get(from).merge(to, amount, Double::sum);
    }
    
    /**
     * Record settlement payment
     */
    public void settleUp(String from, String to, double amount) {
        Map<String, Double> fromBalances = balances.get(from);
        if (fromBalances != null && fromBalances.containsKey(to)) {
            double currentBalance = fromBalances.get(to);
            double newBalance = currentBalance - amount;
            
            if (Math.abs(newBalance) < 0.01) {
                fromBalances.remove(to);
            } else {
                fromBalances.put(to, newBalance);
            }
            
            System.out.println(String.format("✓ %s paid %s $%.2f", from, to, amount));
        }
    }
    
    /**
     * Get balance between two users
     */
    public double getBalance(String user1, String user2) {
        double balance = 0;
        
        // Check if user1 owes user2
        if (balances.containsKey(user1) && balances.get(user1).containsKey(user2)) {
            balance -= balances.get(user1).get(user2);
        }
        
        // Check if user2 owes user1
        if (balances.containsKey(user2) && balances.get(user2).containsKey(user1)) {
            balance += balances.get(user2).get(user1);
        }
        
        return balance;
    }
    
    /**
     * Show all balances for user
     */
    public void showBalances(String userId) {
        User user = users.get(userId);
        System.out.println("\n=== Balances for " + user.getName() + " ===");
        
        boolean hasBalances = false;
        
        // Show who user owes
        if (balances.containsKey(userId)) {
            for (Map.Entry<String, Double> entry : balances.get(userId).entrySet()) {
                if (entry.getValue() > 0.01) {
                    String otherUser = users.get(entry.getKey()).getName();
                    System.out.println(String.format("  You owe %s: $%.2f", 
                        otherUser, entry.getValue()));
                    hasBalances = true;
                }
            }
        }
        
        // Show who owes user
        for (Map.Entry<String, Map<String, Double>> userEntry : balances.entrySet()) {
            String otherUserId = userEntry.getKey();
            if (!otherUserId.equals(userId) && userEntry.getValue().containsKey(userId)) {
                double amount = userEntry.getValue().get(userId);
                if (amount > 0.01) {
                    String otherUser = users.get(otherUserId).getName();
                    System.out.println(String.format("  %s owes you: $%.2f", 
                        otherUser, amount));
                    hasBalances = true;
                }
            }
        }
        
        if (!hasBalances) {
            System.out.println("  All settled up! ✓");
        }
    }
    
    public User getUser(String userId) {
        return users.get(userId);
    }
}
