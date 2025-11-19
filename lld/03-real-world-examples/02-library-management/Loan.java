import java.util.Objects;

/**
 * Loan - Represents a book loan transaction
 * 
 * Interview Key Points:
 * - Tracks loan status
 * - Handles due dates
 * - Calculates fines
 */
public class Loan {
    private final String loanId;
    private final Member member;
    private final BookCopy bookCopy;
    private final long checkoutDate;
    private final long dueDate;
    private long returnDate;
    private double fine;
    private boolean returned;
    
    private static final long LOAN_PERIOD = 14 * 24 * 60 * 60 * 1000L; // 14 days in milliseconds
    private static final double FINE_PER_DAY = 1.0; // $1 per day
    
    public Loan(String loanId, Member member, BookCopy bookCopy) {
        this.loanId = loanId;
        this.member = member;
        this.bookCopy = bookCopy;
        this.checkoutDate = System.currentTimeMillis();
        this.dueDate = checkoutDate + LOAN_PERIOD;
        this.returned = false;
        this.fine = 0.0;
        
        // Mark book copy as unavailable
        bookCopy.markUnavailable();
    }
    
    // Getters
    public String getLoanId() { return loanId; }
    public Member getMember() { return member; }
    public BookCopy getBookCopy() { return bookCopy; }
    public long getCheckoutDate() { return checkoutDate; }
    public long getDueDate() { return dueDate; }
    public long getReturnDate() { return returnDate; }
    public double getFine() { return fine; }
    public boolean isReturned() { return returned; }
    
    /**
     * Return the book and calculate any fine
     */
    public void returnBook() {
        if (returned) {
            throw new IllegalStateException("Book already returned");
        }
        
        this.returnDate = System.currentTimeMillis();
        this.returned = true;
        
        // Calculate fine if returned late
        if (returnDate > dueDate) {
            long daysLate = (returnDate - dueDate) / (24 * 60 * 60 * 1000);
            this.fine = daysLate * FINE_PER_DAY;
            member.addFine(fine);
        }
        
        // Mark book copy as available
        bookCopy.markAvailable();
    }
    
    /**
     * Check if loan is overdue
     */
    public boolean isOverdue() {
        return !returned && System.currentTimeMillis() > dueDate;
    }
    
    /**
     * Get days until due (negative if overdue)
     */
    public long getDaysUntilDue() {
        if (returned) return 0;
        return (dueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000);
    }
    
    @Override
    public String toString() {
        return String.format("Loan: %s - %s borrowed by %s (Due: %d days)", 
            loanId, bookCopy.getBook().getTitle(), member.getName(), getDaysUntilDue());
    }
}