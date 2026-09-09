import java.util.*;
import java.util.stream.Collectors;

/**
 * Library - Main controller class (Singleton)
 * 
 * Interview Key Points:
 * - Singleton pattern for library instance
 * - Manages all library operations
 * - Thread-safe implementation
 * - Search functionality
 */
public class Library {
    private static Library instance;
    
    // Data stores
    private final Map<String, Book> books;
    private final Map<String, BookCopy> bookCopies;
    private final Map<String, Member> members;
    private final Map<String, Loan> loans;
    private final Map<String, List<BookCopy>> bookToCopies;
    
    private Library() {
        this.books = new HashMap<>();
        this.bookCopies = new HashMap<>();
        this.members = new HashMap<>();
        this.loans = new HashMap<>();
        this.bookToCopies = new HashMap<>();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized Library getInstance() {
        if (instance == null) {
            instance = new Library();
        }
        return instance;
    }
    
    // ========== Book Management ==========
    
    /**
     * Add a new book to the library
     */
    public synchronized void addBook(Book book) {
        if (books.containsKey(book.getIsbn())) {
            throw new IllegalArgumentException("Book with ISBN already exists");
        }
        books.put(book.getIsbn(), book);
        bookToCopies.put(book.getIsbn(), new ArrayList<>());
        System.out.println("Added book: " + book);
    }
    
    /**
     * Add copies of a book
     */
    public synchronized void addBookCopies(String isbn, int count) {
        Book book = books.get(isbn);
        if (book == null) {
            throw new IllegalArgumentException("Book with ISBN not found");
        }
        
        List<BookCopy> copies = bookToCopies.get(isbn);
        for (int i = 0; i < count; i++) {
            String copyId = isbn + "-" + (copies.size() + 1);
            BookCopy copy = new BookCopy(copyId, book);
            bookCopies.put(copyId, copy);
            copies.add(copy);
        }
        System.out.println("Added " + count + " copies of " + book.getTitle());
    }
    
    // ========== Member Management ==========
    
    /**
     * Add a new member
     */
    public synchronized void addMember(Member member) {
        if (members.containsKey(member.getMemberId())) {
            throw new IllegalArgumentException("Member ID already exists");
        }
        members.put(member.getMemberId(), member);
        System.out.println("Added member: " + member);
    }
    
    // ========== Loan Operations ==========
    
    /**
     * Check out a book to a member
     */
    public synchronized Loan checkoutBook(String memberId, String isbn) {
        // Validate member
        Member member = members.get(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found");
        }
        if (!member.isActive()) {
            throw new IllegalStateException("Member account is inactive");
        }
        if (member.getFineAmount() > 0) {
            throw new IllegalStateException("Member has unpaid fines");
        }
        
        // Find available copy
        List<BookCopy> copies = bookToCopies.get(isbn);
        if (copies == null || copies.isEmpty()) {
            throw new IllegalArgumentException("Book not found");
        }
        
        BookCopy availableCopy = copies.stream()
            .filter(BookCopy::isAvailable)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No copies available"));
        
        // Create loan
        String loanId = UUID.randomUUID().toString();
        Loan loan = new Loan(loanId, member, availableCopy);
        loans.put(loanId, loan);
        
        System.out.println("Checked out: " + availableCopy + " to " + member);
        return loan;
    }
    
    /**
     * Return a book
     */
    public synchronized void returnBook(String loanId) {
        Loan loan = loans.get(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("Loan not found");
        }
        if (loan.isReturned()) {
            throw new IllegalStateException("Book already returned");
        }
        
        loan.returnBook();
        System.out.println("Returned: " + loan.getBookCopy());
        
        if (loan.getFine() > 0) {
            System.out.println("Fine charged: $" + loan.getFine());
        }
    }
    
    // ========== Search Operations ==========
    
    /**
     * Search books by title (case-insensitive partial match)
     */
    public List<Book> searchBooksByTitle(String title) {
        return books.values().stream()
            .filter(book -> book.getTitle().toLowerCase()
                .contains(title.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * Search books by author
     */
    public List<Book> searchBooksByAuthor(String author) {
        return books.values().stream()
            .filter(book -> book.getAuthor().toLowerCase()
                .contains(author.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all overdue loans
     */
    public List<Loan> getOverdueLoans() {
        return loans.values().stream()
            .filter(Loan::isOverdue)
            .collect(Collectors.toList());
    }
    
    // ========== System Status ==========
    
    /**
     * Get library statistics
     */
    public String getStatistics() {
        long availableCopies = bookCopies.values().stream()
            .filter(BookCopy::isAvailable)
            .count();
            
        long activeLoans = loans.values().stream()
            .filter(loan -> !loan.isReturned())
            .count();
            
        return String.format("""
            Library Statistics:
            - Total Books: %d
            - Total Copies: %d
            - Available Copies: %d
            - Total Members: %d
            - Active Loans: %d
            - Overdue Loans: %d
            """,
            books.size(),
            bookCopies.size(),
            availableCopies,
            members.size(),
            activeLoans,
            getOverdueLoans().size()
        );
    }
}