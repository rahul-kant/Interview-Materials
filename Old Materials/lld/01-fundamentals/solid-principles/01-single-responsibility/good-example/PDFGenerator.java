/**
 * PDFGenerator - Handles PDF document generation
 * 
 * Single Responsibility: Generate PDF documents
 * 
 * Benefits:
 * - Easy to change PDF library (iText, Apache PDFBox, etc.)
 * - Can be tested independently
 * - Can be reused for other PDF needs (reports, receipts, etc.)
 * - PDF format changes don't affect other classes
 */
public class PDFGenerator {
    
    private String templatePath;
    private String outputPath;
    
    public PDFGenerator(String templatePath, String outputPath) {
        this.templatePath = templatePath;
        this.outputPath = outputPath;
    }
    
    /**
     * Generates a PDF document for an invoice
     * @param invoice The invoice to generate PDF for
     * @param calculator Calculator to get totals
     * @return Path to the generated PDF
     */
    public String generateInvoicePDF(Invoice invoice, InvoiceCalculator calculator) {
        System.out.println("\n[PDF GENERATOR] Generating PDF...");
        System.out.println("Template: " + templatePath);
        
        String pdfFilePath = outputPath + invoice.getInvoiceId() + ".pdf";
        System.out.println("Output: " + pdfFilePath);
        
        try {
            // Simulate PDF generation steps
            System.out.println("Step 1: Creating PDF document...");
            System.out.println("Step 2: Adding header (Company logo, invoice number)...");
            System.out.println("Step 3: Adding customer information...");
            System.out.println("        Customer: " + invoice.getCustomerName());
            System.out.println("        Email: " + invoice.getCustomerEmail());
            System.out.println("        Date: " + invoice.getInvoiceDate());
            
            System.out.println("Step 4: Adding items table...");
            for (InvoiceItem item : invoice.getItems()) {
                System.out.println("        - " + item.getDescription() + 
                                 " (" + item.getQuantity() + " x $" + item.getPrice() + ")");
            }
            
            System.out.println("Step 5: Adding totals section...");
            System.out.println("        Subtotal: $" + String.format("%.2f", calculator.calculateSubtotal(invoice)));
            System.out.println("        Tax: $" + String.format("%.2f", calculator.calculateTax(invoice)));
            System.out.println("        Total: $" + String.format("%.2f", calculator.calculateTotal(invoice)));
            
            System.out.println("Step 6: Adding footer (terms and conditions)...");
            System.out.println("Step 7: Finalizing PDF document...");
            
            System.out.println("[PDF GENERATOR] PDF generated successfully!");
            System.out.println("Location: " + pdfFilePath);
            
            return pdfFilePath;
        } catch (Exception e) {
            System.out.println("[PDF GENERATOR ERROR] " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generates a generic PDF document
     * @param title Document title
     * @param content Document content
     * @param filename Output filename
     * @return Path to generated PDF
     */
    public String generatePDF(String title, String content, String filename) {
        System.out.println("\n[PDF GENERATOR] Generating PDF: " + filename);
        String pdfPath = outputPath + filename;
        System.out.println("[PDF GENERATOR] PDF created: " + pdfPath);
        return pdfPath;
    }
}
