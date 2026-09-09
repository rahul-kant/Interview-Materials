/**
 * InvoiceItem - Represents a single item in an invoice
 * 
 * Simple data class with single responsibility:
 * - Store information about an invoice line item
 */
public class InvoiceItem {
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
    
    public double getLineTotal() {
        return price * quantity;
    }
    
    @Override
    public String toString() {
        return "InvoiceItem{" +
                "description='" + description + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
