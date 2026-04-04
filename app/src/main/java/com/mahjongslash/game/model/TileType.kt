package com.mahjongslash.game.model

/**
 * Represents the 34 unique Mahjong tile types.
 * Each has a suit, a value (1-9 for suited, 0 for honors), and display characters.
 */
enum class TileSuit {
    CHARACTERS, // 萬子
    DOTS,       // 筒子
    BAMBOO,     // 索子
    WIND,       // 風牌
    DRAGON      // 三元牌
}

data class TileType(
    val suit: TileSuit,
    val value: Int,
    val face: String,      // Main character shown on tile face
    val label: String,     // Small label (suit indicator)
    val id: Int,           // Unique id 0-33
) {
    val isHonor: Boolean get() = suit == TileSuit.WIND || suit == TileSuit.DRAGON
    val isSuited: Boolean get() = !isHonor

    /** Can this tile form a sequence with others? Only suited tiles can. */
    val canSequence: Boolean get() = isSuited

    /** Short debug display name, e.g. "三萬" or "中" */
    val displayName: String get() = "$face$label"
}

object TileSet {
    private val CHAR_NUMERALS = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
    private val DOT_FACES = arrayOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨")
    private val BAMBOO_FACES = arrayOf("１", "２", "３", "４", "５", "６", "７", "８", "９")

    val allTypes: List<TileType> = buildList {
        var id = 0
        // Characters 1-9
        for (i in 1..9) {
            add(TileType(TileSuit.CHARACTERS, i, CHAR_NUMERALS[i - 1], "萬", id++))
        }
        // Dots 1-9
        for (i in 1..9) {
            add(TileType(TileSuit.DOTS, i, DOT_FACES[i - 1], "筒", id++))
        }
        // Bamboo 1-9
        for (i in 1..9) {
            add(TileType(TileSuit.BAMBOO, i, BAMBOO_FACES[i - 1], "索", id++))
        }
        // Winds
        add(TileType(TileSuit.WIND, 1, "東", "風", id++))
        add(TileType(TileSuit.WIND, 2, "南", "風", id++))
        add(TileType(TileSuit.WIND, 3, "西", "風", id++))
        add(TileType(TileSuit.WIND, 4, "北", "風", id++))
        // Dragons
        add(TileType(TileSuit.DRAGON, 1, "中", "", id++))
        add(TileType(TileSuit.DRAGON, 2, "發", "", id++))
        add(TileType(TileSuit.DRAGON, 3, "白", "", id++))
    }

    /** Check if tiles form a valid pair (2 identical) */
    fun isPair(a: TileType, b: TileType): Boolean = a.id == b.id

    /** Check if 3 tiles form a valid sequence (consecutive same suit, no honors) */
    fun isSequence(a: TileType, b: TileType, c: TileType): Boolean {
        if (!a.canSequence || !b.canSequence || !c.canSequence) return false
        if (a.suit != b.suit || b.suit != c.suit) return false
        val values = listOf(a.value, b.value, c.value).sorted()
        return values[1] == values[0] + 1 && values[2] == values[1] + 1
    }

    /** Check if 3 tiles form a valid triplet (3 identical) */
    fun isTriplet(a: TileType, b: TileType, c: TileType): Boolean =
        a.id == b.id && b.id == c.id
}
