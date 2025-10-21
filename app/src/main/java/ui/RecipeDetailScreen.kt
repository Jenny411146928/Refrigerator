package tw.edu.pu.csim.refrigerator.ui

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    uid: String?,
    onBack: () -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    favoriteRecipes: SnapshotStateList<Triple<String, String, String?>>
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var link by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf<List<String>>(emptyList()) }
    var steps by remember { mutableStateOf<List<String>>(emptyList()) }
    var servings by remember { mutableStateOf<String?>(null) }
    var totalTime by remember { mutableStateOf<String?>(null) }
    var fridgeSet by remember { mutableStateOf(setOf<String>()) }

    // 🔹 讀取食譜資料
    LaunchedEffect(recipeId) {
        val doc = db.collection("recipes").document(recipeId).get().await()
        title = doc.getString("title") ?: ""
        imageUrl = doc.getString("imageUrl")
        link = doc.getString("link") ?: ""
        ingredients = (doc.get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        steps = (doc.get("steps") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        servings = doc.get("yield")?.toString()
        totalTime = doc.get("time")?.toString()
    }

    // ✅ 暫時假資料（未連 Firebase）
    LaunchedEffect(Unit) {
        fridgeSet = setOf("雞蛋", "牛奶", "番茄", "菠菜", "洋蔥", "雞胸肉")
    }

    /* ✅ Firebase 實際連線版本（之後可用）
    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).collection("fridge")
                .addSnapshotListener { snap, _ ->
                    val names = snap?.documents
                        ?.mapNotNull { it.getString("name") }
                        ?.toSet() ?: emptySet()
                    fridgeSet = names
                }
        }
    }
    */

    val isFavorite by remember(favoriteRecipes, recipeId) {
        derivedStateOf { favoriteRecipes.any { it.first == recipeId } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        // --- 圖片 ---
        item {
            Box(modifier = Modifier.height(250.dp)) {
                AsyncImage(
                    model = imageUrl ?: "https://i.imgur.com/zMZxU8v.jpg",
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(42.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            }
        }

        // --- 標題 + 作者 + 收藏 ---
        item {
            val parts = title.split(" by ", limit = 2)
            val recipeName = parts.getOrNull(0) ?: title
            val author = parts.getOrNull(1)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipeName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            if (isFavorite)
                                favoriteRecipes.removeAll { it.first == recipeId }
                            else
                                favoriteRecipes.add(Triple(recipeId, recipeName, imageUrl))
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorite) Color(0xFFE53935) else Color(0xFF8A8A8A),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                author?.let {
                    Text(
                        text = "by $it",
                        fontSize = 19.sp,
                        color = Color(0xFF6E6E6E),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    InfoPill(
                        iconRes = R.drawable.people,
                        text = if (!servings.isNullOrBlank()) "${servings} 人份" else "未提供"
                    )
                    InfoPill(iconRes = R.drawable.clock, text = totalTime ?: "未提供")
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("食材", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // --- 食材區 ---
        itemsIndexed(ingredients) { index, ingredient ->
            val owned = fridgeSet.any { it.contains(ingredient.take(2)) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}. $ingredient", fontSize = 16.sp)
                if (owned) {
                    Icon(Icons.Default.Check, contentDescription = "已有", tint = Color(0xFF4CAF50))
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "加入購物車",
                        tint = Color(0xFF607D8B),
                        modifier = Modifier.clickable {
                            onAddToCart(FoodItem(name = ingredient))
                        }
                    )
                }
            }
        }

        // --- 步驟 ---
        if (steps.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "作法步驟",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        steps.forEachIndexed { index, step ->
                            val stepNumber = if (step.trim().startsWith("步驟") ||
                                step.trim().firstOrNull()?.isDigit() == true
                            ) "" else "${index + 1}. "

                            Text(
                                text = stepNumber + step,
                                fontSize = 16.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            if (index != steps.lastIndex) {
                                Divider(
                                    color = Color(0xFFE0E0E0),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 前往來源頁面 ---
            if (link.isNotBlank()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalButton(
                            onClick = {
                                runCatching {
                                    CustomTabsIntent.Builder()
                                        .setShowTitle(true)
                                        .build()
                                        .launchUrl(context, Uri.parse(link))
                                }.onFailure {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    context.startActivity(intent)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFE3E6ED)
                            )
                        ) {
                            Text("前往來源頁面")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoPill(iconRes: Int, text: String) {
    Surface(color = Color(0xFFF2F2F2), shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 14.sp)
        }
    }
}
