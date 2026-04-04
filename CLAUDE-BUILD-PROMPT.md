# Build Prompt: Mahjong Slash v2 (切牌)

> **Context:** This is a fresh rebuild of Mahjong Slash. The v1 prototype proved the concept works but had deep structural problems (thread safety, allocation-heavy rendering, god class engine, debug code baked into production). This v2 keeps the same visual identity but simplifies mechanics and prioritizes polish + performance over feature count.
>
> **Goal:** A Play Store-ready .aab that looks and feels premium. Smooth 60fps, satisfying slash-and-shatter, sound + haptics. Ship one polished mode, not four broken ones.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose for menus/overlays
- **Game canvas:** Custom Compose `Canvas` for gameplay (2D, no game engine)
- **Architecture:** MVVM — ViewModel + StateFlow, small focused classes (no god classes)
- **Persistence:** DataStore for preferences/high scores (Room is overkill here)
- **Audio:** SoundPool for SFX, MediaPlayer for ambient music
- **Haptics:** `performHapticFeedback()` on match/miss/shatter
- **Target:** Android API 26+, portrait only
- **Output:** Signed .aab for Play Store

---

## CRITICAL: Performance Rules

These are non-negotiable. Every line of rendering code must follow them:

1. **ZERO allocations in the draw loop.** No `Path()`, `Offset()`, `Color.copy()`, `listOf()`, `.filter()`, `.map()`, `TextMeasurer.measure()` inside any per-frame code. Pre-allocate, cache, or pool everything.
2. **Tile bitmaps must be pre-rendered.** Draw each tile type once into an `ImageBitmap` cache at startup. During gameplay, just `drawImage()` — never re-draw tile faces per frame.
3. **Background texture must be a pre-rendered bitmap.** Draw the rice paper grain once into an `ImageBitmap`, then blit it every frame. Never draw individual circles/dots in a loop.
4. **State snapshots must be truly immutable.** Use immutable data classes with `List` (not `MutableList`). Game logic produces a new snapshot; renderer only reads it. No mutable objects leaking through.
5. **Object pooling for particles/fragments.** Shatter fragments, slash trail points, and floating text should come from fixed-size pools — never allocate/GC in the hot path.
6. **Split state flows.** Separate `StateFlow` for render state (changes every frame) vs HUD state (changes on score/combo/health events). Prevents recomposition storms on HUD composables.
7. **All game logic on a single thread.** Engine update + gesture callbacks must be synchronized. Use `Mutex` or channel-based message passing — never concurrent mutation.
8. **No debug code in production.** Use `BuildConfig.DEBUG` guards or strip entirely. Never ship hitbox overlays or auto-play.

---

## Visual Direction: "Ink & Ivory"

This game must NOT look like a typical vibe-coded app. It should feel hand-crafted, warm, and tactile.

### Color Palette
```
Background:       #1A1714 (dark warm, never pure black)
Tile Face:        #F2E8D5 (ivory)
Tile Edge:        #C4B49A (depth shadow)
Tile Engraving:   #2C2520 (dark brown for characters)
Accent Red:       #B44033 (stamps, highlights, dragon tiles)
Accent Gold:      #C49B40 (scores, achievements, rank)
Accent Green:     #4A7C59 (green dragon, nature elements)
Warm Shadow:      #0D0B09 (brown-black, never blue-grey)
Rice Paper:       #E8DCC8 (menu backgrounds, overlays)
```

### Visual Rules
- Tiles = thick 3D-looking mahjong tiles — ivory face, visible side edge for depth, engraved text/symbols
- Ink wash aesthetic — brush stroke effects, calligraphy-style text for titles
- Slash trails = calligraphy brush strokes (thick center, tapered ends), NOT generic glow lines
- Warm brown shadows only, never blue-grey
- No standard Material Design components — buttons look like tiles, the whole UI feels like carved objects on a calligrapher's desk
- Shatter effect = tile cracks into ivory fragments that tumble with physics

### Reference Games (for visual direction only)
- Okami HD — sumi-e ink brush effects
- GRIS — watercolor aesthetic, minimal HUD
- Fruit Ninja — slash mechanic feel, combo feedback
- Beautiful Mahjong (Play Store) — realistic 3D tile rendering

---

## Simplified Game Mechanics

### Tile Set
34 unique tiles, 4 copies each = 136 in the wall:
- **Characters (萬子):** 1-9 (一萬 through 九萬)
- **Dots (筒子):** 1-9 (一筒 through 九筒)
- **Bamboo (索子):** 1-9 (一索 through 九索)
- **Winds (風牌):** East 東, South 南, West 西, North 北
- **Dragons (三元牌):** Red 中, Green 發, White 白

