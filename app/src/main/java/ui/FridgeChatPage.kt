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

    val mainFridge = remember(fridgeList) {
        fridgeList.firstOrNull { it.editable }
    }

    val mainFridgeId = mainFridge?.id

    val mainFoodList = remember(mainFridgeId, fridgeFoodMap) {
        if (mainFridgeId != null) {
            fridgeFoodMap[mainFridgeId] ?: emptyList()
        } else emptyList()
    }

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

                        viewModel.addFridgeMessage(input, mainFoodList)

                        input = ""
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            items(messages) { msg ->
                when (msg.type) {


                    "recipe_cards" -> {
                        val recipes = decodeOrParseRecipeCards(msg.content)
                        RecipeCardsBlock(
                            title = "🍱 主冰箱推薦料理",
                            recipes = recipes,
                            foodList = mainFoodList,
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
    }
}