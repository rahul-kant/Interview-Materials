# Online Shopping System (E-Commerce) - LLD Interview Problem

## 📋 Problem Statement

Design an e-commerce system like Amazon that:
- Displays product catalog
- Manages shopping cart
- Processes orders
- Handles payments
- Manages inventory
- Tracks shipments

## 🎯 Key Requirements

### Functional Requirements:
1. User registration and authentication
2. Browse products by category
3. Search products
4. Add to cart
5. Apply coupons/discounts
6. Checkout and payment
7. Order tracking
8. Inventory management
9. Product reviews and ratings
10. Wishlist management
11. Seller management
12. Return and refund

### Non-Functional Requirements:
1. Handle concurrent orders
2. Inventory consistency
3. High availability
4. Scalability
5. Payment security

## 🏗️ Design Components

### Core Classes:
1. **User** - Customer/Seller
2. **Product** - Product details
3. **Category** - Product categories
4. **Cart** - Shopping cart
5. **Order** - Order information
6. **Payment** - Payment processing
7. **Inventory** - Stock management
8. **Shipment** - Delivery tracking
9. **Review** - Product reviews

## 🎨 Design Patterns Used

1. **Factory Pattern** - Product creation
2. **Strategy Pattern** - Payment methods, pricing
3. **Observer Pattern** - Order status notifications
4. **Decorator Pattern** - Discounts and offers
5. **Singleton Pattern** - Inventory manager
6. **Builder Pattern** - Complex order creation

## 💡 SOLID Principles Applied

- **SRP**: Separate cart from order management
- **OCP**: Extensible payment methods
- **LSP**: Different user types
- **ISP**: Specific interfaces
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. How to handle concurrent inventory updates?
2. Cart abandonment handling
3. Order fulfillment workflow
4. Payment gateway integration
5. Recommendation system
6. Search optimization
7. Database schema design
8. Scaling for millions of products
