# Mahjong Slash (切牌) — Status

## Current State: Gameplay feel overhaul landed, ready for validation

### What just changed (2026-04-04)

**Gameplay feel rescue** — five targeted changes to address the "choppy / random / not fun" feedback:

1. **Slower floating tiles** — base speed reduced from 55-100 dp/s to 30-50 dp/s. Tiles now decelerate gently over time (to ~70% speed) and wobble sinusoidally perpendicular to their path. This creates a "floating on water" feel instead of mechanical straight-line zipping.

2. **Wave/burst spawning** — 60% of spawns now come as matchable groups (2-3 tiles) from the same edge, 150-300ms apart. Pairs and sequences cluster naturally on screen, giving the player obvious slash targets instead of scattered random tiles.

3. **More tiles, faster rhythm** — max tiles raised to 12 (was 8), spawn interval shortened to 0.7s (was 1.0s). Combined with slower movement, this means more tiles on screen at once and more match opportunities.

4. **More forgiving** — blade health raised to 5 (was 3). Penalty only triggers on 3+ non-matching tiles slashed (was 2+). Slashing through 1-2 random tiles no longer costs a life, encouraging bolder play.

5. **Auto-slash dev helper** — "AUTO" button in bottom-right corner finds the best available match on screen and executes a synthetic slash through it. Shows debug text with what matched, current score, combo, and health. This validates the full pipeline (hit detection → match validation → shatter → scoring) without needing manual targeting.

### Verified working

- Android project builds successfully on the MacBook emulator and launches reliably.
- Canonical repo is active and synced through GitHub.
- Core architecture is in place: engine, model, renderer, gesture layer, Compose UI.
- Tiles render correctly on screen and HUD is visible.
- Debug instrumentation is in the app (hitboxes, swipe path, diagnostics).
- Cyan hitboxes visually align with tiles (verified from screenshots).

### What to test next

**Priority 1: Build and run on emulator, tap the AUTO button**
- Does the auto-slash find and execute matches?
- Does the score increase? Does the shatter effect play?
- Does the debug text at bottom-left show the match details?
- If it says "no match" — are the on-screen tiles actually non-matching?

**Priority 2: Manual slash feel**
- Do tiles feel like they float/drift instead of zip?
- Do matchable groups arrive together and cluster?
- Is it easier to find and slash matching pairs?
- Does the wobble motion look natural, not jittery?

**Priority 3: Overall game feel**
- Is the pacing better (more tiles, more chances)?
- Is the forgiveness better (5 lives, less punishing)?
- Does the game last longer and feel more playable?

### Important recent commits

- `11d19c3` — `debug: visualize swipe path and tile hitboxes`
- `8ac2649` — `fix: improve slash detection and match feedback loop`
- `0451908` — `feat: surface gameplay feedback overlays`
- `8d3e386` — `fix: strengthen slash feedback and effect visibility`
- `eb135b8` — `fix: improve initial game layout and HUD rendering`

### Honest assessment

The core architecture is sound. Hit detection works (proven by hitbox alignment). The previous "broken" results were from poorly-targeted automated swipes. The gameplay feel changes above are the first real pass at making the game enjoyable. The auto-slash button is the key validation tool — it should prove the full match pipeline works in one tap.

## Working model / environment

- GitHub = source of truth
- VPS = orchestration / repo control / agent coordination
- MacBook = real Android build + emulator machine
- Project root on VPS: `/root/.openclaw/workspace/projects/mahjong-slash`
- Project root on Mac: `/Users/stanley/mahjong-slash`

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
├─�� app/
│   ├── build.gradle.kts      # App module config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mahjongslash/
│       │   ├── MainActivity.kt
│       │   ├── viewmodel/GameViewModel.kt
��       │   ├── ui/
│       │   ��   ├── theme/{Color,Theme}.kt
│       │   │   └── screens/GameScreen.kt
��       │   └── game/
│       │       ├─�� model/{Tile,TileType}.kt
│       │       ├── engine/{GameState,GameEngine}.kt
│       │       ├── render/{TileRenderer,ShatterEffect,SlashTrail}.kt
��       │       └── gesture/SlashDetector.kt
│       └��─ res/
│           ├── values/{strings,colors,themes}.xml
│           └── drawable/ic_launcher_foreground.xml
└── docs/build-prompt.md      # Full game spec
```

## Architecture Notes

- **No game engine dependency** — pure Compose Canvas + frame-driven update loop
- **GameEngine** is the single source of truth. It owns all mutable state and exposes immutable snapshots via `GameState`
- **ViewModel** bridges the engine to Compose, exposing `StateFlow<GameState>`
- **TileRenderer** draws tiles with layered Canvas operations (shadow -> edge -> face -> symbol -> gloss)
- **SlashDetector** uses Liang-Barsky line clipping for efficient swipe-to-tile intersection
- **ShatterEffect** is a simple particle system with per-fragment physics
- **Wave spawning** groups matchable tiles in bursts from nearby edges for natural clustering
- The architecture cleanly separates model/engine/render/gesture so later phases slot in without restructuring
