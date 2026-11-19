package com.dressup.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dressup.core.data.DressUpData
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.data.profile.ProfileRepository
import com.dressup.core.model.Profile
import com.dressup.core.ui.DressUpTab
import com.dressup.core.ui.DressUpTabBar
import com.dressup.core.ui.displayName
import com.dressup.feature.backup.BackupRoute
import com.dressup.feature.categories.CategoriesRoute
import com.dressup.feature.diary.DiaryEditorRoute
import com.dressup.feature.diary.DiaryEntryRoute
import com.dressup.feature.diary.DiaryRoute
import com.dressup.feature.garment.GarmentDetailRoute
import com.dressup.feature.garment.GarmentEditorRoute
import com.dressup.feature.outfits.OutfitDetailRoute
import com.dressup.feature.outfits.OutfitEditorRoute
import com.dressup.feature.outfits.OutfitsRoute
import com.dressup.feature.profiles.ProfilesRoute
import com.dressup.feature.settings.SettingsScreen
import com.dressup.feature.wardrobe.WardrobeRoute

private object Routes {
    const val WARDROBE = "wardrobe"
    const val OUTFITS = "outfits"
    const val DIARY = "diary"
    const val GARMENT = "garment/{id}"
    const val EDITOR = "editor?id={id}"
    const val OUTFIT = "outfit/{id}"
    const val OUTFIT_EDITOR = "outfit-editor?id={id}"
    const val ENTRY = "diary/{id}"
    const val ENTRY_EDITOR = "diary-editor?id={id}&outfit={outfit}&garment={garment}"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val PROFILES = "profiles"
    const val BACKUP = "backup"
    const val NO_ID = -1L

    fun garment(id: Long) = "garment/$id"
    fun editor(id: Long?) = "editor?id=${id ?: NO_ID}"
    fun outfit(id: Long) = "outfit/$id"
    fun outfitEditor(id: Long?) = "outfit-editor?id=${id ?: NO_ID}"
    fun entry(id: Long) = "diary/$id"
    fun entryEditor(id: Long? = null, outfit: Long? = null, garment: Long? = null) =
        "diary-editor?id=${id ?: NO_ID}&outfit=${outfit ?: NO_ID}&garment=${garment ?: NO_ID}"
}

/**
 * Navigation between the features, which never know about each other (GUIDELINES.md §2.2). Built for one [profile]:
 * switching profile rebuilds it ([ProfileScope]).
 */
