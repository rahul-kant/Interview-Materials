/**
 * InvoiceRepository - Handles database operations for invoices
 * 
 * Single Responsibility: Persist and retrieve invoices from database
 * 
 * Benefits:
 * - Easy to change database implementation
 * - Can switch from MySQL to PostgreSQL without affecting other classes
 * - Easy to test with mock database
 * - Can be reused for different invoice types
 */
public class InvoiceRepository {
    
    private String dbConnectionString;
    private String dbUsername;
    private String dbPassword;
    
    public InvoiceRepository(String dbConnectionString, String dbUsername, String dbPassword) {
        this.dbConnectionString = dbConnectionString;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }
    
    /**
     * Saves an invoice to the database
     * @param invoice The invoice to save
     * @param calculator Calculator to get total amount
     * @return true if saved successfully
     */
    public boolean save(Invoice invoice, InvoiceCalculator calculator) {
        System.out.println("\n[REPOSITORY] Saving invoice to database...");
        System.out.println("Connection: " + dbConnectionString);
        System.out.println("User: " + dbUsername);
        
        try {
            // Simulate database operations
            System.out.println("Executing SQL:");
            System.out.println("  INSERT INTO invoices (id, customer, email, date, total)");
            System.out.println("  VALUES ('" + invoice.getInvoiceId() + "', '" + 
                             invoice.getCustomerName() + "', '" +
                             invoice.getCustomerEmail() + "', '" +
                             invoice.getInvoiceDate() + "', " +
                             calculator.calculateTotal(invoice) + ")");
            
            // Save invoice items
            for (InvoiceItem item : invoice.getItems()) {
                System.out.println("  INSERT INTO invoice_items (invoice_id, description, price, quantity)");
                System.out.println("  VALUES ('" + invoice.getInvoiceId() + "', '" +
                                 item.getDescription() + "', " +
                                 item.getPrice() + ", " +
                                 item.getQuantity() + ")");
            }
            
            System.out.println("[REPOSITORY] Invoice saved successfully!");
            return true;
        } catch (Exception e) {
            System.out.println("[REPOSITORY ERROR] Failed to save: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Retrieves an invoice from the database
     * @param invoiceId The invoice ID to retrieve
     * @return The invoice (simulated)
     */
    public Invoice findById(String invoiceId) {
        System.out.println("\n[REPOSITORY] Retrieving invoice: " + invoiceId);
        System.out.println("Executing SQL: SELECT * FROM invoices WHERE id = '" + invoiceId + "'");
        // In real implementation, would query database and construct Invoice object
        System.out.println("[REPOSITORY] Invoice retrieved");
        return null; // Simplified for demo
    }
    
    /**
     * Deletes an invoice from the database
     * @param invoiceId The invoice ID to delete
     * @return true if deleted successfully
     */
    public boolean delete(String invoiceId) {
        System.out.println("\n[REPOSITORY] Deleting invoice: " + invoiceId);
        System.out.println("Executing SQL: DELETE FROM invoices WHERE id = '" + invoiceId + "'");
        System.out.println("[REPOSITORY] Invoice deleted");
        return true;
    }
}
