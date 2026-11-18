package com.dressup.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dressup.core.data.DressUpData
import com.dressup.feature.categories.CategoriesRoute
import com.dressup.feature.garment.GarmentDetailRoute
import com.dressup.feature.garment.GarmentEditorRoute
import com.dressup.feature.wardrobe.WardrobeRoute

private object Routes {
    const val WARDROBE = "wardrobe"
    const val GARMENT = "garment/{id}"
    const val EDITOR = "editor?id={id}"
    const val CATEGORIES = "categories"
    const val NO_ID = -1L

    fun garment(id: Long) = "garment/$id"
    fun editor(id: Long?) = "editor?id=${id ?: NO_ID}"
}

/** Navigation between the features, which never know about each other (GUIDELINES.md §2.2). */
@Composable
fun DressUpNavHost(data: DressUpData) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.WARDROBE) {
        composable(Routes.WARDROBE) {
            WardrobeRoute(
                data = data,
                onOpenGarment = { nav.navigate(Routes.garment(it)) },
                onAddGarment = { nav.navigate(Routes.editor(null)) },
                onManageCategories = { nav.navigate(Routes.CATEGORIES) },
            )
        }
        composable(Routes.GARMENT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            GarmentDetailRoute(
                data = data,
                garmentId = id,
                onBack = { nav.popBackStack(Routes.WARDROBE, inclusive = false) },
                onEdit = { nav.navigate(Routes.editor(it)) },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = Routes.NO_ID }),
        ) { entry ->
            val id = entry.arguments?.getLong("id")?.takeIf { it != Routes.NO_ID }
            GarmentEditorRoute(
                data = data,
                garmentId = id,
                onCancel = { nav.popBackStack() },
                onDone = { savedId ->
                    nav.popBackStack()
                    // A new garment opens its page; an edit returns to the page it came from.
                    if (id == null) nav.navigate(Routes.garment(savedId))
                },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesRoute(data = data, onBack = { nav.popBackStack() })
        }
    }
}