@Composable
fun DressUpNavHost(profiles: ProfileRepository, backup: ProfileBackup, data: DressUpData, profile: Profile, appVersion: String) {
    val nav = rememberNavController()
    val id = listOf(navArgument("id") { type = NavType.LongType })
    val optionalId = listOf(optional("id"))
    val tabBar: @Composable (DressUpTab) -> Unit = { tab -> DressUpTabBar(tab) { nav.switchTab(it) } }
    NavHost(navController = nav, startDestination = Routes.WARDROBE) {
        composable(Routes.WARDROBE) {
            WardrobeRoute(
                data = data,
                profileName = profile.displayName(),
                onOpenGarment = { nav.navigate(Routes.garment(it)) },
                onAddGarment = { nav.navigate(Routes.editor(null)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                bottomBar = { tabBar(DressUpTab.WARDROBE) },
            )
        }
        composable(Routes.OUTFITS) {
            OutfitsRoute(
                data = data,
                profileName = profile.displayName(),
                onOpenOutfit = { nav.navigate(Routes.outfit(it)) },
                onNewOutfit = { nav.navigate(Routes.outfitEditor(null)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                bottomBar = { tabBar(DressUpTab.OUTFITS) },
            )
        }
        composable(Routes.DIARY) {
            DiaryRoute(
                data = data,
                profileName = profile.displayName(),
                onOpenEntry = { nav.navigate(Routes.entry(it)) },
                onLogWear = { nav.navigate(Routes.entryEditor()) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                bottomBar = { tabBar(DressUpTab.DIARY) },
            )
        }
        composable(Routes.GARMENT, arguments = id) { entry ->
            GarmentDetailRoute(
                data = data,
                garmentId = entry.longArg("id") ?: return@composable,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.editor(it)) },
                onLogWear = { nav.navigate(Routes.entryEditor(garment = it)) },
                onOpenEntry = { nav.navigate(Routes.entry(it)) },
            )
        }
        composable(Routes.EDITOR, arguments = optionalId) { entry ->
            val garmentId = entry.longArg("id")
            GarmentEditorRoute(
                data = data,
                garmentId = garmentId,
                onCancel = { nav.popBackStack() },
                onDone = { savedId ->
                    nav.popBackStack()
                    // A new garment opens its page; an edit returns to the page it came from.
                    if (garmentId == null) nav.navigate(Routes.garment(savedId))
                },
            )
        }
        composable(Routes.OUTFIT, arguments = id) { entry ->
            OutfitDetailRoute(
                data = data,
                outfitId = entry.longArg("id") ?: return@composable,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.outfitEditor(it)) },
                onOpenGarment = { nav.navigate(Routes.garment(it)) },
                onLogWear = { nav.navigate(Routes.entryEditor(outfit = it)) },
                onOpenEntry = { nav.navigate(Routes.entry(it)) },
            )
        }
        composable(Routes.OUTFIT_EDITOR, arguments = optionalId) { entry ->
            val outfitId = entry.longArg("id")
            OutfitEditorRoute(data = data, outfitId = outfitId) { savedId ->
                nav.popBackStack()
                // A new outfit opens its page; an edit returns to it, and a deleted one's page closes by itself.
                if (outfitId == null && savedId != null) nav.navigate(Routes.outfit(savedId))
            }
        }
        composable(Routes.ENTRY, arguments = id) { entry ->
            DiaryEntryRoute(
                data = data,
                entryId = entry.longArg("id") ?: return@composable,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.entryEditor(id = it)) },
                onOpenOutfit = { nav.navigate(Routes.outfit(it)) },
                onOpenGarment = { nav.navigate(Routes.garment(it)) },
            )
        }
        composable(Routes.ENTRY_EDITOR, arguments = listOf(optional("id"), optional("outfit"), optional("garment"))) { entry ->
            DiaryEditorRoute(
                data = data,
                entryId = entry.longArg("id"),
                outfitId = entry.longArg("outfit"),
                garmentId = entry.longArg("garment"),
                onDone = { nav.popBackStack() },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesRoute(data = data, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                profileName = profile.displayName(),
                appVersion = appVersion,
                onProfiles = { nav.navigate(Routes.PROFILES) },
                onCategories = { nav.navigate(Routes.CATEGORIES) },
                onBackup = { nav.navigate(Routes.BACKUP) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.PROFILES) {
            ProfilesRoute(profiles = profiles, onBack = { nav.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupRoute(
                backup = backup,
                profileId = profile.id,
                profileName = profile.displayName(),
                onSwitchTo = profiles::switchTo,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

private fun optional(name: String) = navArgument(name) { type = NavType.LongType; defaultValue = Routes.NO_ID }

private fun NavBackStackEntry.longArg(name: String): Long? = arguments?.getLong(name)?.takeIf { it != Routes.NO_ID }

/** The tabs share one back stack root (the wardrobe), so Back from another tab goes to the wardrobe, then out of the app. */
private fun NavHostController.switchTab(tab: DressUpTab) {
    val route = when (tab) {
        DressUpTab.WARDROBE -> return run { popBackStack(Routes.WARDROBE, inclusive = false) }
        DressUpTab.OUTFITS -> Routes.OUTFITS
        DressUpTab.DIARY -> Routes.DIARY
    }
    navigate(route) {
        popUpTo(Routes.WARDROBE)
        launchSingleTop = true
    }
}
