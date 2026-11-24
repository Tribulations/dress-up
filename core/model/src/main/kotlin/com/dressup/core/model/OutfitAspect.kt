package com.dressup.core.model

/**
 * The things an outfit can be rated on, besides its overall rating (ARCHITECTURE.md §5.2). Like [GarmentAspect] a fixed
 * list; [key] is stored (in the database and in exports), so it must never change once released.
 */
enum class OutfitAspect(override val key: String) : Aspect {
    COMFORT("comfort"),
    STYLE("style"),
    FIT("fit"),
    OCCASION("occasion"),
    ;

    companion object {
        fun fromKeyOrNull(key: String?): OutfitAspect? = entries.firstOrNull { it.key == key }
    }
}
