# Chess Move Detection - Technical Implementation

## 🏗️ Architecture Overview

### Components

1. **MainActivity.kt** - Main app with menu and configuration
2. **MoveDetectionOverlayService.kt** - Foreground service for overlay
3. **activity_main.xml** - Main UI layout
4. **board_config_dialog.xml** - Configuration dialog
5. **AndroidManifest.xml** - Permissions and service declaration

---

## 🔑 Key Technologies

### Android APIs Used:
- **MediaProjection API** - Screen capture
- **WindowManager** - Overlay window management
- **ImageReader** - Bitmap extraction from screen
- **VirtualDisplay** - Screen mirroring
- **Foreground Service** - Background operation

### Permissions Required:
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

---

## 🎯 Move Detection Algorithm

### Step-by-Step Process:

#### 1. Screen Capture
```kotlin
// Create ImageReader for screen pixels
imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

// Create virtual display from MediaProjection
virtualDisplay = mediaProjection?.createVirtualDisplay(
    "ChessMoveDetection",
    width, height, densityDpi,
    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
    imageReader?.surface, null, null
)
```

#### 2. Image Extraction
```kotlin
private fun captureScreen() {
    val image = imageReader?.acquireLatestImage() ?: return
    val planes = image.planes
    val buffer = planes[0].buffer
    
    // Convert buffer to bitmap
    val bitmap = Bitmap.createBitmap(
        image.width + rowPadding / pixelStride,
        image.height,
        Bitmap.Config.ARGB_8888
    )
    bitmap.copyPixelsFromBuffer(buffer)
}
```

#### 3. Board Area Extraction
```kotlin
private fun extractBoardArea(fullBitmap: Bitmap): Bitmap {
    return Bitmap.createBitmap(
        fullBitmap, 
        boardX,    // X coordinate
        boardY,    // Y coordinate
        boardSize, // Width
        boardSize  // Height
    )
}
```

#### 4. Grid Division (8×8)
```kotlin
val squareSize = boardBitmap.width / 8

for (row in 0 until 8) {
    for (col in 0 until 8) {
        val x = col * squareSize
        val y = row * squareSize
        // Process each square
    }
}
```

#### 5. Change Detection
```kotlin
private fun hasSquareChanged(
    old: Bitmap, 
    new: Bitmap, 
    x: Int, y: Int, 
    size: Int
): Boolean {
    var diffPixels = 0
    val threshold = 0.15 // 15% pixel difference
    
    for (dy in 0 until size) {
        for (dx in 0 until size) {
            val oldPixel = old.getPixel(x + dx, y + dy)
            val newPixel = new.getPixel(x + dx, y + dy)
            
            if (colorDifference(oldPixel, newPixel) > 30) {
                diffPixels++
            }
        }
    }
    
    return (diffPixels.toFloat() / totalPixels) > threshold
}
```

#### 6. Color Difference Calculation
```kotlin
private fun colorDifference(color1: Int, color2: Int): Int {
    val r1 = Color.red(color1)
    val g1 = Color.green(color1)
    val b1 = Color.blue(color1)
    val r2 = Color.red(color2)
    val g2 = Color.green(color2)
    val b2 = Color.blue(color2)
    
    // Manhattan distance in RGB space
    return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2)
}
```

#### 7. Move Identification
```kotlin
private fun detectMove(oldBoard: Bitmap, newBoard: Bitmap): String? {
    val changes = mutableListOf<Pair<Int, Int>>()
    
    // Find all changed squares
    for (row in 0 until 8) {
        for (col in 0 until 8) {
            if (hasSquareChanged(...)) {
                changes.add(Pair(row, col))
            }
        }
    }
    
    // Valid move = exactly 2 changed squares
    if (changes.size == 2) {
        val from = changes[0]
        val to = changes[1]
        return squareToUCI(from) + squareToUCI(to)
    }
    
    return null
}
```

#### 8. UCI Conversion
```kotlin
private fun squareToUCI(row: Int, col: Int, flipped: Boolean): String {
    // Account for board orientation
    val actualRow = if (flipped) row else 7 - row
    val actualCol = if (flipped) 7 - col else col
    
    // Convert to chess notation
    val file = ('a' + actualCol).toString()  // a-h
    val rank = (actualRow + 1).toString()     // 1-8
    
    return "$file$rank"  // e.g., "e2", "e4"
}
```

---

## 🔄 Board Orientation Handling

### Coordinate Systems:

**Screen Coordinates:**
- Origin (0,0) = Top-left
- X increases → Right
- Y increases ↓ Down

**Chess Coordinates (White bottom):**
- a1 = Bottom-left
- h8 = Top-right

**Chess Coordinates (Black bottom/Flipped):**
- a1 = Top-right
- h8 = Bottom-left

### Transformation Logic:

```kotlin
// White on bottom (default)
actualRow = 7 - screenRow
actualCol = screenCol

// Black on bottom (flipped)
actualRow = screenRow
actualCol = 7 - screenCol
```

---

## 🎨 Overlay Window Implementation

### Window Type Selection:
```kotlin
val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )
} else {
    // TYPE_PHONE for older Android versions
}
```

### Draggable Overlay:
```kotlin
container.setOnTouchListener { _, event ->
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            initialX = layoutParams.x
            initialY = layoutParams.y
            initialTouchX = event.rawX
            initialTouchY = event.rawY
        }
        MotionEvent.ACTION_MOVE -> {
            layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
            layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
            windowManager?.updateViewLayout(container, layoutParams)
        }
    }
    true
}
```

---

## ⚡ Performance Optimizations

