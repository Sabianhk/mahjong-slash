package com.mahjongslash.game.engine

import androidx.compose.ui.geometry.Offset
import com.mahjongslash.game.gesture.SlashDetector
import com.mahjongslash.game.model.Tile
import com.mahjongslash.game.model.TileSet
import com.mahjongslash.game.model.TileState
import com.mahjongslash.game.model.TileType
import com.mahjongslash.game.render.ShatterEffect
import com.mahjongslash.game.render.SlashTrail
import com.mahjongslash.game.render.SlashTrailPoint
import com.mahjongslash.ui.theme.AccentGold
import com.mahjongslash.ui.theme.AccentGoldBright
import com.mahjongslash.ui.theme.AccentRed
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Core game engine. Manages tile spawning, movement, slash detection,
 * match validation, and score/combo tracking.
 */
/**
 * Callback for audio/haptic events. Decouples engine from Android framework.
 */
interface GameEventListener {
    fun onSlashValid(comboLevel: Int)
    fun onSlashInvalid()
    fun onComboIncrement(comboLevel: Int)
    fun onComboBreak()
    fun onBladeDamage(remainingHealth: Int)
    fun onGameOver()
}

class GameEngine {
    var eventListener: GameEventListener? = null
    private var nextInstanceId = 0L
    private var screenW = 0f
    private var screenH = 0f
    private var density = 1f

    // Live game objects
    private val tiles = mutableListOf<Tile>()
    private val shatterEffects = mutableListOf<ShatterEffect>()
    private val slashTrails = mutableListOf<SlashTrail>()
    private val floatingTexts = mutableListOf<FloatingText>()

    // Scoring
    private var score = 0
    private var combo = 0
    private var lastMatchTime = 0L
    private var bladeHealth = 3

    // Screen flash
    private var flashAlpha = 0f
    private var flashColor = AccentGold

    // Spawning
    private var timeSinceLastSpawn = 0f
    private val difficulty = DifficultyScaler()
    private val rng = Random(System.nanoTime())

    // Wave/burst spawning: queue of tiles to spawn in quick succession
    private data class QueuedSpawn(val type: TileType, val edge: Int, val laneOffset: Float)
    private val spawnQueue = mutableListOf<QueuedSpawn>()
    private var burstDelay = 0f

    // Matchability pool
    private val recentPool = mutableListOf<TileType>()
    private val poolSize = 6
    private var tilesSpawnedFromPool = 0
    private val poolRefreshInterval = 12

    // Echo spawn: probabilistically spawn a tile that matches something on screen
    private val echoChance = 0.55f

    // Current active slash
    private var activeSlashPoints = mutableListOf<Offset>()
    private var activeTrail: SlashTrail? = null
    private var lastSlashPosition: Offset = Offset.Zero

    // Combo timing
    private val comboTimeoutMs = 3000L

    // Stats tracking
    private var tilesCleared = 0
    private var maxCombo = 0
    private var totalSlashes = 0
    private var validSlashes = 0

    // Hint system: after 3 consecutive misses, highlight a valid pair
    private var consecutiveMisses = 0
    private var hintTileIds = setOf<Long>()
    private var hintExpireTime = 0L

    // Screen shake
    private var shakeIntensity = 0f
    private var shakeOffsetX = 0f
    private var shakeOffsetY = 0f

    // Slow-motion on big combos
    private var timeDilation = 1f       // 1.0 = normal, 0.3 = slow-mo
    private var timeDilationRemaining = 0f

    // Game over animation
    private var gameOverAnimPhase = false
    private var gameOverAnimElapsed = 0f
    private var gameOverNextShatterIdx = 0
    private val GAME_OVER_FREEZE_SECS = 0.4f       // Freeze before shattering starts
    private val GAME_OVER_SHATTER_INTERVAL = 0.08f  // Time between each tile shattering
    private val GAME_OVER_TOTAL_SECS = 2.5f         // Total anim before transitioning
    private var frozenTileOrder = mutableListOf<Long>() // order to shatter tiles

    // Frame counter — ensures every GameState snapshot is unique for StateFlow emission
    private var frameCount = 0L

    // How long a shattering tile stays visible (fading out alongside fragments)
    private val SHATTER_VISIBLE_SECS = 0.35f

