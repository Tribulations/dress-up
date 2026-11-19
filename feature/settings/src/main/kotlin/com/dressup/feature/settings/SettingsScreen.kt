package com.dressup.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource

/** The settings menu, opened from the wardrobe's gear. It holds no state of its own. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileName: String,
    appVersion: String,
    onProfiles: () -> Unit,
    onCategories: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SettingsRow(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.settings_profiles),
                summary = stringResource(R.string.settings_profile_active, profileName),
                onClick = onProfiles,
            )
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(R.string.settings_categories),
                summary = stringResource(R.string.settings_categories_summary),
                onClick = onCategories,
            )
            SettingsRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.settings_about),
                summary = stringResource(R.string.settings_version, appVersion),
                onClick = null,
            )
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, summary: String, onClick: (() -> Unit)?) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}
