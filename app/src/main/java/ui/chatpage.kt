@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import ui.BotMessage
import ui.BotThinkingMessage
import ui.RecipeCardsBlock
import ui.UserMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import com.example.myapplication.calculateDaysRemainingSafely
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import tw.edu.pu.csim.refrigerator.R
data class ModeOption(
    val id: String,
    val label: String,
    val icon: Int
)

val modeOptions = listOf(
    ModeOption(
        id = "fridge",
        label = "幫你清冰箱!",
        icon = R.drawable.icon_clean_fridge
    ),
    ModeOption(
        id = "recipe",
        label = "今天想吃...",
        icon = R.drawable.icon_fried_egg
    )
)
fun calculateDaysRemaining(date: String?, dayLeft: String?): Int {
    if (date.isNullOrBlank() || dayLeft.isNullOrBlank()) return 0

    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
        val added = sdf.parse(date)
        val now = Date()

        val diffDays = ((now.time - added.time) / (1000 * 60 * 60 * 24)).toInt()

        val validDays = dayLeft.split(" ").first().toInt()

        val remaining = validDays - diffDays
        if (remaining < 0) 0 else remaining

    } catch (e: Exception) {
        0
    }
}
fun calculateDaysRemainingUnified(dateString: String, fallbackDaysRemaining: Int): Int {
    if (dateString.isBlank()) return fallbackDaysRemaining

    val today = Calendar.getInstance().time
    val patterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "yyyy/M/d")

    var expireDate: Date? = null
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.isLenient = false
            expireDate = sdf.parse(dateString)
            if (expireDate != null) break
        } catch (_: Exception) {}
    }

    if (expireDate == null) return fallbackDaysRemaining

    val diff = expireDate.time - today.time
    return (diff / (1000 * 60 * 60 * 24)).toInt()
}

fun calculateDaysRemainingFromOldData(dateStr: String?): Int {
    if (dateStr.isNullOrBlank()) return 0

    return try {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
        val added = sdf.parse(dateStr)
        val now = Date()

        val diff = (now.time - added.time) / (1000 * 60 * 60 * 24)


        val validDays = 180

        val remaining = validDays - diff.toInt()
        if (remaining < 0) 0 else remaining
    } catch (e: Exception) {
        0
    }
}

fun fixDaysRemaining(food: FoodItem): Int {
    return calculateDaysRemainingSafely(food.date, food.daysRemaining)
}

