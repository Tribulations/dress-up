package com.dressup.core.model

/**
 * A garment category such as pants or shirt (ARCHITECTURE.md §5).
 *
 * Built-in categories ([builtIn]) can be hidden but not renamed or deleted. The user's own can be renamed, and deleted
 * while no garment uses them.
 */
data class Category(
    val id: Long,
    val name: String,
    val builtIn: Boolean,
    val hidden: Boolean,
)
