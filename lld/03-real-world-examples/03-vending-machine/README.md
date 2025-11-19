# Vending Machine System - LLD Interview Problem

## 📋 Problem Statement

Design a vending machine system that:
- Displays available products
- Accepts coins/notes
- Dispenses selected products
- Returns change
- Handles different states (idle, accepting money, dispensing)

## 🎯 Key Requirements

### Functional Requirements:
1. Display available products with prices
2. Accept multiple payment methods (coins, notes, card)
3. Select product
4. Dispense product
5. Return change
6. Handle inventory management
7. Handle different machine states

### Non-Functional Requirements:
1. Thread-safe operations
2. Handle concurrent requests
3. Maintain inventory
4. Track transactions

## 🏗️ Design Components

### Core Classes:
1. **VendingMachine** - Main controller
2. **Product** - Product information
3. **Inventory** - Manages stock
4. **Coin** - Payment denominations
5. **State Pattern** - Machine states
6. **PaymentProcessor** - Handle payments

## 🎨 Design Patterns Used

1. **State Pattern** - Machine states (Idle, Accepting Money, Dispensing)
2. **Singleton Pattern** - VendingMachine instance
3. **Factory Pattern** - Product creation
4. **Strategy Pattern** - Payment strategies

## 💡 SOLID Principles Applied

- **SRP**: Each class has single responsibility
- **OCP**: Extensible for new products/payment methods
- **LSP**: All states implement State interface
- **ISP**: Specific interfaces for different operations
- **DIP**: Depend on abstractions (State interface)

## 🔍 Interview Discussion Points

1. How to handle concurrent requests?
2. How to implement refund mechanism?
3. How to add new products dynamically?
4. How to handle partial payments?
5. Error handling strategies
6. Inventory restocking mechanism

## 🧪 Test Scenarios

1. Normal product purchase
2. Insufficient funds
3. Out of stock
4. Exact change scenario
5. Change return scenario
6. Multiple consecutive purchases
7. Cancel transaction
