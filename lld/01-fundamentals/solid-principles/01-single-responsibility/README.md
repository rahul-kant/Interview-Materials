# Single Responsibility Principle (SRP)

## 📖 Definition

> "A class should have one, and only one, reason to change."
> — Robert C. Martin

In simpler terms: **Each class should have only ONE job or responsibility.**

## 🎯 What Does This Mean?

A class should focus on doing **one thing well**. If you need to change a class for multiple different reasons, it's violating SRP.

### Example Questions to Ask:
- Does this class have more than one reason to change?
- Is this class doing too many things?
- Can I split this class into smaller, focused classes?

## ❌ Why Violating SRP is Bad

### Problems with Multiple Responsibilities:

1. **Hard to Maintain**
   - Changes in one responsibility can break another
   - Difficult to understand what the class does

2. **Hard to Test**
   - Need to test multiple behaviors in one class
   - Tests become complex and fragile

3. **Low Reusability**
   - Can't reuse one part without bringing everything else

4. **High Coupling**
   - Class depends on multiple things
   - Changes ripple through the codebase

## 💡 Real-World Analogy

Think of a restaurant:

**Bad (Violates SRP):**
- One person: Chef + Waiter + Cashier + Cleaner
- If they're sick, everything stops!
- They might be good at cooking but bad at cleaning

**Good (Follows SRP):**
- Chef: Cooks food
- Waiter: Serves customers
- Cashier: Handles payments
- Cleaner: Maintains hygiene
- Each person has ONE clear responsibility

## 📝 Example: Invoice System

### Scenario:
We're building an invoice management system. An invoice needs to:
1. Store invoice data (amount, customer, items)
2. Calculate total with taxes
3. Save to database
4. Send email to customer
5. Generate PDF report
6. Print invoice

### Bad Example (Violates SRP):
A single `Invoice` class doing EVERYTHING - 6 different responsibilities!

**Problems:**
- If email service changes, Invoice class changes
- If database schema changes, Invoice class changes
- If PDF format changes, Invoice class changes
- If tax calculation changes, Invoice class changes
- Hard to test each responsibility independently
- Hard to reuse just one part

### Good Example (Follows SRP):
Separate classes, each with ONE responsibility:
- `Invoice` - Stores invoice data
- `InvoiceCalculator` - Calculates totals/taxes
- `InvoiceRepository` - Saves to database
- `EmailService` - Sends emails
- `PDFGenerator` - Generates PDF
- `PrintService` - Prints invoice

**Benefits:**
- Each class has one reason to change
- Easy to test each class independently
- Easy to replace/modify one part without affecting others
- Classes are reusable in other contexts

## 🔍 How to Identify SRP Violations

### Warning Signs:
1. **Class name has "And", "Or", "Manager"**
   - `UserAndOrderManager` ❌
   - Better: `UserManager`, `OrderManager` ✅

2. **Too many dependencies**
   - Class imports many different types of classes
   - Needs multiple external services

3. **Large classes**
   - More than 200-300 lines
   - Many methods doing different things

4. **Multiple reasons to change**
   - Ask: "Why would this class change?"
   - If more than one answer, it violates SRP

5. **Hard to name the class**
   - If you struggle to give it a clear, single-purpose name
   - Names that are too generic or too long

## ✅ How to Apply SRP

### Step-by-Step Approach:

1. **Identify Responsibilities**
   - List all things the class does
   - Group related behaviors

2. **Extract to New Classes**
   - Create new class for each responsibility
   - Give clear, descriptive names

3. **Define Clear Interfaces**
   - Each class should have clear public methods
   - Hide implementation details

4. **Compose Objects**
   - Original class uses new classes
   - Delegation instead of doing everything

## 📊 Before and After Comparison

### Before (Bad Example):
```
Invoice
├── Data (fields)
├── Calculate()
├── SaveToDatabase()
├── SendEmail()
├── GeneratePDF()
└── Print()

6 Responsibilities = 6 Reasons to Change ❌
```

### After (Good Example):
```
Invoice (Data only)
InvoiceCalculator
InvoiceRepository
EmailService
PDFGenerator
PrintService

Each has 1 Responsibility = 1 Reason to Change ✅
```

## 🎓 Practice Exercise

Look at this class and identify SRP violations:

```java
class UserAccount {
    private String username;
    private String password;
    
    public void createUser() { }
    public void deleteUser() { }
    public void sendWelcomeEmail() { }
    public void logActivity() { }
    public void generateReport() { }
    public void backupData() { }
}
```

### Questions:
1. How many responsibilities does this class have?
2. What would you name the new classes?
3. How would you refactor this?

### Answer:
**5 Responsibilities:**
1. User Management (create, delete)
2. Email Service (send welcome email)
3. Activity Logging
4. Report Generation
5. Data Backup

**Refactored Classes:**
- `User` - User data
- `UserService` - Create/delete users
- `EmailService` - Send emails
- `ActivityLogger` - Log activities
- `ReportGenerator` - Generate reports
- `BackupService` - Backup data

## 🔑 Key Takeaways

✅ **One class, one responsibility**
✅ **One reason to change**
✅ **High cohesion within class**
✅ **Low coupling between classes**
✅ **Easy to test and maintain**
✅ **Better code organization**

## 💡 Interview Tips

### Common Questions:
**Q: What is Single Responsibility Principle?**
A: A class should have only one reason to change. Each class should focus on doing one thing well.

**Q: How do you identify SRP violations?**
A: Look for classes with multiple responsibilities, many dependencies, large size, or multiple reasons to change.

**Q: Doesn't SRP create too many small classes?**
A: Yes, you'll have more classes, but each is simpler, more focused, and easier to maintain. Benefits outweigh the cost.

**Q: Give a real-world example of SRP.**
A: Provide the Invoice example or restaurant analogy (see above).

## 📁 Code Examples

- `bad-example/` - Shows what NOT to do (violates SRP)
- `good-example/` - Shows the correct approach (follows SRP)

Study both to understand the difference!

## ➡️ Next Steps

1. Review the bad example to see violations
2. Study the good example to see the solution
3. Compare the differences
4. Try refactoring your own code to follow SRP
5. Move on to Open/Closed Principle

---

**Remember**: SRP is about **organizing code logically** so that each piece has a clear, single purpose. This makes your code more maintainable and testable!
