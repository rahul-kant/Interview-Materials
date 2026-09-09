# Snake and Ladder Game - LLD Interview Problem

## 📋 Problem Statement

Design a Snake and Ladder board game that:
- Simulates the classic board game
- Handles multiple players
- Implements snake and ladder logic
- Dice rolling mechanism
- Win condition detection

## 🎯 Key Requirements

### Functional Requirements:
1. Initialize board with snakes and ladders
2. Multiple players (2-4)
3. Dice roll (1-6)
4. Move players based on dice
5. Snake: Move down
6. Ladder: Move up
7. Exact 100 to win
8. Turn-based gameplay

### Non-Functional Requirements:
1. Fair dice rolling
2. No concurrent moves
3. Clear game state
4. Extensible board size

## 🏗️ Design Components

### Core Classes:
1. **Board** - Game board with snakes/ladders
2. **Player** - Player with position
3. **Dice** - Random number generator
4. **Snake** - Head to tail mapping
5. **Ladder** - Start to end mapping
6. **Game** - Main game controller

## 🎨 Design Patterns Used

1. **Singleton Pattern** - Single board instance
2. **Factory Pattern** - Game creation
3. **Strategy Pattern** - Different board configurations

## 💡 SOLID Principles Applied

- **SRP**: Separate board, dice, player logic
- **OCP**: Extensible board sizes
- **DIP**: Depend on abstractions

## 🔍 Interview Discussion Points

1. **Random Number Generation**:
   - Fair dice rolling
   - Seed for reproducibility
   - Testing with mocked dice

2. **Board Representation**:
   - HashMap for snakes/ladders
   - Array vs Map tradeoffs
   - Memory optimization

3. **Win Condition**:
   - Exact 100 vs first to reach
   - Bounce back logic
   - Multiple winners

4. **Extensibility**:
   - Variable board size
   - Multiple dice
   - Custom rules

## 🎲 Algorithm

```
1. Roll dice
2. New position = current + dice value
3. If new position > 100: stay at current
4. Check for snake at new position
5. Check for ladder at new position
6. Update player position
7. Check win condition
8. Next player's turn
```

## 📊 Complexity

- Time: O(1) per move
- Space: O(snakes + ladders + players)
