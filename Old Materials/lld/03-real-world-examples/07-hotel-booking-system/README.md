# Hotel Booking System - LLD Interview Problem

## 📋 Problem Statement

Design a hotel booking system like Booking.com that:
- Allows searching hotels by location, dates, price
- Displays room availability
- Books rooms
- Manages reservations
- Processes payments
- Handles cancellations

## 🎯 Key Requirements

### Functional Requirements:
1. Search hotels by location, dates, price range
2. Filter by amenities, ratings
3. View room types and availability
4. Book single/multiple rooms
5. Modify bookings
6. Cancel bookings with refund policy
7. Payment processing
8. Generate booking confirmation
9. Review and rating system
10. Handle overbooking scenarios

### Non-Functional Requirements:
1. Handle concurrent bookings
2. Real-time availability
3. Scalability for multiple hotels
4. Transaction consistency
5. Data consistency across systems

## 🏗️ Design Components

### Core Classes:
1. **Hotel** - Hotel information
2. **Room** - Room details
3. **RoomType** - Standard, Deluxe, Suite
4. **Booking** - Reservation record
5. **Customer** - User information
6. **Payment** - Payment processing
7. **SearchService** - Hotel search
8. **AvailabilityManager** - Room availability

## 🎨 Design Patterns Used

1. **Factory Pattern** - Room type creation
2. **Strategy Pattern** - Pricing strategies
3. **Observer Pattern** - Availability notifications
4. **Builder Pattern** - Complex search queries
5. **Singleton Pattern** - Booking manager

## 💡 SOLID Principles Applied

- **SRP**: Separate search from booking
- **OCP**: Extensible room types and pricing
- **LSP**: Different room types
- **ISP**: Specific interfaces
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. How to handle concurrent room bookings?
2. Room locking mechanism
3. Overbooking strategy
4. Search optimization
5. Cancellation policies
6. Dynamic pricing implementation
7. Database schema design
