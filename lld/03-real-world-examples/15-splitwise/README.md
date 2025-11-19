# Splitwise - Expense Sharing System

## 📋 Problem Statement

Design an expense sharing application like Splitwise that:
- Tracks shared expenses among users
- Splits bills equally or by percentage/amount
- Calculates who owes whom
- Simplifies debts (minimizes transactions)
- Supports groups
- Settles up payments

## 🎯 Key Requirements

### Functional Requirements:
1. Add expense with split types
2. Calculate balances between users
3. Show who owes whom
4. Settle up (record payment)
5. Simplify debts (minimize transactions)
6. Support groups
7. Different split types (equal, exact, percentage)
8. Expense history

### Non-Functional Requirements:
1. Accurate calculations
2. Handle concurrent transactions
3. Fast balance queries
4. Support many users/groups

## 🏗️ Design Components

### Core Classes:
1. **User** - User entity
2. **Expense** - Expense record
3. **Split** - How expense is split
4. **Group** - Group of users
5. **BalanceSheet** - Tracks balances
6. **SplitwiseSystem** - Main service
7. **Transaction** - Settlement record

## 🎨 Design Patterns Used

1. **Strategy Pattern** - Different split strategies
2. **Singleton Pattern** - Single system instance
3. **Factory Pattern** - Split creation
4. **Observer Pattern** - Notifications

## 💡 SOLID Principles Applied

- **SRP**: Separate split logic from balance calculation
- **OCP**: Extensible split types
- **LSP**: Split strategy substitution
- **ISP**: Specific interfaces

## 🔍 Interview Discussion Points

1. **Split Types**:
   - Equal: Amount / n
   - Exact: Specify each person's share
   - Percentage: Split by percentages

2. **Debt Simplification**:
   - Greedy algorithm
   - Min heap approach
   - Graph-based solution
   - Minimize transaction count

3. **Balance Calculation**:
   - Two-way mapping (A owes B, B owes A)
   - Net balance = owe - lent
   - Efficient queries with HashMap

4. **Scalability**:
   - Sharding by user
   - Event sourcing for history
   - CQRS for read/write separation

5. **Concurrency**:
   - Optimistic locking
   - Eventual consistency
   - Transaction queuing

## 📊 Algorithm: Simplify Debts

```
1. Calculate net balance for each user
2. Separate creditors (positive) and debtors (negative)
3. Use min heap/greedy to match max creditor with max debtor
4. Repeat until all balanced
```

Time: O(n log n), Space: O(n)

## 🔒 Edge Cases

- Floating point precision
- Circular debts
- Group member changes
- Partial settlements
