package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val visible: MutableState<Boolean> = mutableStateOf(false)
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatPage() {
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    val messageList = remember { mutableStateListOf<ChatMessage>() }
    var isBotTyping by remember { mutableStateOf(false) }
    var botTriggerIndex by remember { mutableStateOf(0) }
    var pendingBotMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var latestUserMessage by remember { mutableStateOf<ChatMessage?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF6F7))
    ) {

        // Chat list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            for (message in messageList) {
                AnimatedVisibility(
                    visible = message.visible.value,
                    enter = slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { it / 2 }
                    ) + fadeIn(animationSpec = tween(300))
                ) {
                    if (message.isUser) {
                        UserMessage(message.text)
                    } else {
                        BotMessage(message.text)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (isBotTyping) {
                AnimatedVisibility(visible = true) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/3431dac7-e175-4f33-aa70-421156db3789",
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

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("輸入你想吃的食材") },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color(0xFFE0E0E0),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val input = messageText.text.trim()
                    if (input.isNotEmpty()) {
                        val userMessage = ChatMessage(true, input)
                        val botMessage = ChatMessage(false, "推薦你「番茄炒蛋」！")

                        messageList.add(userMessage)
                        messageText = TextFieldValue("")
                        isBotTyping = true
                        pendingBotMessage = botMessage
                        latestUserMessage = userMessage
                        botTriggerIndex++
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFBDBDBD), shape = RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
            }
        }

        // 動畫顯示用戶訊息
        LaunchedEffect(latestUserMessage) {
            latestUserMessage?.let {
                delay(50)
                it.visible.value = true
            }
        }

        // 延遲 bot 回覆
        LaunchedEffect(botTriggerIndex) {
            if (botTriggerIndex > 0 && pendingBotMessage != null) {
                delay(1000L)
                isBotTyping = false
                messageList.add(pendingBotMessage!!)
                delay(50)
                pendingBotMessage!!.visible.value = true
                pendingBotMessage = null
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
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/3431dac7-e175-4f33-aa70-421156db3789",
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

    Text(
        text = ".".repeat(animatedDots.value),
        color = Color.Gray
    )
}
