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
class GameEngine {
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
    private var spawnInterval = 1.0f
    private var maxTilesOnScreen = 8
    private val rng = Random(System.nanoTime())

    // Matchability pool
    private val recentPool = mutableListOf<TileType>()
    private val poolSize = 6
    private var tilesSpawnedFromPool = 0
    private val poolRefreshInterval = 12

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
        floatingTexts.clear()
        score = 0
        combo = 0
        bladeHealth = 3
        flashAlpha = 0f
        timeSinceLastSpawn = spawnInterval
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

        // Remove tiles that have left the screen
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

        // Update floating texts
        for (ft in floatingTexts) {
            ft.elapsed += dt
        }
        floatingTexts.removeAll { !it.isAlive }

        // Decay screen flash
        if (flashAlpha > 0f) {
            flashAlpha = (flashAlpha - dt * 3f).coerceAtLeast(0f)
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

        val slashed = SlashDetector.detectSlashedTiles(
            activeSlashPoints, tiles.filter { it.state == TileState.ALIVE }, density
        )

        if (slashed.isNotEmpty()) {
            val matchResult = findBestMatch(slashed)

            if (matchResult != null) {
                // Valid match — shatter, score, flash gold
                for (tile in matchResult.tiles) {
                    tile.state = TileState.SHATTERING
                    shatterEffects.add(ShatterEffect.create(tile, density))
                }

                combo++
                lastMatchTime = System.currentTimeMillis()
                val multiplier = comboMultiplier(combo)
                val points = (matchResult.baseScore * multiplier).toInt()
                score += points

                // Color the trail gold on success
                activeTrail?.resultColor = AccentGoldBright

                // Floating score popup at centroid of matched tiles
                val cx = matchResult.tiles.map { it.position.x }.average().toFloat()
                val cy = matchResult.tiles.map { it.position.y }.average().toFloat()
                val label = if (combo > 1) {
                    "+$points ×${comboMultiplierText(combo)}"
                } else {
                    "+$points"
                }
                floatingTexts.add(FloatingText(
                    position = Offset(cx, cy),
                    text = label,
                    color = AccentGoldBright,
                ))

                // Screen flash — gold
                flashAlpha = 0.25f
                flashColor = AccentGold

                for (tile in matchResult.tiles) {
                    tile.state = TileState.DEAD
                }
            } else if (slashed.size >= 2) {
                // Invalid match — penalty, flash red
                combo = 0
                bladeHealth = (bladeHealth - 1).coerceAtLeast(0)

                // Color the trail red on failure
                activeTrail?.resultColor = AccentRed

                // Floating penalty text
                val cx = slashed.map { it.position.x }.average().toFloat()
                val cy = slashed.map { it.position.y }.average().toFloat()
                floatingTexts.add(FloatingText(
                    position = Offset(cx, cy),
                    text = "MISS",
                    color = AccentRed,
                ))

                // Screen flash — red
                flashAlpha = 0.2f
                flashColor = AccentRed
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
        recentPool.addAll(shuffled.take(poolSize))
    }

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
        return recentPool[rng.nextInt(recentPool.size)]
    }

    private fun spawnTile() {
        val tileType = pickTileType()
        val tileW = Tile.WIDTH_DP * density
        val tileH = Tile.HEIGHT_DP * density

        val edge = rng.nextInt(4)
        val baseSpeed = 80f + rng.nextFloat() * 60f
        val speed = baseSpeed * density

        val topInset = 60f * density
        val bottomInset = 60f * density
        val playTop = topInset + tileH
        val playBottom = screenH - bottomInset - tileH
        val playHeight = playBottom - playTop

        val (startPos, vel) = when (edge) {
            0 -> {
                val y = playTop + rng.nextFloat() * playHeight
                val angle = -30f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(-tileW, y) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            1 -> {
                val y = playTop + rng.nextFloat() * playHeight
                val angle = 150f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(screenW + tileW, y) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            2 -> {
                val x = tileW + rng.nextFloat() * (screenW - tileW * 2)
                val angle = 60f + rng.nextFloat() * 60f
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                Offset(x, -tileH) to Offset(cos(rad) * speed, sin(rad) * speed)
            }
            else -> {
                val x = tileW + rng.nextFloat() * (screenW - tileW * 2)
                val angle = -120f + rng.nextFloat() * 60f
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
        floatingTexts = floatingTexts.toList(),
        score = score,
        combo = combo,
        bladeHealth = bladeHealth,
        phase = if (bladeHealth <= 0) GamePhase.GAME_OVER else GamePhase.PLAYING,
        screenWidth = screenW,
        screenHeight = screenH,
        flashAlpha = flashAlpha,
        flashColor = flashColor,
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
