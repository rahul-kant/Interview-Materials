# Rate Limiter System - LLD Interview Problem

## 📋 Problem Statement

Design a rate limiter that:
- Limits number of requests per user/IP
- Supports multiple algorithms
- Handles distributed systems
- Configurable time windows
- Provides clear feedback when limit exceeded

## 🎯 Key Requirements

### Functional Requirements:
1. Allow/deny request based on rate limit
2. Configure limits (requests per second/minute/hour)
3. Support user-based and IP-based limiting
4. Multiple rate limiting algorithms
5. Handle distributed rate limiting
6. Provide remaining quota information

### Non-Functional Requirements:
1. Low latency
2. Thread-safe
3. Memory efficient
4. Scalable
5. Accurate counting

## 🏗️ Design Components

### Core Classes:
1. **RateLimiter** - Main interface
2. **TokenBucketLimiter** - Token bucket algorithm
3. **SlidingWindowLimiter** - Sliding window algorithm
4. **FixedWindowLimiter** - Fixed window algorithm
5. **LeakyBucketLimiter** - Leaky bucket algorithm
6. **UserQuota** - User rate limit info

## 🎨 Design Patterns Used

1. **Strategy Pattern** - Different algorithms
2. **Factory Pattern** - Limiter creation
3. **Decorator Pattern** - Distributed wrapper
4. **Singleton Pattern** - Global rate limiter

## 💡 SOLID Principles Applied

- **SRP**: Separate algorithm from storage
- **OCP**: Extensible algorithms
- **LSP**: Algorithm substitution
- **ISP**: Specific interfaces
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. Token bucket vs sliding window vs fixed window
2. Distributed rate limiting (Redis)
3. Race condition handling
4. Memory vs accuracy tradeoffs
5. How to handle burst traffic?
6. Rate limiting at different layers (API Gateway, Application, Database)
7. Response headers for rate limit info
