package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import tw.edu.pu.csim.refrigerator.openai.OpenAIClient
import android.util.Log
import ui.UiRecipe
import ui.decodeOrParseRecipeCards
import ui.encodeRecipeCards
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel : ViewModel() {

    val fridgeMessages = mutableStateListOf<ChatMessage>()
    val recipeMessages = mutableStateListOf<ChatMessage>()
    val allMessages = mutableStateListOf<ChatMessage>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    // ✅ 台灣時區日期
    private fun getTodayId(): String {
        val df = SimpleDateFormat("yyyyMMdd", Locale.TAIWAN)
        df.timeZone = TimeZone.getTimeZone("Asia/Taipei")
        return df.format(Date())
    }

    /** ✅ 儲存訊息到 Firestore（以日期分文件） */
    private fun saveMessageToFirestore(tab: String, message: ChatMessage) {
        val uid = auth.currentUser?.uid ?: return
        val today = getTodayId()

        val data = hashMapOf(
            "tab" to tab,
            "role" to message.role,
            "content" to message.content,
            "type" to message.type,
            "timestamp" to message.timestamp
        )

        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(today)
            .collection("messages")
            .add(data)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "✅ 已儲存訊息到 Firestore ($today/$tab)")
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "❌ 儲存訊息失敗: ${e.message}")
            }
    }

    /** ✅ 從 Firestore 載入指定日期的聊天紀錄 */
    fun loadMessagesFromFirestore(date: String = getTodayId()) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("chats")
            .document(date)
            .collection("messages")
            .get()
            .addOnSuccessListener { snapshot ->
                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ChatMessage::class.java)
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "❌ 解析 ChatMessage 失敗: ${e.message}")
                        null
                    }
                }.sortedBy { it.timestamp }

                fridgeMessages.clear()
                recipeMessages.clear()
                allMessages.clear() // ✅ 新增

                messages.forEach {
                    when (it.tab) {
                        "fridge" -> fridgeMessages.add(it)
                        "recipe" -> recipeMessages.add(it)
                        else -> {
                            // 沒有 tab 的舊資料，用原本的分類方式
                            if (it.type == "recipe_cards" || it.role == "bot")
                                recipeMessages.add(it)
                            else
                                fridgeMessages.add(it)
                        }
                    }
                }