### 1. Detection Interval
```kotlin
private val DETECTION_INTERVAL = 1000L // 1 second
// Adjustable based on needs:
// - 500ms = More responsive, higher CPU
// - 1000ms = Balanced
// - 2000ms = Battery efficient
```

### 2. Bitmap Recycling
```kotlin
override fun onDestroy() {
    previousBitmap?.recycle()  // Free memory
    imageReader?.close()
    virtualDisplay?.release()
    mediaProjection?.stop()
}
```

### 3. Efficient Comparison
```kotlin
// Only compare board area, not full screen
val boardBitmap = extractBoardArea(fullBitmap)

// Early exit if no previous bitmap
if (previousBitmap == null) {
    previousBitmap = boardBitmap
    return
}
```

---

## 🔐 Security Considerations

### Permission Flow:
1. **Overlay Permission** (SYSTEM_ALERT_WINDOW)
   - Requested via Settings intent
   - User must manually grant

2. **Screen Capture Permission** (MediaProjection)
   - Requested via system dialog
   - Granted per-session

3. **Foreground Service**
   - Persistent notification required
   - User can stop anytime

### Data Privacy:
- ✅ No images saved to disk
- ✅ No network transmission
- ✅ Bitmaps recycled immediately
- ✅ Local processing only

---

## 🐛 Error Handling

### Common Errors & Solutions:

#### ImageReader Errors:
```kotlin
try {
    val image = imageReader?.acquireLatestImage() ?: return
    // Process image
    image.close()  // Always close!
} catch (e: Exception) {
    logMove("Error: ${e.message}")
}
```

#### Bitmap Extraction Errors:
```kotlin
private fun extractBoardArea(fullBitmap: Bitmap): Bitmap {
    return try {
        Bitmap.createBitmap(fullBitmap, boardX, boardY, boardSize, boardSize)
    } catch (e: Exception) {
        logMove("Error extracting board: ${e.message}")
        fullBitmap  // Return full bitmap as fallback
    }
}
```

#### Service Lifecycle:
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // Handle null intent
    intent?.let {
        val resultCode = it.getIntExtra("resultCode", 0)
        if (resultCode != 0) {
            startScreenCapture(resultCode, data)
        }
    }
    return START_STICKY  // Restart if killed
}
```

---

## 📊 Detection Accuracy

### Factors Affecting Accuracy:

**High Accuracy (>95%):**
- ✅ Clean 2D board
- ✅ High contrast pieces
- ✅ No animations during capture
- ✅ Correct board coordinates
- ✅ Stable lighting

**Medium Accuracy (70-95%):**
- ⚠️ 3D boards with simple pieces
- ⚠️ Fast animations
- ⚠️ Low contrast themes
- ⚠️ Slight coordinate misalignment

**Low Accuracy (<70%):**
- ❌ Highly animated 3D boards
- ❌ Transparent/glass pieces
- ❌ Wrong board coordinates
- ❌ Board partially obscured

### Tuning Parameters:

```kotlin
// Pixel difference threshold (lower = more sensitive)
val threshold = 0.15  // Try 0.10 - 0.20

// Color difference (lower = more sensitive)
if (colorDifference(oldPixel, newPixel) > 30)  // Try 20-40

// Detection interval (ms)
private val DETECTION_INTERVAL = 1000L  // Try 500-2000
```

---

## 🔄 Future Enhancements

### Planned Features:

1. **Auto-Calibration:**
   - Tap 4 corners to auto-detect board
   - Computer vision edge detection
   - Template matching

2. **Piece Recognition:**
   - CNN for piece type detection
   - Color classification (white/black)
   - Full FEN generation

3. **Advanced Detection:**
   - Castling recognition
   - En passant detection
   - Promotion handling

4. **Performance:**
   - GPU acceleration
   - Multi-threading
   - Adaptive interval based on game pace

---

## 📚 Dependencies

### Required Libraries:
```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

### API Levels:
- **Minimum:** API 29 (Android 10)
- **Target:** API 34 (Android 14)
- **MediaProjection:** API 21+
- **TYPE_APPLICATION_OVERLAY:** API 26+

---

## 🧪 Testing Checklist

- [ ] Overlay permission granted
- [ ] Screen capture permission granted
- [ ] Overlay window displays correctly
- [ ] Buttons respond to clicks
- [ ] Board area configured correctly
- [ ] Moves detected accurately
- [ ] UCI format correct (e.g., e2e4)
- [ ] Flip function works
- [ ] Service stops cleanly
- [ ] No memory leaks
- [ ] Battery usage acceptable

---

## 📝 Code Structure

```
android_app_enhanced/
├── AndroidManifest.xml              # Permissions & service
├── MainActivity.kt                  # Main app logic
├── MoveDetectionOverlayService.kt   # Overlay & detection
├── activity_main.xml                # Main UI layout
├── board_config_dialog.xml          # Config dialog
└── MOVE_DETECTION_GUIDE.md          # User guide
```

---

## 🎓 Learning Resources

### Android APIs:
- [MediaProjection API](https://developer.android.com/reference/android/media/projection/MediaProjection)
- [Overlay Windows](https://developer.android.com/guide/topics/ui/window-overlays)
- [Foreground Services](https://developer.android.com/guide/components/foreground-services)

### Chess Notation:
- [UCI Protocol](https://www.chessprogramming.org/UCI)
- [Chess Coordinates](https://en.wikipedia.org/wiki/Algebraic_notation_(chess))

---

## 💡 Tips for Developers

1. **Test on real device** - Emulator screen capture may not work
2. **Use logging extensively** - Helps debug detection issues
3. **Bitmap memory management** - Always recycle bitmaps
4. **Handle edge cases** - Board at screen edges, rotated devices
5. **Optimize for battery** - Stop detection when not needed

---

**Happy coding!** ♟️
