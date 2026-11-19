package com.dressup.feature.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.profile.ProfileRepository
import com.dressup.core.model.Profile
import com.dressup.core.ui.NameDialog
import com.dressup.core.ui.displayName

@Composable
fun ProfilesRoute(profiles: ProfileRepository, onBack: () -> Unit) {
    val viewModel: ProfilesViewModel = viewModel(factory = viewModelFactory { initializer { ProfilesViewModel(profiles) } })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfilesScreen(state = state, viewModel = viewModel, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilesScreen(state: ProfilesUiState, viewModel: ProfilesViewModel, onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(it.textRes) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profiles_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.profiles_back))
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
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.profiles_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            item { Header(stringResource(R.string.profiles_own)) }
            items(state.own, key = { it.id }) { profile ->
                val active = profile.id == state.activeId
                ProfileRow(
                    profile = profile,
                    icon = Icons.Filled.Person,
                    summary = stringResource(if (active) R.string.profiles_active else R.string.profiles_tap_to_switch),
                    active = active,
                    onClick = { viewModel.select(profile) },
                ) {
                    IconButton(onClick = { viewModel.startRename(profile) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.profiles_rename))
                    }
                    // Not offered where it would be refused: the active profile, or the last one of your own.
                    if (!active && state.own.size > 1) {
                        IconButton(onClick = { viewModel.askDelete(profile) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.profiles_delete))
                        }
                    }
                }
            }
            item { Header(stringResource(R.string.profiles_test_section)) }
            item { TestUserRow(state, viewModel) }
        }
    }

    Dialogs(state, viewModel)
}

@Composable
private fun TestUserRow(state: ProfilesUiState, viewModel: ProfilesViewModel) {
    val testUser = state.testUser
    val active = testUser != null && testUser.id == state.activeId
    val summary = when {
        state.busy == ProfilesBusy.PREPARING_TEST_USER -> stringResource(R.string.profiles_preparing)
        state.busy == ProfilesBusy.RESETTING_TEST_USER -> stringResource(R.string.profiles_resetting)
        testUser == null -> stringResource(R.string.profiles_test_user_create)
        active -> stringResource(R.string.profiles_active) + " · " + stringResource(R.string.profiles_test_user_summary)
        else -> stringResource(R.string.profiles_test_user_summary)
    }
    ProfileRow(
        profile = testUser ?: Profile(id = "", name = "", isTestUser = true),
        icon = Icons.Filled.Face,
        summary = summary,
        active = active,
        onClick = { if (testUser == null) viewModel.openTestUser() else viewModel.select(testUser) },
    ) {
        when {
            state.busy != null -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            testUser != null -> {
                IconButton(onClick = viewModel::askReset) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.profiles_reset))
                }
                if (!active) {
                    IconButton(onClick = { viewModel.askDelete(testUser) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.profiles_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    icon: ImageVector,
    summary: String,
    active: Boolean,
    onClick: () -> Unit,
    actions: @Composable () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                if (active) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = { Text(profile.displayName()) },
        supportingContent = { Text(summary) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
                Spacer(Modifier.width(4.dp))
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun Dialogs(state: ProfilesUiState, viewModel: ProfilesViewModel) {
    state.dialog?.let { dialog ->
        NameDialog(
            title = stringResource(if (dialog.profileId == null) R.string.profiles_add else R.string.profiles_rename),
            initialName = dialog.initialName,
            error = dialog.error?.let { stringResource(it.textRes) },
            onConfirm = viewModel::submitName,
            onDismiss = viewModel::closeDialog,
        )
    }
    state.confirmDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.profiles_delete_title, profile.displayName())) },
            text = {
                Text(stringResource(if (profile.isTestUser) R.string.profiles_delete_test_text else R.string.profiles_delete_text))
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.profiles_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.profiles_cancel)) } },
        )
    }
    if (state.confirmReset) {
        AlertDialog(
            onDismissRequest = viewModel::cancelReset,
            title = { Text(stringResource(R.string.profiles_reset_title)) },
            text = { Text(stringResource(R.string.profiles_reset_text)) },
            confirmButton = { TextButton(onClick = viewModel::confirmReset) { Text(stringResource(R.string.profiles_reset)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelReset) { Text(stringResource(R.string.profiles_cancel)) } },
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

private val ProfilesMessage.textRes: Int
    get() = when (this) {
        ProfilesMessage.CANT_DELETE_ACTIVE -> R.string.profiles_cant_delete_active
        ProfilesMessage.CANT_DELETE_LAST -> R.string.profiles_cant_delete_last
        ProfilesMessage.TEST_USER_RESET -> R.string.profiles_reset_done
        ProfilesMessage.TEST_USER_FAILED -> R.string.profiles_test_user_failed
    }
