import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * BAD EXAMPLE - Violates Single Responsibility Principle
 * 
 * This Invoice class has MULTIPLE responsibilities:
 * 1. Storing invoice data (items, customer info)
 * 2. Calculating totals and taxes
 * 3. Saving to database
 * 4. Sending emails
 * 5. Generating PDF reports
 * 6. Printing invoices
 * 
 * Problems:
 * - If email service changes, Invoice class must change
 * - If database changes, Invoice class must change
 * - If PDF format changes, Invoice class must change
 * - If tax calculation changes, Invoice class must change
 * - Hard to test each responsibility independently
 * - High coupling - depends on many external systems
 * - Low cohesion - methods are not closely related
 */
public class Invoice {
    
    // Invoice data fields
    private String invoiceId;
    private String customerName;
    private String customerEmail;
    private List<InvoiceItem> items;
    private Date invoiceDate;
    private double taxRate;
    
    // Database connection fields (Responsibility 3)
    private String dbConnectionString;
    private String dbUsername;
    private String dbPassword;
    
    // Email service fields (Responsibility 4)
    private String smtpServer;
    private int smtpPort;
    private String emailUsername;
    private String emailPassword;
    
    // PDF settings (Responsibility 5)
    private String pdfTemplatePath;
    private String pdfOutputPath;
    
    public Invoice(String invoiceId, String customerName, String customerEmail, double taxRate) {
        this.invoiceId = invoiceId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.taxRate = taxRate;
        this.items = new ArrayList<>();
        this.invoiceDate = new Date();
        
        // Initialize database connection (mixed with data initialization!)
        this.dbConnectionString = "jdbc:mysql://localhost:3306/invoices";
        this.dbUsername = "admin";
        this.dbPassword = "password123";
        
        // Initialize email settings
        this.smtpServer = "smtp.gmail.com";
        this.smtpPort = 587;
        this.emailUsername = "invoices@company.com";
        this.emailPassword = "emailpass123";
        
        // Initialize PDF settings
        this.pdfTemplatePath = "/templates/invoice_template.pdf";
        this.pdfOutputPath = "/output/invoices/";
    }
    
    public void addItem(InvoiceItem item) {
        items.add(item);
    }
    
    // RESPONSIBILITY 2: Calculate totals and taxes
    // If tax rules change, this class must change
    public double calculateSubtotal() {
        double subtotal = 0;
        for (InvoiceItem item : items) {
            subtotal += item.getPrice() * item.getQuantity();
        }
        return subtotal;
    }
    
    public double calculateTax() {
        return calculateSubtotal() * taxRate;
    }
    
    public double calculateTotal() {
        return calculateSubtotal() + calculateTax();
    }
    
    // RESPONSIBILITY 3: Database operations
    // If database schema changes, this class must change
    // If we switch databases, this class must change
    public void saveToDatabase() {
        System.out.println("\n[DATABASE] Saving invoice to database...");
        System.out.println("Connecting to: " + dbConnectionString);
        System.out.println("Username: " + dbUsername);
        
        // Simulate database connection and save
        try {
            // In real code, this would be JDBC/Hibernate operations
            System.out.println("INSERT INTO invoices VALUES ('" + invoiceId + "', '" + 
                             customerName + "', " + calculateTotal() + ")");
            
            for (InvoiceItem item : items) {
                System.out.println("INSERT INTO invoice_items VALUES ('" + invoiceId + "', '" + 
                                 item.getDescription() + "', " + item.getPrice() + ", " + 
                                 item.getQuantity() + ")");
            }
            
            System.out.println("[DATABASE] Invoice saved successfully!");
        } catch (Exception e) {
            System.out.println("[DATABASE ERROR] Failed to save invoice: " + e.getMessage());
        }
    }
    
