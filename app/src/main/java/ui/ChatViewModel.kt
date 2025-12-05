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
import com.google.firebase.firestore.DocumentSnapshot
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

    private fun getTodayId(): String {
        val df = SimpleDateFormat("yyyyMMdd", Locale.TAIWAN)
        df.timeZone = TimeZone.getTimeZone("Asia/Taipei")
        return df.format(Date())
    }

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


//清理食材清單
    private val EXCLUDED_INGS = setOf(
        "鹽", "胡椒", "黑胡椒", "白胡椒", "胡椒粉", "黑胡椒粉", "白胡椒粉",
        "水", "糖", "砂糖", "白砂糖",
        "醬油", "蠔油",
        "油", "食用油", "橄欖油", "植物油",
        "味精", "味素"
    )
    private val ESSENTIAL_INGS = setOf("蔥", "青蔥", "大蔥", "薑", "老薑", "蒜", "大蒜")

    private fun isCondiment(name: String): Boolean {
        val n = name.trim()
        if (ESSENTIAL_INGS.any { n.contains(it, ignoreCase = true) }) return false
        return EXCLUDED_INGS.any { n.equals(it, true) || n.contains(it, true) }
    }
    private fun getSafeString(doc: DocumentSnapshot, field: String): String {
        return try {
            when (val v = doc.get(field)) {
                is String -> v
                is List<*> -> v.joinToString(",") { it.toString() }   // 陣列 → 字串
                is Map<*, *> -> v.values.joinToString(",") { it.toString() }  // map → 字串
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    private fun cleanedIngredients(doc: DocumentSnapshot): List<String> {

        val rawList =
            (doc.get("ingredientsList") as? List<*>)   // List<Any?>
                ?: (doc.get("ingredients") as? List<*>)
                ?: emptyList<Any>()

        return rawList.mapNotNull { item ->

            // ① 若 item 是 String → 直接用
            if (item is String) {
                val clean = item.trim()
                if (clean.isNotBlank()) clean else null
            }

            // ② 若 item 是 Map（例： { "name": "空心菜" } ）→ 抓 name 欄位
            else if (item is Map<*, *>) {
                val name = item["name"] as? String ?: item["text"] as? String
                val clean = name?.trim()
                if (!clean.isNullOrBlank()) clean else null
            }

            // ③ 其它型態（null 或奇怪資料） → 忽略
            else null

        }
            // 去除調味料
            .filter { ing ->
                ing.isNotBlank() && !isCondiment(ing)
            }
            .distinct()
    }
//這段目前就是最大混亂來源
private fun detectUserQueryType(ir: AIIntentResult): String {

    val userText = (ir.include + listOfNotNull(ir.cuisine, ir.style))
        .joinToString(" ")
        .lowercase()

    // -------------------------
    // ① 食材模式（幫你清冰箱）
    // -------------------------
    val includes = ir.include.map { it.trim() }.filter { it.isNotBlank() }

    val dessertKeywords = listOf(
        "甜點", "甜品", "甜食", "點心", "下午茶", "蛋糕", "布丁",
        "餅乾", "塔", "派", "冰淇淋", "甜湯", "甜湯圓", "紅豆湯",
        "抹茶甜點", "可麗露", "馬卡龍", "鬆餅", "可麗餅", "甜甜圈"
    )

    // ❗ 避免把「甜點」當成食材（會造成找不到食譜）
    val isDessertWordOnly = includes.all { kw ->
        dessertKeywords.any { d -> d.contains(kw) || kw.contains(d) }
    }

    val isRealIngredient = includes.isNotEmpty() &&
            !isDessertWordOnly &&      // ⭐ 關鍵：排除甜點詞彙
            includes.all { kw ->
                kw.length <= 4 &&
                        !kw.contains("料理") &&
                        !kw.contains("風味") &&
                        !kw.contains("式")
            }

    if (isRealIngredient) return "ingredient"


    // -------------------------
    // ② 各式料理風格（台式 日式 西式…）
    // -------------------------
    val cuisine = ir.cuisine?.trim().orEmpty()
    if (cuisine.isNotBlank() && !cuisine.equals("null", true)) {
        return "cuisine"
    }

    // -------------------------
    // ③ 辣度（mild / spicy）
    // -------------------------
    if (ir.spiciness == "mild" || ir.spiciness == "spicy") {
        return "spice"
    }

    // -------------------------
    // ④ 健康 / 低卡 / 家常等風格
    // -------------------------
    val style = ir.style?.trim().orEmpty()
    if (style.isNotBlank() && !style.equals("null", true)) {
        return "style"
    }

    // -------------------------
    // ⑤ ⭐ 甜點 / 點心模式（核心區）
    // -------------------------
    if (dessertKeywords.any { kw -> userText.contains(kw) }) {
        return "dessert"
    }

    // -------------------------
    // ⑥ 其他模式
    // -------------------------
    return "other"
}



    private fun toMainCategory(name: String): String = when {
        listOf("豬", "五花", "梅花", "絞肉").any { name.contains(it, true) } -> "豬肉"
        listOf("牛", "肋", "肩", "里肌").any { name.contains(it, true) } -> "牛肉"
        listOf("雞", "土雞", "雞胸", "雞腿", "雞翅").any { name.contains(it, true) } -> "雞肉"
        listOf("蝦", "魚", "蟹", "蛤", "貝", "魷", "花枝", "章魚", "海鮮").any {
            name.contains(
                it,
                true
            )
        } -> "海鮮"

        listOf("豆腐", "豆皮", "豆干").any { name.contains(it, true) } -> "豆腐"
        listOf("蛋").any { name.contains(it, true) } -> "蛋"
        else -> {
            // 其它一律當作蔬菜，之後可細分
            "蔬菜"
        }
    }
    private fun ingredientMatchesQuery(ingredient: String, query: String): Boolean {
        val ing = ingredient.trim()
        val kw = query.trim()

        // 特別處理「蛋」相關：排除豆腐類
        if (kw.contains("蛋") && !kw.contains("豆腐")) {
            // 只要這個食材名字裡有「豆腐」，就不要當成雞蛋主角
            if (ing.contains("豆腐")) return false
            // 例如：雞蛋、蛋液、雞蛋液、炒蛋
            return ing.contains("蛋")
        }

        // 特別處理「梅花肉」：一定要真的有「梅花」字樣
        if (kw.contains("梅花")) {
            return ing.contains("梅花")
        }

        // 大分類：豬肉
        if (kw == "豬肉") {
            return listOf("豬肉", "梅花肉", "五花肉", "豬絞肉", "里肌", "豬排")
                .any { key -> ing.contains(key, ignoreCase = true) }
        }

        // 大分類：牛肉
        if (kw == "牛肉") {
            return listOf("牛肉", "牛絞肉", "牛排", "牛腩", "牛里肌")
                .any { key -> ing.contains(key, ignoreCase = true) }
        }

        // 大分類：雞肉
        if (kw == "雞肉") {
            return listOf("雞肉", "雞腿", "雞胸", "雞翅", "雞里肌", "土雞")
                .any { key -> ing.contains(key, ignoreCase = true) }
        }

        // 大分類：羊肉
        if (kw == "羊肉") {
            return listOf("羊肉", "羊排", "羊小排")
                .any { key -> ing.contains(key, ignoreCase = true) }
        }

        // 大分類：海鮮
        if (kw == "海鮮") {
            return listOf("蝦", "魚", "蟹", "蛤", "貝", "魷魚", "花枝", "章魚", "透抽")
                .any { key -> ing.contains(key, ignoreCase = true) }
        }

        // 一般情況：用「包含」判斷（例如：香菇、青江菜、豆芽菜）
        return ing.contains(kw, ignoreCase = true)
    }

    // 從冰箱食材列表萃取「主要類別統計」（主食材優先）
    private fun fridgeMainBuckets(foodList: List<FoodItem>): Map<String, Int> {
        return foodList.map { it.name }
            .map { toMainCategory(it) }
            .groupingBy { it }
            .eachCount()
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

                // ✅ 切換日期時清空所有訊息，避免疊加
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

                // ✅ 更新 allMessages，但防止重複的食譜卡（以內容 content 去重）
                allMessages.clear()
                allMessages.addAll(
                    (fridgeMessages + recipeMessages)
                        .distinctBy { msg ->
                            if (msg.type == "recipe_cards") msg.content
                            else "${msg.timestamp}_${msg.content}"
                        }
                        .sortedBy { it.timestamp }
                )

                Log.d(
                    "ChatViewModel",
                    "📦 已載入 ${messages.size} 筆紀錄 ($date)，去重後：${allMessages.size}"
                )
            }
            .addOnFailureListener {
                Log.e("ChatViewModel", "❌ 無法載入聊天紀錄: ${it.message}")
            }
    }
    fun loadMessagesFromFirestoreToday() {
        loadMessagesFromFirestore(getTodayId())
    }




    private fun getMainFridgeFood(
        fridgeList: List<FridgeCardData>,
        fridgeFoodMap: Map<String, List<FoodItem>>
    ): List<FoodItem> {
        val mainFridge = fridgeList.firstOrNull { it.editable } ?: return emptyList()
        return fridgeFoodMap[mainFridge.id] ?: emptyList()
    }


    //整個 Chat 的大腦
    fun handleUserInput(tab: String, userInput: String, foodList: List<FoodItem>) {
        Log.w("DEBUG", "🧊 冰箱食材清單 = ${foodList.joinToString { it.name }}")

        val msg = ChatMessage(role = "user", content = userInput, type = "text")
        saveMessageToFirestore(tab, msg)

        if (tab == "fridge") fridgeMessages.add(msg) else recipeMessages.add(msg)

        val loading = ChatMessage(role = "bot", content = "loading", type = "loading")
        if (tab == "fridge") fridgeMessages.add(loading) else recipeMessages.add(loading)

        OpenAIClient.analyzeUserIntent(userInput) { intentResult ->
            Log.e("DEBUG_INTENT", "GPT 回傳 intentResult = $intentResult")
            if (tab == "fridge") fridgeMessages.removeIf { it.type == "loading" }
            else recipeMessages.removeIf { it.type == "loading" }

            if (intentResult == null) {
                val err = ChatMessage("bot", "😵‍💫 我沒聽懂，可以再描述一次想吃什麼嗎？", "text")
                if (tab == "fridge") fridgeMessages.add(err) else recipeMessages.add(err)
                saveMessageToFirestore(tab, err)
                return@analyzeUserIntent
            }

            var fixedIntent = intentResult
            if (fixedIntent.cuisine != null && fixedIntent.cuisine.equals("null", ignoreCase = true)) {
                fixedIntent = fixedIntent.copy(cuisine = "")
            }

            val isIngredientOnly =
                ingredientKeywords.any { kw -> userInput.contains(kw, ignoreCase = true) }

            if (isIngredientOnly) {
                fetchRecipesByIntent(
                    tab,
                    fixedIntent.copy(intent = "find_recipe"),
                    foodList,
                    userInput          // ⭐ 新增這個
                )
                return@analyzeUserIntent
            }

            if (userInput.length <= 4 && userInput.count { it.isLetterOrDigit() } <= 4) {
                fetchRecipesByIntent(
                    tab,
                    fixedIntent.copy(intent = "find_recipe"),
                    foodList,
                    userInput          // ⭐ 新增這個
                )
                return@analyzeUserIntent
            }


            // ✅ 改這裡用 fixedIntent
            when (fixedIntent.intent) {
                "chat" -> {
                    val r = ChatMessage("bot", fixedIntent.reply ?: "我只懂料理喔～🍳", "text")
                    if (tab == "fridge") fridgeMessages.add(r) else recipeMessages.add(r)
                    saveMessageToFirestore(tab, r)
                }

                "ask" -> {
                    val r = ChatMessage(
                        "bot",
                        fixedIntent.reply ?: "想吃台式、日式還是西式呢？要不要無辣？",
                        "text"
                    )
                    if (tab == "fridge") fridgeMessages.add(r) else recipeMessages.add(r)
                    saveMessageToFirestore(tab, r)
                }

                else -> {
                    val includeMissing = intentResult.include.filter { kw ->
                        val kwClean = kw.replace("一顆", "")
                            .replace("一些", "")
                            .replace("少許", "")
                            .replace("大", "")
                            .replace("小", "")
                            .replace("的", "")
                            .trim()

                        foodList.none { f ->
                            val nameClean = f.name
                                .replace("一顆", "")
                                .replace("一些", "")
                                .replace("少許", "")
                                .replace("大", "")
                                .replace("小", "")
                                .replace("的", "")
                                .trim()
                            nameClean.contains(kwClean, ignoreCase = true) ||
                                    kwClean.contains(nameClean, ignoreCase = true)
                        }
                    }

                    if (tab == "fridge" && includeMissing.isNotEmpty()) {

                        val warn = ChatMessage(
                            "bot",
                            "😅 你的冰箱裡沒有：${includeMissing.joinToString("、")}。\n以下是我依照冰箱現有食材「可以組合出來」的料理給你參考～",
                            "text"
                        )
                        fridgeMessages.add(warn)
                        saveMessageToFirestore("fridge", warn)

                        // ⭐ 強制觸發推薦卡片
                        fetchRecipesByIntent(
                            tab,
                            fixedIntent.copy(intent = "find_recipe", include = emptyList()),
                            foodList,
                            userInput      // ⭐ 新增這個
                        )

                        //return@analyzeUserIntent
                    }

// 這一行也要加 userInput
                    fetchRecipesByIntent(tab, fixedIntent, foodList, userInput)

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
    /** ✅ 將 ISO 8601 時間（PT15M / PT1H30M）轉換成可讀格式 */
    private fun formatRecipeDuration(raw: String?): String {
        if (raw.isNullOrBlank()) return "未提供"

        val regex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?""")
        val match = regex.find(raw) ?: return raw

        val hours = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
        val minutes = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0

        return when {
            hours > 0 && minutes > 0 -> "${hours}小時${minutes}分鐘"
            hours > 0 -> "${hours}小時"
            minutes > 0 -> "${minutes}分鐘"
            else -> "未提供"
        }
    }
    fun calcDaysLeft(expireDate: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)
            val exp = sdf.parse(expireDate) ?: return 999
            val now = Date()

            val diff = exp.time - now.time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            999
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

        getRecentRecipeHistory(7) { used ->
            db.collection("recipes")
                .get()
                .addOnSuccessListener { snapshot ->


                    val scored = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        if (id in used) return@mapNotNull null

                        val title = doc.getString("title") ?: return@mapNotNull null

                        // ✅ 改用乾淨 ingredients（同 fetchRecipesByIntent）
                        val ings = cleanedIngredients(doc)

                        val steps = (doc.get("steps") as? List<String>) ?: emptyList()
                        val imageUrl = doc.getString("imageUrl")
                        val rawTime = doc.getString("time")
                        val time = formatRecipeDuration(rawTime)
                        val yieldStr = when (val y = doc.get("yield")) {
                            is String -> y
                            is Number -> y.toString()
                            null -> null
                            else -> y.toString()
                        }

                        // ✅ 保留你原本的 keyword matching
                        val isKeywordMatch = keyword.isNullOrBlank()
                                || title.contains(keyword!!, true)
                                || ings.any { it.contains(keyword, true) }

                        // ✅ 將 ingredients 對比乾淨後的 ings
                        val matchCount = ings.count { ing ->
                            ingredients.any { f -> ing.contains(f, ignoreCase = true) }
                        }

                        val ratio =
                            if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0

                        if (isKeywordMatch && ratio >= 0.4)
                            Triple(
                                UiRecipe(
                                    title,
                                    ings.toMutableList(),     // ✅ 回傳乾淨 ingredients
                                    steps.toMutableList(),
                                    imageUrl,
                                    yieldStr,
                                    time
                                ),
                                ratio,
                                doc.id
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
    }

    private fun fetchRecipesBasedOnKeyword(keyword: String, foodList: List<FoodItem>) {
        val thinking = ChatMessage("bot", "🔍 幫你找找和「$keyword」有關的料理...", "loading")
        recipeMessages.add(thinking)

        val fridgeIngredients = foodList.map { it.name }

        getRecentRecipeHistory(7) { used ->
            db.collection("recipes")
                .get()
                .addOnSuccessListener { snapshot ->


                    val scored = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        if (id in used) return@mapNotNull null

                        val title = doc.getString("title") ?: return@mapNotNull null

                        // ✅ 使用乾淨 ingredients
                        val ings = cleanedIngredients(doc)

                        val steps = (doc.get("steps") as? List<String>) ?: emptyList()
                        val imageUrl = doc.getString("imageUrl")
                        val rawTime = doc.getString("time")
                        val time = formatRecipeDuration(rawTime)
                        val yieldStr = when (val y = doc.get("yield")) {
                            is String -> y
                            is Number -> y.toString()
                            null -> null
                            else -> y.toString()
                        }

                        val isKeywordMatch =
                            title.contains(keyword, true)
                                    || ings.any { it.contains(keyword, true) }

                        val matchCount = ings.count { ing ->
                            fridgeIngredients.any { f -> ing.contains(f, ignoreCase = true) }
                        }

                        val ratio =
                            if (ings.isNotEmpty()) matchCount.toDouble() / ings.size else 0.0

                        if (isKeywordMatch || ratio >= 0.4)
                            Triple(
                                UiRecipe(
                                    title,
                                    ings.toMutableList(),     // ✅ 乾淨 ingredients
                                    steps.toMutableList(),
                                    imageUrl,
                                    yieldStr,
                                    time
                                ),
                                ratio,
                                doc.id
                            )
                        else null

                    }.sortedByDescending { it.second }

                    recipeMessages.remove(thinking)

                    val top = scored.map { it.first }.take(5)

                    if (top.isEmpty()) {
                        val noResult =
                            ChatMessage(
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
                val jsonPrompt = """
                    你現在是一個標準化的料理資料輸出助手。
                    
                    ⚠️ 請務必只回傳「JSON 陣列格式」，不能出現任何解說文字、前言、後綴、數字編號、句子。
                    
                    格式必須完全如下（欄位必須齊全）：
                    
                    [
                      {
                        "title": "料理名稱",
                        "ingredients": ["食材1", "食材2"],
                        "steps": ["步驟1", "步驟2"],
                        "imageUrl": "",
                        "yield": "",
                        "time": ""
                      }
                    ]
                    
                    ❗ 不可以在 JSON 陣列外輸出任何文字  
                    ❗ 不可以輸出自然語言句子  
                    ❗ 不可以加編號（例如 1. 2. 3. ）  
                    ❗ 不可以解釋說明  
                    
                    以下是使用者的要求：
                    $prompt
                    """.trimIndent()


                val reply = kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
                    OpenAIClient.askChatGPT(listOf(ChatMessage("system", jsonPrompt))) { r ->
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

    // ✅ 儲存這次推薦的 recipeIds（做「一週內不重複」判斷用）
    private fun saveRecipeHistory(recipeIds: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        val today = getTodayId()

        val data = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "recipes" to recipeIds
        )

        db.collection("users")
            .document(uid)
            .collection("history")
            .document(today)
            .set(data)
    }

    // ✅ 讀取最近 days 天內推薦過的 recipeIds
    private fun getRecentRecipeHistory(days: Int = 7, callback: (Set<String>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        // 計算 7 天前的日期 ID，例如 "20241105"
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        val dateLimit = SimpleDateFormat("yyyyMMdd", Locale.TAIWAN)
            .format(cal.time)

        db.collection("users")
            .document(uid)
            .collection("history")
            .get()
            .addOnSuccessListener { snapshot ->
                val recent = snapshot.documents
                    .filter { it.id >= dateLimit }   // ✅ 過濾最近 7 天內的紀錄
                    .flatMap { it.get("recipes") as? List<String> ?: emptyList() }
                    .toSet()

                callback(recent)
            }
    }
    private val ingredientKeywords = listOf(
        "草莓", "香蕉", "芒果", "蘋果", "葡萄", "藍莓", "鳳梨", "奇異果",
        "空心菜", "高麗菜", "小黃瓜", "番茄", "洋蔥", "花椰菜", "菠菜",
        "雞蛋", "雞胸肉", "豬肉", "牛肉", "蝦", "魚", "蛤蜊", "豆腐"
        // 想加更多再加
    )

    /** 🆕 依 AIIntentResult 從資料庫「篩選 + 打分 + 以卡片回覆」 */
    private fun fetchRecipesByIntent(tab: String, ir: AIIntentResult, foodList: List<FoodItem>,userInput: String) {
        val loading = ChatMessage("bot", "🍳 幫你找符合的料理...", "loading")
        if (tab == "fridge") fridgeMessages.add(loading) else recipeMessages.add(loading)

        val fridgeNames = foodList.map { it.name }
        val fridgeBuckets = fridgeMainBuckets(foodList)
        // 🔍 判斷這次是問什麼類型，以及冰箱裡缺哪些指定的食材
        val qType = detectUserQueryType(ir)  // "ingredient" | "cuisine" | "spice" | "style" | "other"

        // 冰箱分頁才需要管「冰箱有沒有那個食材」
        val missingKeywords = if (tab == "fridge" && qType == "ingredient") {
            ir.include.map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { kw ->
                    // 排除看起來不像食材的字（例如：台式料理 / 韓式）
                    if (kw.length > 5 || kw.contains("料理") || kw.contains("式") || kw.contains("null")) {
                        false
                    } else {
                        fridgeNames.none { f ->
                            f.contains(kw, ignoreCase = true) || kw.contains(f, ignoreCase = true)
                        }
                    }
                }
        } else {
            emptyList()
        }

        getRecentRecipeHistory(7) { usedRecipes ->   // ✅ 讀取最近 7 天紀錄
            db.collection("recipes")
                .get()
                .addOnSuccessListener { snapshot ->

                    if (tab == "fridge") fridgeMessages.removeIf { it.type == "loading" }
                    else recipeMessages.removeIf { it.type == "loading" }

                    val include = ir.include.map { it.trim() }.filter { it.isNotBlank() }
                    val avoid = ir.avoid.map { it.trim() }.filter { it.isNotBlank() }
// 將 "null" 視為空字串，避免出現「null風味料理」
                    val cuisine = ir.cuisine
                        ?.trim()
                        ?.takeUnless { it.equals("null", ignoreCase = true) }
                        .orEmpty()
                    val style = ir.style?.trim().orEmpty()
                    val wantMild = ir.spiciness == "mild"
                    val wantSpicy = ir.spiciness == "spicy"

                    val spicyKeywords =
                        listOf("辣", "辣椒", "麻辣", "花椒", "剁椒", "韓式辣醬", "泡菜", "香辣")
                    val oilyKeywords = listOf(
                        "炸", "酥炸", "油炸", "酥脆", "奶油", "鮮奶油", "砂糖", "糖", "培根", "起司"
                    )
                    val lightKeywords =
                        listOf("蒸", "汆燙", "水煮", "涼拌", "清炒", "清燉", "蔬菜", "雞胸")

                    fun containsAny(hay: String, keys: List<String>) =
                        keys.any { k -> k.isNotBlank() && hay.contains(k, ignoreCase = true) }

                    fun listContainsAny(list: List<String>, keys: List<String>) =
                        list.any { s -> containsAny(s, keys) }

                    val results = snapshot.documents.mapNotNull { doc ->
                        // 🆕（1）檢查使用者是否想吃甜點


// 🆕（2）讀取資料庫 dishType
                        // 讀取 dishType
                        val dishRaw = doc.get("dishType")
                        val dishType = when (dishRaw) {
                            is String -> dishRaw
                            is List<*> -> dishRaw.joinToString("、") { it.toString() }
                            is Map<*, *> -> dishRaw.values.joinToString("、") { it.toString() }
                            else -> ""
                        }

// （1）使用者是否想吃甜點
                        val wantDessert =
                            userInput.contains("甜點", true) ||
                                    userInput.contains("點心", true) ||
                                    userInput.contains("甜食", true) ||
                                    userInput.contains("下午茶", true) ||
                                    ir.include.any {
                                        it.contains("甜", true) ||
                                                it.contains("點", true) ||
                                                it.contains("dessert", true)
                                    }


// （2）資料庫是否標示為甜點
                        val isDessertDb =
                            dishType.contains("dessert", true) ||
                                    dishType.contains("snack", true) ||   // 如果你希望 snack 也算甜點（看你要不要保留）
                                    dishType.contains("點心", true) ||
                                    dishType.contains("甜點", true)


// （3）想吃甜點但不是甜點 → 排除
// -----------------------------
// 🆕 依使用者意圖 qType = "dessert" 進行篩選
// -----------------------------
                        /*if (qType == "dessert") {

                            val isDessertOrSnack =
                                dishType.contains("dessert", true) ||
                                        dishType.contains("snack", true) ||
                                        dishType.contains("點心", true) ||
                                        dishType.contains("甜點", true)


                            if (!isDessertOrSnack) return@mapNotNull null
                        }*/

                        val recipeId = doc.id

                        // ✅ 若 recipeId 在 7 天內出現過 → 直接排除
                        if (recipeId in usedRecipes) return@mapNotNull null

                        val title = doc.getString("title") ?: return@mapNotNull null
                        val ingsClean = cleanedIngredients(doc)
                        val steps = (doc.get("steps") as? List<String>) ?: emptyList()
                        val imageUrl = doc.getString("imageUrl")

                        // ✅ 改這裡：套用我們的時間轉換函式
                        val rawTime = doc.getString("time")
                        val time = formatRecipeDuration(rawTime)

                        val yieldStr = when (val y = doc.get("yield")) {
                            is String -> y
                            is Number -> y.toString()
                            null -> null
                            else -> y.toString()
                        }

                        // ⚠️ 這四個欄位全部改成安全取值（不管後端是 字串 / 陣列 / 物件 / null 都不會閃退）

// mainIngredient 問題最少，但保險起見也用 get()
                        val mainIng = doc.get("mainIngredient")?.toString() ?: ""

// cuisine 一般是字串，但可能會有 null
                        val cuisineRaw = doc.get("cuisine")
                        val cuisineTag = when (cuisineRaw) {
                            is String -> cuisineRaw
                            is List<*> -> cuisineRaw.joinToString("、") { it.toString() }
                            is Map<*, *> -> cuisineRaw.values.joinToString("、") { it.toString() }
                            else -> ""
                        }

// method 可能是字串、陣列、物件 → 全部安全轉成字串
                        val methodRaw = doc.get("method")
                        val methodTag = when (methodRaw) {
                            is String -> methodRaw
                            is List<*> -> methodRaw.joinToString("、") { it.toString() }
                            is Map<*, *> -> methodRaw.values.joinToString("、") { it.toString() }
                            else -> ""
                        }

// dishType 也可能是字串或陣列（這是你現在會 crash 的主因）


                        val blob = (listOf(title) + ingsClean + steps).joinToString("\n")

                        // 🚫 避免詞
                        if (avoid.isNotEmpty() && (containsAny(title, avoid)
                                    || listContainsAny(ingsClean, avoid)
                                    || listContainsAny(steps, avoid))
                        ) return@mapNotNull null

                        // 🚫 mild 避開辣
                        if (wantMild && (containsAny(title, spicyKeywords)
                                    || listContainsAny(ingsClean, spicyKeywords))
                        ) return@mapNotNull null

                        // 🧊 冰箱模式：所有推薦必須「冰箱食材命中率 ≥ 40%」
                        if (tab == "fridge") {

                            val matchCount = ingsClean.count { ing ->
                                fridgeNames.any { f -> ing.contains(f, ignoreCase = true) }
                            }

                            val ratio =
                                if (ingsClean.isNotEmpty()) matchCount.toDouble() / ingsClean.size else 0.0

                            // 🍳 不達標 → 不推薦
                            if (ratio < 0.4) return@mapNotNull null
                        }

                        // 🧪 問「食材」時：食譜裡要真的有包含使用者指定的食材
                        // - 冰箱分頁：只有在「冰箱其實有這些食材」的情況才硬性要求
                        // - 今晚想吃什麼分頁（recipe）：只要是問食材，就一定要命中
                        if (qType == "ingredient" && include.isNotEmpty() &&
                            (tab != "fridge" || missingKeywords.isEmpty())
                        ) {
                            val hasIncludeMatch = include.any { kw ->
                                ingsClean.any { ing -> ingredientMatchesQuery(ing, kw) }
                            }
                            if (!hasIncludeMatch) return@mapNotNull null
                        }




                        // ✅✅✅ 打分開始
                        var score = 0.0

                        // 🔹 Step 1: 關鍵字與名稱匹配（讓名稱優先權最高）
                        val userQuery = buildString {
                            append(ir.cuisine.orEmpty())
                            if (ir.include.isNotEmpty()) append(" " + ir.include.joinToString(" "))
                            if (!ir.style.isNullOrBlank()) append(" " + ir.style)
                        }.trim()

                        val nameMatch = title.contains(userQuery, ignoreCase = true)
                        if (nameMatch) score += 10.0

                        // (1) include 關鍵字
                        include.forEach { k -> if (containsAny(blob, listOf(k))) score += 2.0 }

                        // (2) spicy / mild 偏好
                        if (wantSpicy && (containsAny(title, spicyKeywords)
                                    || listContainsAny(ingsClean, spicyKeywords))
                        ) score += 1.5
                        if (wantMild && !(containsAny(title, spicyKeywords)
                                    || listContainsAny(ingsClean, spicyKeywords))
                        ) score += 1.0

                        // ✅ include 食材直接存在 ingredients → 大幅加分
                        include.forEach { kw ->
                            if (ingsClean.any { it.contains(kw, ignoreCase = true) }) score += 1.8
                        }

                        // (3) 料理風格 / 健康類型
                        if (style in listOf("健康", "減脂", "低卡")) {
                            if (containsAny(blob, lightKeywords)) score += 1.2
                            if (!containsAny(blob, oilyKeywords)) score += 1.0
                        }
                        if (cuisine.isNotBlank() && cuisineTag.isNotBlank()) {

                            val q = cuisine.replace("料理", "").replace("風味", "").trim()

                            val match = cuisineTag.contains(q, true)
                                    || q.contains(cuisineTag, true)
                                    || cuisineTag.contains(cuisine, true)
                                    || cuisine.contains(cuisineTag, true)

                            if (match) score += 5.0    // 提高比重
                        }


                        // (4) ✅ 主食材
                        if (mainIng.isNotBlank()) {
                            val boost = fridgeBuckets[mainIng] ?: 0
                            if (boost > 0) score += 3.0 + boost * 0.5
                        }

                        // (5) 次食材
                        if (fridgeNames.isNotEmpty()) {
                            val match = ingsClean.count { ing ->
                                fridgeNames.any { f -> ing.contains(f, true) }
                            }
                            val ratio =
                                if (ingsClean.isNotEmpty()) match.toDouble() / ingsClean.size else 0.0
                            score += ratio
                        }

                        // (6) 使用者輸入精準詞
                        include.firstOrNull()?.let { kw ->
                            val kwMain = toMainCategory(kw)
                            if (mainIng.isNotBlank() && mainIng == kwMain) score += 2.0
                            if (ingsClean.any { it.contains(kw, true) }) score += 0.8
                            if (title.contains(kw, true)) score += 4.0
                        }

                        // ✅ 回傳結果
                        Triple(
                            UiRecipe(
                                title,
                                ingsClean.toMutableList(),
                                steps.toMutableList(),
                                imageUrl,
                                yieldStr,
                                time,
                                doc.id
                            ),
                            score,
                            doc.id
                        )
                    }.sortedByDescending { it.second }
                    Log.d("ChatViewModel", "fetchRecipesByIntent: tab=$tab, results=${results.size}, include=$include, cuisine=$cuisine")

                    // 🆕 新增：在產生結果之後，先判斷「問食材但冰箱沒有」的情境（只在冰箱模式）
                    if (tab == "fridge") {


// ② 若是問食材 & 冰箱「全部都有」→ 說太好了冰箱有
                        if (qType == "ingredient" && missingKeywords.isEmpty() && include.isNotEmpty()) {

                            val found = include.joinToString("、") { it }
                            val okText = "😄 太好了！你的冰箱裡有：$found\n我幫你推薦可以用這些食材做的料理喔～"

                            val okMsg = ChatMessage("bot", okText, "text")
                            fridgeMessages.add(okMsg)
                            saveMessageToFirestore("fridge", okMsg)
                            // ❗ 不 return → 要讓後面正常推薦食譜
                        }

// ③ 若冰箱缺少詢問的食材 → 說冰箱沒有 + 用冰箱現有食材組合推薦
                        if (qType == "ingredient" && missingKeywords.isNotEmpty()) {

                            // 1) 對話訊息
                            val warnText = "😅 你的冰箱裡沒有：${missingKeywords.joinToString("、")}。\n" +
                                    "以下是我依照你目前冰箱現有食材「可以組合出來」的料理給你參考～"

                            val warn = ChatMessage("bot", warnText, "text")
                            fridgeMessages.add(warn)
                            saveMessageToFirestore("fridge", warn)

                            // 2) 選出能用冰箱做出來的候選料理
                            val fridgeBasedList = results.filter { triple ->
                                val ings = triple.first.ingredients
                                val hit = ings.count { ing -> fridgeNames.any { f -> ing.contains(f, true) } }
                                val ratio = if (ings.isNotEmpty()) hit.toDouble() / ings.size else 0.0
                                hit >= 1 && ratio >= 0.5
                            }.take(5)

                            if (fridgeBasedList.isNotEmpty()) {
                                val jsonList = fridgeBasedList.map { r ->
                                    mapOf(
                                        "title" to r.first.name,
                                        "ingredients" to r.first.ingredients,
                                        "steps" to r.first.steps,
                                        "imageUrl" to r.first.imageUrl,
                                        "yield" to r.first.servings,
                                        "time" to r.first.totalTime
                                    )
                                }
                                val contentJson = gson.toJson(jsonList)

                                val alreadyExists = fridgeMessages.any {
                                    it.type == "recipe_cards" && it.content == contentJson
                                }
                                if (!alreadyExists) {
                                    val card = ChatMessage("bot", contentJson, "recipe_cards")
                                    fridgeMessages.add(card)
                                    saveMessageToFirestore("fridge", card)
                                }

                                // 完成冰箱模式 → 不要再繼續 fallback
                                return@addOnSuccessListener
                            } else {
                                Log.w("ChatViewModel", "⚠️ 冰箱能組合的候選為空（ingredient-missing branch）")
                                // 不 return，讓後面 Step 4 fallback 去處理
                            }
                        }

                    }

                    // ✅ Step 4: 冰箱太少 → 完全做不出任何料理
                    // ✅ Step 4: 完全找不到可以推薦的 DB 食譜 → 文案 + fallback
                    val top = results.take(5).map { it.first }
                    Log.d("ChatViewModel", "fetchRecipesByIntent: topSize=${top.size}")


                    // ❗ 只有冰箱模式才會顯示「你的冰箱缺少…」的錯誤
                    if (top.isEmpty()) {
                        val err = ChatMessage(
                            "bot",
                            "😅 查無相關資料庫食譜喔～換個關鍵字試看看？",
                            "text"
                        )
                        val cuisineName = ir.cuisine
                            ?.trim()
                            ?.takeUnless { it.equals("null", ignoreCase = true) }
                        if (tab == "fridge") {
                            val warnText = when (qType) {
                                "ingredient" ->
                                    "😅 你的冰箱缺少你指定的食材，因此無法做出你想要的料理。\n我會推薦冰箱能做、最接近需求的料理給你。"

                                "cuisine" ->
                                    "😅 你的冰箱缺少「${cuisineName ?: "這種"}料理」常用的食材，因此無法做出正統風味。\n我會推薦冰箱能做、風味接近的料理給你。"

                                "spice" ->
                                    "😅 你的冰箱沒有足夠的食材來做符合你辣度偏好的料理。\n我會推薦冰箱能做、但盡量符合你口味的料理給你。"

                                "style" ->
                                    "😅 你的冰箱沒有符合你指定風格的食材，我會推薦冰箱能做、風味接近的料理給你。"

                                else ->
                                    "😅 你的冰箱食材不足以做出你想吃的料理類型，我會推薦冰箱能做、最接近需求的料理給你。"
                            }

                            val warn = ChatMessage("bot", warnText, "text")
                            fridgeMessages.add(warn)
                            saveMessageToFirestore("fridge", warn)
                        }


                        // 先試著用打分結果挑一些「勉強接近」的
                        val fallbackList = results.filter { it.second >= 0.2 }.take(5)
                        if (fallbackList.isNotEmpty()) {
                            val jsonList = fallbackList.map { r ->
                                mapOf(
                                    "title" to r.first.name,
                                    "ingredients" to r.first.ingredients,
                                    "steps" to r.first.steps,
                                    "imageUrl" to r.first.imageUrl,
                                    "yield" to r.first.servings,
                                    "time" to r.first.totalTime
                                )
                            }
                            val contentJson = gson.toJson(jsonList)
                            val card = ChatMessage("bot", contentJson, "recipe_cards")
                            if (tab == "fridge") fridgeMessages.add(card) else recipeMessages.add(card)
                            saveMessageToFirestore(tab, card)
                        } else {
                            // 🔥 真的完全找不到 → 用 GPT 幫忙生幾道料理當最後保險
                            val wishText = buildString {
                                if (include.isNotEmpty()) {
                                    append(include.joinToString("、"))
                                }
                                if (cuisine.isNotBlank()) {
                                    if (isNotEmpty()) append("的")
                                    append(cuisine).append("料理")
                                }
                            }.ifBlank { "好吃又簡單的家常菜" }


                        }

                        return@addOnSuccessListener
                    }

                    val recommendedIds = top.mapNotNull { it.id }
                    saveRecipeHistory(recommendedIds)

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

                    // ✅ 僅在「今晚想吃什麼」模式下顯示溫暖提示訊息
                    if (tab == "recipe") {
                        val cleanCuisine = ir.cuisine
                            ?.trim()
                            ?.takeUnless { it.equals("null", ignoreCase = true) }

                        val introText = when {
                            !cleanCuisine.isNullOrBlank() ->
                                "🍳 我幫你找到了幾道「${cleanCuisine}」風味的料理，看看有沒有你的菜吧！"

                            ir.include.isNotEmpty() ->
                                "🍽️ 根據你的關鍵字，我挑了幾道可能會喜歡的料理給你～"

                            else ->
                                "🍳 我幫你挑了幾道人氣家常料理，看看想不想試試看！"
                        }

                        val introMsg = ChatMessage("bot", introText, "text")
                        recipeMessages.add(introMsg)
                        saveMessageToFirestore("recipe", introMsg)
                    }


                    // ✅ 統一推薦卡生成（含防重複）
                    val botMsg = ChatMessage("bot", contentJson, "recipe_cards")
                    val alreadyExists = (if (tab == "fridge") fridgeMessages else recipeMessages)
                        .any { it.type == "recipe_cards" && it.content == contentJson }

                    if (!alreadyExists) {
                        if (tab == "fridge") fridgeMessages.add(botMsg) else recipeMessages.add(botMsg)
                        saveMessageToFirestore(tab, botMsg)
                        Log.d("ChatViewModel", "✅ 已新增推薦卡 ($tab)")
                    } else {
                        Log.w("ChatViewModel", "⚠️ 重複推薦卡片被略過 ($tab)")
                    }
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

}