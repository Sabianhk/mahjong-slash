package com.mahjongslash.game.engine

import androidx.compose.ui.geometry.Offset
import com.mahjongslash.game.gesture.SlashDetector
import com.mahjongslash.game.model.Tile
import com.mahjongslash.game.model.TileSet
import com.mahjongslash.game.model.TileState
import com.mahjongslash.game.model.TileSuit
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
    private var bladeHealth = 5

    // Screen flash
    private var flashAlpha = 0f
    private var flashColor = AccentGold

    // Spawning
    private var timeSinceLastSpawn = 0f
    private var spawnInterval = 0.7f
    private var maxTilesOnScreen = 12
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

    // How long a shattering tile stays visible (fading out alongside fragments)
    private val SHATTER_VISIBLE_SECS = 0.35f

    // Debug (temporary — remove after validation)
    private var debugLastSlash = ""
    private var debugLastPath = listOf<Offset>()

    // Auto-slash result for debug display
    private var autoSlashResult = ""

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
        bladeHealth = 5
        flashAlpha = 0f
        timeSinceLastSpawn = spawnInterval
        spawnQueue.clear()
        burstDelay = 0f
        autoSlashResult = ""
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

        // Process burst spawn queue (rapid successive spawns for grouped tiles)
        if (spawnQueue.isNotEmpty()) {
            burstDelay -= dt
            if (burstDelay <= 0f) {
                val queued = spawnQueue.removeFirst()
                spawnTileFromQueue(queued)
                burstDelay = 0.15f + rng.nextFloat() * 0.15f // 150-300ms between burst tiles
            }
        }

        // Spawn tiles (or queue a burst group)
        timeSinceLastSpawn += dt
        if (spawnQueue.isEmpty() && timeSinceLastSpawn >= spawnInterval &&
            tiles.count { it.state == TileState.ALIVE } < maxTilesOnScreen) {
            queueSpawnGroup()
            timeSinceLastSpawn = 0f
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
                val wobble = sin((age * wobbleFreq + wobblePhase).toDouble()).toFloat() * wobbleAmp * dt
                val perpX = if (vLen > 0.01f) -tile.velocity.y / vLen else 0f
                val perpY = if (vLen > 0.01f) tile.velocity.x / vLen else 0f

                tile.position = Offset(
                    tile.position.x + tile.velocity.x * speedFactor * dt + perpX * wobble,
                    tile.position.y + tile.velocity.y * speedFactor * dt + perpY * wobble
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
                tile.shatterElapsed += dt
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

        val slashResult = SlashDetector.detectSlashedTiles(
            activeSlashPoints, tiles.filter { it.state == TileState.ALIVE }, density
        )
        val slashed = slashResult.tiles

        // Debug diagnostics (temporary — remove after validation)
        val aliveTiles = tiles.filter { it.state == TileState.ALIVE }
        val aliveTileCount = aliveTiles.size
        debugLastPath = activeSlashPoints.toList()

        // Build detailed debug: path bounds vs tile bounds
        val pathXs = activeSlashPoints.map { it.x }
        val pathYs = activeSlashPoints.map { it.y }
        val pathBounds = if (pathXs.isNotEmpty())
            "px[${pathXs.min().toInt()}..${pathXs.max().toInt()}]" +
            "py[${pathYs.min().toInt()}..${pathYs.max().toInt()}]"
        else "nopath"

        val tileInfo = if (aliveTiles.isNotEmpty()) {
            val txs = aliveTiles.map { it.position.x }
            val tys = aliveTiles.map { it.position.y }
            "tx[${txs.min().toInt()}..${txs.max().toInt()}]" +
            "ty[${tys.min().toInt()}..${tys.max().toInt()}]"
        } else "notiles"

        // Find nearest tile distance to any path point for miss diagnosis
        var nearestDist = Float.MAX_VALUE
        for (pt in activeSlashPoints) {
            for (tile in aliveTiles) {
                val dx = pt.x - tile.position.x
                val dy = pt.y - tile.position.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < nearestDist) nearestDist = dist
            }
        }
        val nearStr = if (nearestDist < Float.MAX_VALUE) "near=${nearestDist.toInt()}px" else ""

        debugLastSlash = "len=${slashResult.lengthDp.toInt()}dp pts=${slashResult.pointCount} " +
            "hit=${slashed.size}/${aliveTileCount} st=${slashResult.status}\n" +
            "$pathBounds $tileInfo $nearStr d=${density}"

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

                // Tiles stay SHATTERING — update() will fade and transition to DEAD
            } else if (slashed.size >= 3) {
                // Invalid match — penalty only when slashing 3+ non-matching tiles
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

    /**
     * Build a pool that always contains at least one matchable group:
     *  - Pick a random suited tile and include 2-3 consecutive values in that suit (sequence-ready).
     *  - Duplicate one type so pairs are immediately possible.
     *  - Fill remaining slots randomly.
     */
    private fun refreshPool() {
        recentPool.clear()
        val suited = TileSet.allTypes.filter { it.isSuited }

        // Seed a sequence-capable run of 3 consecutive values in one suit
        val baseSuit = listOf(TileSuit.CHARACTERS, TileSuit.DOTS, TileSuit.BAMBOO).random(rng)
        val startVal = rng.nextInt(7) + 1 // 1..7 so startVal+2 <= 9
        for (v in startVal..startVal + 2) {
            recentPool.add(suited.first { it.suit == baseSuit && it.value == v })
        }

        // Add a duplicate of one of them so a pair is always possible
        recentPool.add(recentPool[rng.nextInt(recentPool.size)])

        // Fill remaining slots from other types
        val remaining = TileSet.allTypes.filter { it !in recentPool }.shuffled(rng)
        for (t in remaining) {
            if (recentPool.size >= poolSize) break
            recentPool.add(t)
        }
        recentPool.shuffle(rng)
    }

    /**
     * Pick the next tile type, biased toward matchability.
     * With [echoChance] probability, instead of a random pool pick,
     * choose a type that would form a pair or sequence with a tile already on screen.
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

        // Echo spawn: match something already on screen
        val alive = tiles.filter { it.state == TileState.ALIVE }
        if (alive.isNotEmpty() && rng.nextFloat() < echoChance) {
            val target = alive[rng.nextInt(alive.size)]
            // 60% chance: duplicate (pair-ready), 40% chance: adjacent value (sequence-ready)
            return if (rng.nextFloat() < 0.6f || target.type.isHonor) {
                target.type
            } else {
                val delta = if (rng.nextBoolean()) 1 else -1
                val adjVal = target.type.value + delta
                TileSet.allTypes.firstOrNull { it.suit == target.type.suit && it.value == adjVal }
                    ?: target.type // fallback to duplicate if at suit boundary
            }
        }

        return recentPool[rng.nextInt(recentPool.size)]
    }

    /**
     * Queue a group of tiles to spawn in a burst. ~60% of the time spawns a
     * matchable group (pair or sequence) from the same edge/area so they cluster.
     * The rest of the time spawns a single tile.
     */
    private fun queueSpawnGroup() {
        val edge = rng.nextInt(4)
        val baseLane = rng.nextFloat() // 0..1 position along the edge

        if (rng.nextFloat() < 0.6f) {
            // Burst: spawn a matchable group (2-3 tiles) from nearby positions
            val groupSize = if (rng.nextFloat() < 0.35f) 3 else 2

            // Pick the first tile type
            val firstType = pickTileType()
            spawnQueue.add(QueuedSpawn(firstType, edge, baseLane))

            if (groupSize >= 2) {
                // Second tile: either duplicate (pair) or adjacent (sequence)
                val secondType = if (rng.nextFloat() < 0.5f || firstType.isHonor) {
                    firstType // pair-ready
                } else {
                    val delta = if (rng.nextBoolean()) 1 else -1
                    val adjVal = firstType.value + delta
                    TileSet.allTypes.firstOrNull { it.suit == firstType.suit && it.value == adjVal }
                        ?: firstType
                }
                val laneJitter = (rng.nextFloat() - 0.5f) * 0.15f
                spawnQueue.add(QueuedSpawn(secondType, edge, (baseLane + laneJitter).coerceIn(0f, 1f)))
            }

            if (groupSize >= 3) {
                // Third tile: complete the triplet or sequence
                val thirdType = if (spawnQueue.size >= 2 && spawnQueue[0].type == spawnQueue[1].type) {
                    firstType // triplet
                } else {
                    // Try to complete a sequence
                    val vals = listOf(spawnQueue[0].type.value, spawnQueue[1].type.value).sorted()
                    val needed = if (vals[1] - vals[0] == 1) {
                        // consecutive pair — add the next or previous
                        listOf(vals[0] - 1, vals[1] + 1).filter { it in 1..9 }.randomOrNull()
                    } else null
                    if (needed != null) {
                        TileSet.allTypes.firstOrNull { it.suit == firstType.suit && it.value == needed }
                            ?: firstType
                    } else firstType
                }
                val laneJitter = (rng.nextFloat() - 0.5f) * 0.15f
                spawnQueue.add(QueuedSpawn(thirdType, edge, (baseLane + laneJitter).coerceIn(0f, 1f)))
            }

            burstDelay = 0f // spawn first one immediately
        } else {
            // Single tile spawn
            spawnQueue.add(QueuedSpawn(pickTileType(), edge, baseLane))
            burstDelay = 0f
        }
    }

    private fun spawnTileFromQueue(queued: QueuedSpawn) {
        val tileW = Tile.WIDTH_DP * density
        val tileH = Tile.HEIGHT_DP * density

        // Slower base speed: 30-50 dp/s (was 55-100) for a floating feel
        val baseSpeed = 30f + rng.nextFloat() * 20f
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
     * DEV HELPER: Finds the best available match on screen and auto-slashes through it.
     * Simulates a gesture path through the matched tiles' positions.
     * Returns a description of what happened for debug display.
     */
    fun triggerAutoSlash(): String {
        val alive = tiles.filter { it.state == TileState.ALIVE }
        if (alive.size < 2) {
            autoSlashResult = "AUTO: no tiles (${alive.size} alive)"
            return autoSlashResult
        }

        // Try to find the best match among all alive tiles
        val bestMatch = findBestMatch(alive)
        if (bestMatch == null) {
            // List what's on screen for diagnosis
            val types = alive.map { "${it.type.displayName}" }.joinToString(", ")
            autoSlashResult = "AUTO: no match in ${alive.size} tiles: $types"
            return autoSlashResult
        }

        // Build a synthetic slash path through the matched tile positions
        val sorted = bestMatch.tiles.sortedBy { it.position.x }
        val pathPoints = mutableListOf<Offset>()

        val first = sorted.first().position
        val last = sorted.last().position
        val dx = last.x - first.x
        val dy = last.y - first.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        // Use a large enough extend to guarantee the path exceeds MIN_SLASH_LENGTH_DP
        val extend = 60f * density

        // Direction: if tiles are nearly co-located, slash diagonally
        val dirX = if (dist > 5f) dx / dist else 0.707f
        val dirY = if (dist > 5f) dy / dist else 0.707f

        pathPoints.add(Offset(first.x - dirX * extend, first.y - dirY * extend))
        for (tile in sorted) {
            pathPoints.add(tile.position)
        }
        pathPoints.add(Offset(last.x + dirX * extend, last.y + dirY * extend))

        // Execute the slash through the engine's normal pipeline
        onSlashStart(pathPoints.first())
        for (i in 1 until pathPoints.size - 1) {
            onSlashMove(pathPoints[i])
        }
        onSlashEnd(pathPoints.last())

        val matchDesc = "${bestMatch.type.name} [${bestMatch.tiles.map { it.type.displayName }.joinToString("+")}]"
        autoSlashResult = "AUTO: $matchDesc → score=$score combo=$combo hp=$bladeHealth"
        return autoSlashResult
    }

    private fun snapshot(): GameState = GameState(
        tiles = tiles.filter { it.state != TileState.DEAD }.toList(),
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
        debugLastSlash = debugLastSlash,
        debugLastPath = debugLastPath,
        debugDensity = density,
        debugAutoSlash = autoSlashResult,
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