    // RESPONSIBILITY 4: Email operations
    // If email service changes, this class must change
    // If email template changes, this class must change
    public void sendEmail() {
        System.out.println("\n[EMAIL] Sending invoice email...");
        System.out.println("SMTP Server: " + smtpServer + ":" + smtpPort);
        System.out.println("From: " + emailUsername);
        System.out.println("To: " + customerEmail);
        
        // Simulate email sending
        try {
            // In real code, this would use JavaMail API
            String emailBody = generateEmailBody();
            System.out.println("\n--- Email Content ---");
            System.out.println(emailBody);
            System.out.println("--------------------");
            System.out.println("[EMAIL] Invoice sent successfully to " + customerEmail);
        } catch (Exception e) {
            System.out.println("[EMAIL ERROR] Failed to send email: " + e.getMessage());
        }
    }
    
    private String generateEmailBody() {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(customerName).append(",\n\n");
        body.append("Thank you for your business!\n\n");
        body.append("Invoice #: ").append(invoiceId).append("\n");
        body.append("Date: ").append(invoiceDate).append("\n\n");
        body.append("Items:\n");
        
        for (InvoiceItem item : items) {
            body.append("  - ").append(item.getDescription())
                .append(" (").append(item.getQuantity()).append(" x $")
                .append(item.getPrice()).append(")\n");
        }
        
        body.append("\nSubtotal: $").append(String.format("%.2f", calculateSubtotal())).append("\n");
        body.append("Tax: $").append(String.format("%.2f", calculateTax())).append("\n");
        body.append("Total: $").append(String.format("%.2f", calculateTotal())).append("\n");
        body.append("\nPlease pay within 30 days.\n\nBest regards,\nAccounting Department");
        
        return body.toString();
    }
    
    // RESPONSIBILITY 5: PDF generation
    // If PDF library changes, this class must change
    // If PDF format changes, this class must change
    public void generatePDF() {
        System.out.println("\n[PDF] Generating PDF invoice...");
        System.out.println("Template: " + pdfTemplatePath);
        System.out.println("Output: " + pdfOutputPath + invoiceId + ".pdf");
        
        // Simulate PDF generation
        try {
            // In real code, this would use iText or Apache PDFBox
            System.out.println("Creating PDF document...");
            System.out.println("Adding invoice header...");
            System.out.println("Adding items table...");
            System.out.println("Adding totals...");
            System.out.println("Adding footer...");
            System.out.println("[PDF] Invoice PDF generated successfully!");
        } catch (Exception e) {
            System.out.println("[PDF ERROR] Failed to generate PDF: " + e.getMessage());
        }
    }
    
    // RESPONSIBILITY 6: Printing
    // If printer settings change, this class must change
    public void printInvoice() {
        System.out.println("\n[PRINT] Printing invoice...");
        System.out.println("Connecting to default printer...");
        
        // Simulate printing
        try {
            System.out.println("\n========================================");
            System.out.println("         INVOICE #" + invoiceId);
            System.out.println("========================================");
            System.out.println("Customer: " + customerName);
            System.out.println("Date: " + invoiceDate);
            System.out.println("----------------------------------------");
            
            for (InvoiceItem item : items) {
                System.out.printf("%-20s %2d x $%6.2f = $%7.2f\n",
                    item.getDescription(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getPrice() * item.getQuantity());
            }
            
            System.out.println("----------------------------------------");
            System.out.printf("Subtotal:%26s$%7.2f\n", "", calculateSubtotal());
            System.out.printf("Tax (%.0f%%):%26s$%7.2f\n", taxRate * 100, "", calculateTax());
            System.out.printf("TOTAL:%29s$%7.2f\n", "", calculateTotal());
            System.out.println("========================================");
            System.out.println("[PRINT] Invoice printed successfully!");
        } catch (Exception e) {
            System.out.println("[PRINT ERROR] Failed to print invoice: " + e.getMessage());
        }
    }
    
    // Getters
    public String getInvoiceId() {
        return invoiceId;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public List<InvoiceItem> getItems() {
        return items;
    }
}

/**
 * Simple class to represent an invoice item
 */
class InvoiceItem {
    private String description;
    private double price;
    private int quantity;
    
    public InvoiceItem(String description, double price, int quantity) {
        this.description = description;
        this.price = price;
        this.quantity = quantity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public double getPrice() {
        return price;
    }
    
    public int getQuantity() {
        return quantity;
    }
}
