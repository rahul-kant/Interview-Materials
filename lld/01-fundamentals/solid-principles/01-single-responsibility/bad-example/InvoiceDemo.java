/**
 * Demo for BAD Example - Violates Single Responsibility Principle
 * 
 * This demonstrates the problems with having multiple responsibilities in one class.
 * Notice how the Invoice class is doing EVERYTHING!
 */
public class InvoiceDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  INVOICE SYSTEM - BAD EXAMPLE (SRP)");
        System.out.println("========================================");
        System.out.println("\nThis example shows what happens when a class");
        System.out.println("has MULTIPLE responsibilities (violates SRP)");
        System.out.println("========================================\n");
        
        // Create an invoice
        Invoice invoice = new Invoice("INV-001", "John Doe", "john@example.com", 0.10);
        
        // Add some items
        invoice.addItem(new InvoiceItem("Laptop", 999.99, 1));
        invoice.addItem(new InvoiceItem("Mouse", 25.50, 2));
        invoice.addItem(new InvoiceItem("Keyboard", 75.00, 1));
        
        System.out.println("Invoice created with " + invoice.getItems().size() + " items");
        System.out.println("Customer: " + invoice.getCustomerName());
        
        // Now the problems begin...
        
        // Problem 1: Invoice class handles calculations
        System.out.println("\n--- Calculating Totals ---");
        System.out.println("Subtotal: $" + String.format("%.2f", invoice.calculateSubtotal()));
        System.out.println("Tax: $" + String.format("%.2f", invoice.calculateTax()));
        System.out.println("Total: $" + String.format("%.2f", invoice.calculateTotal()));
        
        // Problem 2: Invoice class handles database operations
        invoice.saveToDatabase();
        
        // Problem 3: Invoice class handles email operations
        invoice.sendEmail();
        
        // Problem 4: Invoice class handles PDF generation
        invoice.generatePDF();
        
        // Problem 5: Invoice class handles printing
        invoice.printInvoice();
        
        // Summary of problems
        System.out.println("\n\n========================================");
        System.out.println("         PROBLEMS WITH THIS DESIGN");
        System.out.println("========================================");
        System.out.println("\n❌ ISSUE 1: MULTIPLE RESPONSIBILITIES");
        System.out.println("  - Invoice class has 6 different responsibilities");
        System.out.println("  - Violates Single Responsibility Principle");
        
        System.out.println("\n❌ ISSUE 2: HARD TO MAINTAIN");
        System.out.println("  - If email service changes, must modify Invoice class");
        System.out.println("  - If database changes, must modify Invoice class");
        System.out.println("  - If PDF library changes, must modify Invoice class");
        System.out.println("  - If tax rules change, must modify Invoice class");
        
        System.out.println("\n❌ ISSUE 3: HARD TO TEST");
        System.out.println("  - Can't test calculation logic without database");
        System.out.println("  - Can't test email without entire invoice");
        System.out.println("  - Each test requires full setup of all dependencies");
        
        System.out.println("\n❌ ISSUE 4: HIGH COUPLING");
        System.out.println("  - Invoice depends on database, email, PDF, printer");
        System.out.println("  - Changes in any dependency affect Invoice");
        System.out.println("  - Can't reuse one part without the others");
        
        System.out.println("\n❌ ISSUE 5: LOW COHESION");
        System.out.println("  - Methods are not closely related");
        System.out.println("  - calculateTax() and sendEmail() have nothing in common");
        System.out.println("  - Class is doing too many unrelated things");
        
        System.out.println("\n❌ ISSUE 6: DIFFICULT TO EXTEND");
        System.out.println("  - Want to add SMS notification? Modify Invoice!");
        System.out.println("  - Want to support multiple databases? Modify Invoice!");
        System.out.println("  - Want different tax calculations? Modify Invoice!");
        
        System.out.println("\n========================================");
        System.out.println("         WHAT SHOULD WE DO?");
        System.out.println("========================================");
        System.out.println("\n✅ SOLUTION: Apply Single Responsibility Principle");
        System.out.println("\n1. Separate Data from Behavior");
        System.out.println("   - Invoice class: Only stores invoice data");
        System.out.println("\n2. Create Dedicated Classes for Each Responsibility");
        System.out.println("   - InvoiceCalculator: Handles calculations");
        System.out.println("   - InvoiceRepository: Handles database operations");
        System.out.println("   - EmailService: Handles email sending");
        System.out.println("   - PDFGenerator: Handles PDF generation");
        System.out.println("   - PrintService: Handles printing");
        System.out.println("\n3. Benefits:");
        System.out.println("   - Each class has ONE responsibility");
        System.out.println("   - Easy to test each class independently");
        System.out.println("   - Easy to change one part without affecting others");
        System.out.println("   - Can reuse components in different contexts");
        System.out.println("   - Better code organization");
        
        System.out.println("\n========================================");
        System.out.println("   👉 See 'good-example' folder for the");
        System.out.println("   proper implementation following SRP!");
        System.out.println("========================================\n");
    }
}
