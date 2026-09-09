# Parking Lot System - Complete Implementation

## 🎯 Problem Statement

Design a parking lot system that can accommodate different types of vehicles. This is one of the **most frequently asked** LLD problems in interviews.

**Asked at:** Amazon, Microsoft, Google, Walmart, Uber

---

## 📋 Requirements

### Functional Requirements:
1. The parking lot has multiple floors
2. Each floor has multiple parking spots
3. Different types of spots: Small, Medium, Large, Handicapped
4. Different types of vehicles: Motorcycle, Car, Truck, Van
5. A vehicle can only park in a spot that fits its size
6. Issue a ticket when vehicle enters
7. Calculate fee when vehicle exits
8. Display available spots for each floor
9. Track entry and exit times

### Non-Functional Requirements:
1. System should handle concurrent parking requests
2. Efficient spot assignment algorithm
3. Easy to extend for new vehicle/spot types
4. Thread-safe operations

---

## 🏗️ Design Approach

### Step 1: Identify Core Entities

1. **ParkingLot** - The main system (Singleton)
2. **Floor** - Each floor in the parking lot
3. **ParkingSpot** - Individual parking spot (Abstract)
   - SmallSpot, MediumSpot, LargeSpot, HandicappedSpot
4. **Vehicle** - Base vehicle class (Abstract)
   - Motorcycle, Car, Truck, Van
5. **Ticket** - Entry/exit tracking
6. **FeeCalculator** - Calculate parking fees
7. **DisplayBoard** - Show available spots per floor

### Step 2: Design Patterns Used

1. **Singleton Pattern**: ParkingLot (only one instance)
2. **Factory Pattern**: Create vehicles and spots
3. **Strategy Pattern**: Different fee calculation strategies
4. **Abstract Class**: Vehicle and ParkingSpot hierarchies

### Step 3: SOLID Principles Applied

- **SRP**: Each class has single responsibility
- **OCP**: Easy to add new vehicle/spot types
- **LSP**: All vehicles/spots substitutable
- **ISP**: Focused interfaces
- **DIP**: Depend on abstractions

---

## 📊 Class Diagram

```
ParkingLot (Singleton)
├── List<Floor>
└── FeeCalculator

Floor
├── int floorNumber
├── List<ParkingSpot>
└── DisplayBoard

ParkingSpot (Abstract)
├── String spotId
├── SpotType type
├── boolean isAvailable
├── Vehicle vehicle
└── Subclasses:
    ├── SmallSpot
    ├── MediumSpot
    ├── LargeSpot
    └── HandicappedSpot

Vehicle (Abstract)
├── String licenseNumber
├── VehicleType type
└── Subclasses:
    ├── Motorcycle
    ├── Car
    ├── Truck
    └── Van

Ticket
├── String ticketId
├── Vehicle vehicle
├── ParkingSpot spot
├── LocalDateTime entryTime
└── LocalDateTime exitTime

FeeCalculator (Strategy)
└── calculateFee(Ticket) : double
```

---

## 💻 Implementation

See the following files for complete implementation:

### Core Classes:
1. **[VehicleType.java](VehicleType.java)** - Enum for vehicle types
2. **[SpotType.java](SpotType.java)** - Enum for spot types
3. **[Vehicle.java](Vehicle.java)** - Abstract vehicle class
4. **[Motorcycle.java](Motorcycle.java)** - Motorcycle implementation
5. **[Car.java](Car.java)** - Car implementation
6. **[Truck.java](Truck.java)** - Truck implementation
7. **[Van.java](Van.java)** - Van implementation
8. **[ParkingSpot.java](ParkingSpot.java)** - Abstract spot class
9. **[SmallSpot.java](SmallSpot.java)** - Small spot implementation
10. **[MediumSpot.java](MediumSpot.java)** - Medium spot implementation
11. **[LargeSpot.java](LargeSpot.java)** - Large spot implementation
12. **[HandicappedSpot.java](HandicappedSpot.java)** - Handicapped spot
13. **[Ticket.java](Ticket.java)** - Ticket class
14. **[FeeCalculator.java](FeeCalculator.java)** - Fee calculation
15. **[DisplayBoard.java](DisplayBoard.java)** - Display board
16. **[Floor.java](Floor.java)** - Floor management
17. **[ParkingLot.java](ParkingLot.java)** - Main system (Singleton)
18. **[ParkingLotDemo.java](ParkingLotDemo.java)** - Demo application

---

