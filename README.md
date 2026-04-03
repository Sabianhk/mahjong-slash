# Mahjong Slash (切牌)

An arcade reflex game where Mahjong tiles float across the screen and players slash valid pairs/sequences with swipe gestures. Fruit Ninja meets Mahjong tile-matching logic.

**Art style:** Ink & Ivory — dark warm backgrounds, thick 3D ivory tiles, calligraphy brush stroke slash trails, ink wash aesthetic.

## Quick Start

1. Open this project in Android Studio (Ladybug 2024.2+)
2. Let Gradle sync (auto-downloads wrapper JAR + dependencies)
3. Run on a device or emulator (API 26+)

## Structure

- `app/` — Android app module (Kotlin + Jetpack Compose)
- `docs/build-prompt.md` — Full game specification
- `STATUS.md` — Build progress and architecture notes

## Tech Stack

- Kotlin, Jetpack Compose, Compose Canvas (no game engine)
- ViewModel + StateFlow for state management
- Portrait orientation only, API 26+

## Current Phase

Phase 1 complete — tiles render, move, and shatter when slashed. Match validation (pairs, sequences, triplets) works. See `STATUS.md` for details.
