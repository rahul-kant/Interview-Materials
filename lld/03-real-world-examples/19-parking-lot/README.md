# Parking Lot System - LLD Interview Problem

## 📋 Problem Statement

Design a parking lot system that:
- Supports multiple vehicle types (Car, Bike, Truck)
- Multiple parking spot sizes (Compact, Large, Handicapped)
- Handles entry and exit
- Calculates parking fees
- Tracks available spots
- Nearest spot allocation

## 🎯 Key Requirements

### Functional Requirements:
1. Multiple vehicle types
2. Different spot types
3. Find available spot
4. Park vehicle
5. Remove vehicle (exit)
6. Calculate fees
7. Display availability
8. Multiple floors support

### Non-Functional Requirements:
1. Thread-safe operations
2. Efficient spot allocation
3. Real-time availability
4. Scalable

## 🏗️ Design Components

### Core Classes:
1. **Vehicle** - Base vehicle class
2. **ParkingSpot** - Spot entity
3. **ParkingLot** - Main system
4. **Ticket** - Parking ticket
5. **FeeCalculator** - Calculate charges

## 🎨 Design Patterns Used

1. **Singleton Pattern** - Single parking lot
2. **Strategy Pattern** - Fee calculation
3. **Factory Pattern** - Vehicle/Spot creation

## 💡 SOLID Principles Applied

- **SRP**: Separate vehicle, spot, fee logic
- **OCP**: Extensible vehicle types
- **LSP**: Vehicle hierarchy

## 🔍 Interview Discussion Points

1. **Spot Allocation**:
   - First available
   - Nearest to entrance
   - By floor
   - Priority (handicapped)

2. **Concurrency**:
   - Thread-safe booking
   - Prevent double booking
   - Synchronized methods

3. **Fee Calculation**:
   - Hourly rates
   - Vehicle type based
   - Spot type based
   - Peak hour pricing

4. **Scalability**:
   - Multiple floors
   - Multiple entrances
   - Distributed system

## 📊 Algorithm

```
Park Vehicle:
1. Find available spot for vehicle type
2. If found:
   - Allocate spot
   - Generate ticket
   - Return ticket
3. Else: No spot available

Unpark Vehicle:
1. Get ticket
2. Calculate fee
3. Free spot
4. Return fee
```

## 🔒 Vehicle-Spot Mapping

- Bike → Compact/Large
- Car → Compact/Large
- Truck → Large only
