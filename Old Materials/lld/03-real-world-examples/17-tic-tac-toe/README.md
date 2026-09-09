# Tic-Tac-Toe Game - LLD Interview Problem

## 📋 Problem Statement

Design a Tic-Tac-Toe game that:
- 3x3 grid board
- Two players (X and O)
- Turn-based gameplay
- Win detection (row, column, diagonal)
- Draw detection
- Move validation

## 🎯 Key Requirements

### Functional Requirements:
1. Initialize 3x3 board
2. Validate moves
3. Alternate turns
4. Detect win (row/col/diagonal)
5. Detect draw
6. Display board
7. Prevent invalid moves

### Non-Functional Requirements:
1. Fast win detection (O(1))
2. Clean code
3. Extensible board size
4. Undo move support

## 🏗️ Design Components

### Core Classes:
1. **Board** - Game board (3x3 grid)
2. **Player** - Player with symbol (X/O)
3. **Game** - Game controller
4. **Move** - Move position

## 🎨 Design Patterns Used

1. **Strategy Pattern** - Different win strategies
2. **Command Pattern** - Move validation

## 💡 SOLID Principles Applied

- **SRP**: Separate board, player, game logic
- **OCP**: Extensible win conditions

## 🔍 Interview Discussion Points

1. **Win Detection**:
   - Brute force: O(n²) check all
   - Optimized: O(1) with counters
   - Diagonal detection

2. **Board Representation**:
   - 2D array
   - 1D array with index mapping
   - HashMap

3. **Extensibility**:
   - N x N board
   - More than 2 players
   - Different win conditions

## 🎯 Win Detection Algorithm

```
For each row/col/diagonal:
  Count X and O
  If count(X) == n: X wins
  If count(O) == n: O wins
```

Optimized: Track counts during each move

## 📊 Complexity

- Time: O(1) per move with counters
- Space: O(n²) for board
