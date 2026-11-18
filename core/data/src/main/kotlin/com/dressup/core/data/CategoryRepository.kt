package com.dressup.core.data

import com.dressup.core.data.db.CategoryDao
import com.dressup.core.data.db.CategoryEntity
import com.dressup.core.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CategoryRepository {
    /** All categories, hidden ones included: built-in first, then the user's own alphabetically. */
    val categories: Flow<List<Category>>

    suspend fun add(name: String): CategoryNameResult

    /** Renames one of the user's own categories. Built-in ones can't be renamed. */
    suspend fun rename(id: Long, name: String): CategoryNameResult

    suspend fun setHidden(id: Long, hidden: Boolean)

    /** Deletes one of the user's own categories, unless a garment still uses it. */
    suspend fun delete(id: Long): DeleteCategoryResult
}

sealed interface CategoryNameResult {
    data class Saved(val id: Long) : CategoryNameResult
    data object Blank : CategoryNameResult
    data object Duplicate : CategoryNameResult
}

enum class DeleteCategoryResult { DELETED, IN_USE, BUILT_IN }

/** The longest category name we accept; longer ones don't fit on a chip. */
const val MAX_CATEGORY_NAME_LENGTH = 30

internal class RoomCategoryRepository(private val dao: CategoryDao) : CategoryRepository {
    override val categories: Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun add(name: String): CategoryNameResult {
        val clean = cleanName(name)
        validate(clean, exceptId = NO_ID)?.let { return it }
        val id = dao.insert(CategoryEntity(name = clean, builtInKey = null, hidden = false, sortOrder = 0))
        return CategoryNameResult.Saved(id)
    }

    override suspend fun rename(id: Long, name: String): CategoryNameResult {
        val clean = cleanName(name)
        validate(clean, exceptId = id)?.let { return it }
        dao.rename(id, clean)
        return CategoryNameResult.Saved(id)
    }

    override suspend fun setHidden(id: Long, hidden: Boolean) = dao.setHidden(id, hidden)

    override suspend fun delete(id: Long): DeleteCategoryResult {
        val category = dao.get(id) ?: return DeleteCategoryResult.DELETED
        if (category.builtInKey != null) return DeleteCategoryResult.BUILT_IN
        if (dao.garmentCount(id) > 0) return DeleteCategoryResult.IN_USE
        dao.delete(id)
        return DeleteCategoryResult.DELETED
    }

    private suspend fun validate(name: String, exceptId: Long): CategoryNameResult? = when {
        name.isEmpty() -> CategoryNameResult.Blank
        dao.countWithName(name, exceptId) > 0 -> CategoryNameResult.Duplicate
        else -> null
    }

    private companion object {
        const val NO_ID = -1L
    }
}

internal fun cleanName(name: String): String = name.trim().replace(Regex("\\s+"), " ").take(MAX_CATEGORY_NAME_LENGTH)
