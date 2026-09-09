# Notification Service (Pub-Sub) - LLD Interview Problem

## 📋 Problem Statement

Design a notification service that:
- Multiple notification channels (Email, SMS, Push)
- User preferences
- Topic-based subscriptions
- Retry logic for failures
- Batch notifications

## 🎯 Key Requirements

### Functional Requirements:
1. Subscribe/unsubscribe to topics
2. Send notifications
3. Multiple channels
4. User preferences
5. Notification history
6. Retry mechanism

### Non-Functional Requirements:
1. Asynchronous delivery
2. High throughput
3. Fault tolerant
4. Scalable

## 🏗️ Design Components

### Core Classes:
1. **NotificationService** - Main service
2. **Channel** - Email, SMS, Push
3. **Subscriber** - User subscription
4. **Topic** - Notification topic
5. **Message** - Notification message

## 🎨 Design Patterns Used

1. **Observer Pattern** - Pub-Sub model
2. **Strategy Pattern** - Different channels
3. **Template Method** - Send notification

## 💡 SOLID Principles Applied

- **SRP**: Separate channels
- **OCP**: Extensible channels
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. **Pub-Sub Model**:
   - Topic-based routing
   - Fan-out to subscribers
   - Message persistence

2. **Reliability**:
   - Retry logic
   - Dead letter queue
   - Acknowledgments

3. **Scalability**:
   - Message queue (Kafka/RabbitMQ)
   - Async processing
   - Rate limiting

## 📊 Complexity

- Subscribe: O(1)
- Publish: O(n) where n = subscribers
- Space: O(topics × subscribers)
