package com.dressup.core.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

enum class DressUpTab { WARDROBE, OUTFITS }

/** The bottom bar of the app's top-level screens. The app decides what each tab shows (GUIDELINES.md §2.2). */
@Composable
fun DressUpTabBar(selected: DressUpTab, onSelect: (DressUpTab) -> Unit) {
    NavigationBar {
        DressUpTab.entries.forEach { tab ->
            val label = stringResource(if (tab == DressUpTab.WARDROBE) R.string.core_ui_tab_wardrobe else R.string.core_ui_tab_outfits)
            NavigationBarItem(
                selected = tab == selected,
                onClick = { if (tab != selected) onSelect(tab) },
                icon = {
                    Icon(
                        painterResource(if (tab == DressUpTab.WARDROBE) R.drawable.core_ui_ic_wardrobe else R.drawable.core_ui_ic_outfits),
                        contentDescription = null,
                    )
                },
                label = { Text(label) },
            )
        }
    }
}
