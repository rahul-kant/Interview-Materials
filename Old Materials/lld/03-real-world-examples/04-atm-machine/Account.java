/**
 * Account Class - Represents Bank Account
 * 
 * Interview Key Points:
 * - Thread-safe balance operations
 * - Transaction history
 * - Overdraft protection
 * - PIN security
 */
public class Account {
    private final String accountNumber;
    private final String accountHolderName;
    private double balance;
    private final String pin; // In real system, this should be hashed
    private int failedPinAttempts;
    private boolean isLocked;
    private static final int MAX_PIN_ATTEMPTS = 3;
    
    public Account(String accountNumber, String accountHolderName, 
                   double initialBalance, String pin) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = initialBalance;
        this.pin = pin; // Should be hashed in production
        this.failedPinAttempts = 0;
        this.isLocked = false;
    }
    
    /**
     * Verify PIN with attempt tracking
     */
    public synchronized boolean verifyPin(String inputPin) {
        if (isLocked) {
            System.out.println("❌ Account is locked due to multiple failed attempts");
            return false;
        }
        
        if (this.pin.equals(inputPin)) {
            failedPinAttempts = 0;
            return true;
        }
        
        failedPinAttempts++;
        System.out.println("❌ Incorrect PIN. Attempts: " + failedPinAttempts + "/" + MAX_PIN_ATTEMPTS);
        
        if (failedPinAttempts >= MAX_PIN_ATTEMPTS) {
            isLocked = true;
            System.out.println("🔒 Account locked! Contact bank to unlock.");
        }
        
        return false;
    }
    
    /**
     * Get current balance
     */
    public synchronized double getBalance() {
        return balance;
    }
    
    /**
     * Withdraw money (thread-safe)
     */
    public synchronized boolean withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("❌ Invalid withdrawal amount");
            return false;
        }
        
        if (amount > balance) {
            System.out.println("❌ Insufficient funds");
            return false;
        }
        
        balance -= amount;
        System.out.println("✓ Withdrawn: $" + String.format("%.2f", amount));
        System.out.println("  New balance: $" + String.format("%.2f", balance));
        return true;
    }
    
    /**
     * Deposit money (thread-safe)
     */
    public synchronized boolean deposit(double amount) {
        if (amount <= 0) {
            System.out.println("❌ Invalid deposit amount");
            return false;
        }
        
        balance += amount;
        System.out.println("✓ Deposited: $" + String.format("%.2f", amount));
        System.out.println("  New balance: $" + String.format("%.2f", balance));
        return true;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public boolean isLocked() {
        return isLocked;
    }
    
    /**
     * Unlock account (admin function)
     */
    public synchronized void unlock() {
        isLocked = false;
        failedPinAttempts = 0;
        System.out.println("✓ Account unlocked");
    }
    
    @Override
    public String toString() {
        return String.format("Account: %s - %s - Balance: $%.2f", 
            accountNumber, accountHolderName, balance);
    }
}