### Core Loop (SIMPLIFIED — pairs only)
1. Tiles spawn from screen edges, float across in various patterns
2. Player swipes/slashes across tiles
3. If slashed tiles contain a **matching pair** (2 identical tiles) → valid match
4. Valid match → tiles shatter, 100 pts awarded, combo increments
5. Invalid slash (no pair in slashed tiles) → blade takes 1 damage, combo resets
6. Tiles that exit screen: no penalty
7. Game over when blade health reaches 0

### Gesture Detection
- Slash = touch down → drag → touch up. Minimum path 100dp
- Sample swipe path, check intersection with tile bounds (+8dp padding for forgiveness)
- If swipe crosses multiple tiles, find any valid pair. Remaining non-matching tiles: no penalty
- Velocity-based hit prediction for fast-moving tiles

### Combo System
- Consecutive matches within 3 seconds increase combo
- Combo 1: x1.0, Combo 2: x1.5, Combo 3: x2.0, Combo 4: x2.5, Combo 5+: x3.0
- 3 seconds without a match → combo resets
- Visual: combo counter with ink brush number, screen edge pulses on high combos

### Blade Health
- 3 health (shown as 3 tile icons in HUD — intact → cracked → gone)
- Damage on invalid slash only
- Power-up "Gold Lacquer" restores 1 health (spawns every 45-90s, tap to collect)

### Difficulty Scaling (per 30 seconds survived)
- 0-30s: Slow, linear movement, max 6 tiles, spawn every 2.0s
- 30-60s: Add sine wave, medium speed, max 8 tiles, spawn every 1.5s
- 60-90s: Add drift/cluster, faster, max 10 tiles, spawn every 1.2s
- 90-120s: Fast, max 12 tiles, spawn every 0.9s
- 120s+: Max speed, max 14 tiles, spawn every 0.6s

### Anti-Frustration
- Always ensure at least 1 valid pair exists on screen
- After 3 consecutive misses, highlight valid pairs for 3 seconds (ink glow effect)

---

## One Game Mode: Classic (経典)

Endless survival. Difficulty ramps. 3 blade health. Local high score.

That's it. One mode, done well.

---

## Screens & Flow

```
Splash → Main Menu → Game → Pause / Game Over → Play Again / Menu
                ├── High Scores (local, simple list)
                └── Settings (sound, haptics)
```

### HUD (overlay on game canvas)
- **Top-left:** Score (large, gold) + combo counter below
- **Top-right:** 3 blade health tiles + pause button (red seal stamp 止)
- **Center:** Clear — gameplay area
- **Bottom:** Power-up indicator when available

### Splash
- Brush calligraphy 切牌, subtitle "MAHJONG SLASH"
- Single tile visual, red seal stamp
- Tap anywhere to continue

### Main Menu
- Tile-shaped buttons (Play, High Scores, Settings)
- High score in gold, last score shown
- Background: subtle floating tiles in slow motion (reuse game renderer)

### Game Over / Results
- Grade stamp (based on score thresholds): 神 Divine / 極 Extreme / 優 Excellent / 良 Good / 初 Novice
- Final score, tiles cleared, max combo, accuracy %
- Play Again / Menu buttons as tile shapes

### Settings
- Sound on/off
- Haptics on/off
- Hand preference (left/right — shifts pause button)
- Styled as physical objects, not Material toggles

---

## Audio Design

Implement with `SoundPool` for SFX, `MediaPlayer` for music. Use placeholder `.ogg` files initially — create simple synthesized sounds or silence files as stubs.

### SFX Triggers
- `slash_valid` — ceramic crack + bright chime (pitch up with combo level)
- `slash_invalid` — dull thud + low tone
- `tile_shatter` — satisfying ceramic break (multiple pitch variants)
- `combo_increment` — ascending musical note
- `combo_break` — low reverb thud
- `blade_crack` — sharp crack sound
- `game_over` — deep bell toll, resonant
- `power_up` — bright sparkle chime
- `menu_tap` — soft ceramic click

### Music
- Menu: ambient koto, calm
- Gameplay: starts minimal, layers build as difficulty ramps

### Haptics
- Valid slash: `HapticFeedbackConstants.CONFIRM`
- Invalid slash: `HapticFeedbackConstants.REJECT`  
- Tile shatter: short vibration burst
- Game over: long vibration

---

## Architecture (Small, Focused Classes)

