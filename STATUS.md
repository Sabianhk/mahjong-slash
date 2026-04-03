# Mahjong Slash (切牌) — Build Status

## Current State: Phase 1 Complete

### What's Done

**Phase 1 — Scaffold + Proof of Life**

- Android project scaffold (Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12)
- Package structure: `com.mahjongslash.{game,ui,viewmodel}`
- **Tile model**: All 34 Mahjong tile types (Characters, Dots, Bamboo, Winds, Dragons) with Chinese characters
- **Tile rendering**: 3D-look tiles drawn on Compose Canvas — ivory face, depth edge, warm shadow, engraved symbols with suit-specific colors, subtle gloss highlight
- **Tile spawning**: Tiles spawn from all 4 screen edges with randomized entry angles and gentle speed
- **Swipe detection**: Drag gesture → path sampling → Liang-Barsky line-rect intersection against tile hit bounds (with 8dp forgiveness padding)
- **Match validation**: Pair (2 identical = 100pts), Sequence (3 consecutive same suit = 200pts), Triplet (3 identical = 350pts). Finds highest-scoring valid subset from slashed tiles
- **Shatter effect**: 8-fragment explosion with physics (velocity, gravity, rotation, fade)
- **Slash trail**: Calligraphy brush stroke effect — ink-black path with smooth bezier curves, outer glow, inner highlight, fade-out
- **Combo system**: Consecutive matches within 3s increment combo (×1.0 → ×3.0 multiplier)
- **Blade health**: 3 health points, lost on invalid slash. Game over when 0
- **HUD**: Score (gold), combo multiplier (red), blade health indicators
- **Game over**: Overlay with 終 character, final score, tap-to-restart
- **Ink & Ivory theme**: Dark warm background (#1A1714), ivory tiles, accent red/gold, no Material Design components
- **Rice paper grain**: Subtle dot texture on background

### What Remains

**Phase 2 — Tile System + Match Logic** (partially done — match logic is in, needs full symbol rendering polish, more movement patterns)

**Phase 3 — Game Loop + HUD** (partially done — core loop works, needs difficulty scaling, health tile visuals)

**Phase 4 — Menus + Navigation**: Splash, main menu, mode select, pause, results, settings screens

**Phase 5 — Remaining Modes + Power-Ups**: Time Attack, Zen, Challenge modes. Gold Lacquer, Slow Ink, Reveal power-ups. Audio trigger system

**Phase 6 — Progression + Persistence**: Room DB, player profile, collection, shop, leaderboards, ranks

**Phase 7 — Polish + Store Prep**: Visual polish, app icon, feature graphic, ProGuard, signed .aab

## How to Build & Run

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- Android SDK 35 (API 35)
- JDK 17

### Steps
1. Open the project root (`mahjong-slash/`) in Android Studio
2. Android Studio will auto-download the Gradle wrapper JAR and sync dependencies
3. Connect a device or start an emulator (API 26+)
4. Run the `app` configuration

### Project Structure
```
mahjong-slash/
├── build.gradle.kts          # Root build config
├── settings.gradle.kts       # Module + repo config
├── gradle.properties         # JVM args, AndroidX
├── app/
│   ├── build.gradle.kts      # App module config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mahjongslash/
│       │   ├── MainActivity.kt
│       │   ├── viewmodel/GameViewModel.kt
│       │   ├── ui/
│       │   │   ├── theme/{Color,Theme}.kt
│       │   │   └── screens/GameScreen.kt
│       │   └── game/
│       │       ├── model/{Tile,TileType}.kt
│       │       ├── engine/{GameState,GameEngine}.kt
│       │       ├── render/{TileRenderer,ShatterEffect,SlashTrail}.kt
│       │       └── gesture/SlashDetector.kt
│       └── res/
│           ├── values/{strings,colors,themes}.xml
│           └── drawable/ic_launcher_foreground.xml
└── docs/build-prompt.md      # Full game spec
```

## Architecture Notes

- **No game engine dependency** — pure Compose Canvas + frame-driven update loop
- **GameEngine** is the single source of truth. It owns all mutable state and exposes immutable snapshots via `GameState`
- **ViewModel** bridges the engine to Compose, exposing `StateFlow<GameState>`
- **TileRenderer** draws tiles with layered Canvas operations (shadow → edge → face → symbol → gloss)
- **SlashDetector** uses Liang-Barsky line clipping for efficient swipe-to-tile intersection
- **ShatterEffect** is a simple particle system with per-fragment physics
- The architecture cleanly separates model/engine/render/gesture so later phases slot in without restructuring
