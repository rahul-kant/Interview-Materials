import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * GOOD EXAMPLE - Follows Single Responsibility Principle
 * 
 * This Invoice class now has ONLY ONE responsibility:
 * - Storing invoice data (the invoice's state)
 * 
 * All other responsibilities have been extracted to separate classes:
 * - InvoiceCalculator: Handles calculations
 * - InvoiceRepository: Handles database operations
 * - EmailService: Handles email sending
 * - PDFGenerator: Handles PDF generation
 * - PrintService: Handles printing
 * 
 * Benefits:
 * - Invoice only changes if invoice data structure changes
 * - Each responsibility can be tested independently
 * - Easy to modify one aspect without affecting others
 * - Components are reusable in other contexts
 * - Better separation of concerns
 */
public class Invoice {
    
    // Invoice data fields - This is the ONLY responsibility now
    private String invoiceId;
    private String customerName;
    private String customerEmail;
    private List<InvoiceItem> items;
    private Date invoiceDate;
    private double taxRate;
    
    public Invoice(String invoiceId, String customerName, String customerEmail, double taxRate) {
        this.invoiceId = invoiceId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.taxRate = taxRate;
        this.items = new ArrayList<>();
        this.invoiceDate = new Date();
    }
    
    // Only methods related to managing invoice data
    public void addItem(InvoiceItem item) {
        items.add(item);
    }
    
    public void removeItem(InvoiceItem item) {
        items.remove(item);
    }
    
    // Getters - providing access to invoice data
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
        return new ArrayList<>(items); // Return copy to maintain encapsulation
    }
    
    public Date getInvoiceDate() {
        return new Date(invoiceDate.getTime()); // Return copy to maintain encapsulation
    }
    
    public double getTaxRate() {
        return taxRate;
    }
    
    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceId='" + invoiceId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", items=" + items.size() +
                ", invoiceDate=" + invoiceDate +
                ", taxRate=" + taxRate +
                '}';
    }
}
