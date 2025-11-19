/**
 * Demo for GOOD Example - Follows Single Responsibility Principle
 * 
 * This demonstrates how separating responsibilities into different classes
 * makes the code more maintainable, testable, and flexible.
 * 
 * Notice how each class has ONE clear responsibility!
 */
public class InvoiceDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  INVOICE SYSTEM - GOOD EXAMPLE (SRP)");
        System.out.println("========================================");
        System.out.println("\nThis example shows proper separation of");
        System.out.println("responsibilities (follows SRP)");
        System.out.println("========================================\n");
        
        // Step 1: Create the invoice (only stores data)
        System.out.println("STEP 1: Creating Invoice");
        System.out.println("------------------------");
        Invoice invoice = new Invoice("INV-001", "John Doe", "john@example.com", 0.10);
        invoice.addItem(new InvoiceItem("Laptop", 999.99, 1));
        invoice.addItem(new InvoiceItem("Mouse", 25.50, 2));
        invoice.addItem(new InvoiceItem("Keyboard", 75.00, 1));
        System.out.println("✓ Invoice created with " + invoice.getItems().size() + " items");
        System.out.println("✓ Customer: " + invoice.getCustomerName());
        
        // Step 2: Use InvoiceCalculator (handles calculations)
        System.out.println("\n\nSTEP 2: Calculating Totals");
        System.out.println("---------------------------");
        InvoiceCalculator calculator = new InvoiceCalculator();
        System.out.println("✓ Subtotal: $" + String.format("%.2f", calculator.calculateSubtotal(invoice)));
        System.out.println("✓ Tax: $" + String.format("%.2f", calculator.calculateTax(invoice)));
        System.out.println("✓ Total: $" + String.format("%.2f", calculator.calculateTotal(invoice)));
        
        // Step 3: Use InvoiceRepository (handles database)
        System.out.println("\n\nSTEP 3: Saving to Database");
        System.out.println("---------------------------");
        InvoiceRepository repository = new InvoiceRepository(
            "jdbc:mysql://localhost:3306/invoices",
            "admin",
            "password123"
        );
        boolean saved = repository.save(invoice, calculator);
        System.out.println("✓ Invoice saved: " + saved);
        
        // Step 4: Use EmailService (handles emails)
        System.out.println("\n\nSTEP 4: Sending Email");
        System.out.println("----------------------");
        EmailService emailService = new EmailService(
            "smtp.gmail.com",
            587,
            "invoices@company.com",
            "emailpass123"
        );
        boolean emailSent = emailService.sendInvoiceEmail(invoice, calculator);
        System.out.println("✓ Email sent: " + emailSent);
        
        // Step 5: Use PDFGenerator (handles PDF creation)
        System.out.println("\n\nSTEP 5: Generating PDF");
        System.out.println("-----------------------");
        PDFGenerator pdfGenerator = new PDFGenerator(
            "/templates/invoice_template.pdf",
            "/output/invoices/"
        );
        String pdfPath = pdfGenerator.generateInvoicePDF(invoice, calculator);
        System.out.println("✓ PDF generated: " + (pdfPath != null));
        
        // Step 6: Use PrintService (handles printing)
        System.out.println("\n\nSTEP 6: Printing Invoice");
        System.out.println("-------------------------");
        PrintService printService = new PrintService("HP LaserJet Pro");
        boolean printed = printService.printInvoice(invoice, calculator);
        System.out.println("✓ Invoice printed: " + printed);
        
        // Summary of benefits
        System.out.println("\n\n========================================");
        System.out.println("      BENEFITS OF THIS DESIGN (SRP)");
        System.out.println("========================================");
        
        System.out.println("\n✅ BENEFIT 1: SINGLE RESPONSIBILITY");
        System.out.println("   - Invoice: Only stores data");
        System.out.println("   - InvoiceCalculator: Only calculates");
        System.out.println("   - InvoiceRepository: Only handles database");
        System.out.println("   - EmailService: Only sends emails");
        System.out.println("   - PDFGenerator: Only generates PDFs");
        System.out.println("   - PrintService: Only prints");
        
        System.out.println("\n✅ BENEFIT 2: EASY TO MAINTAIN");
        System.out.println("   - Change email provider? Modify only EmailService");
        System.out.println("   - Change database? Modify only InvoiceRepository");
        System.out.println("   - Change PDF library? Modify only PDFGenerator");
        System.out.println("   - Change tax rules? Modify only InvoiceCalculator");
        
        System.out.println("\n✅ BENEFIT 3: EASY TO TEST");
        System.out.println("   - Test calculations without database");
        System.out.println("   - Test email without creating invoices");
        System.out.println("   - Mock dependencies easily");
        System.out.println("   - Unit tests are simple and focused");
        
        System.out.println("\n✅ BENEFIT 4: LOW COUPLING");
        System.out.println("   - Classes are independent");
        System.out.println("   - Changes in one don't affect others");
        System.out.println("   - Easy to replace implementations");
        
        System.out.println("\n✅ BENEFIT 5: HIGH COHESION");
        System.out.println("   - Related methods are together");
        System.out.println("   - Each class has a clear purpose");
        System.out.println("   - Code is organized logically");
        
        System.out.println("\n✅ BENEFIT 6: REUSABILITY");
        System.out.println("   - EmailService can send any email");
        System.out.println("   - PDFGenerator can generate any PDF");
        System.out.println("   - PrintService can print anything");
        System.out.println("   - InvoiceRepository can be used by other features");
        
        System.out.println("\n✅ BENEFIT 7: EASY TO EXTEND");
        System.out.println("   - Want SMS notifications? Add SMSService");
        System.out.println("   - Want Excel reports? Add ExcelGenerator");
        System.out.println("   - Want multiple databases? Add new Repository");
        System.out.println("   - No need to modify existing classes!");
        
        System.out.println("\n========================================");
        System.out.println("           COMPARISON");
        System.out.println("========================================");
        
        System.out.println("\nBAD Example (One Class):");
        System.out.println("  - 1 class with 6 responsibilities");
        System.out.println("  - 6 reasons to change");
        System.out.println("  - Hard to test");
        System.out.println("  - High coupling");
        System.out.println("  - Cannot reuse parts");
        
        System.out.println("\nGOOD Example (SRP):");
        System.out.println("  - 6 classes, each with 1 responsibility");
        System.out.println("  - Each has 1 reason to change");
        System.out.println("  - Easy to test");
        System.out.println("  - Low coupling");
        System.out.println("  - All parts are reusable");
        
        System.out.println("\n========================================");
        System.out.println("         KEY TAKEAWAYS");
        System.out.println("========================================");
        System.out.println("\n1. Each class should have ONE responsibility");
        System.out.println("2. One responsibility = One reason to change");
        System.out.println("3. Separation makes code maintainable");
        System.out.println("4. Independent classes are easy to test");
        System.out.println("5. Low coupling enables flexibility");
        System.out.println("6. SRP is the foundation of good design");
        
        System.out.println("\n========================================");
        System.out.println("   This is the POWER of SRP! 🎉");
        System.out.println("========================================\n");
    }
}
