import java.util.Objects;

/**
 * Book - Represents a book in the library
 * 
 * Interview Key Points:
 * - Immutable book details
 * - ISBN as unique identifier
 * - Builder pattern for creation
 */
public class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final String publisher;
    private final int publicationYear;
    private final String category;
    
    private Book(Builder builder) {
        this.isbn = builder.isbn;
        this.title = builder.title;
        this.author = builder.author;
        this.publisher = builder.publisher;
        this.publicationYear = builder.publicationYear;
        this.category = builder.category;
    }
    
    // Getters
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public int getPublicationYear() { return publicationYear; }
    public String getCategory() { return category; }
    
    // Builder class
    public static class Builder {
        private String isbn;
        private String title;
        private String author;
        private String publisher;
        private int publicationYear;
        private String category;
        
        public Builder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder author(String author) {
            this.author = author;
            return this;
        }
        
        public Builder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }
        
        public Builder publicationYear(int year) {
            this.publicationYear = year;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Book build() {
            return new Book(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Book: %s by %s (ISBN: %s)", title, author, isbn);
    }
}