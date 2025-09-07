package tw.edu.pu.csim.refrigerator.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    uid: String?,
    onBack: () -> Unit,
    onAddToCart: (FoodItem) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }

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
        servings = doc.get("servings")?.toString()?.takeIf { it.isNotBlank() }
        totalTime = doc.get("totalTime")?.toString()?.takeIf { it.isNotBlank() }
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                // 食譜名稱：加大、加粗
                // 食譜名稱：加大、加粗，並加大與資訊列的間距
                Text(
                    text = title.ifBlank { "（未命名食譜）" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp) // 👈 讓名稱和下方資訊不要太擠
                )

// 人數與時間（有資料才顯示）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    servings?.let {
                        InfoPill(
                            iconRes = R.drawable.people, // 放在 res/drawable 的人數圖
                            text = it
                        )
                    }
                    totalTime?.let {
                        InfoPill(
                            iconRes = R.drawable.clock,   // 放在 res/drawable 的時間圖
                            text = it
                        )
                    }
                }

            }
        }

        // 食材
        item { Spacer(Modifier.height(8.dp)) }
        item { SectionTitle("食材") }
        itemsIndexed(ingredients) { _, name ->
            val owned = fridgeSet.contains(name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, fontSize = 16.sp)
                if (owned) {
                    Icon(Icons.Filled.Check, contentDescription = "已有", tint = MaterialTheme.colorScheme.primary)
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

        // 步驟（有資料才顯示）
        if (steps.isNotEmpty()) {
            item { Spacer(Modifier.height(16.dp)) }
            item { SectionTitle("步驟") }
            itemsIndexed(steps) { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }

        // 來源
        if (link.isNotBlank()) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                TextButton(
                    onClick = { /* TODO: CustomTabs 開啟 link */ },
                    modifier = Modifier.padding(start = 12.dp, bottom = 24.dp)
                ) {
                    Text("前往來源頁面")
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
