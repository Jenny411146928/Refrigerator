package ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tw.edu.pu.csim.refrigerator.ui.theme.RefrigeratorTheme
import tw.edu.pu.csim.refrigerator.R
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import tw.edu.pu.csim.refrigerator.ui.RecipeDetailScreen

class RecipeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra("name") ?: "無標題"
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""

        setContent {
            RefrigeratorTheme {
                Scaffold(
                    topBar = {
                        SimpleTopAppBar(title = name) { finish() }
                    },
                    bottomBar = {
                        SimpleBottomNavigationBar()
                    }
                ) { innerPadding ->
                    RecipeDetailScreen(
                        recipeName = name,
                        imageUrl = imageUrl,
                        isFavorite = true,
                        onToggleFavorite = {},
                        ingredients = listOf("牛肉" to true, "紅蘿蔔" to false),
                        steps = listOf("切肉", "炒蔥薑", "加醬油煮"),
                        onAddToCart = { ingredientName ->
                            Log.d("RecipeDetail", "$ingredientName 要加入購物車")
                        },
                        onBack = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
        }
    )
}

@Composable
fun SimpleBottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO: 回首頁 */ },
            icon = { Icon(Icons.Default.Home, contentDescription = "首頁") },
            label = { Text("首頁") }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Star, contentDescription = "食譜") },
            label = { Text("食譜") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Person, contentDescription = "個人") },
            label = { Text("個人") }
        )
    }
}
