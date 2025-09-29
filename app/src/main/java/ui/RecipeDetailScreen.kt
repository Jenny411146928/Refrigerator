package tw.edu.pu.csim.refrigerator.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    uid: String?,
    onBack: () -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    favoriteRecipes: SnapshotStateList<Triple<String, String, String?>> // 收藏清單
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var link by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf<List<String>>(emptyList()) }
    var steps by remember { mutableStateOf<List<String>>(emptyList()) }
    var servings by remember { mutableStateOf<String?>(null) }   // 例：「3 份」
    var totalTime by remember { mutableStateOf<String?>(null) }  // 例：「30 分鐘」
    var fridgeSet by remember { mutableStateOf(setOf<String>()) }

    // 讀取食譜
    LaunchedEffect(recipeId) {
        val doc = db.collection("recipes").document(recipeId).get().await()
        title = doc.getString("title") ?: ""
        imageUrl = doc.getString("imageUrl")
        link = doc.getString("link") ?: ""
        @Suppress("UNCHECKED_CAST")
        ingredients = (doc.get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        steps = (doc.get("steps") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        servings = doc.get("yield")?.toString()?.takeIf { it.isNotBlank() }
        totalTime = doc.get("time")?.toString()?.takeIf { it.isNotBlank() }
        Log.d("RecipeDetail", "進入食譜詳情 recipeId = $recipeId")

    }

    // 監聽冰箱清單
    LaunchedEffect(uid) {
        if (uid != null) {
            db.collection("users").document(uid).collection("fridge")
                .addSnapshotListener { snap, _ ->
                    val names = snap?.documents?.mapNotNull { it.getString("name") }?.toSet() ?: emptySet()
                    fridgeSet = names
                }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 大圖 + 返回
        item {
            Box(Modifier.height(250.dp)) {
                AsyncImage(
                    model = imageUrl ?: "https://i.imgur.com/zMZxU8v.jpg",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(36.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White, shape = RoundedCornerShape(50))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        }

        // 標題 + 人數/時間
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp)
            ) {
                // 🔹 拆分 title (recipeName, author)
                val parts = title.split(" by ", limit = 2)
                val recipeName = parts.getOrNull(0) ?: title
                val author = parts.getOrNull(1)

                // 標題 + 收藏愛心（同一行）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // 食譜名稱 + 作者
                    Text(
                        text = recipeName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp,

                        modifier = Modifier.weight(1f) // 標題佔滿左邊空間

                    )
                    //var isFavorite by remember { mutableStateOf(false) }
                    //IconButton(onClick = { isFavorite = !isFavorite }) {
                    //    Icon(
                    //        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    //        contentDescription = "收藏食譜",
                    //        tint = if (isFavorite) Color.Red else Color.Gray,
                    //        modifier = Modifier.size(30.dp) // 控制收藏愛心大小
                    //    )
                    //}
                    // 判斷目前是否在收藏清單裡
                    val isFavorite by remember(favoriteRecipes, recipeId) {
                        derivedStateOf { favoriteRecipes.any { it.first == recipeId } }
                    }

                    IconButton(onClick = {
                        if (isFavorite) {
                            // 移除收藏
                            favoriteRecipes.removeAll { it.first == recipeId }
                        } else {
                            // 加入收藏 (存 id, title, imageUrl)
                            favoriteRecipes.add(Triple(recipeId, recipeName, imageUrl))
                        }
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "加入收藏",
                            tint = if (isFavorite) Color.Red else Color.Gray,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                author?.let {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "by $it",
                        fontSize = 18.sp, // 比標題小
                        color = Color.Gray, // 用灰色區分
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 4.dp) // 與標題拉開一點距離
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 人數與時間（永遠顯示，沒資料就顯示「未提供」）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp) // 控制左右間距
                ) {
                    InfoPill(
                        iconRes = R.drawable.people, // 人數圖示
                        text = servings?.takeIf { it.isNotBlank() }?.plus(" 人份") ?: "未提供"
                    )
                    InfoPill(
                        iconRes = R.drawable.clock,   // 時間圖示
                        text = totalTime?.takeIf { it.isNotBlank() } ?: "未提供"
                    )
                }
            }
        }

        // 食材
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // 標題
                Text(
                    text = "食材",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // 卡片
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3E6ED)), // 淺藍灰
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ingredients.forEach { name ->
                            val owned = fridgeSet.contains(name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontSize = 16.sp)
                                if (owned) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "已有",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "加入購物車",
                                        modifier = Modifier.clickable {
                                            onAddToCart(FoodItem(name = name))
                                            if (uid != null) {
                                                val ref = db.collection("users").document(uid)
                                                    .collection("fridge").document(name)
                                                ref.set(
                                                    mapOf(
                                                        "name" to name,
                                                        "have" to true,
                                                        "updatedAt" to FieldValue.serverTimestamp()
                                                    ),
                                                    SetOptions.merge()
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 步驟
        if (steps.isNotEmpty()) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // 標題
                    Text(
                        text = "步驟",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // 卡片
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)), // 比食材更淺
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            steps.forEachIndexed { index, step ->
                                Text(
                                    text = "${index + 1}. $step",
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 來源
        if (link.isNotBlank()) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
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
                            containerColor = Color(0xFFE3E6ED), // 呼應食材卡片的藍灰色
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("前往來源頁面")
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

/** 區塊標題（大一點、加粗） */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
    )
}

/** 小圓角資訊膠囊（圖片 + 文字） */
@Composable
private fun InfoPill(
    iconRes: Int,
    text: String
) {
    Surface(
        color = Color(0xFFF2F2F2),
        shape = RoundedCornerShape(50),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified  // 若你的圖是彩色，避免被染色
            )
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 14.sp)
        }
    }
}