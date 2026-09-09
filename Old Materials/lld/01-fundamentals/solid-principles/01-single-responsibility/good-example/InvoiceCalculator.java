/**
 * InvoiceCalculator - Handles all invoice calculations
 * 
 * Single Responsibility: Calculate invoice totals, taxes, and discounts
 * 
 * Benefits:
 * - Easy to test calculation logic independently
 * - Can change tax rules without affecting other classes
 * - Can be reused for different types of invoices
 * - Clear, focused purpose
 */
public class InvoiceCalculator {
    
    /**
     * Calculates the subtotal (before tax) for an invoice
     * @param invoice The invoice to calculate
     * @return The subtotal amount
     */
    public double calculateSubtotal(Invoice invoice) {
        double subtotal = 0;
        for (InvoiceItem item : invoice.getItems()) {
            subtotal += item.getLineTotal();
        }
        return subtotal;
    }
    
    /**
     * Calculates the tax amount for an invoice
     * @param invoice The invoice to calculate
     * @return The tax amount
     */
    public double calculateTax(Invoice invoice) {
        double subtotal = calculateSubtotal(invoice);
        return subtotal * invoice.getTaxRate();
    }
    
    /**
     * Calculates the total (subtotal + tax) for an invoice
     * @param invoice The invoice to calculate
     * @return The total amount
     */
    public double calculateTotal(Invoice invoice) {
        return calculateSubtotal(invoice) + calculateTax(invoice);
    }
    
    /**
     * Applies a discount to the subtotal
     * @param invoice The invoice to calculate
     * @param discountPercentage Discount percentage (0.0 to 1.0)
     * @return The discounted total
     */
    public double calculateDiscountedTotal(Invoice invoice, double discountPercentage) {
        double subtotal = calculateSubtotal(invoice);
        double discountAmount = subtotal * discountPercentage;
        double discountedSubtotal = subtotal - discountAmount;
        double tax = discountedSubtotal * invoice.getTaxRate();
        return discountedSubtotal + tax;
    }
    
    /**
     * Gets invoice summary with all calculations
     * @param invoice The invoice to summarize
     * @return Formatted summary string
     */
    public String getInvoiceSummary(Invoice invoice) {
        StringBuilder summary = new StringBuilder();
        summary.append("Invoice Summary for: ").append(invoice.getInvoiceId()).append("\n");
        summary.append("Items: ").append(invoice.getItems().size()).append("\n");
        summary.append("Subtotal: $").append(String.format("%.2f", calculateSubtotal(invoice))).append("\n");
        summary.append("Tax: $").append(String.format("%.2f", calculateTax(invoice))).append("\n");
        summary.append("Total: $").append(String.format("%.2f", calculateTotal(invoice))).append("\n");
        return summary.toString();
    }
}
