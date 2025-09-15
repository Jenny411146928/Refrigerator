@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.FoodItem

// 🔹 聊天訊息資料類
data class ChatMessage(
    val role: String,
    val content: String,
    val type: String = "text" // text / options / recipe / steps
)

// 🔹 ChatPage 的 ViewModel（保存訊息，避免返回後被清空）
class ChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    var waitingForDish by mutableStateOf(false)

    fun addMessage(message: ChatMessage) {
        _messages.add(message)
    }

    fun clear() {
        _messages.clear()
    }
}

@Composable
fun ChatPage(
    foodList: List<FoodItem> = emptyList(),
    onAddToCart: (String) -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val messageList = viewModel.messages
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()

    // 依照日期分組
    val grouped = messageList.groupBy { java.time.LocalDate.now() } // ⚠️ 這裡先簡化成今天
    // 如果你之後想多天保存，可以改成 message.timestamp.toLocalDate()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            grouped.forEach { (date, messages) ->
                // 日期 header（sticky）
                stickyHeader {
                    DateHeader(date)
                }
                items(messages) { message ->
                    when (message.type) {
                        "options" -> BotOptions(
                            onSelectFridge = {
                                viewModel.addMessage(ChatMessage("user", "冰箱推薦"))
                                viewModel.addMessage(ChatMessage("bot", "我幫你準備「冰箱推薦」的推薦！"))
                            },
                            onSelectCustom = {
                                viewModel.addMessage(ChatMessage("user", "今天想吃什麼料理"))
                                viewModel.addMessage(ChatMessage("bot", "你今天想吃什麼料理呢？"))
                                viewModel.waitingForDish = true
                            }
                        )
                        "recipe" -> BotRecipeMessage(
                            recipeName = message.content,
                            ingredients = listOf("娃娃菜 一包", "五花豬肉片 一盒", "醬油 適量"),
                            foodList = foodList,
                            onAddToCart = onAddToCart
                        )
                        "steps" -> BotStepMessage(
                            steps = message.content.split("\n")
                        )
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

        ChatInput(
            text = inputText.text,
            onTextChange = { inputText = TextFieldValue(it) },
            onSend = {
                if (inputText.text.isNotBlank()) {
                    val userMsg = inputText.text
                    viewModel.addMessage(ChatMessage("user", userMsg))
                    coroutineScope.launch {
                        if (viewModel.waitingForDish) {
                            viewModel.addMessage(ChatMessage("bot", userMsg, type = "recipe"))
                            viewModel.addMessage(
                                ChatMessage(
                                    "bot",
                                    "燒一鍋水\n加入麵條\n放入蔬菜與肉片\n調味即可",
                                    type = "steps"
                                )
                            )
                            viewModel.waitingForDish = false
                        } else {
                            viewModel.addMessage(ChatMessage("bot", "⚠️ 系統連線失敗，請稍後再試"))
                        }
                    }
                    inputText = TextFieldValue("")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        if (messageList.isEmpty()) {
            viewModel.addMessage(ChatMessage("bot", "👋 嗨！今天想吃什麼料理呢？", type = "options"))
        }
    }
}

@Composable
fun BotOptions(onSelectFridge: () -> Unit, onSelectCustom: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFD9DEE8)) // 淺灰藍
            .padding(12.dp)
    ) {
        Text("👋 嗨！今天想吃什麼料理呢？", color = Color.DarkGray, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OptionButton("🍱 冰箱推薦", onClick = onSelectFridge)
        Spacer(modifier = Modifier.height(6.dp))
        OptionButton("🍜 今天想吃什麼料理", onClick = onSelectCustom)
    }
}

@Composable
fun BotRecipeMessage(
    recipeName: String,
    ingredients: List<String>, // 只放食材名稱
    foodList: List<FoodItem>,
    onAddToCart: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFD9DEE8))
            .padding(12.dp)
    ) {
        Text(
            "推薦給你「$recipeName」🍜",
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        ingredients.forEachIndexed { index, name ->
            val hasIt = foodList.any { it.name.contains(name.take(2)) } // ✅ 判斷冰箱是否有
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
    }
}

@Composable
fun BotStepMessage(steps: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3CD)) // 淺黃色背景
            .padding(12.dp)
    ) {
        Text(
            "📖 料理步驟",
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        steps.forEachIndexed { index, step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // 🔹 改成小圓點
                Box(
                    modifier = Modifier
                        .size(6.dp) // 黑點大小調小
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(step, color = Color.DarkGray, fontSize = 15.sp)
            }

            // 🔹 中間加分隔線（最後一行不加）
            if (index != steps.lastIndex) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("|", color = Color.Gray, fontSize = 14.sp)
                }
            }
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
fun ChatInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
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
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
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
fun DateHeader(date: java.time.LocalDate) {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("M月d日 (E)")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0)),
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
