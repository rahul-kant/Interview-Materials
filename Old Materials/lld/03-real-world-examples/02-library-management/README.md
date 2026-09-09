# Library Management System - Complete Implementation

## 🎯 Problem Statement

Design a library management system for managing books, members, and borrowing operations.

**Asked at:** Flipkart, Amazon, Microsoft, Walmart

---

## 📋 Requirements

### Functional Requirements:
1. Multiple copies of same book (BookItem concept)
2. Different member types: Student, Faculty, Premium
3. Different book categories
4. Borrow and return books
5. Fine calculation for late returns
6. Search books by title, author, ISBN
7. Reserve books when not available
8. Track borrowing history
9. Max books limit per member type

### Non-Functional Requirements:
1. Handle concurrent borrowing requests
2. Efficient search
3. Easy to extend
4. Data persistence ready

---

## 🏗️ Design

### Core Entities:
1. **Library** (Singleton) - Main system
2. **Book** - Book metadata (title, author, ISBN)
3. **BookItem** - Physical copy of a book
4. **Member** (Abstract) - Library member
   - Student, Faculty, Premium
5. **BookLending** - Lending transaction
6. **Catalog** - Search functionality
7. **Librarian** - Staff operations

### Design Patterns:
- **Singleton**: Library instance
- **Factory**: Create different member types
- **Observer**: Notify when reserved book available
- **Strategy**: Different fine calculation strategies

### SOLID Principles:
- **SRP**: BookItem manages availability, Member manages borrowing
- **OCP**: Easy to add new member types
- **LSP**: All members are substitutable
- **ISP**: Separate interfaces for Borrowable, Reservable
- **DIP**: Depend on abstractions

---

## 💻 Implementation Files

### Core Classes:
1. **[BookCategory.java](BookCategory.java)** - Enum
2. **[Book.java](Book.java)** - Book metadata
3. **[BookItem.java](BookItem.java)** - Physical copy
4. **[Member.java](Member.java)** - Abstract member
5. **[Student.java](Student.java)** - Student member
6. **[Faculty.java](Faculty.java)** - Faculty member
7. **[Premium.java](Premium.java)** - Premium member
8. **[BookLending.java](BookLending.java)** - Lending record
9. **[FineCalculator.java](FineCalculator.java)** - Calculate fines
10. **[Catalog.java](Catalog.java)** - Search functionality
11. **[Library.java](Library.java)** - Main system
12. **[LibraryDemo.java](LibraryDemo.java)** - Demo app

---

## 🔑 Key Features

### 1. Member Types with Different Limits

| Member Type | Max Books | Borrow Duration |
|-------------|-----------|-----------------|
| Student     | 5 books   | 14 days        |
| Faculty     | 10 books  | 30 days        |
| Premium     | 15 books  | 60 days        |

### 2. Fine Calculation
- **Base**: $1 per day late
- **Student**: 50% discount on fines
- **Faculty**: No fines for first week
- **Premium**: No fines

### 3. Reservation System
- Queue when book not available
- Auto-notify when available
- 24 hour hold period

---

## 🎯 Interview Discussion Points

### 1. Multiple Copies Handling
**Q: How do you handle multiple copies of same book?**

A: Book vs BookItem separation:
- **Book**: Metadata (title, author, ISBN)
- **BookItem**: Physical copy with barcode
- One Book can have many BookItems

### 2. Concurrent Borrowing
**Q: Two members try to borrow last copy simultaneously?**

A: Use synchronized methods and transaction locking

### 3. Search Optimization
**Q: How to optimize search for large libraries?**

A: Multiple strategies:
- Index by title, author, ISBN
- Use trie for autocomplete
- Cache popular searches

### 4. Overdue Books
**Q: How to handle members who never return books?**

A: Implement:
- Email reminders
- Account suspension
- Escalating fines
- Legal action threshold

---

## 🧪 Test Scenarios

### Test 1: Basic Borrowing
```java
Student student = new Student("S001", "John");
BookItem item = findAvailableItem("ISBN-123");
library.checkout(student, item);
// Verify: item not available, student has book
```

### Test 2: Fine Calculation
```java
// Borrow book, set due date to past
BookLending lending = new BookLending(student, item);
lending.setDueDate(LocalDate.now().minusDays(5));
double fine = library.returnBook(lending);
// Verify: fine = $2.50 (student gets 50% discount)
```

### Test 3: Reservation
```java
// All copies borrowed
BookItem item = library.reserveBook(member, book);
// Verify: member added to queue
// When book returned, member notified
```

---

## 📈 Complexity

### Time Complexity:
- Add book: O(1)
- Search by title: O(n) or O(log n) with indexing
- Checkout: O(1)
- Return: O(1)

### Space Complexity:
- O(B) for books, where B = number of book items
- O(M) for members
- O(L) for active lendings

---

## 🚀 Extensions

### 1. Digital Library
- Add eBooks
- Different lending rules
- No physical copies

### 2. Inter-Library Loan
- Request from other libraries
- Track transfers
- Extended due dates

### 3. Reading Recommendations
- Based on history
- Similar books
- Popular in category

---

**Time to Complete in Interview: 45-60 minutes**

**See the code files for complete working implementation!**
