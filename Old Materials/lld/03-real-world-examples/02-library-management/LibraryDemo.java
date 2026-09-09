/**
 * LibraryDemo - Main class to demonstrate library system
 */

import java.util.*;
public class LibraryDemo {
    public static void main(String[] args) {
        Library library = Library.getInstance();
        
        // Add some books
        Book book1 = new Book.Builder()
            .isbn("978-0134685991")
            .title("Effective Java")
            .author("Joshua Bloch")
            .publisher("Addison-Wesley")
            .publicationYear(2017)
            .category("Programming")
            .build();
            
        Book book2 = new Book.Builder()
            .isbn("978-0132350884")
            .title("Clean Code")
            .author("Robert C. Martin")
            .publisher("Prentice Hall")
            .publicationYear(2008)
            .category("Programming")
            .build();
            
        library.addBook(book1);
        library.addBook(book2);
        
        // Add copies
        library.addBookCopies(book1.getIsbn(), 3);
        library.addBookCopies(book2.getIsbn(), 2);
        
        // Add members
        Member member1 = new Member("M001", "John Doe", "john@example.com", "123-456-7890");
        Member member2 = new Member("M002", "Jane Smith", "jane@example.com", "123-456-7891");
        
        library.addMember(member1);
        library.addMember(member2);
        
        // Demonstrate checkouts
        try {
            Loan loan1 = library.checkoutBook(member1.getMemberId(), book1.getIsbn());
            System.out.println("\nLoan created: " + loan1);
            
            Loan loan2 = library.checkoutBook(member2.getMemberId(), book2.getIsbn());
            System.out.println("Loan created: " + loan2);
            
            // Return a book
            library.returnBook(loan1.getLoanId());
            
            // Search functionality
            System.out.println("\nSearching for 'Java' books:");
            library.searchBooksByTitle("Java").forEach(System.out::println);
            
            // Library statistics
            System.out.println("\n" + library.getStatistics());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}