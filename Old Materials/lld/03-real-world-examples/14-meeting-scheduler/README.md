# Meeting Scheduler (Calendar) - LLD Interview Problem

## 📋 Problem Statement

Design a meeting scheduler system like Google Calendar that:
- Schedules meetings for users
- Checks availability
- Handles conflicts
- Supports recurring meetings
- Sends notifications
- Manages meeting rooms

## 🎯 Key Requirements

### Functional Requirements:
1. Create/update/delete meetings
2. Check user availability
3. Find common free slots
4. Handle meeting conflicts
5. Send meeting invites
6. Accept/decline invitations
7. Recurring meetings support
8. Meeting room booking
9. Notifications/reminders

### Non-Functional Requirements:
1. Handle concurrent bookings
2. Prevent double booking
3. Fast availability checks
4. Scalable (millions of users)
5. Real-time updates

## 🏗️ Design Components

### Core Classes:
1. **Meeting** - Meeting entity
2. **User** - User with calendar
3. **Calendar** - User's calendar
4. **TimeSlot** - Time interval
5. **MeetingScheduler** - Main service
6. **MeetingRoom** - Conference room
7. **Notification** - Meeting notifications

## 🎨 Design Patterns Used

1. **Singleton Pattern** - Single scheduler instance
2. **Observer Pattern** - Meeting notifications
3. **Strategy Pattern** - Conflict resolution
4. **Factory Pattern** - Meeting creation

## 💡 SOLID Principles Applied

- **SRP**: Separate scheduling from notifications
- **OCP**: Extensible conflict resolution
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. **Conflict Detection**:
   - Interval overlap algorithm
   - O(n log n) with sorting
   - Interval tree for fast queries

2. **Finding Common Slots**:
   - Merge all busy intervals
   - Find gaps between intervals
   - Handle multiple attendees

3. **Scalability**:
   - Sharding by user/time
   - Caching busy/free info
   - Event-driven architecture

4. **Recurring Meetings**:
   - Daily, weekly, monthly patterns
   - Exception handling
   - End conditions

5. **Double Booking Prevention**:
   - Optimistic locking
   - Distributed locks (Redis)
   - Transaction management

## 📊 Algorithm

### Check Availability:
```
1. Get all meetings for user in time range
2. Sort by start time
3. Check for overlaps with new meeting
4. Return true if no conflicts
```

### Find Common Free Slots:
```
1. Get busy intervals for all attendees
2. Merge overlapping intervals
3. Find gaps >= meeting duration
4. Return free slots
```

## 🔒 Concurrency Handling

- Pessimistic locking for booking
- Optimistic locking with version numbers
- Compare-and-swap for room booking
- Queue for conflict resolution
