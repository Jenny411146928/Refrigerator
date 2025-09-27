package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListPage(navController: NavController) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var all by remember { mutableStateOf(listOf<RecipeCardItem>()) }
    var featured by remember { mutableStateOf(listOf<RecipeCardItem>()) }

    // 🔹 從 Firestore 載入食譜
    LaunchedEffect(Unit) {
        loading = true
        val db = FirebaseFirestore.getInstance()
        val snap = db.collection("recipes").limit(200).get().await()
        val list = snap.documents.mapNotNull { d ->
            val title = d.getString("title") ?: return@mapNotNull null
            val img = d.getString("imageUrl")
            @Suppress("UNCHECKED_CAST")
            val ingredients =
                (d.get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            RecipeCardItem(id = d.id, title = title, imageUrl = img, ingredients = ingredients)
        }
        all = list
        featured = list.shuffled().take(20)
        loading = false
    }

    // 🔹 搜尋邏輯
    val items = remember(query, featured, all) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) featured
        else all.filter { r ->
            r.title.lowercase().contains(q) || r.ingredients.any { it.lowercase().contains(q) }
        }.take(100)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // 搜尋欄
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(1000.dp))
                .background(Color(0xFFD9D9D9))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = "Search Icon",
                modifier = Modifier.padding(end = 8.dp).size(22.dp),
                tint = Color.Unspecified
            )
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜尋食譜") },
                textStyle = TextStyle(color = Color(0xFF504848), fontSize = 15.sp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items, key = { it.id }) { recipe ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable {
                                val encodedId = Uri.encode(recipe.id)
                                navController.navigate("recipeDetailById/$encodedId")
                            }
                    ) {
                        Column {
                            // 統一圖片高度
                            AsyncImage(
                                model = recipe.imageUrl ?: "https://i.imgur.com/zMZxU8v.jpg",
                                contentDescription = recipe.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            // 標題灰底區塊
                            val titleBoxHeight = with(LocalDensity.current) {
                                (MaterialTheme.typography.bodyLarge.lineHeight * 2).toDp() + 16.dp
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEAEAEA))
                                    .height(titleBoxHeight)
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val cleanTitle = recipe.title.substringBefore(" by ").trim()

                                Text(
                                    text = cleanTitle,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔹 食譜資料卡片模型
data class RecipeCardItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val ingredients: List<String>
)
