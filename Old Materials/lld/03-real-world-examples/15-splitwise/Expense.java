import java.time.LocalDateTime;
import java.util.*;

/**
 * Expense represents a shared expense
 */
public class Expense {
    private String expenseId;
    private String description;
    private double amount;
    private String paidBy;
    private List<String> participants;
    private Map<String, Double> splits; // userId -> amount owed
    private LocalDateTime createdAt;
    
    public Expense(String expenseId, String description, double amount,
                  String paidBy, Map<String, Double> splits) {
        this.expenseId = expenseId;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splits = new HashMap<>(splits);
        this.participants = new ArrayList<>(splits.keySet());
        this.createdAt = LocalDateTime.now();
    }
    
    public String getExpenseId() { return expenseId; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getPaidBy() { return paidBy; }
    public Map<String, Double> getSplits() { return new HashMap<>(splits); }
    public List<String> getParticipants() { return new ArrayList<>(participants); }
    
    @Override
    public String toString() {
        return String.format("Expense: %s ($%.2f) paid by %s", 
            description, amount, paidBy);
    }
}
