package com.dressup.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryNameResult
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.DeleteCategoryResult
import com.dressup.core.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The open name dialog: adding a category when [categoryId] is null, otherwise renaming it. */
data class NameDialogState(val categoryId: Long?, val initialName: String, val error: NameError? = null)

enum class NameError { BLANK, DUPLICATE }

enum class CategoriesMessage { IN_USE }

data class CategoriesUiState(
    val builtIn: List<Category> = emptyList(),
    val own: List<Category> = emptyList(),
    val dialog: NameDialogState? = null,
    val confirmDelete: Category? = null,
    val message: CategoriesMessage? = null,
)

private data class Overlay(
    val dialog: NameDialogState? = null,
    val confirmDelete: Category? = null,
    val message: CategoriesMessage? = null,
)

class CategoriesViewModel(private val categories: CategoryRepository) : ViewModel() {
    private val overlay = MutableStateFlow(Overlay())

    val uiState: StateFlow<CategoriesUiState> = combine(categories.categories, overlay) { all, o ->
        CategoriesUiState(
            builtIn = all.filter { it.builtIn },
            own = all.filterNot { it.builtIn },
            dialog = o.dialog,
            confirmDelete = o.confirmDelete,
            message = o.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CategoriesUiState())

    fun setHidden(category: Category, hidden: Boolean) {
        viewModelScope.launch { categories.setHidden(category.id, hidden) }
    }

    fun startAdd() = overlay.update { it.copy(dialog = NameDialogState(categoryId = null, initialName = "")) }

    fun startRename(category: Category) =
        overlay.update { it.copy(dialog = NameDialogState(categoryId = category.id, initialName = category.name)) }

    fun closeDialog() = overlay.update { it.copy(dialog = null) }

    fun submitName(name: String) {
        val dialog = overlay.value.dialog ?: return
        viewModelScope.launch {
            val result = dialog.categoryId?.let { categories.rename(it, name) } ?: categories.add(name)
            when (result) {
                is CategoryNameResult.Saved -> closeDialog()
                CategoryNameResult.Blank -> overlay.update { it.copy(dialog = dialog.copy(error = NameError.BLANK)) }
                CategoryNameResult.Duplicate -> overlay.update { it.copy(dialog = dialog.copy(error = NameError.DUPLICATE)) }
            }
        }
    }

    fun askDelete(category: Category) = overlay.update { it.copy(confirmDelete = category) }

    fun cancelDelete() = overlay.update { it.copy(confirmDelete = null) }

    fun confirmDelete() {
        val category = overlay.value.confirmDelete ?: return
        overlay.update { it.copy(confirmDelete = null) }
        viewModelScope.launch {
            if (categories.delete(category.id) == DeleteCategoryResult.IN_USE) {
                overlay.update { it.copy(message = CategoriesMessage.IN_USE) }
            }
        }
    }

    fun messageShown() = overlay.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
