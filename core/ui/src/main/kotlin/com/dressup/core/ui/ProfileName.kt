package com.dressup.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dressup.core.model.Profile

/** The name to show for a profile: the test user's is a translatable text, not a stored name. */
@Composable
fun Profile.displayName(): String = if (isTestUser) stringResource(R.string.core_ui_test_user) else name
