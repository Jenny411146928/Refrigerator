@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.model.ChatMessage

// ---------------- ChatPage ----------------
@Composable
fun ChatPage(
    foodList: List<FoodItem> = emptyList(),
    onAddToCart: (String) -> Unit = {},
    viewModel: ChatViewModel
) {
    val messageList = viewModel.messages
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 🔹 選項列用的狀態
    var selectedTab by remember { mutableStateOf("📋 全部") }
    val categories = listOf("📋 全部", "🥬 食材小幫手", "🍳 食譜推薦", "🛒 購物提醒")

    // ✅ 當訊息數變動，自動捲到底部
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.size - 1)
        }
    }

    // ✅ 確保至少有一次 BotOptions（但交給 ViewModel 控制）
    LaunchedEffect(Unit) {
        if (messageList.isEmpty()) {
            viewModel.ensureOptionsMessage()
        }
    }

    // ✅ 根據分類篩選訊息
    val filteredMessages = when (selectedTab) {
        "🥬 食材小幫手" -> messageList.filter { it.type == "ingredients" }
        "🍳 食譜推薦" -> messageList.filter { it.type == "recommendations" || it.type == "steps" }
        "🛒 購物提醒" -> messageList.filter { it.type == "reminder" }
        else -> messageList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
    ) {
        // 🔹 上方 LazyRow 選項列
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedTab == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Color(0xFFFFFEB6) else Color(0xFFE3E6ED))
                        .clickable { selectedTab = category }
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.Black,
                        maxLines = 1
                    )
                }
            }
        }

        // 🔹 聊天訊息 + 快速捲到底按鈕
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                val grouped = filteredMessages.groupBy {
                    java.time.Instant.ofEpochMilli(it.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                }
                grouped.forEach { (date, messagesForDate) ->
                    item { DateHeader(date) }
                    items(messagesForDate) { message ->
                        when (message.type) {
                            "options" -> BotOptions(
                                onSelectFridge = {
                                    viewModel.addMessage(ChatMessage("user", "冰箱推薦"))
                                    viewModel.askAI(foodList.map { it.name }, checkFridge = true)
                                },
                                onSelectCustom = {
                                    viewModel.addMessage(ChatMessage("user", "今天想吃什麼料理"))
                                    viewModel.askAI()
                                }
                            )
                            "recommendations" -> {
                                BotMessage("根據您的食材，我為您推薦以下料理：\n${message.content}")
                            }
                            "ingredients" -> {
                                val ingredients = message.content.split(",")
                                BotRecipeMessage(
                                    recipeName = "料理建議",
                                    ingredients = ingredients,
                                    foodList = foodList,
                                    onAddToCart = onAddToCart
                                )
                            }
                            "steps" -> {
                                val steps = message.content.split("||")
                                BotStepMessage(steps)
                            }
                            "reminder" -> {
                                BotMessage("🛒 購物提醒：${message.content}")
                            }
                            else -> {
                                if (message.role == "user") {
                                    UserMessage(message.content)
                                } else {
                                    BotMessage(message.content)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            // ⬇️ 快速捲到底按鈕
            if (listState.firstVisibleItemIndex < messageList.size - 3) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(messageList.size - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Color(0xFFABB7CD)
                ) {
                    Text("⬇", color = Color.White, fontSize = 18.sp)
                }
            }
        }

        // 🔹 底部輸入框
        ChatInput(
            text = inputText.text,
            onTextChange = { inputText = TextFieldValue(it) },
            onSend = {
                if (inputText.text.isNotBlank()) {
                    val userMsg = inputText.text
                    viewModel.addMessage(ChatMessage("user", userMsg))  // ⬅ Firestore 同步

                    coroutineScope.launch {
                        val prompt = """
                            使用者輸入料理名稱：$userMsg
                            請輸出完整的「食材清單」與「料理步驟」，
                            務必分成兩個段落顯示，標題分別為【食材清單】與【步驟】。
                            禁止推薦購買冰箱或家電，僅能推薦料理。
                            請附上簡單貼心提醒（保存技巧、健康小建議）。
                        """.trimIndent()
                        viewModel.askAI(foodList.map { it.name }, customPrompt = prompt)
                    }
                    inputText = TextFieldValue("")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun DateHeader(date: java.time.LocalDate) {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("M月d日 (E)")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFEB6)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.format(formatter),
            color = Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
fun BotOptions(onSelectFridge: () -> Unit, onSelectCustom: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFD9DEE8))
            .padding(12.dp)
    ) {
        Text("👋嗨！要用哪種方式幫你找料理呢？", color = Color.DarkGray, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OptionButton("🍱 冰箱推薦", onClick = onSelectFridge)
        Spacer(modifier = Modifier.height(6.dp))
        OptionButton("🍜 今天想吃什麼料理", onClick = onSelectCustom)
    }
}

@Composable
fun BotRecipeMessage(
    recipeName: String,
    ingredients: List<String>,
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
        // 🔹 標題區塊
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFABB7CD))
                .padding(12.dp)
        ) {
            Text(
                "推薦給你「$recipeName」🍜",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            ingredients.forEachIndexed { index, name ->
                val hasIt = foodList.any { it.name.contains(name.take(2)) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name,
                        color = Color.DarkGray,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasIt) {
                        Text("✔", fontSize = 20.sp, color = Color(0xFF4CAF50))
                    } else {
                        Text(
                            "+",
                            fontSize = 22.sp,
                            color = Color.Black,
                            modifier = Modifier.clickable { onAddToCart(name) }
                        )
                    }
                }
                if (index != ingredients.lastIndex) {
                    Divider(
                        color = Color.Gray.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 小提醒
            Text(
                "💡 小提醒：缺少食材時，可以直接點「+」加入購物車哦！",
                color = Color(0xFF666666),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun BotStepMessage(steps: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3CD))
            .padding(12.dp)
    ) {
        Text("📖 料理步驟", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        steps.forEach { step ->
            Text(
                step,
                color = Color.DarkGray,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

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

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
            singleLine = true
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

@Composable
fun OptionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFABB7CD))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 15.sp)
    }
}
