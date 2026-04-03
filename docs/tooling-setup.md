# Tooling Setup — Claude Code + Android

Two GitHub-sourced tools are integrated for AI-assisted Android development:

## 1. claude-android-ninja (Agent Skill)

**Source:** https://github.com/Drjacky/claude-android-ninja
**Installed at:** `.claude/skills/claude-android-ninja/`

A knowledge skill that Claude Code loads automatically. It provides:
- Production Android architecture patterns (MVVM, Hilt, Room 3, Compose)
- 26 reference guides (compose patterns, coroutines, navigation, testing, etc.)
- Gradle convention plugin templates
- ProGuard, Detekt, version catalog templates

**How it works:** Claude reads `SKILL.md` and the `references/` directory when working on Android/Kotlin code. No runtime dependencies — it's pure documentation/config that improves code generation quality.

**Status: VERIFIED** — Installed and available. No external dependencies needed.

## 2. mobile-mcp (MCP Server)

**Source:** https://github.com/mobile-next/mobile-mcp
**Configured in:** `.claude/settings.json`

An MCP server that gives Claude direct control over Android devices/emulators:
- List devices, install/launch/terminate apps
- Take screenshots, read accessibility trees
- Tap, swipe, type, navigate — all via MCP tools
- Screen recording

**Configuration:**
```json
{
  "mcpServers": {
    "mobile-mcp": {
      "command": "npx",
      "args": ["-y", "@mobilenext/mobile-mcp@latest"]
    }
  }
}
```

**Runtime requirements:**
- Node.js v18+ (v22+ recommended) — **AVAILABLE** (v22.22.1)
- Android Platform Tools (`adb`) — **NOT INSTALLED**
- Java / Android SDK — **NOT INSTALLED**
- A running emulator or connected device — **NOT AVAILABLE**

**Status: PREPARED, NOT FUNCTIONAL** — The MCP config is wired in and will activate automatically once `adb` is on PATH. The server itself is fetched on-demand via `npx` (no vendored code).

## Blockers

| Requirement | Status | To fix |
|---|---|---|
| Node.js 18+ | OK (v22.22.1) | — |
| Java (JDK 17+) | MISSING | `apt install openjdk-17-jdk` or SDKMAN |
| Android SDK / `adb` | MISSING | Install via `sdkmanager` or Android Studio |
| Emulator / device | MISSING | `sdkmanager "emulator" "system-images;..."` or USB device |
| Gradle build | UNTESTED | Needs Java + Android SDK first |

## Quick bootstrap (when SDK is available)

```bash
# 1. Install Android command-line tools (if not using Android Studio)
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools
# Download cmdline-tools from https://developer.android.com/studio#command-tools
# unzip into $ANDROID_HOME/cmdline-tools/latest/

# 2. Install platform tools (provides adb)
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 3. (Optional) Create an emulator
sdkmanager "emulator" "system-images;android-34;google_apis;x86_64"
avdmanager create avd -n mahjong_test -k "system-images;android-34;google_apis;x86_64"
emulator -avd mahjong_test &

# 4. Build the project
./gradlew assembleDebug

# 5. mobile-mcp will auto-detect devices once adb is available
```
