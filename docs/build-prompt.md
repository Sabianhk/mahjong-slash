# Build Prompt: Mahjong Slash (切牌)

You are building a complete Android game called **Mahjong Slash (切牌)** from scratch. This is an arcade reflex game where Mahjong tiles float across the screen and players slash valid pairs/sequences with swipe gestures. Think Fruit Ninja meets Mahjong tile-matching logic.

The game needs to be **Play Store ready** as an .aab bundle when done.

---

## Visual Direction

This game must NOT look like a typical vibe-coded app. It should feel hand-crafted, warm, and tactile — like it was made by a small obsessive indie studio.

**Art style: "Ink & Ivory"**
- Dark warm backgrounds (#1A1714, never pure black) with subtle rice paper grain texture
- Tiles rendered as thick 3D-looking Mahjong tiles — ivory face (#F2E8D5), visible side edge for depth (#C4B49A), engraved text/symbols
- Ink wash aesthetic — brush stroke effects, watercolor bleeds, calligraphy-style text for titles
- Accent red (#B44033) used sparingly for stamps and highlights
- Accent gold (#C49B40) for scores and achievements
- Warm brown shadows only, never blue-grey
- No standard Material Design components — buttons should look like tiles, toggles like wooden latches, the whole UI should feel like carved objects on a calligrapher's desk
- Slash trails should look like calligraphy brush strokes, not generic glow lines

**Study these references for visual direction:**
- Okami HD — sumi-e ink wash painting style, brush effects (https://gameuidatabase.com/gameData.php?id=410)
- GRIS — watercolor aesthetic, minimal HUD, letting art breathe (https://www.rachelsharonbooth.com/game-analysis-gris)
- Fruit Ninja — slash mechanic feel, HUD layout, combo feedback (https://www.gameuidatabase.com/gameData.php?id=1312)
- Beautiful Mahjong on Play Store — realistic 3D tile rendering

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose for all menus and overlays
- **Game canvas:** Custom Compose `Canvas` for the gameplay view (2D, no game engine)
- **Rendering:** 2D with parallax depth illusion for tiles (thick tile look via layered drawing, not actual 3D)
- **State:** ViewModel + StateFlow
- **Persistence:** Room DB
- **Audio:** SoundPool for SFX, MediaPlayer for music
- **Target:** Android API 26+, portrait orientation only
- **Output:** Signed .aab for Play Store

---

## The Game

### Tile Set
Use a simplified Mahjong set — 34 unique tiles, 4 copies each = 136 total in the "wall":

- **Characters (萬子):** 1-9 (一萬 through 九萬)
- **Dots (筒子):** 1-9 (一筒 through 九筒)
- **Bamboo (索子):** 1-9 (一索 through 九索)
- **Winds (風牌):** East 東, South 南, West 西, North 北
- **Dragons (三元牌):** Red 中, Green 發, White 白

### Core Loop
1. Tiles spawn from screen edges with randomized entry angles
2. Tiles float across screen in various patterns (linear, sine wave, drift, cluster, curved stream, spiral)
3. Player swipes/slashes across tiles
4. System checks if slashed tiles form a valid Mahjong match:
 - **Pair:** 2 identical tiles → 100 pts
 - **Sequence:** 3 consecutive tiles of same suit (e.g. 1-2-3 of Dots) → 200 pts. Honor tiles can't form sequences. No wrapping (8-9-1 invalid)
 - **Triplet:** 3 identical tiles → 350 pts
5. Valid match → tiles shatter, score awarded, combo increments
6. Invalid slash (tiles don't match) → blade takes 1 damage, combo resets
7. Tiles that exit screen edge without being slashed: no penalty
8. Game over when blade health reaches 0

### Gesture Detection
- Slash = touch down → drag → touch up. Minimum path length 100dp to prevent accidental taps
- Sample swipe path every frame, check intersection with tile bounding rects (+ 8dp padding for forgiveness)
- If swipe crosses multiple tiles, find the highest-scoring valid subset. Remaining non-matching tiles in the swipe path: no penalty
- Tap gesture (for power-ups only): touch down + up within 200ms, movement < 10dp

### Combo System
- Each consecutive match within 3 seconds increases combo
- Combo 1: ×1.0, Combo 2: ×1.5, Combo 3: ×2.0, Combo 4: ×2.5, Combo 5+: ×3.0
- If 3 seconds pass without a match, combo resets to 0

### Blade Health
- 3 health (shown as 3 tiles in HUD — intact → cracked → destroyed)
- Damage on invalid slash or slashing a single tile with no match
- Power-up "Gold Lacquer" restores 1 health (spawns every 45-90s, activated by TAP not slash)

### Difficulty Scaling (Classic mode, per minute survived)
- Minutes 0-1: Linear and drift patterns only, slow speed, max 6 tiles on screen, spawn every 2.0s
- Minutes 1-2: Add sine wave, medium speed, max 8 tiles, spawn every 1.5s
- Minutes 2-3: Add cluster, faster, max 10 tiles, spawn every 1.2s
- Minutes 3-4: Add stream, fast, max 12 tiles, spawn every 0.9s
- Minutes 5+: All patterns, max speed, max 14 tiles, spawn every 0.6s, honor tiles more frequent

### Anti-Frustration
- Always ensure at least 1 valid match exists on screen at any time
- After 3 consecutive wrong slashes, auto-spawn "Reveal" power-up that highlights valid matches for 3 seconds

### Power-Ups (3 total)
1. **Gold Lacquer (金繕い):** Restore 1 blade health. Tap to collect. Spawns every 45-90s as a glowing gold tile fragment.
2. **Slow Ink (墨流し):** All tiles move at 30% speed for 5 seconds. Slash to activate. Spawns every 60-120s as a dark ink droplet.
3. **Reveal (明):** Highlights valid matches for 3 seconds. Auto-triggers after 3 wrong slashes. Matching tiles glow with connected ink lines.

---

## Game Modes

### Classic (経典)
Endless survival. Difficulty ramps every 30 seconds. 3 blade health. Leaderboard by high score.

### Time Attack (速攻)
60-second timer. 5 blade health (more forgiving). Fast tile spawn from the start. Leaderboard by score.

### Zen (禅)
No blade damage, no score pressure. Slow gentle tile patterns. No leaderboard. Player exits when they want.

### Challenge (挑戦)
Daily seeded level (same tile sequence for all players). 3 blade health, no continues. Daily leaderboard. Track consecutive day streaks.

---

## Progression

### Ink Points (IP)
Earned from every game: IP = score ÷ 10 (rounded down). Used to unlock cosmetics.

### Ranks (cumulative IP thresholds)
0 → 書生 Scholar, 1K → 筆者 Writer, 5K → 墨客 Ink Guest, 15K → 書家 Calligrapher, 40K → 達人 Master, 100K → 名人 Grandmaster, 250K → 仙人 Immortal

### Collection
Track all 34 unique tile types: discovered/undiscovered, times matched, first match date. Completion milestones unlock cosmetic rewards.

### Unlockable Cosmetics (all earned via IP, no real money IAP)
**Blade styles** (slash trail effects): Ink Classic (default), Cherry Blossom, Frost, Golden Phoenix, Shadow — cost 2K-10K IP
**Tile skins:** Classic Ivory (default), Jade Stone, Dark Ebony, Paper & Ink — cost 5K-15K IP
**Table backgrounds:** Midnight (default), Bamboo Forest, Temple Garden, Stormy Sea — cost 3K-8K IP

---

## Screens & Flow

```
Splash → Main Menu
 ├── Play → Mode Select → [Game Canvas + HUD Overlay]
 │ ├── Pause → Resume / Restart / Quit
 │ └── Game Over → Results → Play Again / Menu
 ├── Collection (tile gallery showing discovered tiles)
 ├── Leaderboard (local, per mode)
 ├── Shop (browse & purchase cosmetics with IP)
 └── Settings (sound, difficulty, hand preference L/R, tile style, language)
```

### HUD Layout (overlay on game canvas, transparent background)
- **Top-left:** Score (large engraved numerals) + combo counter below (brush stroke marks)
- **Top-right:** 3 blade health tiles + pause button (red seal stamp with 止 character)
- **Center:** Completely clear — gameplay area
- **Bottom:** Power-up indicator (only when available)

### Key Screen Details
- **Splash:** Brush calligraphy 切牌, subtitle "MAHJONG SLASH", single tile visual, red seal stamp version mark
- **Main Menu:** Navigation as tile-shaped buttons, NOT a standard list. Player rank as seal stamp, high score in gold
- **Mode Select:** 4 modes as scroll/card elements with mode name, description, best score. "Begin" as a prominent tile button
- **Results:** Grade stamp (神/極/優/良/初 based on score thresholds), final score, stats grid (tiles cleared, max combo, accuracy, time), play again / menu buttons
- **Collection:** Grid of all 34 tiles. Undiscovered = dark silhouette. Discovered = full render. Tapping shows stats
- **Settings:** Grouped sections, controls styled as physical objects not standard toggles

---

## Audio

Design the sound system with these trigger points. Use placeholder sounds initially — I can replace with real assets later:

- `tile_slash_valid` — ceramic crack + chime (pitch up with combo)
- `tile_slash_invalid` — dull thud
- `tile_shatter` — satisfying ceramic break
- `combo_increment` — ascending musical note
- `combo_break` — low reverb thud
- `blade_crack` — sharp crack
- `game_over` — deep bell toll
- `power_up_collect` — bright chime
- `menu_tap` — soft ceramic click
- Background music: ambient koto/shamisen for menus, intensity builds during gameplay

---

## Build Phases

Work through these in order. Each phase should be fully working before moving to the next:

**Phase 1 — Scaffold + Proof of Life**
Set up the project, dependencies, folder structure. Get a single tile rendering on a Compose Canvas, moving across the screen. Implement basic swipe detection. Tile shatters when slashed. This is just proving the core rendering and input works.

**Phase 2 — Tile System + Match Logic**
Full 34-tile set with proper Mahjong symbols rendered on tile faces. Multiple tiles spawning with movement patterns. Pair/sequence/triplet validation. Valid/invalid slash feedback.

**Phase 3 — Game Loop + HUD**
Score, combo system with multipliers and timeout, blade health, game over state. HUD overlay with all elements. Classic mode fully playable end-to-end.

**Phase 4 — Menus + Navigation**
Splash, main menu, mode select, pause, results screen, settings. Full screen flow wired up with Compose Navigation. Apply the Ink & Ivory visual language to all screens.

**Phase 5 — Remaining Modes + Power-Ups**
Time Attack, Zen, Challenge modes. All 3 power-ups. Difficulty scaling. Audio trigger system with placeholders.

**Phase 6 — Progression + Persistence**
Room DB for player profile, scores, collection, unlocks. Shop screen for cosmetics. Leaderboard (local). Rank progression.

**Phase 7 — Polish + Store Prep**
Visual polish pass (textures, animations, transitions). App icon (512×512), feature graphic (1024×500). Manifest cleanup, ProGuard rules, .aab build, Play Store listing prep.

---

## Important Notes

- Prioritize feel over features. A perfectly polished Phase 3 is worth more than a rushed Phase 7. The slash-and-shatter moment MUST feel satisfying.
- Tile rendering is the visual identity of this game. Spend time making tiles look like thick, carved, real objects — not flat rectangles with text.
- Don't use any game engine or heavy library. Compose Canvas is sufficient for this scope.
- All cosmetics are earned through gameplay (IP). No real-money purchases.
- Portrait orientation only. Respect notch/punch-hole safe areas.
- The game should feel complete and polished at every phase — not like a prototype.
