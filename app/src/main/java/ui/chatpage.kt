// ChatPage.kt
package tw.edu.pu.csim.refrigerator.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import tw.edu.pu.csim.refrigerator.openai.OpenAIClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(foodList: List<FoodItem>) {
    var selectedStyle by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var selectedTaste by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("") }

    var selectedServing by remember { mutableStateOf("") }
    var conditionsSubmitted by remember { mutableStateOf(false) }

    val messageList = remember { mutableStateListOf<ChatMessage>() }
    var isBotTyping by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF6F7))
            .padding(12.dp)
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messageList) { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
                ) {
                    if (message.role == "user") {
                        UserMessage(message.content)
                    } else {
                        BotMessage(message.content)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (isBotTyping) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = "https://img.icons8.com/color/48/robot.png",
                            contentDescription = "bot",
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterVertically)
                                .padding(end = 6.dp)
                        )
                        Text("FoodieBot 正在思考", color = Color.Gray)
                        DotLoadingAnimation()
                    }
                }
            }
        }

        if (!conditionsSubmitted) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "👋 嗨！今天想吃什麼料理呢？先幫我選幾個條件吧～",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444)
                )
                Spacer(Modifier.height(8.dp))

                Text("料理風格：")
                OptionRow(options = listOf("台式", "日式", "泰式", "美式", "韓式")
                    , selected = selectedStyle) {
                    selectedStyle = it
                }

                Text("烹調方式：")
                OptionRow(options = listOf("炒", "煮", "炸", "蒸", "烤"), selected = selectedMethod) {
                    selectedMethod = it
                }

                Text("食物類型：")
                OptionRow(options = listOf("正餐", "甜點", "湯品", "小菜"), selected = selectedType) {
                    selectedType = it
                }

                Text("烹調難易度：")
                OptionRow(options = listOf("簡單", "中等", "挑戰"), selected = selectedDifficulty) {
                    selectedDifficulty = it
                }


                Text("幾人份：")
                OptionRow(options = listOf("1人", "2人", "家庭"), selected = selectedServing) {
                    selectedServing = it
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val foodNames = foodList.mapNotNull { it.name?.takeIf { it.isNotBlank() } }.joinToString("、")
                        if (foodNames.isBlank()) {
                            messageList.add(ChatMessage("assistant", "找不到冰箱裡的食材喔～請先新增一些！"))
                            return@Button
                        }
                        val prompt = "我冰箱裡有這些食材：$foodNames，我想吃${selectedStyle}風格、${selectedMethod}方式的${selectedType}，難易度${selectedDifficulty}，份量約${selectedServing}，請推薦一個料理並說明做法。"

                        messageList.add(ChatMessage("user", prompt))
                        conditionsSubmitted = true
                        isBotTyping = true
                        OpenAIClient.askChatGPT(messageList) { reply ->
                            isBotTyping = false
                            if (reply != null) messageList.add(ChatMessage("assistant", reply))
                            else messageList.add(ChatMessage("assistant", "⚠️ 發生錯誤，請稍後再試。"))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFABB7CD),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("送出條件並推薦料理")
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun UserMessage(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .background(Color(0xFFD9D9D9), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(text, color = Color.Black)
        }
    }
}

@Composable
fun BotMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://img.icons8.com/color/48/robot.png",
            contentDescription = "bot",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically)
                .padding(end = 6.dp)
        )
        Box(
            modifier = Modifier
                .background(Color(0xFF898989), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(text, color = Color.White)
        }
    }
}

@Composable
fun DotLoadingAnimation() {
    val dotCount = 3
    val delayTime = 300
    val animatedDots = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(delayTime.toLong())
            animatedDots.value = (animatedDots.value + 1) % (dotCount + 1)
        }
    }
    Text(text = ".".repeat(animatedDots.value), color = Color.Gray)
}

@Composable
fun OptionRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = option,
                        color = if (option == selected) Color.Black else Color(0xFF444444)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFABB7CD),
                    selectedLabelColor = Color.Black,
                    containerColor = Color.Transparent,
                    labelColor = Color(0xFF444444)
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
