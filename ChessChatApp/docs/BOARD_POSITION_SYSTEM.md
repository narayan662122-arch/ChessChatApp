# Chess Board Position System - Move Calculation Guide

## 📐 Your Board Coordinates

### Configured Area:
```
X Range: 12 → 710 pixels
Y Range: 502 → 1203 pixels
Board Width: 698 pixels (710 - 12)
Board Height: 701 pixels (1203 - 502)
```

**Note:** The board is nearly square (698×701), perfect for chess detection!

---

## 🎯 How Move Calculation Works

### Step 1: Image Cropping
```kotlin
// Extract only the chessboard area from full screen
val fullScreenBitmap = captureScreen()
val boardBitmap = Bitmap.createBitmap(
    fullScreenBitmap,
    12,    // X start
    502,   // Y start
    698,   // Width
    701    // Height
)
```

**Result:** Isolated chessboard image (698×701 pixels)

---

### Step 2: Board Position System (8×8 Grid)

The cropped board is divided into 64 squares:

```
Square Size:
- Width per square: 698 ÷ 8 = 87.25 pixels
- Height per square: 701 ÷ 8 = 87.625 pixels
- Approximate: 87×87 pixels per square
```

#### Grid Layout (White on Bottom):
```
  a    b    c    d    e    f    g    h
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ a8 │ b8 │ c8 │ d8 │ e8 │ f8 │ g8 │ h8 │ 8
├────┼────┼────┼────┼────┼────┼────┼────┤
│ a7 │ b7 │ c7 │ d7 │ e7 │ f7 │ g7 │ h7 │ 7
├────┼────┼────┼────┼────┼────┼────┼────┤
│ a6 │ b6 │ c6 │ d6 │ e6 │ f6 │ g6 │ h6 │ 6
├────┼────┼────┼────┼────┼────┼────┼────┤
│ a5 │ b5 │ c5 │ d5 │ e5 │ f5 │ g5 │ h5 │ 5
├────┼────┼────┼────┼────┼────┼────┼────┤
│ a4 │ b4 │ c4 │ d4 │ e4 │ f4 │ g4 │ h4 │ 4
├────┼────┼────┼────┼────┼────┼────┼────┤
│ a3 │ b3 │ c3 │ d3 │ e3 │ f3 │ g3 │ h3 │ 3
├────┼────┼────┼────┼────┼────┼────┼────┤
│ a2 │ b2 │ c2 │ d2 │ e2 │ f2 │ g2 │ h2 │ 2
├────┼────┼────┼────┼────┼────┼────┼────┤
│ a1 │ b1 │ c1 │ d1 │ e1 │ f1 │ g1 │ h1 │ 1
└────┴────┴────┴────┴────┴────┴────┴────┘
```

---

### Step 3: Position to Pixel Mapping

#### Coordinate System:
```kotlin
// Each square position in the 8x8 grid
for (row in 0 until 8) {
    for (col in 0 until 8) {
        val pixelX = col * 87  // Column → X position
        val pixelY = row * 87  // Row → Y position
        
        // Extract square image (87×87 pixels)
        val square = Bitmap.createBitmap(
            boardBitmap,
            pixelX,
            pixelY,
            87,
            87
        )
    }
}
```

#### Example Pixel Positions:
| Square | Grid Position | Pixel Range (X,Y) |
|--------|---------------|-------------------|
| **a1** | (7, 0) | (0, 609) → (87, 696) |
| **e2** | (6, 4) | (348, 522) → (435, 609) |
| **e4** | (4, 4) | (348, 348) → (435, 435) |
| **g8** | (0, 6) | (522, 0) → (609, 87) |

---

### Step 4: Image Comparison

For each square, compare current frame vs previous frame:

```kotlin
fun hasSquareChanged(
    oldSquare: Bitmap,
    newSquare: Bitmap
): Boolean {
    var differentPixels = 0
    val totalPixels = 87 * 87  // 7,569 pixels
    
    for (y in 0 until 87) {
        for (x in 0 until 87) {
            val oldPixel = oldSquare.getPixel(x, y)
            val newPixel = newSquare.getPixel(x, y)
            
            // Calculate color difference
            val diff = abs(red(oldPixel) - red(newPixel)) +
                      abs(green(oldPixel) - green(newPixel)) +
                      abs(blue(oldPixel) - blue(newPixel))
            
            if (diff > 30) {  // Threshold
                differentPixels++
            }
        }
    }
    
    // If >15% pixels changed, square has changed
    return (differentPixels.toFloat() / totalPixels) > 0.15
}
```

