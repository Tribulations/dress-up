package com.dressup.core.model

/**
 * A colour a garment can have; a garment has any number of them (none, one, or e.g. blue and red). [MULTI] is for
 * patterns with many colours, and can be picked together with others. A fixed list keeps filtering simple.
 *
 * [key] is what the database stores, so it must never change once released. [argb] is only for the swatch.
 */
enum class GarmentColour(val key: String, val argb: Long) {
    BLACK("black", 0xFF1B1B1B),
    WHITE("white", 0xFFF5F5F0),
    GREY("grey", 0xFF8E8E8E),
    BEIGE("beige", 0xFFD9C7A7),
    BROWN("brown", 0xFF7A4E2D),
    RED("red", 0xFFC62828),
    PINK("pink", 0xFFEC8FB0),
    ORANGE("orange", 0xFFEF7D2D),
    YELLOW("yellow", 0xFFF2C94C),
    GREEN("green", 0xFF4C8C4A),
    BLUE("blue", 0xFF3E7CC9),
    NAVY("navy", 0xFF1F2F57),
    PURPLE("purple", 0xFF7E57C2),
    MULTI("multi", 0xFFFFFFFF),
    ;

    companion object {
        fun fromKeyOrNull(key: String?): GarmentColour? = entries.firstOrNull { it.key == key }

        /** Stored colours ("blue,red") back as a list: unknown keys and repeats are dropped, the order is kept. */
        fun listFromKeys(keys: String?): List<GarmentColour> =
            keys.orEmpty().split(SEPARATOR).mapNotNull { fromKeyOrNull(it.trim()) }.distinct()

        /** How [listFromKeys] reads them back; null when there are none. */
        fun keysOf(colours: List<GarmentColour>): String? =
            colours.distinct().joinToString(SEPARATOR) { it.key }.ifEmpty { null }

        private const val SEPARATOR = ","
    }
}
