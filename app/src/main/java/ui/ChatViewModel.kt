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
import tw.edu.pu.csim.refrigerator.openai.AIIntentResult
import android.util.Log
import ui.UiRecipe
import ui.decodeOrParseRecipeCards
import ui.encodeRecipeCards
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson

class ChatViewModel : ViewModel() {

    val fridgeMessages = mutableStateListOf<ChatMessage>()
    val recipeMessages = mutableStateListOf<ChatMessage>()
    val allMessages = mutableStateListOf<ChatMessage>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val gson = Gson()

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

                // ✅ 【這裡新增】切換日期時先清空所有訊息，避免疊加
                fridgeMessages.clear()
                recipeMessages.clear()
                allMessages.clear()

                // ✅ 照原本邏輯加入訊息（保持你目前設計）
                messages.forEach { msg ->
                    when (msg.tab) {
                        "fridge" -> {
                            if (fridgeMessages.none { it.timestamp == msg.timestamp && it.content == msg.content }) {
                                fridgeMessages.add(msg)
                            }
                        }

                        "recipe" -> {
                            if (recipeMessages.none { it.timestamp == msg.timestamp && it.content == msg.content }) {
                                recipeMessages.add(msg)
                            }
                        }

                        else -> {
                            if (msg.type == "recipe_cards" || msg.role == "bot") {
                                if (recipeMessages.none { it.timestamp == msg.timestamp && it.content == msg.content }) {
                                    recipeMessages.add(msg)
                                }
                            } else {
                                if (fridgeMessages.none { it.timestamp == msg.timestamp && it.content == msg.content }) {
                                    fridgeMessages.add(msg)
                                }
                            }
                        }
                    }
                }

                // ✅ 更新 allMessages，但不會造成重複
                allMessages.addAll((fridgeMessages + recipeMessages).sortedBy { it.timestamp })

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

    // ----------------------------------------------------------------
    // 🆕 智慧入口：你可以在 UI 直接呼叫這個，或沿用舊函式
    // tab = "fridge" | "recipe"（對應你的兩個分頁）
    // ----------------------------------------------------------------
    fun handleUserInput(tab: String, userInput: String, foodList: List<FoodItem>) {
        val msg = ChatMessage(role = "user", content = userInput, type = "text")
        saveMessageToFirestore(tab, msg)

        if (tab == "fridge") fridgeMessages.add(msg) else recipeMessages.add(msg)

        val loading = ChatMessage(role = "bot", content = "loading", type = "loading")
        if (tab == "fridge") fridgeMessages.add(loading) else recipeMessages.add(loading)

        OpenAIClient.analyzeUserIntent(userInput) { intentResult ->
            // 移除 loading
            if (tab == "fridge") fridgeMessages.removeIf { it.type == "loading" }
            else recipeMessages.removeIf { it.type == "loading" }

            if (intentResult == null) {
                val err = ChatMessage("bot", "😵‍💫 我沒聽懂，可以再描述一次想吃什麼嗎？", "text")
                if (tab == "fridge") fridgeMessages.add(err) else recipeMessages.add(err)
                saveMessageToFirestore(tab, err)
                return@analyzeUserIntent
            }

            when (intentResult.intent) {
                "chat" -> {
                    val r = ChatMessage("bot", intentResult.reply ?: "我只懂料理喔～🍳", "text")
                    if (tab == "fridge") fridgeMessages.add(r) else recipeMessages.add(r)
                    saveMessageToFirestore(tab, r)
                }

                "ask" -> {
                    val r = ChatMessage(
                        "bot",
                        intentResult.reply ?: "想吃台式、日式還是西式呢？要不要無辣？",
                        "text"
                    )
                    if (tab == "fridge") fridgeMessages.add(r) else recipeMessages.add(r)
                    saveMessageToFirestore(tab, r)
                }

                else -> {
                    // find_recipe / 未知但預設當找食譜
                    fetchRecipesByIntent(tab, intentResult, foodList)
                }
            }
        }
    }

