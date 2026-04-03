# Mahjong Slash — Tooling Assessment & Verification

## Goal
Decide which suggested skills/MCPs are actually necessary for the project right now, keep setup lean, install what is justified, and verify what is working.

## Short Answer
Two tools are now installed directly from GitHub (bypassing ClawHub):

1. **claude-android-ninja** — Agent skill for Android/Kotlin/Compose best practices. Vendored into `.claude/skills/`.
2. **mobile-mcp** — MCP server for device/emulator automation. Configured in `.claude/settings.json`.

See `docs/tooling-setup.md` for full details, bootstrap steps, and blocker status.

---

## Recommendation: Necessary vs Optional

### Necessary now
#### 1) mobile-mcp
**Why:** It gives the strongest future loop for emulator/device inspection, screenshots, taps, swipes, and app validation once the Android SDK/emulator stack exists.

**Status:** Package launch verified.

**Verification performed:**
```bash
npx -y @mobilenext/mobile-mcp@latest --help
```
Result: command returned proper CLI usage.

```bash
timeout 8s npx -y @mobilenext/mobile-mcp@latest --stdio </dev/null >/tmp/mahjong-mobile-mcp-smoke.log 2>&1
```
Result: process started and emitted:
```text
mobile-mcp server running on stdio
```
That confirms the package is resolvable and can start as an MCP server.

**Important limitation:** this does **not** prove end-to-end emulator control yet, because the host currently lacks the Android SDK, emulator, adb, and Java.

---

### Now covered by claude-android-ninja
The following were previously listed as separate "helpful but not necessary" items. They are now **superseded** by the vendored `claude-android-ninja` skill, which bundles all of them:
- Kotlin & Android Development patterns
- Android Jetpack Compose patterns
- Android Platform Core (lifecycle, ViewModel, etc.)
- Architecture, testing, Gradle, navigation, theming references

---

### Optional / later-phase quality tools
#### 6) Android Emulator Skill
Only becomes truly useful once emulator tooling exists on the machine.

#### 7) Kotlin Android Reviewer
Good as a review pass after code exists and builds locally.

#### 8) Game Development Orchestrator
Not necessary. Architecture can be managed directly without another orchestration layer.

#### 9) ADB Pilot
Backup option only. Not needed if mobile-mcp works once Android tooling is installed.

---

## What Was Actually Checked

### Existing local skill inventory
Checked installed local skills under:
- `~/.openclaw/workspace/skills`
- `~/.openclaw/workspace/.agents/skills`
- `/usr/lib/node_modules/openclaw/skills`

Result: none of the proposed Android/Kotlin-specific skills were already installed locally.

### ClawHub availability attempt
Attempted minimal installs for likely candidates:
- `kotlin`
- `android`
- `android-adb`

Result: **blocked by ClawHub rate limiting** during resolution, so no clean installation verification was possible in this run.

### Android host prerequisites
Checked for required binaries:
- `sdkmanager`
- `avdmanager`
- `emulator`
- `adb`
- `java`

Result:
- `sdkmanager`: missing
- `avdmanager`: missing
- `emulator`: missing
- `adb`: missing
- `java`: missing

This means any claim that emulator-control skills are fully working would be fake right now.

---

## Honest Conclusion
If we stay rigorous, the only item that can currently be said to be **installed/launchable and partially verified** is:
- **mobile-mcp** via `npx @mobilenext/mobile-mcp@latest`

But the environment is **not yet ready** to verify Android emulator/device interaction.

---

## Best Next Steps
1. Install the Android toolchain on the host:
   - JDK 17
   - Android SDK command-line tools
   - platform-tools (`adb`)
   - emulator
   - at least one Android system image / AVD
2. Re-run mobile-mcp against an actual emulator.
3. Then optionally install only these extra skills:
   - Kotlin & Android Development
   - Android Jetpack Compose
   - Android Docs MCP
4. Add reviewer/emulator helper skills only if they materially improve the loop.

---

## Lean Recommendation
If the goal is **minimum useful setup**, keep it to:
- **mobile-mcp now**
- **Android SDK/JDK/emulator stack next**
- **Kotlin/Compose skill installs after the toolchain is working**

That’s the non-bloated path.