## 🔑 Key Design Decisions

### 1. Spot Assignment Strategy
```java
// Find nearest available spot on lowest floor
for (Floor floor : floors) {
    ParkingSpot spot = floor.findAvailableSpot(vehicle.getType());
    if (spot != null) {
        return spot;
    }
}
```

### 2. Fee Calculation
- Base rate: $2/hour
- Motorcycle: 50% discount
- Truck: 2x rate
- Weekend: 1.5x rate

### 3. Spot Size Mapping
- Motorcycle → Small spot
- Car → Medium spot
- Truck/Van → Large spot
- Any vehicle → Handicapped spot (if available)

---

## 🎯 Interview Discussion Points

### 1. Concurrency Handling
**Q: How do you handle two vehicles trying to park in the same spot?**

A: Use synchronized methods or locks:
```java
public synchronized Ticket parkVehicle(Vehicle vehicle) {
    // Thread-safe parking
}
```

### 2. Spot Assignment Algorithm
**Q: How do you optimize spot assignment?**

A: Multiple strategies:
- Nearest to entrance (minimize walking)
- Fill floor-by-floor (easier management)
- Balanced distribution (spread vehicles)

### 3. Extensibility
**Q: How to add electric vehicle charging spots?**

A:
```java
class ChargingSpot extends ParkingSpot {
    private boolean hasCharger;
    // Additional charging logic
}
```

### 4. Reservation System
**Q: Can users reserve spots in advance?**

A: Add reservation logic:
```java
class ParkingSpot {
    private LocalDateTime reservedUntil;
    public boolean isReserved() {
        return reservedUntil != null && 
               LocalDateTime.now().isBefore(reservedUntil);
    }
}
```

### 5. Payment Integration
**Q: How to handle multiple payment methods?**

A: Use Strategy pattern:
```java
interface PaymentStrategy {
    boolean processPayment(double amount);
}

class CreditCardPayment implements PaymentStrategy { }
class CashPayment implements PaymentStrategy { }
```

---

## 🧪 Test Scenarios

### Test Case 1: Basic Parking
```java
// Park a car
Car car = new Car("ABC-123");
Ticket ticket = parkingLot.parkVehicle(car);
// Verify spot assigned
// Verify ticket issued
```

### Test Case 2: Full Parking Lot
```java
// Fill all spots
// Try to park another vehicle
// Should return null or throw exception
```

### Test Case 3: Fee Calculation
```java
// Park vehicle
// Wait 3 hours (simulated)
// Remove vehicle
// Verify fee = $6 (3 hours * $2/hour)
```

### Test Case 4: Wrong Spot Size
```java
// Try to park truck in small spot
// Should fail
```

---

## 📈 Complexity Analysis

### Time Complexity:
- Park vehicle: O(F * S) where F = floors, S = spots per floor
- Remove vehicle: O(1) if we store spot reference in ticket
- Check availability: O(1) per floor

### Space Complexity:
- O(F * S) for storing all spots
- O(V) for storing active tickets (V = vehicles)

---

## 🚀 Extensions & Variations

### 1. Airport Parking
- Add long-term vs short-term rates
- Add different terminals

### 2. Mall Parking
- First 2 hours free
- Validation stamps from stores

### 3. Valet Parking
- Attendant parks the vehicle
- No spot selection by customer

### 4. Monthly Pass Holders
- Reserved spots
- Flat monthly fee

---

## 💡 Pro Tips for Interview

1. **Start with clarifying questions** - Don't assume requirements
2. **Draw the class diagram** before coding
3. **Discuss trade-offs** - Why this design over alternatives?
4. **Mention scalability** - How to handle 1000 floors?
5. **Consider edge cases** - Full parking, invalid vehicle, etc.
6. **Use design patterns appropriately** - Don't over-engineer

---

## 📝 Time Estimate

- Understanding requirements: 5-10 minutes
- Design discussion: 10-15 minutes
- Implementation: 30-40 minutes
- Testing & edge cases: 5-10 minutes

**Total: 50-75 minutes**

---

## ✅ Checklist for Interview

- [ ] Clarified all requirements
- [ ] Identified core entities
- [ ] Drew class relationships
- [ ] Applied design patterns
- [ ] Followed SOLID principles
- [ ] Handled edge cases
- [ ] Discussed concurrency
- [ ] Mentioned extensibility
- [ ] Wrote clean, readable code
- [ ] Explained design decisions

---

**Next:** Check the code files for complete working implementation!
