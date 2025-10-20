@file:OptIn(ExperimentalMaterial3Api::class)

package tw.edu.pu.csim.refrigerator.ui

import ui.BotMessage
import ui.BotThinkingMessage
import ui.RecipeCardsBlock
import ui.UserMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import ui.decodeOrParseRecipeCards
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatPage(
    navController: NavController,
    viewModel: ChatViewModel,
    foodList: List<FoodItem>,
    fridgeList: List<FridgeCardData>,
    fridgeFoodMap: Map<String, List<FoodItem>>,
    onAddToCart: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("📋 全部") }
    val tabs = listOf("📋 全部", "🍱 冰箱推薦", "🍳 今天想吃什麼料理")
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope() // ✅ 新增：滾動用 CoroutineScope

    // ✅ 台灣時區日期
    val df = remember { SimpleDateFormat("MM/dd (E)", Locale.TAIWAN) }
    df.timeZone = TimeZone.getTimeZone("Asia/Taipei")
    val todayLabel = df.format(Date())

    // ✅ 最近七天日期列表
    val dateList = remember {
        (0..6).map {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -it)
            val date = cal.time
            val id = SimpleDateFormat("yyyyMMdd", Locale.TAIWAN).format(date)
            val label = df.format(date)
            id to label
        }
    }

    var selectedDate by remember { mutableStateOf(todayLabel) }
    var expanded by remember { mutableStateOf(false) }

    val mergedMessages by remember(viewModel.fridgeMessages, viewModel.recipeMessages) {
        derivedStateOf {
            (viewModel.fridgeMessages + viewModel.recipeMessages).sortedBy { it.timestamp }
        }
    }

    // ✅ 自動滾到底部（啟動時 or 新訊息出現時）
    LaunchedEffect(viewModel.fridgeMessages, viewModel.recipeMessages) {
        delay(100) // 稍等載入完成再滾動
        coroutineScope.launch {
            val total = listState.layoutInfo.totalItemsCount
            if (total > 0) {
                listState.animateScrollToItem(total - 1)
            }
        }
    }

    // ✅ 回來時重新載入當天紀錄
    var reloadTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger) {
            viewModel.loadMessagesFromFirestoreToday()
            reloadTrigger = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA))
    ) {

        // ======== 上方分頁 ========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Color(0xFFFFFEB6) else Color(0xFFE3E6ED))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // ======== 🟨 日期區塊（改版） ========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF7C5))
                .clickable { expanded = !expanded }
                .height(28.dp)
                .padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = " $selectedDate",
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "展開日期選單",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color.White)
                    .width(180.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                dateList.forEach { (id, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (label == todayLabel) " 今天 ($label)" else label,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        },
                        onClick = {
                            selectedDate = label
                            expanded = false
                            viewModel.loadMessagesFromFirestore(id)
                        }
                    )
                }
            }
        }

        // ======== 各分頁內容 ========
        when (selectedTab) {
            "🍱 冰箱推薦" -> SimpleChatLayout(
                listState = listState,
                messages = viewModel.fridgeMessages,
                foodList = foodList,
                onAddToCart = onAddToCart
            ) { input ->
                viewModel.addFridgeMessage(input, foodList)
            }

            "🍳 今天想吃什麼料理" -> SimpleChatLayout(
                listState = listState,
                messages = viewModel.recipeMessages,
                foodList = foodList,
                onAddToCart = onAddToCart
            ) { input ->
                viewModel.addRecipeMessage(input, foodList)
            }

            else -> AllChatLayout(
                listState = listState,
                mergedMessages = viewModel.allMessages, // ✅ 改這裡
                foodList = foodList,
                onAddToCart = onAddToCart,
                viewModel = viewModel
            )
        }



    }
}

// ========================== 🍱「冰箱推薦」與「今天想吃什麼料理」共用輸入列 ==========================
@Composable
fun SimpleChatLayout(
    listState: androidx.compose.foundation.lazy.LazyListState,
    messages: List<ChatMessage>,
    foodList: List<FoodItem>,
    onAddToCart: (String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            ChatInputBar(
                text = text,
                onTextChange = { text = it },
                onSendClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                showModeSwitch = false
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(messages) { msg ->
                when (msg.type) {
                    "recipe_cards" -> {
                        val recipes = decodeOrParseRecipeCards(msg.content)
                        RecipeCardsBlock(
                            title = "🍽 推薦料理",
                            recipes = recipes,
                            foodList = foodList,
                            onAddToCart = onAddToCart
                        )
                    }
                    "loading" -> BotThinkingMessage()
                    else -> {
                        if (msg.role == "user") UserMessage(msg.content)
                        else BotMessage(msg.content)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ========================== 📋「全部」頁：含模式切換 ==========================
@Composable
fun AllChatLayout(
    listState: androidx.compose.foundation.lazy.LazyListState,
    mergedMessages: List<ChatMessage>,
    foodList: List<FoodItem>,
    onAddToCart: (String) -> Unit,
    viewModel: ChatViewModel
) {
    var text by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf("冰箱推薦") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            ChatInputBar(
                text = text,
                onTextChange = { text = it },
                onSendClick = {
                    if (text.isNotBlank()) {
                        when (selectedTarget) {
                            "冰箱推薦" -> viewModel.addFridgeMessage(text, foodList)
                            "今天想吃什麼料理" -> viewModel.addRecipeMessage(text, foodList)
                        }
                        text = ""
                    }
                },
                showModeSwitch = true,
                selectedTarget = selectedTarget,
                onModeSelect = { selectedTarget = it },
                expanded = expanded,
                onExpandedChange = { expanded = it }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(mergedMessages) { msg ->
                when (msg.type) {
                    "recipe_cards" -> {
                        val recipes = decodeOrParseRecipeCards(msg.content)
                        RecipeCardsBlock(
                            title = "🍽 推薦料理",
                            recipes = recipes,
                            foodList = foodList,
                            onAddToCart = onAddToCart
                        )
                    }
                    "loading" -> BotThinkingMessage()
                    else -> {
                        if (msg.role == "user") UserMessage(msg.content)
                        else BotMessage(msg.content)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ========================== 💬 輸入欄 ==========================
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    showModeSwitch: Boolean,
    selectedTarget: String = "冰箱推薦",
    onModeSelect: (String) -> Unit = {},
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F6FA))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showModeSwitch) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color(0xFFABB7CD))
                        .clickable { onExpandedChange(!expanded) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedTarget) {
                            "冰箱推薦" -> "🍱"
                            "今天想吃什麼料理" -> "🍳"
                            else -> "✨"
                        },
                        fontSize = 22.sp
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandedChange(false) },
                        modifier = Modifier
                            .background(Color.White)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        listOf("冰箱推薦", "今天想吃什麼料理").forEach { opt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt,
                                        color = if (selectedTarget == opt)
                                            Color(0xFFABB7CD) else Color.Black
                                    )
                                },
                                onClick = {
                                    onModeSelect(opt)
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Transparent),
                color = Color(0xFFE3E6ED),
                shadowElevation = 0.dp
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "輸入訊息…",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF626D85)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFFABB7CD))
                    .clickable { onSendClick() },
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}
