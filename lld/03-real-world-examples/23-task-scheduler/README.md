# Task Scheduler - LLD Interview Problem

## 📋 Problem Statement

Design a task scheduler that:
- Schedule tasks with priorities
- Execute tasks at scheduled time
- Handle dependencies
- Retry failed tasks
- Priority queue for ordering

## 🎯 Key Requirements

### Functional Requirements:
1. Add task with priority
2. Execute tasks in order
3. Task dependencies
4. Recurring tasks
5. Retry mechanism
6. Cancel tasks

### Non-Functional Requirements:
1. Efficient priority handling
2. Thread-safe execution
3. Scalable

## 🏗️ Design Components

### Core Classes:
1. **Task** - Task entity
2. **TaskScheduler** - Main scheduler
3. **PriorityQueue** - Task ordering
4. **TaskExecutor** - Execution engine

## 🎨 Design Patterns Used

1. **Command Pattern** - Task execution
2. **Observer Pattern** - Status notifications
3. **Strategy Pattern** - Scheduling algorithms

## 📊 Complexity

- Add Task: O(log n)
- Execute: O(log n)
- Space: O(n) tasks
