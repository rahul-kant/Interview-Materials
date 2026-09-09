/**
 * PrintService - Handles printing operations
 * 
 * Single Responsibility: Print documents
 * 
 * Benefits:
 * - Easy to change printer configuration
 * - Can be tested independently
 * - Can be reused for other printing needs
 * - Printer changes don't affect other classes
 */
public class PrintService {
    
    private String printerName;
    
    public PrintService(String printerName) {
        this.printerName = printerName;
    }
    
    /**
     * Prints an invoice
     * @param invoice The invoice to print
     * @param calculator Calculator to get totals
     * @return true if printed successfully
     */
    public boolean printInvoice(Invoice invoice, InvoiceCalculator calculator) {
        System.out.println("\n[PRINT SERVICE] Printing invoice...");
        System.out.println("Printer: " + printerName);
        System.out.println("Connecting to printer...");
        
        try {
            // Simulate printing
            System.out.println("\n========================================");
            System.out.println("         INVOICE #" + invoice.getInvoiceId());
            System.out.println("========================================");
            System.out.println("Date: " + invoice.getInvoiceDate());
            System.out.println("Customer: " + invoice.getCustomerName());
            System.out.println("Email: " + invoice.getCustomerEmail());
            System.out.println("----------------------------------------");
            System.out.println("ITEMS:");
            System.out.println("----------------------------------------");
            
            for (InvoiceItem item : invoice.getItems()) {
                System.out.printf("%-20s %2d x $%6.2f = $%7.2f\n",
                    item.getDescription(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getLineTotal());
            }
            
            System.out.println("----------------------------------------");
            System.out.printf("Subtotal:%26s$%7.2f\n", "", calculator.calculateSubtotal(invoice));
            System.out.printf("Tax (%.0f%%):%26s$%7.2f\n", 
                invoice.getTaxRate() * 100, "", calculator.calculateTax(invoice));
            System.out.println("----------------------------------------");
            System.out.printf("TOTAL:%29s$%7.2f\n", "", calculator.calculateTotal(invoice));
            System.out.println("========================================");
            System.out.println("Payment due within 30 days");
            System.out.println("Thank you for your business!");
            System.out.println("========================================\n");
            
            System.out.println("[PRINT SERVICE] Invoice printed successfully!");
            return true;
        } catch (Exception e) {
            System.out.println("[PRINT SERVICE ERROR] " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Prints a generic document
     * @param content Content to print
     * @return true if printed successfully
     */
    public boolean print(String content) {
        System.out.println("\n[PRINT SERVICE] Printing to: " + printerName);
        System.out.println(content);
        System.out.println("[PRINT SERVICE] Document printed!");
        return true;
    }
}