// ✅ 新增：合併成「全部」分頁的資料
                allMessages.addAll(fridgeMessages + recipeMessages)
                allMessages.sortBy { it.timestamp }

                Log.d("ChatViewModel", "📦 已載入 ${messages.size} 筆紀錄 ($date)")


                Log.d("ChatViewModel", "📦 已載入 ${messages.size} 筆紀錄 ($date)")
            }
            .addOnFailureListener {
                Log.e("ChatViewModel", "❌ 無法載入聊天紀錄: ${it.message}")
            }
    }

    /** ✅ 快速載入「今天」紀錄（給 ChatPage 呼叫） */
    fun loadMessagesFromFirestoreToday() {
        loadMessagesFromFirestore(getTodayId())
    }

    /** 🧊 冰箱推薦訊息 */
    fun addFridgeMessage(userInput: String, foodList: List<FoodItem>) {
        val userMessage = ChatMessage(role = "user", content = userInput, type = "text")
        fridgeMessages.add(userMessage)
        fridgeMessages.add(ChatMessage(role = "bot", content = "loading", type = "loading"))

        OpenAIClient.askSmartBot(
            messages = fridgeMessages.filter { it.type != "loading" },
            foodList = foodList,
            mode = "fridge"
        ) { result ->
            fridgeMessages.removeIf { it.type == "loading" }

            if (result != null) {
                fridgeMessages.add(ChatMessage(role = "bot", content = result, type = "text"))
            } else {
                fridgeMessages.add(ChatMessage(role = "bot", content = "⚠️ 出現錯誤，請再試一次", type = "text"))
            }
        }
    }

    /** 🧊 選擇冰箱後觸發 */
    fun onFridgeSelected(fridge: FridgeCardData, fridgeFoodMap: Map<String, List<FoodItem>>) {
        val items = fridgeFoodMap[fridge.id]?.map { it.name } ?: emptyList()
        val botMsg = ChatMessage("bot", "✅ 選擇冰箱「${fridge.name}」，共有 ${items.size} 種食材")
        fridgeMessages.add(botMsg)
        saveMessageToFirestore("fridge", botMsg)
        fetchRecipesBasedOnFridge(items)
    }
    fun addBotMessage(content: String) {
        val msg = ChatMessage(
            role = "assistant",
            content = content,
            type = "text",
            timestamp = System.currentTimeMillis()
        )
        fridgeMessages.add(msg)
    }

    /** 🍳 今晚想吃什麼 */
    fun addRecipeMessage(userInput: String, foodList: List<FoodItem>) {
        val userMessage = ChatMessage(role = "user", content = userInput, type = "text")
        recipeMessages.add(userMessage)
        recipeMessages.add(ChatMessage(role = "bot", content = "loading", type = "loading"))

        OpenAIClient.askSmartBot(
            messages = recipeMessages.filter { it.type != "loading" },
            foodList = foodList,
            mode = "recipe"
        ) { result ->
            recipeMessages.removeIf { it.type == "loading" }

            if (result != null) {
                recipeMessages.add(ChatMessage(role = "bot", content = result, type = "text"))
            } else {
                recipeMessages.add(ChatMessage(role = "bot", content = "⚠️ 出現錯誤，請再試一次", type = "text"))
            }
        }
    }


    /** 🧩 測試訊息 */
    fun addGeneralMessage(text: String) {
        fridgeMessages.add(ChatMessage("user", text))
        recipeMessages.add(ChatMessage("bot", "這是測試回覆，未分類訊息。"))
    }

    /** 🤖 機器人訊息封裝（可指定要放在哪個分頁） */
    private fun addBotMessage(text: String, toFridge: Boolean) {
        val botMsg = ChatMessage("bot", text, tab = if (toFridge) "fridge" else "recipe")
        if (toFridge) {
            fridgeMessages.add(botMsg)
            saveMessageToFirestore("fridge", botMsg)
        } else {
            recipeMessages.add(botMsg)
            saveMessageToFirestore("recipe", botMsg)
        }
    }


    /** 🔍 根據冰箱推薦 */
    private fun fetchRecipesBasedOnFridge(
        ingredients: List<String>,
        keyword: String? = null,
        count: Int = 3
    ) {
        val thinking = ChatMessage("bot", "🤔 機器人正在思考你的冰箱能做什麼料理中... 🍳", "loading")
        fridgeMessages.add(thinking)

        db.collection("recipes")
            .get()
            .addOnSuccessListener { snapshot ->
                val scored = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val ings = (doc.get("ingredients") as? List<String>) ?: emptyList()
                    val steps = (doc.get("steps") as? List<String>) ?: emptyList()

                    val isKeywordMatch = keyword.isNullOrBlank() ||
                            title.contains(keyword!!, true) ||
                            ings.any { it.contains(keyword, true) }

                    val matchCount = ings.count { ing -> ingredients.any { f -> ing.contains(f, true) } }
                    val ratio = if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0
                    if (isKeywordMatch && ratio >= 0.4)
                        Pair(UiRecipe(title, ings.toMutableList(), steps.toMutableList()), ratio)
                    else null
                }.sortedByDescending { it.second }

                fridgeMessages.remove(thinking)

                val top = scored.map { it.first }.take(count)
                if (top.isEmpty()) {
                    val noResult = ChatMessage("bot", "😅 冰箱的食材可能稍微不足，我幫你湊幾道簡單料理試試～")
                    fridgeMessages.add(noResult)
                    saveMessageToFirestore("fridge", noResult)
                    val prompt = """
                        根據冰箱內的食材：${ingredients.joinToString("、")}，
                        即使不夠齊全，也請推薦 2 道簡單、台灣家常風格的料理，
                        並列出【名稱】【食材】【步驟】。
                    """.trimIndent()
                    askSmartAI(ingredients, prompt, 2, true)
                    return@addOnSuccessListener
                }

                val encoded = encodeRecipeCards(top)
                val botMsg = ChatMessage("bot", encoded, "recipe_cards")
                fridgeMessages.add(botMsg)
                saveMessageToFirestore("fridge", botMsg)
            }
            .addOnFailureListener {
                fridgeMessages.remove(thinking)
                val errMsg = ChatMessage("bot", "😢 無法取得食譜資料，請稍後再試。")
                fridgeMessages.add(errMsg)
                saveMessageToFirestore("fridge", errMsg)
            }
    }

    /** 🔍 根據關鍵字推薦 */
    private fun fetchRecipesBasedOnKeyword(keyword: String, foodList: List<FoodItem>) {
        val thinking = ChatMessage("bot", "🔍 幫你找找和「$keyword」有關的料理...", "loading")
        recipeMessages.add(thinking)
        val fridgeIngredients = foodList.map { it.name }

        db.collection("recipes")
            .get()
            .addOnSuccessListener { snapshot ->
                val scored = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val ings = (doc.get("ingredients") as? List<String>) ?: emptyList()
                    val steps = (doc.get("steps") as? List<String>) ?: emptyList()

                    val isKeywordMatch =
                        title.contains(keyword, true) || ings.any { it.contains(keyword, true) }
                    val matchCount =
                        ings.count { ing -> fridgeIngredients.any { f -> ing.contains(f, true) } }
                    val ratio =
                        if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0
                    if (isKeywordMatch || ratio >= 0.4)
                        Pair(UiRecipe(title, ings.toMutableList(), steps.toMutableList()), ratio)
                    else null
                }.sortedByDescending { it.second }

                recipeMessages.remove(thinking)

                val top = scored.map { it.first }.take(5)
                if (top.isEmpty()) {
                    val noResult = ChatMessage("bot", "😅 沒找到很準的結果，我幫你生幾道接近「$keyword」的家常料理～")
                    recipeMessages.add(noResult)
                    saveMessageToFirestore("recipe", noResult)
                    val prompt = """
                        使用者想吃「$keyword」。請推薦 3 道符合台灣人口味的料理，
                        每道包含【名稱】【食材】【步驟】，步驟務必分行清楚。
                    """.trimIndent()
                    askSmartAI(fridgeIngredients, prompt, 3, false)
                } else {
                    val encoded = encodeRecipeCards(top)
                    val botMsg = ChatMessage("bot", encoded, "recipe_cards")
                    recipeMessages.add(botMsg)
                    saveMessageToFirestore("recipe", botMsg)
                }
            }
            .addOnFailureListener {
                recipeMessages.remove(thinking)
                val errMsg = ChatMessage("bot", "😢 無法取得食譜資料，請稍後再試。")
                recipeMessages.add(errMsg)
                saveMessageToFirestore("recipe", errMsg)
            }
    }

    /** 🤖 GPT 智慧補齊推薦 */
    private fun askSmartAI(foodList: List<String>, prompt: String, expectedCount: Int, toFridgeTab: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val target = expectedCount.coerceIn(1, 5)
            var tries = 0
            var combined = ""
            var recipes: List<UiRecipe> = emptyList()

            while ((recipes.size < target || recipes.any { it.ingredients.isEmpty() || it.steps.isEmpty() }) && tries < 3) {
                val reply = kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
                    OpenAIClient.askChatGPT(listOf(ChatMessage("system", prompt))) { r ->
                        cont.resume(r, onCancellation = null)
                    }
                } ?: ""
                combined += "\n$reply"
                val parsed = decodeOrParseRecipeCards(combined)
                val unique = LinkedHashMap<String, UiRecipe>()
                parsed.forEach { r ->
                    val key = r.name.trim().lowercase()
                    if (!unique.containsKey(key)) unique[key] = r
                }
                recipes = unique.values.toList()
                tries++
                if (recipes.size < target) delay(600)
            }

            recipes = recipes.map {
                val ings = if (it.ingredients.isEmpty()) mutableListOf("（AI 未提供內容）") else it.ingredients
                val steps = if (it.steps.isEmpty()) mutableListOf("（AI 未提供步驟）") else it.steps
                it.copy(ingredients = ings, steps = steps)
            }.take(5)

            if (recipes.isNotEmpty()) {
                val encoded = encodeRecipeCards(recipes)
                val botMsg = ChatMessage("bot", encoded, "recipe_cards")
                if (toFridgeTab) {
                    fridgeMessages.add(botMsg)
                    saveMessageToFirestore("fridge", botMsg)
                } else {
                    recipeMessages.add(botMsg)
                    saveMessageToFirestore("recipe", botMsg)
                }
            }
        }
    }
}