---

### Step 5: Move Detection Logic

```kotlin
fun detectMove(oldBoard: Bitmap, newBoard: Bitmap): String? {
    val changedSquares = mutableListOf<Pair<Int, Int>>()
    
    // Check all 64 squares
    for (row in 0 until 8) {
        for (col in 0 until 8) {
            if (hasSquareChanged(oldBoard, newBoard, row, col)) {
                changedSquares.add(Pair(row, col))
            }
        }
    }
    
    // Valid move = exactly 2 squares changed
    if (changedSquares.size == 2) {
        val from = changedSquares[0]
        val to = changedSquares[1]
        
        // Convert to UCI notation
        return positionToUCI(from) + positionToUCI(to)
    }
    
    return null  // Invalid or no move
}
```

---

### Step 6: UCI Conversion with Flip Support

#### White on Bottom (Normal):
```kotlin
fun positionToUCI(row: Int, col: Int, flipped: false): String {
    val rank = (7 - row) + 1     // 7→1, 6→2, ..., 0→8
    val file = 'a' + col          // 0→a, 1→b, ..., 7→h
    return "$file$rank"
}
```

**Examples:**
- Grid (7, 0) → UCI "a1"
- Grid (6, 4) → UCI "e2"
- Grid (4, 4) → UCI "e4"
- Grid (0, 6) → UCI "g8"

#### Black on Bottom (Flipped):
```kotlin
fun positionToUCI(row: Int, col: Int, flipped: true): String {
    val rank = row + 1            // 0→1, 1→2, ..., 7→8
    val file = 'a' + (7 - col)    // 7→a, 6→b, ..., 0→h
    return "$file$rank"
}
```

**Examples (flipped):**
- Grid (7, 0) → UCI "h8"
- Grid (6, 4) → UCI "d7"
- Grid (4, 4) → UCI "e5"
- Grid (0, 6) → UCI "b1"

---

## 🔄 Complete Move Calculation Example

### Scenario: White plays e2→e4

**Frame 1 (Before move):**
```
Crop board (x=12, y=502, size=698×701)
Extract square e2 (grid 6,4) → pixels (348, 522)
Extract square e4 (grid 4,4) → pixels (348, 348)
Save as previousBoard
```

**Frame 2 (After move):**
```
Crop board (x=12, y=502, size=698×701)
Extract square e2 (grid 6,4) → pixels (348, 522) ← NOW EMPTY
Extract square e4 (grid 4,4) → pixels (348, 348) ← NOW OCCUPIED
Compare with previousBoard
```

**Comparison Results:**
```
Square e2 (6,4): 45% pixels changed ✓ (>15% threshold)
Square e4 (4,4): 52% pixels changed ✓ (>15% threshold)
All other squares: <10% change ✗

Changed squares: 2
From: (6, 4) → UCI "e2"
To: (4, 4) → UCI "e4"

Detected Move: "e2e4" ✓
```

---

## 📊 Accuracy Optimization

### Fine-tuning for Your Board:

**1. Detection Threshold:**
```kotlin
// Adjust based on your app's animations
val PIXEL_CHANGE_THRESHOLD = 0.15  // 15% default
// Increase (0.20) if too sensitive
// Decrease (0.10) if missing moves
```

**2. Color Sensitivity:**
```kotlin
// RGB difference threshold
if (colorDiff > 30) { /* pixel changed */ }
// Increase (40-50) for similar colors
// Decrease (20) for high contrast boards
```

**3. Detection Interval:**
```kotlin
// How often to check (milliseconds)
val DETECTION_INTERVAL = 1000L  // 1 second
// Decrease (500) for faster detection
// Increase (2000) for battery saving
```

---

## 🎮 Overlay Flip Button

The **🔄 Flip** button toggles board orientation:

