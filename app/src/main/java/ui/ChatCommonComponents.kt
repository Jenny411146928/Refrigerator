@file:OptIn(ExperimentalMaterial3Api::class)

package ui

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.ui.FridgeCardData
import tw.edu.pu.csim.refrigerator.ui.RecipeCardItem
import ui.UiRecipe
import ui.encodeRecipeCards
import ui.decodeOrParseRecipeCards
import ui.formatRecipeDuration

// ============================== Chat Input ==============================
@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("輸入訊息...", color = Color.Gray) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFE3E6ED),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF8D99B3))
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Text("➤", color = Color.White, fontSize = 18.sp)
        }
    }
}

// ============================== 冰箱選擇區塊 ==============================
@Composable
fun FridgeSelectionBlock(
    fridgeList: List<FridgeCardData>,
    onSelect: (FridgeCardData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE3E6ED))
            .padding(12.dp)
    ) {
        Text("請選擇要使用的冰箱：", color = Color.DarkGray, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))

        if (fridgeList.isEmpty()) {
            Text("（目前沒有冰箱喔，請先新增一個冰箱）", color = Color.Gray, fontSize = 14.sp)
        } else {
            LazyRow {
                items(fridgeList) { fridge: FridgeCardData ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD9DEE8))
                            .clickable { onSelect(fridge) }
                            .padding(10.dp)
                    ) {
                        if (!fridge.imageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = fridge.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(fridge.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ============================== BotMessage ==============================
@Composable
fun BotMessage(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.Top
    ) {

        Image(
            painter = painterResource(id = R.drawable.ic_foodiebot),
            contentDescription = "FoodieBot",
            modifier = Modifier
                .size(52.dp)
                .padding(end = 6.dp)
        )

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFD9DEE8))
                .padding(12.dp)
        ) {
            Text(text, color = Color.DarkGray, fontSize = 15.sp)
        }
    }
}

@Composable
fun UserMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFCCE5FF))
                .padding(12.dp)
        ) {
            Text(text, color = Color.Black, fontSize = 15.sp)
        }
    }
}

