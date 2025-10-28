package tw.edu.pu.csim.refrigerator.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    uid: String?,
    fridgeList: List<FridgeCardData>,          //  新增：冰箱清單
    selectedFridgeId: String,                  //  新增：目前冰箱 ID
    onFridgeChange: (String) -> Unit,          //  新增：切換冰箱時回呼
    fridgeFoodMap: Map<String, MutableList<FoodItem>>, //  新增：所有冰箱的食材資料
    onAddToCart: (FoodItem) -> Unit,
    onBack: () -> Unit,
    favoriteRecipes: SnapshotStateList<Triple<String, String, String?>>,
    navController: NavController
)
{
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()   // ✅ 新增：Compose 專用 coroutine 範圍


    var title by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var link by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf<List<String>>(emptyList()) }
    var steps by remember { mutableStateOf<List<String>>(emptyList()) }
    var servings by remember { mutableStateOf<String?>(null) }
    var totalTime by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recipeId) {
        if (recipeId.isBlank()) return@LaunchedEffect   // ✅ 加這行防止空值閃退
        val doc = db.collection("recipes").document(recipeId).get().await()
        title = doc.getString("title") ?: ""
        imageUrl = doc.getString("imageUrl")
        link = doc.getString("link") ?: ""
        ingredients = (doc.get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        steps = (doc.get("steps") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        servings = doc.get("yield")?.toString()
        totalTime = doc.get("time")?.toString()
    }

    val currentFoodList = fridgeFoodMap[selectedFridgeId] ?: emptyList()
    val ownedNames = remember(currentFoodList) { currentFoodList.map { it.name } }

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

    //  收藏狀態
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
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        lineHeight = 34.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = {
                            scope.launch {   // ✅ 新增這一層，其他一律不改
                                if (isFavorite) {
                                    // ✅ 取消收藏（本地 + Firebase）
                                    favoriteRecipes.removeAll { it.first == recipeId }
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        try {
                                            tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.removeFavoriteRecipe(recipeId)
                                        } catch (e: Exception) {
                                            android.util.Log.e("RecipeDetail", "❌ 移除收藏失敗: ${e.message}")
                                        }
                                    }
                                    Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show()
                                } else {
                                    // ✅ 新增收藏（本地 + Firebase）
                                    favoriteRecipes.add(Triple(recipeId, recipeName, imageUrl))
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        try {
                                            tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.addFavoriteRecipe(
                                                recipeId = recipeId,
                                                title = recipeName,
                                                imageUrl = imageUrl,
                                                link = link
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e("RecipeDetail", "❌ 收藏食譜失敗: ${e.message}")
                                        }
                                    }
                                    Toast.makeText(context, "已加入收藏", Toast.LENGTH_SHORT).show()
                                }
                            }   // ✅ scope.launch 結束
                        },

                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavorite) Color(0xFFE53935) else Color(0xFF8A8A8A),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                }

                author?.let {
                    Text(
                        text = "by $it",
                        fontSize = 17.sp,
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
                Text("選擇冰箱", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // ✅ 下拉選擇冰箱
                var expanded by remember { mutableStateOf(false) }
                val currentFridgeName = fridgeList.find { it.id == selectedFridgeId }?.name ?: "未選擇冰箱"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.refrigerator),
                                contentDescription = "冰箱",
                                tint = Color(0xFF9DA5C1),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = currentFridgeName,
                                fontSize = 16.sp,
                                color = Color(0xFF333333),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF666666)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        fridgeList.forEach { fridge ->
                            DropdownMenuItem(
                                text = { Text(fridge.name) },
                                onClick = {
                                    expanded = false
                                    onFridgeChange(fridge.id) // ✅ 通知外層更新
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 食材區 ---
        item {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "食材",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(ingredients) { index, ingredient ->
            // ✅ 用 foodList 比對冰箱是否有此食材
            val hasIngredient = ownedNames.any {
                it.contains(ingredient, ignoreCase = true) || ingredient.contains(it, ignoreCase = true)
            }

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
                if (hasIngredient) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已有",
                        tint = Color(0xFF4CAF50)
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "加入購物車",
                        tint = Color(0xFF607D8B),
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE3E6ED))
                            .clickable {
                                android.util.Log.d("CartDebug", "🟢 點擊了＋按鈕：$ingredient")
                                Toast
                                    .makeText(context, "$ingredient 已加入購物車！", Toast.LENGTH_SHORT)
                                    .show()
                                onAddToCart(FoodItem(name = ingredient, quantity = "1"))
                            }
                            .padding(4.dp)
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
