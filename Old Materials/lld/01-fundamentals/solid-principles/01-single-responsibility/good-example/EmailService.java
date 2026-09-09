/**
 * EmailService - Handles email sending operations
 * 
 * Single Responsibility: Send emails
 * 
 * Benefits:
 * - Easy to change email provider (Gmail, SendGrid, etc.)
 * - Can be tested with mock email service
 * - Can be reused for other email needs (not just invoices)
 * - Clear, focused purpose
 */
public class EmailService {
    
    private String smtpServer;
    private int smtpPort;
    private String username;
    private String password;
    
    public EmailService(String smtpServer, int smtpPort, String username, String password) {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
    }
    
    /**
     * Sends an invoice email to the customer
     * @param invoice The invoice to send
     * @param calculator Calculator to get totals
     * @return true if sent successfully
     */
    public boolean sendInvoiceEmail(Invoice invoice, InvoiceCalculator calculator) {
        System.out.println("\n[EMAIL SERVICE] Sending invoice email...");
        System.out.println("SMTP: " + smtpServer + ":" + smtpPort);
        System.out.println("From: " + username);
        System.out.println("To: " + invoice.getCustomerEmail());
        
        try {
            String emailBody = generateInvoiceEmailBody(invoice, calculator);
            
            System.out.println("\n--- Email Content ---");
            System.out.println("Subject: Invoice " + invoice.getInvoiceId() + " from Company");
            System.out.println("--------------------");
            System.out.println(emailBody);
            System.out.println("--------------------");
            
            System.out.println("[EMAIL SERVICE] Email sent successfully!");
            return true;
        } catch (Exception e) {
            System.out.println("[EMAIL SERVICE ERROR] " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generates the email body for an invoice
     */
    private String generateInvoiceEmailBody(Invoice invoice, InvoiceCalculator calculator) {
        StringBuilder body = new StringBuilder();
        
        body.append("Dear ").append(invoice.getCustomerName()).append(",\n\n");
        body.append("Thank you for your business!\n\n");
        body.append("Please find your invoice details below:\n\n");
        body.append("Invoice #: ").append(invoice.getInvoiceId()).append("\n");
        body.append("Date: ").append(invoice.getInvoiceDate()).append("\n\n");
        
        body.append("Items:\n");
        body.append("----------------------------------------\n");
        for (InvoiceItem item : invoice.getItems()) {
            body.append(String.format("%-20s %2d x $%6.2f = $%7.2f\n",
                item.getDescription(),
                item.getQuantity(),
                item.getPrice(),
                item.getLineTotal()));
        }
        body.append("----------------------------------------\n\n");
        
        body.append("Subtotal: $").append(String.format("%.2f", calculator.calculateSubtotal(invoice))).append("\n");
        body.append("Tax: $").append(String.format("%.2f", calculator.calculateTax(invoice))).append("\n");
        body.append("----------------------------------------\n");
        body.append("TOTAL: $").append(String.format("%.2f", calculator.calculateTotal(invoice))).append("\n\n");
        
        body.append("Payment is due within 30 days.\n\n");
        body.append("If you have any questions, please contact us.\n\n");
        body.append("Best regards,\n");
        body.append("Accounting Department\n");
        body.append("Company Name\n");
        
        return body.toString();
    }
    
    /**
     * Sends a generic email
     * @param to Recipient email
     * @param subject Email subject
     * @param body Email body
     * @return true if sent successfully
     */
    public boolean sendEmail(String to, String subject, String body) {
        System.out.println("\n[EMAIL SERVICE] Sending email to: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("[EMAIL SERVICE] Email sent!");
        return true;
    }
}
