# Chess Chat Android App

A chess engine chat application with move detection overlay functionality.

## Features

- Chat interface to communicate with chess engine server
- Configurable server URL
- Move detection overlay using screen capture
- Support for black/white piece selection
- Real-time move detection from chessboard area
- Draggable overlay window

## Fixed Issues

✅ **Syntax Error Fixed**: Corrected line 137 in MainActivity.kt where `xInput` was incorrectly used instead of `sizeInput`

## Project Structure

```
ChessChatApp/
├── app/
│   ├── build.gradle                    # App-level build configuration
│   ├── proguard-rules.pro              # ProGuard rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # App manifest
│           ├── java/com/chesschat/app/
│           │   ├── MainActivity.kt     # Main activity (FIXED)
│           │   └── MoveDetectionOverlayService.kt
│           └── res/
│               ├── layout/
│               │   ├── activity_main.xml
│               │   ├── board_config_dialog.xml
│               │   └── board_config_dialog_v2.xml
│               └── values/
│                   ├── colors.xml
│                   ├── strings.xml
│                   └── themes.xml
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle                        # Project-level build configuration
├── settings.gradle                     # Project settings
├── gradle.properties                   # Gradle properties
└── .gitignore                          # Git ignore rules
```

## Requirements

- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9.20

## Dependencies

- AndroidX Core KTX 1.12.0
- AndroidX AppCompat 1.6.1
- Material Components 1.11.0
- OkHttp 4.12.0

## Build Instructions

### Option 1: Using Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `ChessChatApp` folder
4. Click OK
5. Wait for Gradle sync to complete
6. Click "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
7. APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`

### Option 2: Using Command Line

```bash
cd ChessChatApp

# On macOS/Linux:
./gradlew assembleDebug

# On Windows:
gradlew.bat assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Permissions Required

- `INTERNET` - For communicating with chess engine server
- `ACCESS_NETWORK_STATE` - For checking network connectivity
- `SYSTEM_ALERT_WINDOW` - For displaying overlay window
- `FOREGROUND_SERVICE` - For running move detection service
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - For screen capture

## Configuration

### Server Setup

1. Launch the app
2. Enter your chess engine server URL (e.g., `https://xxxx.ngrok-free.app`)
3. Click "Save"

### Board Configuration

1. Press the menu button (⋮)
2. Select "Detect Moves"
3. Configure the chessboard coordinates:
   - **X**: Horizontal position from left edge (default: 12px)
   - **Y**: Vertical position from top edge (default: 502px)
   - **Size**: Board width/height (default: 698px)
4. Click "Start Detection" or "Use Defaults"

## Usage

1. **Start Game**: Press menu → "Start"
2. **Choose Color**: Press menu → "Black" or "White"
3. **Enter Moves**: Type moves in UCI format (e.g., `e2e4`)
4. **Auto Detection**: Press menu → "Detect Moves" to enable overlay

## Move Detection

The overlay service captures the screen and detects piece movements:

- Divides the board into 8×8 grid
- Compares consecutive frames
- Detects when exactly 2 squares change (indicating a move)
- Converts positions to UCI notation
- Supports board flipping for black/white orientation

## Notes

- Move detection requires screen capture permission
- Overlay window can be dragged to reposition
- Detection runs every 1 second when active
- Board coordinates may need adjustment based on your chess app

## Troubleshooting

**Overlay not showing:**
- Grant "Display over other apps" permission in Android settings

**Move detection not working:**
- Ensure board coordinates are correctly configured
- Check screen capture permission is granted
- Verify the chess board is visible and not obscured

**Connection issues:**
- Verify server URL is correct
- Check internet connection
- Ensure server is running and accessible
