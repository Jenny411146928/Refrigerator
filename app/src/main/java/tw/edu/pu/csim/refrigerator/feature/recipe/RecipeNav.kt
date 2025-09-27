package tw.edu.pu.csim.refrigerator.feature.recipe

import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.ui.RecipeDetailScreen
import tw.edu.pu.csim.refrigerator.ui.RecipeListPage

@Composable
fun RecipeNavRoot(
    uid: String?,
    onAddToCart: (FoodItem) -> Unit,
    favoriteRecipes: SnapshotStateList<Triple<String, String, String?>>
) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "recipeList") {
        composable("recipeList") {
            RecipeListPage(navController = nav)
        }

        composable(
            "recipeDetail/{recipeId}",
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStack ->
            val recipeId = backStack.arguments?.getString("recipeId") ?: return@composable
            RecipeDetailScreen(
                recipeId = recipeId,
                uid = uid,
                onBack = { nav.popBackStack() },
                onAddToCart = onAddToCart,
                favoriteRecipes = favoriteRecipes
            )
        }
    }
}
