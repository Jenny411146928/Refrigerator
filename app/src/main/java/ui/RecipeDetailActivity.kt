package ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.refrigerator.ui.RecipeDetailScreen
import tw.edu.pu.csim.refrigerator.ui.theme.RefrigeratorTheme
import tw.edu.pu.csim.refrigerator.FoodItem

class RecipeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 從 Intent 取出 Firestore 文件 id（呼叫這個 Activity 時請 putExtra("recipeId", "...")）
        val recipeId: String = intent?.getStringExtra("recipeId").orEmpty()
        val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

        setContent {
            RefrigeratorTheme {
                RecipeDetailScreen(
                    recipeId = recipeId,
                    uid = uid,
                    onAddToCart = { _: FoodItem ->
                        // 若你要在 Activity 版本也寫入購物車，可在這裡實作
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
