package com.dressup.feature.categories

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.model.Category
import com.dressup.core.ui.NameDialog
import com.dressup.core.ui.SnackbarMessage
import com.dressup.core.ui.ListHeader

@Composable
fun CategoriesRoute(data: DressUpData, onBack: () -> Unit) {
    val viewModel: CategoriesViewModel = viewModel(
        factory = viewModelFactory { initializer { CategoriesViewModel(data.categories) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesScreen(state = state, viewModel = viewModel, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesScreen(state: CategoriesUiState, viewModel: CategoriesViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    SnackbarMessage(snackbar, stringResource(R.string.categories_in_use).takeIf { state.message == CategoriesMessage.IN_USE }, viewModel::messageShown)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.categories_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::startAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.categories_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            item { ListHeader(stringResource(R.string.categories_own)) }
            if (state.own.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.categories_own_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(state.own, key = { it.id }) { category -> OwnCategoryRow(category, viewModel) }
            item { ListHeader(stringResource(R.string.categories_built_in)) }
            items(state.builtIn, key = { it.id }) { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    supportingContent = { if (category.hidden) Text(stringResource(R.string.categories_hidden)) },
                    trailingContent = {
                        Switch(checked = !category.hidden, onCheckedChange = { shown -> viewModel.setHidden(category, !shown) })
                    },
                )
            }
        }
    }

    state.dialog?.let { dialog ->
        NameDialog(
            title = stringResource(if (dialog.categoryId == null) R.string.categories_add else R.string.categories_rename),
            initialName = dialog.initialName,
            error = dialog.error?.let { stringResource(it.textRes) },
            onConfirm = viewModel::submitName,
            onDismiss = viewModel::closeDialog,
        )
    }
    state.confirmDelete?.let { category ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.categories_delete_title, category.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.categories_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.categories_cancel)) } },
        )
    }
}

@Composable
private fun OwnCategoryRow(category: Category, viewModel: CategoriesViewModel) {
    ListItem(
        headlineContent = { Text(category.name) },
        supportingContent = { if (category.hidden) Text(stringResource(R.string.categories_hidden)) },
        trailingContent = {
            Row {
                Switch(checked = !category.hidden, onCheckedChange = { shown -> viewModel.setHidden(category, !shown) })
                IconButton(onClick = { viewModel.startRename(category) }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.categories_rename))
                }
                IconButton(onClick = { viewModel.askDelete(category) }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.categories_delete))
                }
            }
        },
    )
}

private val NameError.textRes: Int
    get() = when (this) {
        NameError.BLANK -> R.string.categories_name_blank
        NameError.DUPLICATE -> R.string.categories_name_duplicate
    }
