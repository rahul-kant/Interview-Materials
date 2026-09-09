# Elevator System - LLD Interview Problem

## 📋 Problem Statement

Design an elevator control system for a multi-floor building that:
- Efficiently serves multiple floor requests
- Optimizes elevator movement
- Handles multiple elevators
- Minimizes wait time
- Handles emergency scenarios

## 🎯 Key Requirements

### Functional Requirements:
1. Multiple elevators in building
2. Request elevator from any floor
3. Select destination floor
4. Display current floor and direction
5. Open/close doors
6. Emergency stop
7. Handle up/down requests
8. Load balancing across elevators

### Non-Functional Requirements:
1. Optimal scheduling algorithm
2. Energy efficiency
3. Minimize wait time
4. Handle peak hours
5. Safety constraints

## 🏗️ Design Components

### Core Classes:
1. **ElevatorSystem** - Controller
2. **Elevator** - Individual elevator
3. **Request** - Floor request
4. **Direction** - UP/DOWN/IDLE
5. **Scheduler** - Request scheduling
6. **Door** - Door control

## 🎨 Design Patterns Used

1. **Strategy Pattern** - Scheduling algorithms (FCFS, SCAN, LOOK)
2. **State Pattern** - Elevator states
3. **Observer Pattern** - Floor displays
4. **Singleton Pattern** - System controller

## 💡 SOLID Principles Applied

- **SRP**: Separate scheduling from elevator control
- **OCP**: Extensible scheduling algorithms
- **LSP**: Algorithm substitution
- **ISP**: Specific interfaces
- **DIP**: Depend on scheduler interface

## 🔍 Interview Discussion Points

1. Scheduling algorithms (FCFS, SCAN, LOOK)
2. How to handle peak hours?
3. Emergency scenarios
4. Load balancing strategy
5. Energy optimization
6. Concurrent request handling
