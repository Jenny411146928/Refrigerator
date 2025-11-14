@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.KeyboardArrowUp

@Composable
fun ChatPage(
    navController: NavController,
    viewModel: ChatViewModel,
    foodList: List<FoodItem>,
    fridgeList: List<FridgeCardData>,
    fridgeFoodMap: Map<String, List<FoodItem>>,
    onAddToCart: (String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf("📋 全部") }
    val tabs = listOf("📋 全部", "🍱 幫你清冰箱!", "🍳 今天想吃...")
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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

    // ✅ 主冰箱（editable = true）
    val mainFridge = remember(fridgeList) {
        fridgeList.firstOrNull { it.editable }
    }

    // ✅ 主冰箱 ID
    val mainFridgeId = mainFridge?.id

    // ✅ 主冰箱的食材
    val mainFoodList = remember(mainFridgeId, fridgeFoodMap) {
        if (mainFridgeId != null) {
            fridgeFoodMap[mainFridgeId] ?: emptyList()
        } else emptyList()
    }

    var selectedDate by remember { mutableStateOf(todayLabel) }
    var expanded by remember { mutableStateOf(false) }

    val mergedMessages by remember(viewModel.fridgeMessages, viewModel.recipeMessages) {
        derivedStateOf {
            (viewModel.fridgeMessages + viewModel.recipeMessages).sortedBy { it.timestamp }
        }
    }

    // ✅ 自動滾到底部
    LaunchedEffect(viewModel.fridgeMessages, viewModel.recipeMessages) {
        delay(100)
        coroutineScope.launch {
            val total = listState.layoutInfo.totalItemsCount
            if (total > 0) {
                listState.animateScrollToItem(total - 1)
            }
        }
    }

    // ✅ 若無任何訊息，預設顯示一則開場訊息
    LaunchedEffect(Unit) {
        if (viewModel.fridgeMessages.isEmpty() && viewModel.recipeMessages.isEmpty()) {
            viewModel.addBotMessage(
                "輸入食材名稱（例如：雞肉、豆腐），\n我會推薦幾道適合的料理給你喔～🍳"
            )
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

        // ======== 🟨 日期區塊（保持固定高度） ========
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7C5))
                    .clickable { expanded = !expanded }
                    .height(28.dp)
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

            // ✅ 修復白色縫隙
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF5F6FA))
            )
        }

        // ======== 各分頁內容 ========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                "🍱 幫你清冰箱!" -> SimpleChatLayout(
                    listState = listState,
                    messages = viewModel.fridgeMessages,
                    foodList = mainFoodList,          // ← 顯示/標示用也用主冰箱
                    displayFoodList = mainFoodList,   // ← 供卡片比對
                    onAddToCart = onAddToCart,
                    onSendMessage = { input ->
                        viewModel.addFridgeMessage(input, mainFoodList) // ← 主冰箱清單傳進 VM
                    },
                    navController = navController
                )

                "🍳 今天想吃..." -> SimpleChatLayout(
                    listState = listState,
                    messages = viewModel.recipeMessages,
                    foodList = foodList,              // ← 顯示時可用整體清單
                    displayFoodList = foodList,       // 或想維持主冰箱也可改為 mainFoodList
                    onAddToCart = onAddToCart,
                    onSendMessage = { input ->
                        viewModel.addRecipeMessage(input, foodList)     // ← recipe 模式不限制主冰箱
                    },
                    navController = navController
                )

                else -> AllChatLayout(
                    listState = listState,
                    mergedMessages = mergedMessages,
                    foodList = foodList,
                    mainFoodList = mainFoodList,      // ← 傳入讓冰箱模式用主冰箱
                    onAddToCart = onAddToCart,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

// ========================== 🍱/🍳 共用輸入列 + 列表 ==========================
@Composable
fun SimpleChatLayout(
    listState: androidx.compose.foundation.lazy.LazyListState,
    messages: List<ChatMessage>,
    foodList: List<FoodItem>,
    displayFoodList: List<FoodItem>, // ✅ 這個取代原先自由變數 mainFoodList
    onAddToCart: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    navController: NavController
) {
    var text by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // ✅ 控制滾動到底部按鈕顯示
    val showScrollToBottom by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
            lastVisibleIndex < (listState.layoutInfo.totalItemsCount - 2)
        }
    }

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
        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(
                    items = messages,
                    key = { msg -> msg.hashCode().toString() + "_" + msg.timestamp.toString() }
                ) { msg ->
                    when (msg.type) {
                        "recipe_cards" -> {
                            val recipes = decodeOrParseRecipeCards(msg.content)
                            RecipeCardsBlock(
                                title = "🍽 推薦料理",
                                recipes = recipes,
                                foodList = displayFoodList,   // ✅ 用參數，不再用未定義變數
                                onAddToCart = onAddToCart,
                                navController = navController
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

            // ✅ 浮動滾到底部按鈕
            if (showScrollToBottom) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                        }
                    },
                    containerColor = Color(0xFFABB7CD),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 90.dp)
                        .size(46.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "回到底部")
                }
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
    mainFoodList: List<FoodItem>, // ✅ 新增：給冰箱模式用
    onAddToCart: (String) -> Unit,
    viewModel: ChatViewModel,
    navController: NavController
) {
    var text by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf("🍱 幫你清冰箱!") }
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val showScrollToBottom by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
            lastVisibleIndex < (listState.layoutInfo.totalItemsCount - 2)
        }
    }

    Scaffold(
        bottomBar = {
            ChatInputBar(
                text = text,
                onTextChange = { text = it },
                onSendClick = {
                    if (text.isNotBlank()) {
                        when (selectedTarget) {
                            "🍱 幫你清冰箱!" -> viewModel.addFridgeMessage(text, mainFoodList)
                            "🍳 今天想吃..." -> viewModel.addRecipeMessage(text, foodList)
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
        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(
                    items = mergedMessages,
                    key = { msg -> msg.hashCode().toString() + "_" + msg.timestamp.toString() }
                ) { msg ->
                    when (msg.type) {
                        "recipe_cards" -> {
                            val recipes = decodeOrParseRecipeCards(msg.content)
                            RecipeCardsBlock(
                                title = "🍽 推薦料理",
                                recipes = recipes,
                                foodList = foodList, // 這裡顯示全部清單；若要統一主冰箱可改 mainFoodList
                                onAddToCart = onAddToCart,
                                navController = navController
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

            if (showScrollToBottom) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                        }
                    },
                    containerColor = Color(0xFFABB7CD),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 90.dp)
                        .size(46.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "回到底部")
                }
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
                            "🍱 幫你清冰箱!" -> "🍱"
                            "🍳 今天想吃..." -> "🍳"
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
                        listOf("🍱 幫你清冰箱!", "🍳 今天想吃...")
                            .forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt,
                                            color = if (selectedTarget == opt)
                                                Color.Black else Color(0xFFB0B0B0)
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
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFFE3E6ED)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    if (text.isEmpty()) {
                        Text("輸入訊息…", color = Color.Gray, fontSize = 16.sp)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
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
