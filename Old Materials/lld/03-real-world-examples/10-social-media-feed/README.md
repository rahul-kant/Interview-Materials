# Social Media News Feed System - LLD Interview Problem

## 📋 Problem Statement

Design a social media news feed system like Facebook/Twitter that:
- Generates personalized news feed
- Supports posting content
- Handles likes, comments, shares
- Follows/unfollows users
- Real-time updates
- Feed ranking algorithm

## 🎯 Key Requirements

### Functional Requirements:
1. Create posts (text, images, videos)
2. Follow/unfollow users
3. Like, comment, share posts
4. Generate news feed from followed users
5. Feed ranking (chronological, algorithmic)
6. Notifications
7. Privacy settings
8. Trending topics
9. Search users and posts

### Non-Functional Requirements:
1. Low latency feed generation
2. Handle millions of users
3. Real-time updates
4. Scalability
5. Efficient feed generation

## 🏗️ Design Components

### Core Classes:
1. **User** - User profile
2. **Post** - Content post
3. **Feed** - News feed
4. **Comment** - Post comments
5. **Like** - Like information
6. **Friendship** - User relationships
7. **FeedGenerator** - Feed creation
8. **RankingAlgorithm** - Feed ranking
9. **Notification** - User notifications

## 🎨 Design Patterns Used

1. **Strategy Pattern** - Feed ranking algorithms
2. **Observer Pattern** - Real-time updates
3. **Factory Pattern** - Post type creation
4. **Composite Pattern** - Nested comments
5. **Facade Pattern** - Complex feed generation

## 💡 SOLID Principles Applied

- **SRP**: Separate feed generation from ranking
- **OCP**: Extensible ranking algorithms
- **LSP**: Different post types
- **ISP**: Specific interfaces
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. Feed generation approaches (pull vs push)
2. Fan-out on write vs fan-out on read
3. Feed ranking algorithm
4. How to handle celebrity users?
5. Real-time notifications
6. Caching strategies
7. Database design (SQL vs NoSQL)
8. Sharding strategy