@Composable
fun ChatPage(
    navController: NavController,
    viewModel: ChatViewModel,
    foodList: List<FoodItem>,
    fridgeList: List<FridgeCardData>,
    fridgeFoodMap: Map<String, List<FoodItem>>,
    onAddToCart: (String) -> Unit,
) {

    val firestore = FirebaseFirestore.getInstance()
    var chatFoodList by remember { mutableStateOf<List<FoodItem>>(emptyList()) }

    LaunchedEffect(fridgeList) {

        val mainFridge = fridgeList.firstOrNull { it.editable } ?: return@LaunchedEffect

        firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection("fridge")
            .document(mainFridge.id)
            .collection("Ingredient")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { it.toObject(FoodItem::class.java) }
                chatFoodList = list.map { food ->
                    food.copy(
                        daysRemaining = food.daysRemaining
                    )

                }
            }
    }
    data class ChatTab(val id: String, val label: String, val icon: Int?)

    var selectedTab by remember { mutableStateOf("📋 全部") }
    val tabs = listOf("📋 全部", "🍱 幫你清冰箱!", "🍳 今天想吃...")
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    val df = remember { SimpleDateFormat("MM/dd (E)", Locale.TAIWAN) }
    df.timeZone = TimeZone.getTimeZone("Asia/Taipei")
    val todayLabel = df.format(Date())


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


    val mainFridge = remember(fridgeList) {
        fridgeList.firstOrNull { it.editable }
    }


    val mainFridgeId = mainFridge?.id

    val mainFoodList = remember(mainFridgeId, fridgeFoodMap) {
        if (mainFridgeId != null) {
            (fridgeFoodMap[mainFridgeId] ?: emptyList()).map { food ->
                food.copy(
                    daysRemaining = food.daysRemaining
                )
            }
        } else emptyList()
    }


    var selectedDate by remember { mutableStateOf(todayLabel) }
    var expanded by remember { mutableStateOf(false) }

    val mergedMessages by remember(viewModel.fridgeMessages, viewModel.recipeMessages) {
        derivedStateOf {
            (viewModel.fridgeMessages + viewModel.recipeMessages).sortedBy { it.timestamp }
        }
    }

    LaunchedEffect(viewModel.fridgeMessages, viewModel.recipeMessages) {
        delay(100)
        coroutineScope.launch {
            val total = listState.layoutInfo.totalItemsCount
            if (total > 0) {
                listState.scrollToItem(total - 1)
            }
        }
    }





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
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F6FA))
                .padding(vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                tabs.forEach { tab ->
                    val selected = tab == selectedTab

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                if (selected) Color(0xFFFFF7C5)
                                else Color(0xFFE3E6ED)
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            when (tab) {

                                "🍱 幫你清冰箱!" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.icon_clean_fridge),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "幫你清冰箱!",
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                "🍳 今天想吃..." -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.icon_fried_egg),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "今天想吃...",
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                else -> Text(
                                    tab,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

            }
        }



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


        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                "🍱 幫你清冰箱!" -> SimpleChatLayout(
                    listState = listState,
                    messages = viewModel.fridgeMessages,
                    foodList = mainFoodList,
                    displayFoodList = mainFoodList,
                    onAddToCart = onAddToCart,
                    onSendMessage = { input ->
                        viewModel.addFridgeMessage(input, mainFoodList)
                    },
                    navController = navController
                )

                "🍳 今天想吃..." -> SimpleChatLayout(
                    listState = listState,
                    messages = viewModel.recipeMessages,
                    foodList = foodList,
                    displayFoodList = foodList,
                    onAddToCart = onAddToCart,
                    onSendMessage = { input ->
                        viewModel.addRecipeMessage(input, foodList)
                    },
                    navController = navController
                )

                else -> AllChatLayout(
                    listState = listState,
                    mergedMessages = mergedMessages,
                    foodList = foodList,
                    mainFoodList = mainFoodList,
                    onAddToCart = onAddToCart,
                    viewModel = viewModel,
                    navController = navController,
                    fridgeFoodList = chatFoodList

                )
            }
        }
    }
}


