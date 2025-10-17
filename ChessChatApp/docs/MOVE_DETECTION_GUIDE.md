# Chess Move Detection Overlay - User Guide

## 🎯 Overview

The enhanced Chess Chat app now includes a powerful **move detection overlay** that can detect chess moves from another chess app and display them in UCI format in real-time!

### Key Features:
- ✅ **Screen capture** to monitor another chess app
- ✅ **Real-time move detection** using image comparison
- ✅ **UCI format output** (e.g., e2e4, g8f6)
- ✅ **Draggable overlay** window
- ✅ **Board flip support** for both orientations
- ✅ **Configurable detection area**
- ✅ **Auto-detection** every second

---

## 🚀 How to Use

### Step 1: Grant Permissions

When you first launch the app:
1. Grant **Overlay Permission** (required to show floating window)
2. Grant **Screen Capture Permission** (required to detect moves)

### Step 2: Configure Board Area

1. **Open your chess app** (the one you want to monitor)
2. **Measure the chessboard position:**
   - Note the X coordinate (pixels from left edge)
   - Note the Y coordinate (pixels from top edge)
   - Note the board size (width/height in pixels)

3. **Return to Chess Chat app**
4. Tap the **⋮** menu button
5. Select **🎯 Detect Moves**
6. Enter the board coordinates:
   - **X**: Horizontal position (default: 50)
   - **Y**: Vertical position (default: 300)
   - **Size**: Board dimensions (default: 800)

### Step 3: Start Detection

1. Tap **"Start Detection"** button
2. Grant screen capture permission when prompted
3. The **overlay window will appear** on your screen
4. Tap **▶ Start** in the overlay to begin detecting
5. Play moves in your chess app
6. Watch the moves appear in UCI format in the overlay!

---

## 🎮 Overlay Controls

The floating overlay window includes:

| Button | Function |
|--------|----------|
| **▶ Start** | Begin move detection |
| **⏹ Stop** | Pause move detection |
| **🔄 Flip** | Toggle board orientation (for flipped boards) |
| **✖** | Close overlay and stop service |

### Moving the Overlay
- **Drag** the overlay window anywhere on screen
- Position it where it doesn't block your chess game

---

## 📐 Finding Board Coordinates

### Method 1: Developer Tools (Recommended)
1. Enable **Developer Options** on your Android device
2. Enable **Pointer Location** in Developer Options
3. Open your chess app
4. Touch the top-left corner of the board → Note X, Y
5. Touch the bottom-right corner → Calculate size

### Method 2: Screenshot Measurement
1. Take a screenshot with your chess app open
2. Use an image editor to measure:
   - Distance from left edge to board start (X)
   - Distance from top edge to board start (Y)
   - Board width/height (Size)

### Method 3: Trial and Error
1. Start with defaults (X=50, Y=300, Size=800)
2. If detection doesn't work, adjust values
3. Common adjustments:
   - Increase Y if board is lower
   - Decrease Y if board is higher
   - Adjust Size to match your board exactly

---

## 🔄 Board Orientation

### White on Bottom (Default)
- Use the default settings
- No flip needed

### Black on Bottom (Flipped)
1. Configure board area normally
2. Tap **🔄 Flip** in overlay
3. Detection will convert coordinates correctly

---

## 🧪 Testing Move Detection

### Good Test Scenario:
1. Open chess.com or lichess app
2. Start a game (or analysis board)
3. Configure overlay to match board position
4. Start detection
5. Make a move (e.g., move pawn from e2 to e4)
6. Overlay should show: **"Move detected: e2e4"**

### Troubleshooting Detection:

**No moves detected?**
- ✅ Check board coordinates are correct
- ✅ Ensure board is fully visible (not covered)
- ✅ Make sure board size matches exactly
- ✅ Try adjusting detection threshold

**Wrong moves detected?**
- ✅ Click "Flip" if board is upside down
- ✅ Verify board area doesn't include borders
- ✅ Check that coordinates start at board corner

**Too many false detections?**
- ✅ Move overlay away from board
- ✅ Ensure no animations are running
- ✅ Increase detection interval in code

---

## 🎯 How Detection Works

