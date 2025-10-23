package tw.edu.pu.csim.refrigerator.ui

import ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import tw.edu.pu.csim.refrigerator.FoodItem

@Composable
fun RecipeChatPage(
    navController: NavController,
    viewModel: ChatViewModel,
    foodList: List<FoodItem>,
    onAddToCart: (String) -> Unit
) {
    val messages = viewModel.recipeMessages
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
                        viewModel.addRecipeMessage(input, foodList)
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
                            title = "🍳 今晚想吃這些料理",
                            recipes = recipes,
                            foodList = foodList,
                            onAddToCart = onAddToCart,
                            navController = navController    // ✅ 傳進去

                        )
                    }
                    "loading" -> BotThinkingMessage()
                    else -> {
                        if (msg.role == "user") UserMessage(msg.content) else BotMessage(msg.content)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
