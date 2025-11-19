# Ride Sharing System (Uber/Lyft) - LLD Interview Problem

## 📋 Problem Statement

Design a ride-sharing system like Uber that:
- Matches riders with drivers
- Calculates fares
- Tracks real-time location
- Handles payments
- Manages ratings
- Supports multiple ride types

## 🎯 Key Requirements

### Functional Requirements:
1. User registration (Rider/Driver)
2. Request ride with pickup/drop location
3. Match driver based on proximity
4. Real-time location tracking
5. Calculate fare (distance, time, surge pricing)
6. Multiple ride types (UberX, UberXL, Pool)
7. Payment processing
8. Trip history
9. Rating and review system
10. Driver availability management

### Non-Functional Requirements:
1. Low latency matching
2. Real-time location updates
3. High availability
4. Scalability for multiple cities
5. Handle concurrent ride requests

## 🏗️ Design Components

### Core Classes:
1. **User** - Base user class
2. **Rider** - Passenger
3. **Driver** - Driver with vehicle
4. **Ride** - Ride information
5. **Location** - GPS coordinates
6. **PricingStrategy** - Fare calculation
7. **MatchingService** - Driver-rider matching
8. **RideType** - Different ride categories
9. **Payment** - Payment processing

## 🎨 Design Patterns Used

1. **Strategy Pattern** - Pricing strategies, matching algorithms
2. **Observer Pattern** - Location updates
3. **Factory Pattern** - Ride type creation
4. **State Pattern** - Ride states (Requested, Accepted, InProgress, Completed)
5. **Singleton Pattern** - Matching service

## 💡 SOLID Principles Applied

- **SRP**: Separate matching from pricing
- **OCP**: Extensible ride types and pricing
- **LSP**: Different user types
- **ISP**: Specific interfaces for riders/drivers
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. Driver-rider matching algorithm
2. Surge pricing implementation
3. Real-time location tracking
4. How to handle peak hours?
5. Ride sharing (pooling) implementation
6. ETA calculation
7. Cancellation policies
8. Database design for location data