    fun initialize(width: Float, height: Float, density: Float) {
        screenW = width
        screenH = height
        this.density = density
        tiles.clear()
        shatterEffects.clear()
        slashTrails.clear()
        floatingTexts.clear()
        score = 0
        combo = 0
        bladeHealth = 3
        flashAlpha = 0f
        paused = false
        tilesCleared = 0
        maxCombo = 0
        totalSlashes = 0
        validSlashes = 0
        difficulty.reset()
        consecutiveMisses = 0
        hintTileIds = emptySet()
        hintExpireTime = 0L
        shakeIntensity = 0f
        shakeOffsetX = 0f
        shakeOffsetY = 0f
        timeDilation = 1f
        timeDilationRemaining = 0f
        gameOverAnimPhase = false
        gameOverAnimElapsed = 0f
        gameOverNextShatterIdx = 0
        frozenTileOrder.clear()
        timeSinceLastSpawn = difficulty.spawnInterval
        spawnQueue.clear()
        burstDelay = 0f
        refreshPool()
        tilesSpawnedFromPool = 0
    }

    /**
     * Main update tick. Called every frame with delta time in seconds.
     */
    private var paused = false

    fun pause() { paused = true }
    fun resume() {
        paused = false
        // Reset timing so we don't get a huge dt spike after unpause
        lastMatchTime = System.currentTimeMillis()
    }

    fun update(dt: Float): GameState {
        if (screenW == 0f) return GameState()
        if (paused) return snapshot().copy(phase = GamePhase.PAUSED)

        // Game over animation phase
        if (gameOverAnimPhase) {
            return updateGameOverAnim(dt)
        }
        if (bladeHealth <= 0 && !gameOverAnimPhase) {
            startGameOverAnim()
            return updateGameOverAnim(dt)
        }

        // Slow-motion: decay timer in real time, but dilate game dt
        if (timeDilationRemaining > 0f) {
            timeDilationRemaining -= dt
            if (timeDilationRemaining <= 0f) {
                timeDilation = 1f
                timeDilationRemaining = 0f
            }
        }
        val gameDt = dt * timeDilation

        val now = System.currentTimeMillis()

        // Check combo timeout
        if (combo > 0 && now - lastMatchTime > comboTimeoutMs) {
            combo = 0
        }

        // Process burst spawn queue (rapid successive spawns for grouped tiles)
        if (spawnQueue.isNotEmpty()) {
            burstDelay -= gameDt
            if (burstDelay <= 0f) {
                val queued = spawnQueue.removeAt(0)
                spawnTileFromQueue(queued)
                burstDelay = 0.15f + rng.nextFloat() * 0.15f // 150-300ms between burst tiles
            }
        }

        // Spawn tiles (or queue a burst group)
        timeSinceLastSpawn += gameDt
        if (spawnQueue.isEmpty() && timeSinceLastSpawn >= difficulty.spawnInterval &&
            tiles.count { it.state == TileState.ALIVE } < difficulty.maxTiles) {
            queueSpawnGroup()
            timeSinceLastSpawn = 0f
        }

        // Anti-frustration: ensure at least 1 valid pair exists on screen
        ensurePairExists()

        // Expire hint highlight
        if (hintTileIds.isNotEmpty() && now > hintExpireTime) {
            hintTileIds = emptySet()
        }

        // Update tile positions — gentle deceleration + sinusoidal wobble for floating feel
        for (tile in tiles) {
            if (tile.state == TileState.ALIVE) {
                // Gentle deceleration: tiles slow to ~70% speed over 4 seconds
                val age = (now - tile.spawnTime) / 1000f
                val speedFactor = 0.7f + 0.3f / (1f + age * 0.5f)

                // Sinusoidal wobble perpendicular to velocity direction
                val vLen = kotlin.math.sqrt(tile.velocity.x * tile.velocity.x + tile.velocity.y * tile.velocity.y)
                val wobbleAmp = 12f * density // gentle 12dp side-to-side
                val wobbleFreq = 1.5f + (tile.instanceId % 3) * 0.3f // slight per-tile variation
                val wobblePhase = (tile.instanceId * 1.7f) // unique starting phase
                val wobble = sin((age * wobbleFreq + wobblePhase).toDouble()).toFloat() * wobbleAmp * gameDt
                val perpX = if (vLen > 0.01f) -tile.velocity.y / vLen else 0f
                val perpY = if (vLen > 0.01f) tile.velocity.x / vLen else 0f

                tile.position = Offset(
                    tile.position.x + tile.velocity.x * speedFactor * gameDt + perpX * wobble,
                    tile.position.y + tile.velocity.y * speedFactor * gameDt + perpY * wobble
                )
            }
        }

        // Remove tiles that have left the screen
        val margin = Tile.WIDTH_DP * density * 2
        tiles.removeAll { tile ->
            tile.state == TileState.ALIVE && (
                tile.position.x < -margin || tile.position.x > screenW + margin ||
                tile.position.y < -margin || tile.position.y > screenH + margin
            )
        }

        // Update shattering tiles: fade out then mark dead
        for (tile in tiles) {
            if (tile.state == TileState.SHATTERING) {
                tile.shatterElapsed += gameDt
                val progress = (tile.shatterElapsed / SHATTER_VISIBLE_SECS).coerceIn(0f, 1f)
                tile.alpha = 1f - progress
                if (tile.shatterElapsed >= SHATTER_VISIBLE_SECS) {
                    tile.state = TileState.DEAD
                }
            }
        }

        // Remove dead tiles
        tiles.removeAll { it.state == TileState.DEAD }

        // Update shatter effects
        for (effect in shatterEffects) {
            effect.update(gameDt)
        }
        val deadEffects = shatterEffects.filter { !it.isAlive }
        for (dead in deadEffects) ShatterEffect.recycle(dead)
        shatterEffects.removeAll { !it.isAlive }

        // Update slash trail fading
        for (trail in slashTrails) {
            if (trail.isFading) {
                trail.fadeAlpha -= dt / (SlashTrail.FADE_DURATION_MS / 1000f)
            }
        }
        slashTrails.removeAll { !it.isAlive }

        // Update floating texts
        for (ft in floatingTexts) {
            ft.elapsed += gameDt
        }
        floatingTexts.removeAll { !it.isAlive }

        // Decay screen flash
        if (flashAlpha > 0f) {
            flashAlpha = (flashAlpha - gameDt * 3f).coerceAtLeast(0f)
        }

        // Decay screen shake
        if (shakeIntensity > 0.5f) {
            shakeOffsetX = (rng.nextFloat() - 0.5f) * 2f * shakeIntensity
            shakeOffsetY = (rng.nextFloat() - 0.5f) * 2f * shakeIntensity
            shakeIntensity *= (1f - gameDt * 8f).coerceAtLeast(0f) // fast decay
        } else {
            shakeIntensity = 0f
            shakeOffsetX = 0f
            shakeOffsetY = 0f
        }

        return snapshot()
    }

