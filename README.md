# Block Blast

A Block Blast puzzle game for Android, built in Kotlin with a custom `Canvas`-based renderer.

## Gameplay

- **8×8 board** with colored blocks
- **3 random pieces** shown in the tray — drag and drop onto the board
- **Fill a full row or column** to clear it and earn points
- **Combo system** — chain clears within 5 seconds multiply your score
- **Game over** when none of the remaining pieces can be placed
- Tray refreshes with 3 new pieces only after all 3 have been placed

## Install

Use apk in Release

## Building

Requires Android SDK (platform 35), JDK 17+, and Kotlin.

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Requirements

- Android 8.0+ (API 26)
- No external dependencies — pure Android SDK + Kotlin

## Tech Stack

- Kotlin
- Custom `View` with `Canvas` drawing (no XML layouts)
- Zero runtime dependencies
