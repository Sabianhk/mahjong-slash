package com.mahjongslash.game.engine

import com.mahjongslash.game.model.TileSet
import com.mahjongslash.game.model.TileType
import kotlin.random.Random

/**
 * Difficulty tiers for Memory mode.
 */
enum class MemoryDifficulty(val label: String, val kanji: String, val cols: Int, val rows: Int) {
    EASY("EASY", "易", 4, 3),       // 12 cells = 6 pairs
    MEDIUM("MEDIUM", "中", 4, 4),   // 16 cells = 8 pairs
    HARD("HARD", "難", 5, 4),       // 20 cells = 10 pairs
}

/**
 * State of a single memory card.
 */
data class MemoryCard(
    val index: Int,
    val tileType: TileType,
    var faceUp: Boolean = false,
    var matched: Boolean = false,
)

/**
 * Phase of the memory game.
 */
enum class MemoryPhase {
    PLAYING,
    CHECKING,   // Two cards flipped, briefly showing before hiding
    WON,
}

/**
 * Immutable snapshot of memory game state.
 */
data class MemoryState(
    val cards: List<MemoryCard> = emptyList(),
    val cols: Int = 4,
    val rows: Int = 3,
    val phase: MemoryPhase = MemoryPhase.PLAYING,
    val moves: Int = 0,
    val matches: Int = 0,
    val totalPairs: Int = 0,
    val elapsedMs: Long = 0L,
    val difficulty: MemoryDifficulty = MemoryDifficulty.EASY,
    val frameCount: Long = 0L,
)

/**
 * Core logic for the tile memory game.
 * Grid of face-down mahjong tiles — flip two at a time, find matching pairs.
 */
class MemoryEngine {

    private var cards = mutableListOf<MemoryCard>()
    private var cols = 4
    private var rows = 3
    private var difficulty = MemoryDifficulty.EASY

    private var firstFlip: Int = -1
    private var secondFlip: Int = -1
    private var checkTimer = 0f
    private val CHECK_DELAY = 0.6f // seconds to show mismatched pair

    private var moves = 0
    private var matches = 0
    private var totalPairs = 0
    private var phase = MemoryPhase.PLAYING
    private var startTimeMs = 0L
    private var elapsedMs = 0L
    private var frameCount = 0L

    fun initialize(diff: MemoryDifficulty) {
        difficulty = diff
        cols = diff.cols
        rows = diff.rows
        totalPairs = (cols * rows) / 2

        // Pick random tile types for pairs
        val rng = Random(System.nanoTime())
        val types = TileSet.allTypes.shuffled(rng).take(totalPairs)

        // Create pairs and shuffle
        val cardList = mutableListOf<MemoryCard>()
        var idx = 0
        for (type in types) {
            cardList.add(MemoryCard(idx++, type))
            cardList.add(MemoryCard(idx++, type))
        }
        cardList.shuffle(rng)
        // Re-index after shuffle
        for (i in cardList.indices) {
            cardList[i] = cardList[i].copy(index = i)
        }

        cards = cardList
        firstFlip = -1
        secondFlip = -1
        checkTimer = 0f
        moves = 0
        matches = 0
        phase = MemoryPhase.PLAYING
        startTimeMs = System.currentTimeMillis()
        elapsedMs = 0L
        frameCount = 0L
    }

    fun update(dt: Float): MemoryState {
        frameCount++

        if (phase == MemoryPhase.PLAYING || phase == MemoryPhase.CHECKING) {
            elapsedMs = System.currentTimeMillis() - startTimeMs
        }

        // Handle check delay (showing mismatched pair briefly)
        if (phase == MemoryPhase.CHECKING) {
            checkTimer -= dt
            if (checkTimer <= 0f) {
                // Check if the two flipped cards match
                val card1 = cards[firstFlip]
                val card2 = cards[secondFlip]

                if (card1.tileType.id == card2.tileType.id) {
                    // Match!
                    cards[firstFlip] = card1.copy(matched = true, faceUp = true)
                    cards[secondFlip] = card2.copy(matched = true, faceUp = true)
                    matches++

                    if (matches >= totalPairs) {
                        phase = MemoryPhase.WON
                    } else {
                        phase = MemoryPhase.PLAYING
                    }
                } else {
                    // No match — flip both back
                    cards[firstFlip] = card1.copy(faceUp = false)
                    cards[secondFlip] = card2.copy(faceUp = false)
                    phase = MemoryPhase.PLAYING
                }

                firstFlip = -1
                secondFlip = -1
            }
        }

        return snapshot()
    }

    fun onCardTap(index: Int) {
        if (phase != MemoryPhase.PLAYING) return
        if (index < 0 || index >= cards.size) return

        val card = cards[index]
        if (card.faceUp || card.matched) return

        if (firstFlip == -1) {
            // First card flip
            firstFlip = index
            cards[index] = card.copy(faceUp = true)
        } else if (secondFlip == -1 && index != firstFlip) {
            // Second card flip
            secondFlip = index
            cards[index] = card.copy(faceUp = true)
            moves++
            phase = MemoryPhase.CHECKING
            checkTimer = CHECK_DELAY
        }
    }

    /**
     * Calculate score based on moves and time.
     * Fewer moves and less time = higher score.
     */
    fun calculateScore(): Int {
        val basePairs = totalPairs * 100
        val movePenalty = (moves - totalPairs) * 15 // penalty for extra moves beyond perfect
        val timePenalty = (elapsedMs / 1000).toInt() * 2
        return (basePairs - movePenalty - timePenalty).coerceAtLeast(10)
    }

    private fun snapshot() = MemoryState(
        cards = cards.toList(),
        cols = cols,
        rows = rows,
        phase = phase,
        moves = moves,
        matches = matches,
        totalPairs = totalPairs,
        elapsedMs = elapsedMs,
        difficulty = difficulty,
        frameCount = frameCount,
    )
}