### Normal (White Bottom):
- Row 7 = Rank 1 (a1, b1, ..., h1)
- Row 0 = Rank 8 (a8, b8, ..., h8)
- Col 0 = File a
- Col 7 = File h

### Flipped (Black Bottom):
- Row 0 = Rank 1 (a1, b1, ..., h1)
- Row 7 = Rank 8 (a8, b8, ..., h8)
- Col 7 = File a
- Col 0 = File h

**The flip button ensures correct UCI output regardless of board orientation!**

---

## 🧪 Testing Your Setup

### Test Case 1: Opening Move (e2e4)
1. Start detection with your coordinates (12, 502, 698)
2. In chess app, move pawn e2→e4
3. Expected output: `"Move detected: e2e4"`

### Test Case 2: Knight Move (g1f3)
1. Continue from position above
2. Move knight g1→f3
3. Expected output: `"Move detected: g1f3"`

### Test Case 3: Flipped Board
1. Tap 🔄 Flip button
2. Same moves should still detect correctly
3. UCI notation automatically adjusted

---

## 🔍 Troubleshooting

### Issue: No moves detected
**Solution:**
- Verify coordinates (x=12, y=502, size=698)
- Ensure board fully visible in app
- Check detection is started (▶ button)

### Issue: Wrong moves detected
**Solution:**
- Click 🔄 Flip if board is upside down
- Ensure square size is accurate (87×87)
- Check no overlays blocking board

### Issue: Too many false positives
**Solution:**
- Increase PIXEL_CHANGE_THRESHOLD to 0.20
- Increase detection interval to 2000ms
- Ensure no animations during capture

---

## 📐 Visual Representation

### Your Board Area on Screen:
```
Screen (Full)
┌─────────────────────────────────────┐
│                                     │
│                                     │ ← y=502 (board starts here)
│  (12,502)                           │
│     ┌─────────────────────┐         │
│     │                     │         │
│     │   CHESSBOARD        │         │
│     │   698×701 pixels    │         │
│     │                     │         │
│     │   8×8 grid          │         │
│     │   87×87 per square  │         │
│     │                     │         │
│     └─────────────────────┘         │
│                      (710,1203)     │
│                                     │
└─────────────────────────────────────┘
```

### Grid Overlay on Your Board:
```
(12,502)
   ┌───┬───┬───┬───┬───┬───┬───┬───┐
   │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ Row 0 (a8-h8)
   ├───┼───┼───┼───┼───┼───┼───┼───┤
   │   │   │   │   │   │   │   │   │ Row 1 (a7-h7)
   ├───┼───┼───┼───┼───┼───┼───┼───┤
   │   │   │   │   │   │   │   │   │ Row 2 (a6-h6)
   ├───┼───┼───┼───┼───┼───┼───┼───┤
   │   │   │   │   │   │   │   │   │ Row 3 (a5-h5)
   ├───┼───┼───┼───┼───┼───┼───┼───┤
   │   │   │   │   │ X │   │   │   │ Row 4 (e4)
   ├───┼───┼───┼───┼───┼───┼───┼───┤
   │   │   │   │   │   │   │   │   │ Row 5 (a3-h3)
   ├───┼───┼───┼───┼───┼───┼───┼───┤
   │   │   │   │   │ O │   │   │   │ Row 6 (e2)
   ├───┼───┼───┼───┼───┼───┼───┼───┤
   │   │   │   │   │   │   │   │   │ Row 7 (a1-h1)
   └───┴───┴───┴───┴───┴───┴───┴───┘
   Col: 0   1   2   3   4   5   6   7
        a   b   c   d   e   f   g   h

O = Piece at e2 (before)
X = Piece at e4 (after)
Move = "e2e4"
```

---

## ✅ Summary

**Your Board Position System:**
1. ✅ **Crops** board area: x=12-710, y=502-1203
2. ✅ **Divides** into 8×8 grid (87×87 per square)
3. ✅ **Compares** each square pixel-by-pixel
4. ✅ **Detects** exactly 2 changed squares
5. ✅ **Converts** grid positions to UCI format
6. ✅ **Flips** orientation with button toggle
7. ✅ **Outputs** moves like "e2e4", "g1f3", etc.

**This system accurately calculates moves through image cropping and comparison!** ♟️