```
com.mahjongslash/
├── MainActivity.kt              (immersive mode, single activity)
├── game/
│   ├── engine/
│   │   ├── GameLoop.kt          (frame timing, coordinates update + render)
│   │   ├── TileSpawner.kt       (spawn logic, ensures valid pairs exist)
│   │   ├── TilePhysics.kt       (movement patterns, screen bounds)
│   │   ├── MatchValidator.kt    (pair detection from slashed tiles)
│   │   ├── ComboTracker.kt      (combo state, multiplier, timeout)
│   │   ├── DifficultyScaler.kt  (ramps speed/count/rate over time)
│   │   └── GameEngine.kt        (thin coordinator, delegates to above)
│   ├── model/
│   │   ├── TileType.kt          (enum-like: suit, rank, display char)
│   │   ├── Tile.kt              (immutable position/state, ID for pooling)
│   │   └── GameState.kt         (truly immutable snapshot for renderer)
│   ├── gesture/
│   │   └── SlashDetector.kt     (swipe path → hit tiles, with velocity prediction)
│   ├── render/
│   │   ├── TileBitmapCache.kt   (pre-renders tile faces into ImageBitmap)
│   │   ├── BackgroundRenderer.kt (pre-rendered rice paper grain texture)
│   │   ├── TileRenderer.kt      (draws cached bitmaps at positions)
│   │   ├── ShatterEffect.kt     (pooled fragment particles)
│   │   ├── SlashTrail.kt        (calligraphy brush stroke trail)
│   │   └── FloatingText.kt      (score popup, pooled)
│   ├── audio/
│   │   └── AudioManager.kt      (SoundPool + MediaPlayer wrapper)
│   └── pool/
│       └── ObjectPool.kt        (generic fixed-size pool)
├── ui/
│   ├── screens/
│   │   ├── SplashScreen.kt
│   │   ├── MainMenuScreen.kt
│   │   ├── GameScreen.kt        (Canvas + HUD overlay, NO debug code)
│   │   ├── GameOverScreen.kt
│   │   ├── HighScoresScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Type.kt              (custom brush/calligraphy fonts)
│       └── Theme.kt
├── data/
│   └── PreferencesManager.kt    (DataStore for scores + settings)
└── viewmodel/
    └── GameViewModel.kt         (lifecycle-aware, coroutine scopes)
```

---

## Build Phases

Each phase must be fully working and smooth before moving on.

### Phase 1 — Scaffold + Rendering Foundation
- New Android project, dependencies, folder structure
- Pre-rendered tile bitmap cache (all 34 tile types → `ImageBitmap`)
- Pre-rendered background texture
- Single tile on screen, moving smoothly at 60fps
- Basic swipe detection → tile shatters (pooled fragments)
- Sound stub files + SoundPool wired up
- **Checkpoint:** One tile moves, you slash it, it shatters with sound. Buttery smooth.

### Phase 2 — Core Gameplay
- TileSpawner with movement patterns (linear, sine, drift)
- Multiple tiles on screen, pair matching logic
- Slash → find pair → valid/invalid feedback
- Score, combo system, blade health, game over
- HUD overlay (score, combo, health, pause)
- Haptic feedback on all interactions
- Difficulty scaling over time
- Anti-frustration pair guarantee
- **Checkpoint:** Fully playable Classic mode. Feels good to play.

### Phase 3 — Screens + Flow
- Splash → Main Menu → Game → Game Over → Play Again loop
- Ink & Ivory styling on ALL screens (no Material defaults)
- High scores with DataStore persistence
- Settings (sound, haptics, hand preference)
- Navigation wired with Compose Navigation
- **Checkpoint:** Complete app flow, looks polished, everything styled.

### Phase 4 — Polish + Play Store
- Visual polish: transitions between screens, particle effects, screen shake on damage
- Audio: ambient music, all SFX with correct pitch/timing
- Immersive mode (hide system bars)
- App icon (512x512), feature graphic (1024x500)
- ProGuard/R8 rules verified
- Signed .aab build
- Play Store listing assets
- **Checkpoint:** Ready to upload to Play Console.

---

## Important Reminders

- **Performance over features.** A silky-smooth Phase 2 beats a janky Phase 4. Profile with Android Studio Profiler after each phase.
- **No debug code in production.** Use `BuildConfig.DEBUG` or don't write it at all.
- **Test on a real device** (not just emulator) — especially for haptics, touch latency, and frame rate.
- **Portrait only.** Respect notch/cutout safe areas with `WindowInsets`.
- **The slash-and-shatter moment must feel AMAZING.** If this one interaction doesn't feel satisfying, nothing else matters.