### Image Comparison Algorithm:
1. **Captures screen** every 1 second
2. **Extracts chessboard area** based on your coordinates
3. **Divides into 8×8 grid** (64 squares)
4. **Compares each square** with previous image
5. **Detects changes** when pixels differ >15%
6. **Identifies exactly 2 changed squares** = one move
7. **Converts to UCI format** (e.g., e2e4)

### Why 2 Squares?
- **Square 1**: Where piece was (now empty)
- **Square 2**: Where piece moved to (now occupied)
- If more than 2 squares change → ignored (likely animation)

---

## 📱 Supported Chess Apps

Works with **any visual chess app**, including:
- ✅ Chess.com mobile app
- ✅ Lichess mobile app
- ✅ Chess Free
- ✅ Play Magnus
- ✅ Chess Tactics Pro
- ✅ Any app with a visible board!

**Note:** Detection is **visual only** - no app integration needed!

---

## ⚙️ Advanced Configuration

### Saved Settings
Your board configuration is automatically saved:
- X, Y, Size values persist between sessions
- No need to reconfigure every time

### Performance Tips
1. **Close background apps** for smoother detection
2. **Keep chess board static** during detection
3. **Avoid board animations** when possible
4. **Position overlay** away from board area

### Detection Accuracy
- **Best:** Clean board, no animations, good contrast
- **Good:** Standard chess apps, normal lighting
- **Fair:** 3D boards, animated pieces
- **Poor:** Transparent boards, low contrast themes

---

## 🔧 Customization (For Developers)

### Adjust Detection Sensitivity
In `MoveDetectionOverlayService.kt`:
```kotlin
// Change detection interval (default 1000ms)
private val DETECTION_INTERVAL = 1000L

// Change pixel difference threshold (default 15%)
val threshold = 0.15

// Change color difference sensitivity (default 30)
if (colorDifference(oldPixel, newPixel) > 30)
```

### Change Overlay Appearance
- Modify overlay colors in `createOverlayView()`
- Adjust button sizes and layout
- Customize move log display

---

## 🆘 Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Overlay won't show | Check overlay permission in Settings |
| Screen capture denied | Restart app and grant permission |
| Wrong coordinates | Use developer pointer location tool |
| Board not detected | Ensure full board is visible |
| Flipped moves | Tap "Flip" button in overlay |
| Service crashes | Check Android version (need 5.0+) |
| High battery drain | Stop detection when not in use |

---

## 🔒 Privacy & Security

- ✅ **No data sent** to external servers
- ✅ **Local processing** only
- ✅ **Screen capture** only when active
- ✅ **No storage** of images
- ✅ **Overlay stops** when service closed

---

## 📊 Example Use Cases

### 1. **Analyzing Your Games**
- Play on chess.com app
- Detect all moves automatically
- Send moves to chess engine for analysis

### 2. **Learning from Others**
- Watch chess streams/videos
- Detect moves in real-time
- Study positions immediately

### 3. **Game Notation**
- Play OTB (over-the-board)
- Use app on tablet to detect moves
- Generate automatic PGN notation

### 4. **Engine Analysis**
- Detect opponent moves
- Auto-send to chess engine
- Get instant evaluation

---

## 🎓 Tips & Best Practices

1. **First-time setup:**
   - Use default values first
   - Adjust only if needed

2. **For multiple apps:**
   - Save different configs per app
   - Note which settings work where

3. **For tournaments:**
   - Test detection before game
   - Ensure overlay doesn't block board

4. **Battery saving:**
   - Stop detection when not needed
   - Close overlay completely

---

## 🔄 Updates & Future Features

### Coming Soon:
- [ ] Auto-calibration by tapping corners
- [ ] Multiple board profiles
- [ ] PGN export functionality  
- [ ] Integration with online engines
- [ ] Piece recognition (not just moves)
- [ ] Support for variants (Chess960, etc.)

---

## 📝 Changelog

### Version 2.0 (Current)
- ✅ Added move detection overlay
- ✅ Screen capture support
- ✅ UCI format output
- ✅ Configurable board area
- ✅ Board flip support
- ✅ Draggable overlay window

### Version 1.0
- Basic chess engine chat
- Menu with game controls
- Server integration

---

## 🤝 Support

For help or issues:
1. Check this guide first
2. Verify all permissions granted
3. Test with default coordinates
4. Restart app if needed

**Enjoy detecting moves!** ♟️
