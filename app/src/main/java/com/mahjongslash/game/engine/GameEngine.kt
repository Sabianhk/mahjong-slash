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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Core game engine. Manages tile spawning, movement, slash detection,
 * match validation, and score/combo tracking.
 *
 * For Phase 1: simplified spawning with linear movement, basic slash→shatter.
 * Architecture is set up so Phase 2+ additions (patterns, combos, health) slot in cleanly.
 */
class GameEngine {
    private var nextInstanceId = 0L
    private var screenW = 0f
    private var screenH = 0f
    private var density = 1f

    // Live game objects
    private val tiles = mutableListOf<Tile>()
    private val shatterEffects = mutableListOf<ShatterEffect>()
    private val slashTrails = mutableListOf<SlashTrail>()

    // Scoring
    private var score = 0
    private var combo = 0
    private var lastMatchTime = 0L
    private var bladeHealth = 3

    // Spawning
    private var timeSinceLastSpawn = 0f
    private var spawnInterval = 1.0f  // seconds — tighter for denser play
    private var maxTilesOnScreen = 8
    private val rng = Random(System.nanoTime())

    // Matchability: keep a small pool of recent tile types to bias spawns
    // so players actually see matchable tiles on screen
    private val recentPool = mutableListOf<TileType>()
    private val poolSize = 6 // Only draw from this many types at a time
    private var tilesSpawnedFromPool = 0
    private val poolRefreshInterval = 12 // Refresh pool after this many spawns

    // Current active slash
    private var activeSlashPoints = mutableListOf<Offset>()
    private var activeTrail: SlashTrail? = null
    private var lastSlashPosition: Offset = Offset.Zero

    // Combo timing
    private val comboTimeoutMs = 3000L

    fun initialize(width: Float, height: Float, density: Float) {
        screenW = width
        screenH = height
        this.density = density
        tiles.clear()
        shatterEffects.clear()
        slashTrails.clear()
        score = 0
        combo = 0
        bladeHealth = 3
        timeSinceLastSpawn = spawnInterval // Spawn immediately
        refreshPool()
        tilesSpawnedFromPool = 0
    }

    /**
     * Main update tick. Called every frame with delta time in seconds.
     */
    fun update(dt: Float): GameState {
        if (screenW == 0f) return GameState()
        if (bladeHealth <= 0) return snapshot()

        val now = System.currentTimeMillis()

        // Check combo timeout
        if (combo > 0 && now - lastMatchTime > comboTimeoutMs) {
            combo = 0
        }

        // Spawn tiles
        timeSinceLastSpawn += dt
        if (timeSinceLastSpawn >= spawnInterval && tiles.count { it.state == TileState.ALIVE } < maxTilesOnScreen) {
            spawnTile()
            timeSinceLastSpawn = 0f
        }

        // Update tile positions
        for (tile in tiles) {
            if (tile.state == TileState.ALIVE) {
                tile.position = Offset(
                    tile.position.x + tile.velocity.x * dt,
                    tile.position.y + tile.velocity.y * dt
                )
            }
        }

        // Remove tiles that have left the screen (with margin)
        val margin = Tile.WIDTH_DP * density * 2
        tiles.removeAll { tile ->
            tile.state == TileState.ALIVE && (
                tile.position.x < -margin || tile.position.x > screenW + margin ||
                tile.position.y < -margin || tile.position.y > screenH + margin
            )
        }

        // Remove dead tiles
        tiles.removeAll { it.state == TileState.DEAD }

        // Update shatter effects
        for (effect in shatterEffects) {
            effect.update(dt)
        }
        shatterEffects.removeAll { !it.isAlive }

        // Update slash trail fading
        for (trail in slashTrails) {
            if (trail.isFading) {
                trail.fadeAlpha -= dt / (SlashTrail.FADE_DURATION_MS / 1000f)
            }
        }
        slashTrails.removeAll { !it.isAlive }

        return snapshot()
    }

    /**
     * Called when the player starts a swipe.
     */
    fun onSlashStart(position: Offset) {
        activeSlashPoints.clear()
        activeSlashPoints.add(position)
        activeTrail = SlashTrail().also {
            it.points.add(SlashTrailPoint(position, System.currentTimeMillis()))
            slashTrails.add(it)
        }
    }

    /**
     * Called each frame during a swipe with the current touch position.
     */
    fun onSlashMove(position: Offset) {
        activeSlashPoints.add(position)
        activeTrail?.points?.add(SlashTrailPoint(position, System.currentTimeMillis()))
        lastSlashPosition = position
    }

    /**
     * End slash using the last known drag position (for gesture APIs that don't
     * provide coordinates in onDragEnd).
     */
    fun onSlashEndAtLastPosition() {
        onSlashEnd(lastSlashPosition)
    }