    /** 🧊 冰箱推薦訊息（保留既有 API；內部改呼叫 handleUserInput） */
    fun addFridgeMessage(userInput: String, foodList: List<FoodItem>) {
        handleUserInput(tab = "fridge", userInput = userInput, foodList = foodList)
    }

    /** 🍳 今晚想吃什麼（保留既有 API；內部改呼叫 handleUserInput） */
    fun addRecipeMessage(userInput: String, foodList: List<FoodItem>) {
        handleUserInput(tab = "recipe", userInput = userInput, foodList = foodList)
    }

    /** 🧊 選擇冰箱後觸發（保留不動） */
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

    /** 🧩 測試訊息（保留不動） */
    fun addGeneralMessage(text: String) {
        fridgeMessages.add(ChatMessage("user", text))
        recipeMessages.add(ChatMessage("bot", "這是測試回覆，未分類訊息。"))
    }

    /** 🤖 機器人訊息封裝（保留不動） */
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

    // ----------------------------------------------------------------
    // 🔍 既有兩個 DB 搜尋（保留不動，作為備用）
    // ----------------------------------------------------------------
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
                    val imageUrl = doc.getString("imageUrl")
                    val time = doc.getString("time")
                    val yieldStr = doc.getString("yield")

                    val isKeywordMatch = keyword.isNullOrBlank() ||
                            title.contains(keyword!!, true) ||
                            ings.any { it.contains(keyword, true) }