@Composable
fun BotThinkingMessage() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.Top
    ) {

        // 🔧 強制固定大小 + placeholder，避免圖片載入造成「先小後大」跳動
        AsyncImage(
            model = R.drawable.ic_foodiebot, // 你的自訂頭像
            contentDescription = "bot",
            modifier = Modifier
                .size(32.dp)    // ← 統一固定尺寸（跟 BotMessage 一樣）
                .padding(end = 6.dp),
            placeholder = painterResource(R.drawable.ic_foodiebot), // 先用同圖當 placeholder
            error = painterResource(R.drawable.ic_foodiebot)        // 即使載入錯誤也保持大小一致
        )

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFE3E6ED))
                .padding(12.dp)
        ) {
            Text(
                "🤔 機器人正在思考你的料理中...",
                color = Color.DarkGray,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun RecipeCardsBlock(
    title: String,
    recipes: List<UiRecipe>,
    foodList: List<FoodItem>,
    onAddToCart: (String) -> Unit,
    navController: NavController
) {

    // 外層卡片：整體淡藍底 + 邊框
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFD7E0EA)), // 淡藍邊線
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3E6ED)), // ✅ 整體淡藍底
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ✅ 標題列（保留原樣，只去除 icon）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C6370)
                )
            }

            // ✅ 橫向卡片區（保持原本結構）
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipes) { recipe ->
                    Log.e("RecipeCard", "id=${recipe.id}, name=${recipe.name}")
                    var updatedRecipe by remember { mutableStateOf(recipe) }

                    // 🔹 Firestore 補資料（不變）
                    LaunchedEffect(recipe.name) {
                        try {
                            val snapshot = FirebaseFirestore.getInstance()
                                .collection("recipes")
                                .whereEqualTo("title", recipe.name)
                                .get()
                                .await()
                            if (!snapshot.isEmpty) {
                                val doc = snapshot.documents.first()
                                val id = doc.id
                                val img = doc.getString("imageUrl")
                                val yieldVal = doc.getString("yield")
                                val timeVal = formatRecipeDuration(doc.getString("time")) // ✅ 修正這裡

                                updatedRecipe = recipe.copy(
                                    id = id,
                                    imageUrl = img,
                                    servings = yieldVal,
                                    totalTime = timeVal  // ✅ 現在是已轉換好的 15分鐘
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // ✅ 單張食譜卡片（白底 + 陰影）
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .clickable {
                                val id = updatedRecipe.id
                                if (id.isNullOrBlank()) {
                                    Log.e("RecipeCard", "❌ 無法導向，id 為空：${updatedRecipe.name}")
                                    return@clickable
                                }
                                navController.navigate("recipeDetail/$id")
                            }


                        ,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 食譜圖片
                            AsyncImage(
                                model = updatedRecipe.imageUrl
                                    ?: "https://cdn-icons-png.flaticon.com/512/857/857681.png",
                                contentDescription = updatedRecipe.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )

                            // ✅ 食譜名稱（固定高度）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp) // 給文字穩定顯示空間
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = updatedRecipe.name.ifBlank { "未命名料理" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // ✅ 人數與時間固定底部
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = tw.edu.pu.csim.refrigerator.R.drawable.people),
                                    contentDescription = "份量",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Unspecified
                                )

                                // ✅ 自動補上「人份」
                                val servingsText = if (!updatedRecipe.servings.isNullOrBlank()) {
                                    if (updatedRecipe.servings!!.contains("人份"))
                                        updatedRecipe.servings!!
                                    else
                                        updatedRecipe.servings!! + " 人份"
                                } else {
                                    "未提供"
                                }

                                Text(
                                    text = servingsText,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )

                                Icon(
                                    painter = painterResource(id = tw.edu.pu.csim.refrigerator.R.drawable.clock),
                                    contentDescription = "時間",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Unspecified
                                )
                                Text(
                                    text = updatedRecipe.totalTime ?: "未提供",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCardItem(
    recipe: UiRecipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            AsyncImage(
                model = recipe.imageUrl
                    ?: "https://cdn-icons-png.flaticon.com/512/857/857681.png", // ✅ 若資料庫沒有就用備用圖
                contentDescription = recipe.name,
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = recipe.name.ifBlank { "未命名料理" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    maxLines = 2
                )

                Spacer(Modifier.height(8.dp))

                // ✅ 顯示份量 + 時間
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = tw.edu.pu.csim.refrigerator.R.drawable.people),
                        contentDescription = "份量",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = if (!recipe.servings.isNullOrBlank()) "${recipe.servings} 人份" else "未提供",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        painter = painterResource(id = tw.edu.pu.csim.refrigerator.R.drawable.clock),
                        contentDescription = "時間",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = recipe.totalTime ?: "未提供",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

        }
    }


    // ============================== ExpandableRecipeItem ==============================
    @Composable
    fun ExpandableRecipeItem(
        recipe: UiRecipe,
        foodList: List<FoodItem>,
        onAddToCart: (String) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFDFDFE))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    recipe.name.ifBlank { "未命名料理" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(if (expanded) "︿" else "﹀", fontSize = 18.sp, color = Color(0xFF2F3542))
            }

            if (expanded) {
                Text(
                    "食材：",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                val ingredients =
                    if (recipe.ingredients.isEmpty()) listOf("（AI 未提供內容）") else recipe.ingredients
                ingredients.filter { it.isNotBlank() }.forEach { ing ->
                    val name = ing.trim()
                    val hasIt = foodList.any { it.name.contains(name.take(2)) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            modifier = Modifier.weight(1f),
                            color = Color.Black,
                            fontSize = 15.sp
                        )
                        if (hasIt) {
                            Text("✔", color = Color(0xFF4CAF50), fontSize = 18.sp)
                        } else {
                            Text("+", color = Color.Black, fontSize = 20.sp,
                                modifier = Modifier.clickable { onAddToCart(name) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "步驟：",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                val steps = if (recipe.steps.isEmpty()) listOf("（AI 未提供步驟）") else recipe.steps
                steps.filter { it.isNotBlank() }.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. ${step.trim()}",
                        color = Color(0xFF222222),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}