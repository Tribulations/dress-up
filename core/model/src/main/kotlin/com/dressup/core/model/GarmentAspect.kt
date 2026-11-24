package com.dressup.core.model

/**
 * The things a garment can be rated on, besides its overall rating (ARCHITECTURE.md §5). A fixed list, so the same
 * aspects can be compared across the wardrobe. [key] is what the database stores, so it must never change once released.
 */
enum class GarmentAspect(override val key: String) : Aspect {
    FIT("fit"),
    COMFORT("comfort"),
    QUALITY("quality"),
    STYLE("style"),
    EASY_CARE("easy_care"),
    ;

    companion object {
        fun fromKeyOrNull(key: String?): GarmentAspect? = entries.firstOrNull { it.key == key }
    }
}