    /**
     * Called when the player lifts their finger, completing the swipe.
     */
    fun onSlashEnd(position: Offset) {
        activeSlashPoints.add(position)
        activeTrail?.points?.add(SlashTrailPoint(position, System.currentTimeMillis()))
        activeTrail?.isFading = true

        // Detect which tiles were slashed
        val slashed = SlashDetector.detectSlashedTiles(activeSlashPoints, tiles.filter { it.state == TileState.ALIVE }, density)

        if (slashed.isNotEmpty()) {
            // Find best valid match among slashed tiles
            val matchResult = findBestMatch(slashed)

            if (matchResult != null) {
                // Valid match — shatter matched tiles, award points
                for (tile in matchResult.tiles) {
                    tile.state = TileState.SHATTERING
                    shatterEffects.add(ShatterEffect.create(tile, density))
                }

                combo++
                lastMatchTime = System.currentTimeMillis()
                val multiplier = comboMultiplier(combo)
                val points = (matchResult.baseScore * multiplier).toInt()
                score += points

                // Mark shattered tiles as dead after a brief delay (handled in update)
                for (tile in matchResult.tiles) {
                    tile.state = TileState.DEAD
                }
            } else if (slashed.size >= 2) {
                // Invalid match — tiles don't form a valid group
                combo = 0
                bladeHealth = (bladeHealth - 1).coerceAtLeast(0)
            }
            // Single tile slashed with no match: no penalty in Phase 1
            // (spec says penalty for single tile, but we'll refine in Phase 3)
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

    /**
     * Among the slashed tiles, find the highest-scoring valid Mahjong match.
     * Returns null if no valid match exists.
     */
    private fun findBestMatch(slashed: List<Tile>): MatchResult? {
        val candidates = slashed.filter { it.state == TileState.ALIVE }
        if (candidates.size < 2) return null

        var best: MatchResult? = null

        // Check triplets (3 identical) — highest score
        if (candidates.size >= 3) {
            for (i in candidates.indices) {
                for (j in i + 1 until candidates.size) {
                    for (k in j + 1 until candidates.size) {
                        val a = candidates[i]
                        val b = candidates[j]
                        val c = candidates[k]
                        if (TileSet.isTriplet(a.type, b.type, c.type)) {
                            val result = MatchResult(listOf(a, b, c), 350, MatchType.TRIPLET)
                            if (best == null || result.baseScore > best.baseScore) best = result
                        }
                    }
                }
            }
        }

        // Check sequences (3 consecutive same suit)
        if (candidates.size >= 3) {
            for (i in candidates.indices) {
                for (j in i + 1 until candidates.size) {
                    for (k in j + 1 until candidates.size) {
                        val a = candidates[i]
                        val b = candidates[j]
                        val c = candidates[k]
                        if (TileSet.isSequence(a.type, b.type, c.type)) {
                            val result = MatchResult(listOf(a, b, c), 200, MatchType.SEQUENCE)
                            if (best == null || result.baseScore > best.baseScore) best = result
                        }
                    }
                }
            }
        }

        // Check pairs (2 identical) — lowest priority
        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                if (TileSet.isPair(candidates[i].type, candidates[j].type)) {
                    val result = MatchResult(listOf(candidates[i], candidates[j]), 100, MatchType.PAIR)
                    if (best == null || result.baseScore > best.baseScore) best = result
                }
            }
        }

        return best
    }

    private fun refreshPool() {
        recentPool.clear()
        val shuffled = TileSet.allTypes.shuffled(rng)
        // Pick poolSize types, biased toward suited tiles for sequence potential
        recentPool.addAll(shuffled.take(poolSize))
    }

    private fun pickTileType(): TileType {
        tilesSpawnedFromPool++
        if (tilesSpawnedFromPool > poolRefreshInterval) {
            // Keep ~half the pool for continuity, replace the rest
            val keep = recentPool.take(poolSize / 2)
            refreshPool()
            // Ensure kept types are in the new pool
            for (t in keep) {
                if (t !in recentPool && recentPool.size < poolSize) recentPool.add(t)
            }
            tilesSpawnedFromPool = 0
        }
        return recentPool[rng.nextInt(recentPool.size)]
    }

    /**
     * Spawn a new tile from a random screen edge with a velocity toward the play area.
     */
    private fun spawnTile() {
        val tileType = pickTileType()
        val tileW = Tile.WIDTH_DP * density
        val tileH = Tile.HEIGHT_DP * density

        // Choose spawn edge: 0=left, 1=right, 2=top, 3=bottom
        val edge = rng.nextInt(4)
        val baseSpeed = 80f + rng.nextFloat() * 60f // dp/s — gentle Phase 1 speed
        val speed = baseSpeed * density

        // Inset play area to avoid system bars (status bar ~48dp, nav bar ~48dp)
        val topInset = 60f * density
        val bottomInset = 60f * density
        val playTop = topInset + tileH
        val playBottom = screenH - bottomInset - tileH
        val playHeight = playBottom - playTop

        val (startPos, vel) = when (edge) {
            0 -> { // Left edge
                val y = playTop + rng.nextFloat() * playHeight
                val angle = -30f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(-tileW, y) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            1 -> { // Right edge
                val y = playTop + rng.nextFloat() * playHeight
                val angle = 150f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(screenW + tileW, y) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            2 -> { // Top edge
                val x = tileW + rng.nextFloat() * (screenW - tileW * 2)
                val angle = 60f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(x, -tileH) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            else -> { // Bottom edge
                val x = tileW + rng.nextFloat() * (screenW - tileW * 2)
                val angle = -120f + rng.nextFloat() * 60f // heading upward (-120° to -60°)
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(x, screenH + tileH) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
        }

        tiles.add(
            Tile(
                instanceId = nextInstanceId++,
                type = tileType,
                position = startPos,
                velocity = vel,
                spawnTime = System.currentTimeMillis(),
            )
        )
    }

    private fun snapshot(): GameState = GameState(
        tiles = tiles.filter { it.state == TileState.ALIVE }.toList(),
        shatterEffects = shatterEffects.toList(),
        slashTrails = slashTrails.toList(),
        score = score,
        combo = combo,
        bladeHealth = bladeHealth,
        phase = if (bladeHealth <= 0) GamePhase.GAME_OVER else GamePhase.PLAYING,
        screenWidth = screenW,
        screenHeight = screenH,
    )
}

data class MatchResult(
    val tiles: List<Tile>,
    val baseScore: Int,
    val type: MatchType,
)

enum class MatchType {
    PAIR, SEQUENCE, TRIPLET
}
