@file:OptIn(ExperimentalMaterial3Api::class)

package tw.edu.pu.csim.refrigerator.ui

import ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import tw.edu.pu.csim.refrigerator.FoodItem

@Composable
fun FridgeChatPage(
    navController: NavController,
    viewModel: ChatViewModel,
    foodList: List<FoodItem>,
    fridgeList: List<FridgeCardData>,
    fridgeFoodMap: Map<String, List<FoodItem>>,
    onAddToCart: (String) -> Unit
) {
    val messages = viewModel.fridgeMessages
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 自動滾動到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        bottomBar = {
            ChatInput(
                text = input,
                onTextChange = { input = it },
                onSend = {
                    if (input.isNotBlank()) {
                        viewModel.addFridgeMessage(input, foodList)
                        input = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(messages) { msg ->
                when (msg.type) {
                    // 🧊 選冰箱區塊
                    "select_fridge" -> FridgeSelectionBlock(fridgeList) { fridge ->
                        viewModel.onFridgeSelected(fridge, fridgeFoodMap)
                    }

                    // 🍱 食譜卡
                    "recipe_cards" -> {
                        val recipes = decodeOrParseRecipeCards(msg.content)
                        RecipeCardsBlock(
                            title = "🍱 根據冰箱推薦料理",
                            recipes = recipes,
                            foodList = foodList,
                            onAddToCart = onAddToCart,
                            navController = navController    // ✅ 傳進去

                        )
                    }

                    // 🤔 載入中動畫
                    "loading" -> BotThinkingMessage()

                    // 💬 其他一般訊息
                    else -> {
                        if (msg.role == "user")
                            UserMessage(msg.content)
                        else
                            BotMessage(msg.content)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
