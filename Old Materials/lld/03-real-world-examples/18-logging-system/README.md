# Logging System - LLD Interview Problem

## 📋 Problem Statement

Design a logging system like Log4j that:
- Multiple log levels (DEBUG, INFO, WARN, ERROR)
- Log to console and file
- Configurable format
- Thread-safe
- Log rotation
- Performance optimized

## 🎯 Key Requirements

### Functional Requirements:
1. Log with different levels
2. Multiple outputs (console, file)
3. Configurable log format
4. Timestamp inclusion
5. Thread information
6. Log rotation (size-based)
7. Async logging support

### Non-Functional Requirements:
1. Thread-safe operations
2. Minimal performance impact
3. Configurable at runtime
4. Buffered I/O

## 🏗️ Design Components

### Core Classes:
1. **Logger** - Main logger class
2. **LogLevel** - Enum for levels
3. **LogAppender** - Output interface
4. **ConsoleAppender** - Console output
5. **FileAppender** - File output
6. **LogFormatter** - Format messages

## 🎨 Design Patterns Used

1. **Singleton Pattern** - Single logger instance
2. **Observer Pattern** - Multiple appenders
3. **Strategy Pattern** - Different formatters
4. **Chain of Responsibility** - Log level filtering

## 💡 SOLID Principles Applied

- **SRP**: Separate logging, appending, formatting
- **OCP**: Extensible appenders
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. **Thread Safety**:
   - Synchronized methods
   - Concurrent collections
   - Lock-free designs

2. **Performance**:
   - Async logging with queue
   - Buffered I/O
   - Lazy evaluation
   - Log level checks

3. **Log Rotation**:
   - Size-based rotation
   - Time-based rotation
   - Compression
   - Archiving

4. **Format**:
   - Custom patterns
   - JSON format
   - Structured logging

## 📊 Algorithm

```
log(level, message):
  1. Check if level enabled
  2. Format message with timestamp
  3. For each appender:
     - Write formatted message
  4. Flush if needed
```

## 🔒 Concurrency

- Synchronized write methods
- Thread-safe queue for async
- ReentrantLock for file access
