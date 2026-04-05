# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mahjong Slash (切牌) — an arcade reflex game where mahjong tiles float across the screen and players slash valid pairs/sequences with swipe gestures. Fruit Ninja meets mahjong tile-matching. Built with Kotlin + Jetpack Compose Canvas (no game engine dependency).

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Release build (ProGuard + resource shrinking enabled)
./gradlew assembleRelease
```

Requires JDK 17, Android SDK 35, min API 26. Set `ANDROID_HOME` if not using Android Studio.

## Architecture

**MVVM with pure-Kotlin game engine:**

- **GameEngine** (`game/engine/GameEngine.kt`) — Core orchestrator. Handles tile spawning, physics, slash detection, scoring, combos, difficulty scaling. Pure Kotlin, no Android dependencies. Single-threaded, produces immutable `GameState` snapshots each frame.
- **GameViewModel** (`viewmodel/GameViewModel.kt`) — Bridges engine to Compose UI via `StateFlow<GameState>`. Handles lifecycle, audio/haptic callbacks via `GameEventListener`.
- **GameScreen** (`ui/screens/GameScreen.kt`) — Compose Canvas rendering. Consumes `GameState` snapshots, never mutates engine state.

**Rendering pipeline** (`game/render/`):
- `TileBitmapCache` pre-renders all 34 tile types to `ImageBitmap` at startup
- `TileRenderer` draws tiles via single `drawImage()` per tile per frame
- `ShatterEffect` — object-pooled particle system (12 fragments, gravity, rotation)
- `SlashTrail` — triple-layer calligraphy brush stroke effect
- `BackgroundRenderer` — pre-rendered rice paper texture

**Game logic:**
- `SlashDetector` (`game/gesture/`) — Liang-Barsky line clipping for swipe-to-tile hit testing
- `DifficultyScaler` — 5 tiers ramping every 30s (spawn rate, tile count, speed)
- `TileType` (`game/model/`) — 34 mahjong tile types with matching rules (pair, sequence, triplet)
- `ObjectPool` (`game/pool/`) — generic allocation-free pooling for particles/trail points

**Data:** `PreferencesManager` (`data/`) — DataStore for settings and high scores (no Room).

## Performance Rules (Non-Negotiable)

These constraints are critical to the game's architecture:

1. **ZERO allocations in the draw loop** — no `Path()`, `Offset()`, `Color.copy()`, `listOf()`, `.filter()/.map()` per frame
2. **All tile bitmaps pre-rendered** at startup in `TileBitmapCache`
3. **State snapshots are immutable** — engine produces new `GameState`, renderer only reads
4. **Object pooling required** for particles, fragments, trail points
5. **Single-threaded engine** — no concurrent mutation of game state

## Theme: Ink & Ivory

Dark warm backgrounds (#1A1714, never pure black), ivory tile faces (#F2E8D5), gold accents (#C49B40), red stamps (#B44033), ink brown (#2C2419). Never blue-grey shadows. See `ui/theme/Color.kt`.

## MCP Tools

`mobile-mcp` is configured in `.claude/settings.json` for device/emulator control and screenshots via adb.

## Key Reference Docs

- `CLAUDE-BUILD-PROMPT.md` — Full game specification, visual direction, mechanics
- `STATUS.md` — Current build state, recent work, testing checklist