    fun onSlashStart(position: Offset) {
        activeSlashPoints.clear()
        activeSlashPoints.add(position)
        activeTrail = SlashTrail().also {
            it.points.add(SlashTrailPoint(position, System.currentTimeMillis()))
            slashTrails.add(it)
        }
    }

    fun onSlashMove(position: Offset) {
        activeSlashPoints.add(position)
        activeTrail?.points?.add(SlashTrailPoint(position, System.currentTimeMillis()))
        lastSlashPosition = position
    }

    fun onSlashEndAtLastPosition() {
        onSlashEnd(lastSlashPosition)
    }

    fun onSlashEnd(position: Offset) {
        activeSlashPoints.add(position)
        activeTrail?.points?.add(SlashTrailPoint(position, System.currentTimeMillis()))
        activeTrail?.isFading = true

        val slashResult = SlashDetector.detectSlashedTiles(
            activeSlashPoints, tiles.filter { it.state == TileState.ALIVE }, density
        )
        val slashed = slashResult.tiles

        if (slashed.isNotEmpty()) {
            val matchResult = findPairMatch(slashed)

            if (matchResult != null) {
                // Valid pair match — shatter, score, flash gold
                for (tile in matchResult.tiles) {
                    tile.state = TileState.SHATTERING
                    shatterEffects.add(ShatterEffect.create(tile, density))
                }

                combo++
                lastMatchTime = System.currentTimeMillis()
                val multiplier = comboMultiplier(combo)
                val points = (matchResult.baseScore * multiplier).toInt()
                score += points

                activeTrail?.resultColor = AccentGoldBright

                val cx = matchResult.tiles.map { it.position.x }.average().toFloat()
                val cy = matchResult.tiles.map { it.position.y }.average().toFloat()
                val label = if (combo > 1) "+$points ×${comboMultiplierText(combo)}" else "+$points"
                floatingTexts.add(FloatingText(
                    position = Offset(cx, cy),
                    text = label,
                    color = AccentGoldBright,
                ))

                // Combo escalation text — fighting game style
                val comboLabel = comboEscalationLabel(combo)
                if (comboLabel != null) {
                    floatingTexts.add(FloatingText(
                        position = Offset(screenW / 2f, screenH * 0.35f),
                        text = comboLabel,
                        color = AccentGoldBright,
                        duration = 1.2f,
                        scale = comboEscalationScale(combo),
                    ))
                }

                flashAlpha = 0.25f
                flashColor = AccentGold

                // Slow-mo on 3+ combo — 0.3x time for 200ms
                if (combo >= 3) {
                    timeDilation = 0.3f
                    timeDilationRemaining = 0.2f
                }

                tilesCleared += matchResult.tiles.size
                if (combo > maxCombo) maxCombo = combo
                totalSlashes++
                validSlashes++
                consecutiveMisses = 0
                hintTileIds = emptySet()

                eventListener?.onSlashValid(combo)
                if (combo > 1) eventListener?.onComboIncrement(combo)
            } else {
                // No pair found — invalid slash, blade takes damage
                totalSlashes++
                consecutiveMisses++
                if (consecutiveMisses >= 3) {
                    activateHint()
                }
                val hadCombo = combo > 0
                combo = 0
                bladeHealth = (bladeHealth - 1).coerceAtLeast(0)

                activeTrail?.resultColor = AccentRed

                val cx = slashed.map { it.position.x }.average().toFloat()
                val cy = slashed.map { it.position.y }.average().toFloat()
                floatingTexts.add(FloatingText(
                    position = Offset(cx, cy),
                    text = "MISS",
                    color = AccentRed,
                ))

                flashAlpha = 0.2f
                flashColor = AccentRed
                shakeIntensity = 12f * density // screen shake on damage

                eventListener?.onSlashInvalid()
                eventListener?.onBladeDamage(bladeHealth)
                if (hadCombo) eventListener?.onComboBreak()
                if (bladeHealth <= 0) eventListener?.onGameOver()
            }
        }

        activeSlashPoints.clear()
        activeTrail = null
    }