@Composable
fun SimpleChatLayout(
    listState: androidx.compose.foundation.lazy.LazyListState,
    messages: List<ChatMessage>,
    foodList: List<FoodItem>,
    displayFoodList: List<FoodItem>,
    onAddToCart: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    navController: NavController
) {
    var fridgeExpanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()


    val showScrollToBottom by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
            lastVisibleIndex < (listState.layoutInfo.totalItemsCount - 2)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F6FA),
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
                showModeSwitch = false,
                selectedTarget = "幫你清冰箱!",
                fridgeExpanded = fridgeExpanded,
                onFridgeExpandedChange = { fridgeExpanded = it },
                foodList = foodList
            )
        }
    ) { innerPadding ->
        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F6FA)),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 8.dp
                )
            )
            {
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
                                foodList = displayFoodList,
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
            LaunchedEffect(messages.size) {
                delay(50)
                if (listState.layoutInfo.totalItemsCount > 0) {
                    listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
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


@Composable
fun AllChatLayout(
    listState: androidx.compose.foundation.lazy.LazyListState,
    mergedMessages: List<ChatMessage>,
    foodList: List<FoodItem>,
    mainFoodList: List<FoodItem>,
    onAddToCart: (String) -> Unit,
    viewModel: ChatViewModel,
    navController: NavController,
    fridgeFoodList: List<FoodItem>

) {
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!initialized && mainFoodList.isNotEmpty()) {
            initialized = true
            viewModel.updateWelcomeRecipesIfNeeded(mainFoodList)
        }
    }


    var fridgeExpanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf(modeOptions[0].id) }
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
                            "fridge" -> viewModel.addFridgeMessage(text, fridgeFoodList)
                            "recipe" -> viewModel.addRecipeMessage(text, foodList)
                        }

                        text = ""
                    }
                }
                ,
                showModeSwitch = true,
                selectedTarget = selectedTarget,
                onModeSelect = { selectedTarget = it },


                expanded = expanded,
                onExpandedChange = { expanded = it },
                fridgeExpanded = fridgeExpanded,
                onFridgeExpandedChange = { fridgeExpanded = it },
                foodList = fridgeFoodList

            )
        }

    ) { innerPadding ->
        Box {


                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F6FA)),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + 8.dp
                    )
                ) {
                    item {


                        BotMessage(
                            "以下是依據你冰箱食材推薦的料理，\n如需查詢其他料理，可輸入新食材名稱。"
                        )

                        Spacer(Modifier.height(6.dp))


                        if (viewModel.welcomeRecipes.isNotEmpty()) {
                            RecipeCardsBlock(
                                title = "🍽 推薦料理",
                                recipes = viewModel.welcomeRecipes,
                                foodList = fridgeFoodList,
                                onAddToCart = onAddToCart,
                                navController = navController
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

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
                                    foodList = foodList,
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
                LaunchedEffect(mergedMessages.size) {
                    delay(50)
                    if (listState.layoutInfo.totalItemsCount > 0) {
                        listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
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

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    showModeSwitch: Boolean,
    selectedTarget: String,
    onModeSelect: (String) -> Unit = {},
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    fridgeExpanded: Boolean = false,
    onFridgeExpandedChange: (Boolean) -> Unit = {},
    foodList: List<FoodItem> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F6FA))
    ) {

        AnimatedVisibility(
            visible = fridgeExpanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
                    .background(Color.White)
                    .padding(12.dp)
            )
            {
                val modeOptions = listOf(
                    ModeOption(
                        id = "fridge",
                        label = "幫你清冰箱!",
                        icon = R.drawable.icon_clean_fridge
                    ),
                    ModeOption(
                        id = "recipe",
                        label = "今天想吃...",
                        icon = R.drawable.icon_fried_egg
                    )
                )


                val categories = listOf(
                    "全部",
                    "肉類",
                    "海鮮",
                    "蔬菜",
                    "水果",
                    "蛋類",
                    "豆類",
                    "乳製品",
                    "調味料"
                )
                var selectedCategory by remember { mutableStateOf("全部") }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(40.dp))
                                .background(
                                    if (cat == selectedCategory)
                                        Color(0xFFB7C3D0)
                                    else
                                        Color(0xFFE5E8EF)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (cat == selectedCategory) Color.White else Color.DarkGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }


                Spacer(Modifier.height(12.dp))


                val filtered = foodList
                    .filter { item ->
                        val days = fixDaysRemaining(item)
                        when (selectedCategory) {
                            "全部" -> true
                            else -> item.category == selectedCategory
                        }
                    }

                    .sortedBy { fixDaysRemaining(it) }
                var selectedFoodName by remember { mutableStateOf<String?>(null) }
                var lastClickTime by remember { mutableStateOf(0L) }
                filtered.forEach { food ->

                    val isSelected = selectedFoodName == food.name

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFFD6E2FF)
                                else Color.Transparent
                            )
                            .clickable {
                                val now = System.currentTimeMillis()


                                if (now - lastClickTime < 250) {

                                    onTextChange(food.name)
                                    onSendClick()


                                    onFridgeExpandedChange(false)

                                    selectedFoodName = null
                                } else {

                                    selectedFoodName = food.name
                                }

                                lastClickTime = now
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                    ) {

                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                food.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                "剩 ${food.quantity} 個",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4A4A4A)
                            )
                        }

                        val remain = food.daysRemaining

                        Text(
                            "剩餘：${fixDaysRemaining(food)} 天",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
                        )


                        Divider(
                            color = Color(0xFFE0E0E0),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }



        Box(
            modifier = Modifier
                .fillMaxWidth()
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

                        when (selectedTarget) {
                            "fridge" -> {
                                Image(
                                    painter = painterResource(id = R.drawable.icon_clean_fridge),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            "recipe" -> {
                                Image(
                                    painter = painterResource(id = R.drawable.icon_fried_egg),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { onExpandedChange(false) },
                            modifier = Modifier
                                .background(Color.White)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            modeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = option.icon),
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(option.label)
                                        }
                                    },
                                    onClick = {
                                        onModeSelect(option.id)
                                        onExpandedChange(false)
                                    }

                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color(0xFFABB7CD))
                        .clickable { onFridgeExpandedChange(!fridgeExpanded) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_fridge_items),
                        contentDescription = "冰箱食材",
                        modifier = Modifier.size(28.dp)
                    )

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
}