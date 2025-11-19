# Chess Game - LLD Interview Problem

## 📋 Problem Statement

Design a chess game system that:
- 8x8 board with all pieces
- Move validation for each piece
- Check and checkmate detection
- Turn-based gameplay
- Castling, en passant support
- Move history

## 🎯 Key Requirements

### Functional Requirements:
1. Initialize board with pieces
2. Validate moves per piece type
3. Detect check/checkmate
4. Detect stalemate
5. Handle special moves (castling, en passant)
6. Move history
7. Undo move

### Non-Functional Requirements:
1. Clean OOP design
2. Extensible piece types
3. Efficient move validation

## 🏗️ Design Components

### Core Classes:
1. **Board** - 8x8 grid
2. **Piece** - Base piece class
3. **King, Queen, Rook, etc.** - Specific pieces
4. **Move** - Move representation
5. **Game** - Game controller
6. **Player** - White/Black player

## 🎨 Design Patterns Used

1. **Strategy Pattern** - Different piece movements
2. **Command Pattern** - Move execution/undo
3. **Factory Pattern** - Piece creation

## 💡 SOLID Principles Applied

- **SRP**: Each piece has its own movement logic
- **OCP**: Extensible for new piece types
- **LSP**: All pieces extend base Piece class

## 🔍 Interview Discussion Points

1. **Move Validation**:
   - Each piece has unique rules
   - Path validation (blocked)
   - Check validation

2. **Check Detection**:
   - King under attack
   - Limited moves when in check
   - Checkmate vs stalemate

3. **Special Moves**:
   - Castling (king + rook)
   - En passant (pawn capture)
   - Pawn promotion

## 📊 Complexity

- Move Validation: O(1) to O(n)
- Check Detection: O(n²)
- Space: O(1) board size
