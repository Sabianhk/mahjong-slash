# Asset Generation Instructions

These instructions are for an AI agent (OpenClaw/GPT) to generate assets for Mahjong Slash.
Place all generated files in the paths specified. Do NOT modify any Kotlin/XML source code.

---

## 1. App Icon (512x512 PNG)

**Output path:** `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (512x512)

Also generate these sizes in their respective folders:
- `mipmap-xxhdpi/ic_launcher.png` (192x192)
- `mipmap-xhdpi/ic_launcher.png` (128x128)
- `mipmap-hdpi/ic_launcher.png` (72x72)
- `mipmap-mdpi/ic_launcher.png` (48x48)

**Design spec:**
- A single 3D-looking mahjong tile, slightly angled/rotated ~10-15 degrees
- Ivory face color: `#F2E8D5`
- Visible side edge for 3D depth: `#C4B49A`
- A bold red diagonal slash mark across the tile: `#B44033`
- Background: dark warm brown `#1A1714` (NOT pure black)
- The tile should have a subtle warm shadow underneath
- Style: clean, tactile, premium — NOT cartoony or flat
- The slash should look like a calligraphy brush stroke, thick center tapering at ends
- Optional: a single Chinese character on the tile face (中 or 切) in dark brown `#2C2520`
- NO text like "Mahjong Slash" on the icon itself
- PNG format, no transparency (solid background)

**Also generate Play Store icon:**
- `tmp/play_store_icon_512.png` (512x512, same design, PNG)

---

## 2. Feature Graphic (1024x500 PNG)

**Output path:** `tmp/feature_graphic.png`

**Design spec:**
- Wide banner format, 1024x500 pixels
- Dark warm background: `#1A1714`
- Center: 3-4 mahjong tiles scattered/floating, with a dramatic red calligraphy slash cutting through them
- Ivory shatter fragments flying from the slashed tiles
- Title text: "切牌" in large brush calligraphy style, white/ivory colored
- Subtitle below: "MAHJONG SLASH" in clean sans-serif, gold `#C49B40`
- Overall feel: dramatic, premium, ink wash aesthetic
- No borders or frames
- PNG format

---

## 3. Ambient Music

Generate two loopable music tracks. These should be actual synthesized audio, not silence.

### Menu Music
**Output path:** `app/src/main/res/raw/music_menu.ogg`
- Calm, ambient koto/shamisen melody
- Tempo: slow (~60-70 BPM)
- Duration: 30-60 seconds, seamlessly loopable
- Mood: contemplative, zen garden, warm
- Format: OGG Vorbis, 44.1kHz, mono or stereo
- Keep file size under 500KB

### Gameplay Music
**Output path:** `app/src/main/res/raw/music_gameplay.ogg`
- More energetic than menu, but still ambient/atmospheric
- Subtle percussion (taiko-inspired), light melodic elements
- Tempo: moderate (~90-100 BPM)
- Duration: 45-90 seconds, seamlessly loopable
- Should not be distracting — background texture, not foreground
- Format: OGG Vorbis, 44.1kHz, mono or stereo
- Keep file size under 800KB

---

## 4. Improved SFX (Optional — only if current ones sound bad)

The current SFX in `app/src/main/res/raw/` are 8.7KB each (very short). If they sound like basic placeholder beeps, replace them with better versions:

| File | Description | Duration |
|------|-------------|----------|
| `slash_valid.wav` | Ceramic crack + bright chime | 0.3-0.5s |
| `slash_invalid.wav` | Dull thud + low tone | 0.3-0.5s |
| `tile_shatter.wav` | Satisfying ceramic break | 0.3-0.5s |
| `combo_increment.wav` | Ascending bright note | 0.2-0.3s |
| `combo_break.wav` | Low reverb thud | 0.3-0.5s |
| `blade_crack.wav` | Sharp crack sound | 0.2-0.4s |
| `game_over.wav` | Deep bell toll, resonant | 1.0-2.0s |
| `power_up.wav` | Bright sparkle chime | 0.3-0.5s |
| `menu_tap.wav` | Soft ceramic click | 0.1-0.2s |

**Format:** WAV, 16-bit, 44.1kHz, mono. Keep each file under 50KB.

---

## Verification Checklist

After generating assets, verify:
- [ ] All PNG icons are the correct pixel dimensions
- [ ] All images use the specified color palette (no blue, no pure black, no neon)
- [ ] Music files are valid OGG and play correctly
- [ ] WAV files are valid and play correctly
- [ ] All files are in the correct paths relative to project root
- [ ] No source code was modified
