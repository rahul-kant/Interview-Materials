# Movie Ticket Booking System - LLD Interview Problem

## 📋 Problem Statement

Design a movie ticket booking system like BookMyShow that:
- Displays available movies and shows
- Allows seat selection
- Handles concurrent bookings
- Processes payments
- Sends booking confirmation
- Manages cancellations

## 🎯 Key Requirements

### Functional Requirements:
1. Browse movies by city/theater
2. View show timings
3. Select seats (seat map visualization)
4. Handle concurrent seat booking
5. Process payment
6. Generate booking confirmation
7. Cancel booking with refund
8. Apply coupons/offers
9. Manage theaters and screens

### Non-Functional Requirements:
1. Handle concurrent seat selection (locking mechanism)
2. High availability
3. Scalability for multiple cities
4. Transaction consistency
5. Seat hold time limit (10 minutes)

## 🏗️ Design Components

### Core Classes:
1. **Theater** - Cinema complex
2. **Screen** - Individual screen
3. **Movie** - Movie details
4. **Show** - Movie showing
5. **Seat** - Individual seat
6. **Booking** - Booking record
7. **Payment** - Payment processing
8. **SeatLock** - Concurrency control

## 🎨 Design Patterns Used

1. **Observer Pattern** - Seat availability updates
2. **Factory Pattern** - Booking creation
3. **Strategy Pattern** - Payment methods
4. **Singleton Pattern** - Booking system
5. **State Pattern** - Booking states

## 💡 SOLID Principles Applied

- **SRP**: Separate seat management from booking
- **OCP**: Extensible payment methods
- **LSP**: Different seat types
- **ISP**: Specific interfaces for operations
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. How to handle concurrent seat booking?
2. Seat locking mechanism
3. Payment failure handling
4. Seat hold timeout
5. Database schema design
6. Scaling for multiple cities
7. Cancellation and refund policy
