import java.util.Objects;

/**
 * BookCopy - Represents a physical copy of a book
 * 
 * Interview Key Points:
 * - Tracks individual book copies
 * - Maintains copy status
 * - Links to parent Book
 */
public class BookCopy {
    private final String copyId;
    private final Book book;
    private boolean available;
    private String condition;  // NEW, GOOD, FAIR, POOR
    
    public BookCopy(String copyId, Book book) {
        this.copyId = copyId;
        this.book = book;
        this.available = true;
        this.condition = "NEW";
    }
    
    // Getters
    public String getCopyId() { return copyId; }
    public Book getBook() { return book; }
    public boolean isAvailable() { return available; }
    public String getCondition() { return condition; }
    
    // Status management
    public void markUnavailable() {
        this.available = false;
    }
    
    public void markAvailable() {
        this.available = true;
    }
    
    public void updateCondition(String condition) {
        if (!isValidCondition(condition)) {
            throw new IllegalArgumentException("Invalid condition: " + condition);
        }
        this.condition = condition;
    }
    
    private boolean isValidCondition(String condition) {
        return condition != null && 
               (condition.equals("NEW") || 
                condition.equals("GOOD") || 
                condition.equals("FAIR") || 
                condition.equals("POOR"));
    }
    
    @Override
    public String toString() {
        return String.format("Copy %s of %s - %s (%s)", 
            copyId, book.getTitle(), condition, available ? "Available" : "Checked Out");
    }
}