    private fun comboMultiplier(combo: Int): Float = when {
        combo <= 1 -> 1.0f
        combo == 2 -> 1.5f
        combo == 3 -> 2.0f
        combo == 4 -> 2.5f
        else -> 3.0f
    }

    private fun comboMultiplierText(combo: Int): String = when {
        combo <= 1 -> "1.0"
        combo == 2 -> "1.5"
        combo == 3 -> "2.0"
        combo == 4 -> "2.5"
        else -> "3.0"
    }

    private fun findPairMatch(slashed: List<Tile>): MatchResult? {
        val candidates = slashed.filter { it.state == TileState.ALIVE }
        if (candidates.size < 2) return null

        // Find any matching pair (2 identical tiles)
        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                if (TileSet.isPair(candidates[i].type, candidates[j].type)) {
                    return MatchResult(listOf(candidates[i], candidates[j]), 100)
                }
            }
        }

        return null
    }

    /**
     * Build a pool of tile types. Includes a duplicate so pairs are always possible.
     */
    private fun refreshPool() {
        recentPool.clear()
        val shuffled = TileSet.allTypes.shuffled(rng)
        for (t in shuffled) {
            if (recentPool.size >= poolSize - 1) break
            recentPool.add(t)
        }
        // Add a duplicate so a pair is always possible from pool
        recentPool.add(recentPool[rng.nextInt(recentPool.size)])
        recentPool.shuffle(rng)
    }

    /**
     * Pick the next tile type, biased toward matchability.
     * With [echoChance] probability, spawn a duplicate of a tile already on screen.
     */
    private fun pickTileType(): TileType {
        tilesSpawnedFromPool++
        if (tilesSpawnedFromPool > poolRefreshInterval) {
            val keep = recentPool.take(poolSize / 2)
            refreshPool()
            for (t in keep) {
                if (t !in recentPool && recentPool.size < poolSize) recentPool.add(t)
            }
            tilesSpawnedFromPool = 0
        }

        // Echo spawn: duplicate something already on screen (pair-ready)
        val alive = tiles.filter { it.state == TileState.ALIVE }
        if (alive.isNotEmpty() && rng.nextFloat() < echoChance) {
            return alive[rng.nextInt(alive.size)].type
        }

        return recentPool[rng.nextInt(recentPool.size)]
    }

    /**
     * Queue a group of tiles to spawn. ~60% of the time spawns a pair
     * from the same edge so they cluster. Otherwise spawns a single tile.
     */
    private fun queueSpawnGroup() {
        val edge = rng.nextInt(4)
        val baseLane = rng.nextFloat()

        if (rng.nextFloat() < 0.6f) {
            // Burst: spawn a matching pair from nearby positions
            val type = pickTileType()
            spawnQueue.add(QueuedSpawn(type, edge, baseLane))
            val laneJitter = (rng.nextFloat() - 0.5f) * 0.15f
            spawnQueue.add(QueuedSpawn(type, edge, (baseLane + laneJitter).coerceIn(0f, 1f)))
            burstDelay = 0f
        } else {
            // Single tile spawn
            spawnQueue.add(QueuedSpawn(pickTileType(), edge, baseLane))
            burstDelay = 0f
        }
    }

    private fun spawnTileFromQueue(queued: QueuedSpawn) {
        val tileW = Tile.WIDTH_DP * density
        val tileH = Tile.HEIGHT_DP * density

        val baseSpeed = difficulty.baseSpeedDpPerSec + rng.nextFloat() * difficulty.speedVarianceDpPerSec
        val speed = baseSpeed * density

        val topInset = 60f * density
        val bottomInset = 60f * density
        val playTop = topInset + tileH
        val playBottom = screenH - bottomInset - tileH
        val playHeight = playBottom - playTop

        val lane = queued.laneOffset

        val (startPos, vel) = when (queued.edge) {
            0 -> {
                val y = playTop + lane * playHeight
                val angle = -25f + rng.nextFloat() * 50f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(-tileW, y) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            1 -> {
                val y = playTop + lane * playHeight
                val angle = 155f + rng.nextFloat() * 50f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(screenW + tileW, y) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            2 -> {
                val x = tileW + lane * (screenW - tileW * 2)
                val angle = 60f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(x, -tileH) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            else -> {
                val x = tileW + lane * (screenW - tileW * 2)
                val angle = -120f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(x, screenH + tileH) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
        }

        tiles.add(
            Tile(
                instanceId = nextInstanceId++,
                type = queued.type,
                position = startPos,
                velocity = vel,
                spawnTime = System.currentTimeMillis(),
            )
        )
    }

    /**
     * Highlight a valid pair on screen for 3 seconds.
     */
    private fun activateHint() {
        val alive = tiles.filter { it.state == TileState.ALIVE }
        for (i in alive.indices) {
            for (j in i + 1 until alive.size) {
                if (alive[i].type.id == alive[j].type.id) {
                    hintTileIds = setOf(alive[i].instanceId, alive[j].instanceId)
                    hintExpireTime = System.currentTimeMillis() + 3000L
                    consecutiveMisses = 0
                    return
                }
            }
        }
    }

    /**
     * If no valid pair exists among alive tiles, queue a duplicate of a random alive tile.
     */
    private fun ensurePairExists() {
        val alive = tiles.filter { it.state == TileState.ALIVE }
        if (alive.size < 2) return

        // Check if any pair exists
        for (i in alive.indices) {
            for (j in i + 1 until alive.size) {
                if (alive[i].type.id == alive[j].type.id) return // pair exists
            }
        }

        // No pair found — force-queue a duplicate of a random alive tile
        val target = alive[rng.nextInt(alive.size)]
        val edge = rng.nextInt(4)
        val lane = rng.nextFloat()
        spawnQueue.add(QueuedSpawn(target.type, edge, lane))
        burstDelay = 0f
    }

    private fun startGameOverAnim() {
        gameOverAnimPhase = true
        gameOverAnimElapsed = 0f
        gameOverNextShatterIdx = 0
        // Order: shatter from center outward
        val cx = screenW / 2f
        val cy = screenH / 2f
        frozenTileOrder.clear()
        tiles.filter { it.state == TileState.ALIVE }
            .sortedBy { val dx = it.position.x - cx; val dy = it.position.y - cy; dx * dx + dy * dy }
            .forEach { frozenTileOrder.add(it.instanceId) }
    }

    private fun updateGameOverAnim(dt: Float): GameState {
        gameOverAnimElapsed += dt

        // After freeze period, start shattering tiles one by one
        if (gameOverAnimElapsed > GAME_OVER_FREEZE_SECS && gameOverNextShatterIdx < frozenTileOrder.size) {
            val shatterTime = gameOverAnimElapsed - GAME_OVER_FREEZE_SECS
            val targetIdx = (shatterTime / GAME_OVER_SHATTER_INTERVAL).toInt()
                .coerceAtMost(frozenTileOrder.size)

            while (gameOverNextShatterIdx < targetIdx) {
                val tileId = frozenTileOrder[gameOverNextShatterIdx]
                val tile = tiles.find { it.instanceId == tileId && it.state == TileState.ALIVE }
                if (tile != null) {
                    tile.state = TileState.SHATTERING
                    shatterEffects.add(ShatterEffect.create(tile, density))
                }
                gameOverNextShatterIdx++
            }
        }

        // Update shattering tiles
        for (tile in tiles) {
            if (tile.state == TileState.SHATTERING) {
                tile.shatterElapsed += dt
                val progress = (tile.shatterElapsed / SHATTER_VISIBLE_SECS).coerceIn(0f, 1f)
                tile.alpha = 1f - progress
                if (tile.shatterElapsed >= SHATTER_VISIBLE_SECS) {
                    tile.state = TileState.DEAD
                }
            }
        }
        tiles.removeAll { it.state == TileState.DEAD }

        // Update shatter effects
        for (effect in shatterEffects) {
            effect.update(dt)
        }
        val deadEffects = shatterEffects.filter { !it.isAlive }
        for (dead in deadEffects) ShatterEffect.recycle(dead)
        shatterEffects.removeAll { !it.isAlive }

        // Update floating texts
        for (ft in floatingTexts) {
            ft.elapsed += dt
        }
        floatingTexts.removeAll { !it.isAlive }

        // Decay screen shake
        if (shakeIntensity > 0.5f) {
            shakeOffsetX = (rng.nextFloat() - 0.5f) * 2f * shakeIntensity
            shakeOffsetY = (rng.nextFloat() - 0.5f) * 2f * shakeIntensity
            shakeIntensity *= (1f - dt * 8f).coerceAtLeast(0f)
        } else {
            shakeIntensity = 0f
            shakeOffsetX = 0f
            shakeOffsetY = 0f
        }

        // Transition to final GAME_OVER after animation completes
        val phase = if (gameOverAnimElapsed >= GAME_OVER_TOTAL_SECS) {
            gameOverAnimPhase = false
            GamePhase.GAME_OVER
        } else {
            GamePhase.GAME_OVER_ANIM
        }

        return snapshot().copy(phase = phase)
    }

    private fun snapshot(): GameState = GameState(
        frameCount = frameCount++,
        tiles = tiles.filter { it.state != TileState.DEAD }.toList(),
        shatterEffects = shatterEffects.toList(),
        slashTrails = slashTrails.toList(),
        floatingTexts = floatingTexts.toList(),
        score = score,
        combo = combo,
        bladeHealth = bladeHealth,
        phase = when {
            gameOverAnimPhase -> GamePhase.GAME_OVER_ANIM
            bladeHealth <= 0 -> GamePhase.GAME_OVER
            else -> GamePhase.PLAYING
        },
        screenWidth = screenW,
        screenHeight = screenH,
        flashAlpha = flashAlpha,
        flashColor = flashColor,
        shakeOffsetX = shakeOffsetX,
        shakeOffsetY = shakeOffsetY,
        hintTileIds = hintTileIds,
        tilesCleared = tilesCleared,
        maxCombo = maxCombo,
        totalSlashes = totalSlashes,
        validSlashes = validSlashes,
    )
}

private fun comboEscalationLabel(combo: Int): String? = when {
    combo == 3 -> "GREAT"
    combo == 4 -> "AMAZING"
    combo == 5 -> "INCREDIBLE"
    combo >= 6 -> "GODLIKE"
    else -> null
}

private fun comboEscalationScale(combo: Int): Float = when {
    combo == 3 -> 1.0f
    combo == 4 -> 1.3f
    combo == 5 -> 1.6f
    combo >= 6 -> 2.0f
    else -> 1.0f
}

data class MatchResult(
    val tiles: List<Tile>,
    val baseScore: Int,
)
