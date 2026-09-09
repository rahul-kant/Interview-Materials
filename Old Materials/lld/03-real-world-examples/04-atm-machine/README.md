# ATM Machine System - LLD Interview Problem

## 📋 Problem Statement

Design an ATM machine system that:
- Authenticates users with card and PIN
- Checks account balance
- Withdraws cash
- Deposits cash
- Transfers money between accounts
- Prints transaction receipts

## 🎯 Key Requirements

### Functional Requirements:
1. Card authentication
2. PIN verification (3 attempts max)
3. Balance inquiry
4. Cash withdrawal with denomination optimization
5. Cash deposit
6. Money transfer
7. Transaction history
8. Receipt printing

### Non-Functional Requirements:
1. Secure authentication
2. Thread-safe operations
3. Handle concurrent users
4. Transaction atomicity
5. Cash availability management

## 🏗️ Design Components

### Core Classes:
1. **ATM** - Main controller
2. **Card** - ATM card details
3. **Account** - Bank account
4. **Transaction** - Transaction records
5. **CashDispenser** - Manages cash inventory
6. **State Pattern** - ATM states

## 🎨 Design Patterns Used

1. **State Pattern** - ATM states
2. **Factory Pattern** - Transaction creation
3. **Singleton Pattern** - ATM instance
4. **Strategy Pattern** - Transaction strategies

## 💡 SOLID Principles Applied

- **SRP**: Separate classes for each responsibility
- **OCP**: Extensible for new transaction types
- **LSP**: All transactions implement Transaction interface
- **ISP**: Specific interfaces for operations
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. How to handle concurrent withdrawals?
2. PIN security and encryption
3. Transaction rollback mechanism
4. Cash denomination optimization
5. Network failure handling
6. Session timeout management
