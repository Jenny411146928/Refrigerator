@file:OptIn(ExperimentalMaterial3Api::class)

package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.ui.FridgeCardData
import ui.UiRecipe
import ui.encodeRecipeCards
import ui.decodeOrParseRecipeCards

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
        AsyncImage(
            model = "https://img.icons8.com/color/48/robot.png",
            contentDescription = "bot",
            modifier = Modifier
                .size(28.dp)
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

// ============================== BotThinkingMessage ==============================
@Composable
fun BotThinkingMessage() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = "https://img.icons8.com/color/48/robot.png",
            contentDescription = "bot",
            modifier = Modifier
                .size(28.dp)
                .padding(end = 6.dp)
        )
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFE3E6ED))
                .padding(12.dp)
        ) {
            Text("🤔 機器人正在思考你的料理中...", color = Color.DarkGray, fontSize = 15.sp)
        }
    }
}

// ============================== RecipeCardsBlock ==============================
@Composable
fun RecipeCardsBlock(
    title: String,
    recipes: List<UiRecipe>,
    foodList: List<FoodItem>,
    onAddToCart: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFABB7CD))
                .padding(12.dp)
        ) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(12.dp)) {
            recipes.forEachIndexed { index, recipe ->
                ExpandableRecipeItem(
                    recipe = recipe,
                    foodList = foodList,
                    onAddToCart = onAddToCart
                )
                if (index != recipes.lastIndex) {
                    Divider(
                        color = Color.LightGray.copy(alpha = 0.6f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "💡 小提醒：缺少食材時，可以直接點「＋」加入購物車喔！",
            color = Color(0xFF475569),
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ✅ 刪掉重複的 decodeOrParseRecipeCards，使用 ChatRecipeUtils.kt 提供的版本
// （這裡不再重複宣告）


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
                    Text(name, modifier = Modifier.weight(1f), color = Color.Black, fontSize = 15.sp)
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
