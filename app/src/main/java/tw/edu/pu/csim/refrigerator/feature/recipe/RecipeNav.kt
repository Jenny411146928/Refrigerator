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
import androidx.navigation.NavController
import tw.edu.pu.csim.refrigerator.ui.FridgeCardData

@Composable
fun RecipeNavRoot(
    uid: String?,
    onAddToCart: (FoodItem) -> Unit,
    favoriteRecipes: SnapshotStateList<Triple<String, String, String?>>,
    fridgeFoodMap: MutableMap<String, MutableList<FoodItem>>, // ✅ 從 MainActivity 傳入
    fridgeList: List<FridgeCardData>,
    selectedFridgeId: String,
    onFridgeChange: (String) -> Unit
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

            // ✅ 取得目前冰箱的食材清單（若找不到則給空 List）
            val currentFoodList = fridgeFoodMap[selectedFridgeId] ?: emptyList()

            RecipeDetailScreen(
                recipeId = recipeId,
                uid = uid,
                fridgeList = fridgeList,                 // ✅ 傳入冰箱清單
                selectedFridgeId = selectedFridgeId,     // ✅ 傳入目前冰箱 ID
                onFridgeChange = onFridgeChange,         // ✅ 切換時更新
                fridgeFoodMap = fridgeFoodMap,           // ✅ 所有冰箱食材資料
                onAddToCart = onAddToCart,
                onBack = { nav.popBackStack() },
                favoriteRecipes = favoriteRecipes,
                navController = nav
            )
        }
    }
}