                    val matchCount =
                        ings.count { ing -> ingredients.any { f -> ing.contains(f, true) } }
                    val ratio = if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0
                    if (isKeywordMatch && ratio >= 0.4)
                        Triple(
                            UiRecipe(
                                title,
                                ings.toMutableList(),
                                steps.toMutableList(),
                                imageUrl,
                                yieldStr,
                                time
                            ), ratio, doc.id
                        )
                    else null
                }.sortedByDescending { it.second }

                fridgeMessages.remove(thinking)

                val top = scored.map { it.first }.take(count)
                if (top.isEmpty()) {
                    val noResult =
                        ChatMessage("bot", "😅 冰箱的食材可能稍微不足，我幫你湊幾道簡單料理試試～")
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

                // ✅ 用 JSON 回傳，保住 imageUrl/time/yield
                val jsonList = top.map {
                    mapOf(
                        "title" to it.name,
                        "ingredients" to it.ingredients,
                        "steps" to it.steps,
                        "imageUrl" to it.imageUrl,
                        "yield" to it.servings,
                        "time" to it.totalTime
                    )
                }
                val contentJson = gson.toJson(jsonList)

                val botMsg = ChatMessage("bot", contentJson, "recipe_cards")
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
                    val imageUrl = doc.getString("imageUrl")
                    val time = doc.getString("time")
                    val yieldStr = doc.getString("yield")

                    val isKeywordMatch =
                        title.contains(keyword, true) || ings.any { it.contains(keyword, true) }
                    val matchCount =
                        ings.count { ing -> fridgeIngredients.any { f -> ing.contains(f, true) } }
                    val ratio = if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0
                    if (isKeywordMatch || ratio >= 0.4)
                        Triple(
                            UiRecipe(
                                title,
                                ings.toMutableList(),
                                steps.toMutableList(),
                                imageUrl,
                                yieldStr,
                                time
                            ), ratio, doc.id
                        )
                    else null
                }.sortedByDescending { it.second }

                recipeMessages.remove(thinking)

                val top = scored.map { it.first }.take(5)
                if (top.isEmpty()) {
                    val noResult = ChatMessage(
                        "bot",
                        "😅 沒找到很準的結果，我幫你生幾道接近「$keyword」的家常料理～"
                    )
                    recipeMessages.add(noResult)
                    saveMessageToFirestore("recipe", noResult)
                    val prompt = """
                        使用者想吃「$keyword」。請推薦 3 道符合台灣人口味的料理，
                        每道包含【名稱】【食材】【步驟】，步驟務必分行清楚。
                    """.trimIndent()
                    askSmartAI(fridgeIngredients, prompt, 3, false)
                } else {
                    // ✅ 用 JSON 回傳，保住 imageUrl/time/yield
                    val jsonList = top.map {
                        mapOf(
                            "title" to it.name,
                            "ingredients" to it.ingredients,
                            "steps" to it.steps,
                            "imageUrl" to it.imageUrl,
                            "yield" to it.servings,
                            "time" to it.totalTime
                        )
                    }
                    val contentJson = gson.toJson(jsonList)

                    val botMsg = ChatMessage("bot", contentJson, "recipe_cards")
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

    /** 🤖 GPT 智慧補齊推薦（保留不動；但正常情況不再用它產生食譜） */
    private fun askSmartAI(
        foodList: List<String>,
        prompt: String,
        expectedCount: Int,
        toFridgeTab: Boolean
    ) {
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
                val ings =
                    if (it.ingredients.isEmpty()) mutableListOf("（AI 未提供內容）") else it.ingredients
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

    // ----------------------------------------------------------------
    // 🆕 依 AIIntentResult 從資料庫「篩選 + 打分 + 以卡片回覆」
    // ----------------------------------------------------------------
    private fun fetchRecipesByIntent(tab: String, ir: AIIntentResult, foodList: List<FoodItem>) {
        val loading = ChatMessage("bot", "🍳 幫你找符合的料理...", "loading")
        if (tab == "fridge") fridgeMessages.add(loading) else recipeMessages.add(loading)

        db.collection("recipes")
            .get()
            .addOnSuccessListener { snapshot ->
                if (tab == "fridge") fridgeMessages.removeIf { it.type == "loading" }
                else recipeMessages.removeIf { it.type == "loading" }

                val include = ir.include.map { it.trim() }.filter { it.isNotBlank() }
                val avoid = ir.avoid.map { it.trim() }.filter { it.isNotBlank() }
                val cuisine = ir.cuisine?.trim().orEmpty()
                val style = ir.style?.trim().orEmpty()
                val wantMild = ir.spiciness == "mild"
                val wantSpicy = ir.spiciness == "spicy"

                val spicyKeywords =
                    listOf("辣", "辣椒", "麻辣", "花椒", "剁椒", "韓式辣醬", "泡菜", "香辣")
                val oilyKeywords = listOf(
                    "炸",
                    "酥炸",
                    "油炸",
                    "酥脆",
                    "奶油",
                    "鮮奶油",
                    "砂糖",
                    "糖",
                    "培根",
                    "起司"
                )
                val lightKeywords =
                    listOf("蒸", "汆燙", "水煮", "涼拌", "清炒", "清燉", "蔬菜", "雞胸")

                fun containsAny(hay: String, keys: List<String>) =
                    keys.any { k -> k.isNotBlank() && hay.contains(k, ignoreCase = true) }

                fun listContainsAny(list: List<String>, keys: List<String>) =
                    list.any { s -> containsAny(s, keys) }

                fun listContainsAll(list: List<String>, keys: List<String>) =
                    keys.all { k -> list.any { s -> s.contains(k, ignoreCase = true) } }

                val cuisineHints = mapOf(
                    "西式" to listOf("義大利", "奶油", "焗烤", "披薩", "番茄醬", "沙拉", "義式"),
                    "台式" to listOf("三杯", "滷", "魯", "炒", "蔥爆", "蚵仔", "油蔥", "米粉"),
                    "日式" to listOf("味噌", "壽司", "生魚片", "親子丼", "拉麵", "柴魚", "日式"),
                    "韓式" to listOf("泡菜", "韓式", "年糕", "辣醬", "石鍋拌飯"),
                    "中式" to listOf("麻婆", "宮保", "川", "蒸魚", "清蒸", "紅燒", "拌"),
                    "美式" to listOf("漢堡", "牛排", "薯條", "BBQ", "三明治")
                )

                val fridgeNames = foodList.map { it.name }

                val results = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val ings = (doc.get("ingredients") as? List<String>) ?: emptyList()
                    val steps = (doc.get("steps") as? List<String>) ?: emptyList()
                    val imageUrl = doc.getString("imageUrl")
                    val time = doc.getString("time")
                    val yieldStr = doc.getString("yield")

                    val blob = (listOf(title) + ings + steps).joinToString("\n")

                    // 🚫 避免條件
                    if (avoid.isNotEmpty() && (containsAny(title, avoid) || listContainsAny(
                            ings,
                            avoid
                        ) || listContainsAny(steps, avoid))
                    ) {
                        return@mapNotNull null
                    }
                    if (wantMild && (containsAny(title, spicyKeywords) || listContainsAny(
                            ings,
                            spicyKeywords
                        ))
                    ) {
                        return@mapNotNull null
                    }

                    // ✅ 冰箱推薦專屬：覆蓋率檢查
                    if (tab == "fridge" && fridgeNames.isNotEmpty()) {
                        val matchCount =
                            ings.count { ing -> fridgeNames.any { f -> ing.contains(f, true) } }
                        val ratio =
                            if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0
                        if (ratio < 0.5) return@mapNotNull null // ❌ 不推薦覆蓋率低於50%
                    }

                    // 打分數
                    var score = 0.0
                    include.forEach { k -> if (containsAny(blob, listOf(k))) score += 2.0 }
                    if (wantSpicy && (containsAny(title, spicyKeywords) || listContainsAny(
                            ings,
                            spicyKeywords
                        ))
                    ) score += 1.5
                    if (wantMild && !(containsAny(title, spicyKeywords) || listContainsAny(
                            ings,
                            spicyKeywords
                        ))
                    ) score += 1.0
                    if (style in listOf("健康", "減脂", "低卡")) {
                        if (containsAny(blob, lightKeywords)) score += 1.2
                        if (!containsAny(blob, oilyKeywords)) score += 1.0
                    }
                    if (cuisine.isNotBlank()) {
                        val hints = cuisineHints[cuisine]
                        if (hints != null && containsAny(blob, hints)) score += 1.0
                    }

                    // 食材重合度加權（兩種模式都保留）
                    if (fridgeNames.isNotEmpty()) {
                        val matchCount =
                            ings.count { ing -> fridgeNames.any { f -> ing.contains(f, true) } }
                        val ratio =
                            if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0
                        score += ratio
                    }

                    Triple(
                        UiRecipe(
                            title,
                            ings.toMutableList(),
                            steps.toMutableList(),
                            imageUrl,
                            yieldStr,
                            time,
                            doc.id
                        ), score, doc.id
                    )
                }.sortedByDescending { it.second }

                // 之後保持原樣輸出 JSON 卡片（不刪減）
                val top = results.take(5).map { it.first }

                if (top.isEmpty()) {
                    val noResult =
                        ChatMessage("bot", "😅 冰箱的食材好像不太夠，我幫你找幾道接近的料理～", "text")
                    if (tab == "fridge") fridgeMessages.add(noResult) else recipeMessages.add(
                        noResult
                    )
                    saveMessageToFirestore(tab, noResult)
                    return@addOnSuccessListener
                }

                val jsonList = top.map {
                    mapOf(
                        "title" to it.name,
                        "ingredients" to it.ingredients,
                        "steps" to it.steps,
                        "imageUrl" to it.imageUrl,
                        "yield" to it.servings,
                        "time" to it.totalTime
                    )
                }
                val contentJson = gson.toJson(jsonList)
                val botMsg = ChatMessage("bot", contentJson, "recipe_cards")
                if (tab == "fridge") fridgeMessages.add(botMsg) else recipeMessages.add(botMsg)
                saveMessageToFirestore(tab, botMsg)
            }
            .addOnFailureListener { e ->
                if (tab == "fridge") fridgeMessages.removeIf { it.type == "loading" }
                else recipeMessages.removeIf { it.type == "loading" }
                val err = ChatMessage("bot", "😢 無法讀取食譜資料，請稍後再試（${e.message}）", "text")
                if (tab == "fridge") fridgeMessages.add(err) else recipeMessages.add(err)
                saveMessageToFirestore(tab, err)
            }
    }
}