package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import tw.edu.pu.csim.refrigerator.openai.OpenAIClient
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    var waitingForDish = mutableStateOf(false)

    private val db = FirebaseFirestore.getInstance()
    private val today: String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        if (uid != null) {
            observeTodayMessages()   // 監聽今日訊息
            deleteOldMessages()      // 刪掉舊訊息
        }
    }

    /** 新增訊息到對話列表 + 存入 Firestore */
    fun addMessage(message: ChatMessage) {
        if (uid == null) return

        // 避免重複出現 options
        if (message.type == "options" && messages.any { it.type == "options" }) {
            return
        }

        // 本地更新
        messages.add(message)

        // 存進 Firestore（按照日期分類）
        val msgData = hashMapOf(
            "role" to message.role,
            "content" to message.content,
            "type" to message.type,
            "timestamp" to message.timestamp,
            "date" to today
        )

        db.collection("users")
            .document(uid)
            .collection("chats")           // 📂 每個使用者獨立的聊天資料夾
            .document(today)               // 📂 以日期分組（例如 2025-09-29）
            .collection("messages")        // 📂 當天的訊息
            .add(msgData)
    }

    /** 即時監聽「今天的訊息」 */
    private fun observeTodayMessages() {
        if (uid == null) return

        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(today)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                messages.clear()
                for (doc in snapshot.documents) {
                    val role = doc.getString("role") ?: "bot"
                    val content = doc.getString("content") ?: ""
                    val type = doc.getString("type") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                    messages.add(ChatMessage(role, content, type, timestamp))
                }

                // ✅ 如果完全沒有訊息，給一次 options
                if (messages.isEmpty()) {
                    addMessage(
                        ChatMessage(
                            role = "bot",
                            content = "👋嗨！要用哪種方式幫你找料理呢？",
                            type = "options"
                        )
                    )
                }
            }
    }

    /** 刪除昨天以前的訊息 */
    private fun deleteOldMessages() {
        if (uid == null) return

        db.collection("users")
            .document(uid)
            .collection("chats")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    if (doc.id < today) { // 日期比今天小 → 舊的
                        db.collection("users")
                            .document(uid)
                            .collection("chats")
                            .document(doc.id)
                            .delete()
                    }
                }
            }
    }

    /** 呼叫 OpenAI */
    fun askAI(
        foodList: List<String> = emptyList(),
        checkFridge: Boolean = false,
        customPrompt: String? = null
    ) {
        if (uid == null) return

        CoroutineScope(Dispatchers.IO).launch {
            if (checkFridge && foodList.isEmpty()) {
                addMessage(ChatMessage("bot", "🧊 你的冰箱目前是空的，請先新增食材再試試看！"))
                return@launch
            }

            val systemPrompt = customPrompt ?: """
                你是一個料理推薦助理，根據上下文自動判斷使用者意圖：
                - 如果使用者問「冰箱推薦」：
                  你必須根據冰箱食材（${foodList.joinToString("、")}）推薦 3 道不同的料理，
                  只列出料理名稱，且至少要用到一個冰箱食材。
                - 如果使用者問「今天想吃什麼料理」：
                  請根據輸入的料理名稱，提供完整的【食材清單】與【步驟】。
                - 如果使用者說「還有別的嗎」或「換一個」：
                  請再推薦 3 道不同的料理。
                - 請保持回覆簡潔，避免多餘的解釋文字。
            """.trimIndent()

            val context = listOf(ChatMessage("system", systemPrompt)) + messages

            OpenAIClient.askChatGPT(context) { reply ->
                if (reply != null) {
                    if (reply.contains("【食材清單】")) {
                        val parts = reply.split("【步驟】")
                        val ingredientsPart = parts.getOrNull(0)
                            ?.substringAfter("【食材清單】")
                            ?.trim()
                        val stepsPart = parts.getOrNull(1)?.trim()

                        ingredientsPart?.let { ing ->
                            val ingredients = ing.lines()
                                .map { it.trim() }
                                .map { it.replace(Regex("^[0-9\\.\\s\\-:：]+"), "") }
                                .filter { it.isNotBlank() }

                            addMessage(
                                ChatMessage(
                                    role = "bot",
                                    content = ingredients.joinToString(","),
                                    type = "ingredients"
                                )
                            )
                        }

                        stepsPart?.let { st ->
                            val steps = st.lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() && it != ":" && it != "：" }

                            addMessage(
                                ChatMessage(
                                    role = "bot",
                                    content = steps.joinToString("||"),
                                    type = "steps"
                                )
                            )
                        }
                    } else {
                        val cleaned = reply.lines()
                            .filter { it.isNotBlank() }
                            .joinToString("\n")
                            .ifEmpty { reply }

                        addMessage(ChatMessage("bot", cleaned))
                    }

                    // ✅ AI 回答完後，檢查是否需要再給一次 options
                    val lastUserMessage = messages.lastOrNull { it.role == "user" }
                    if (lastUserMessage != null &&
                        !messages.any { it.type == "options" && it.timestamp > lastUserMessage.timestamp }
                    ) {
                        addMessage(
                            ChatMessage(
                                role = "bot",
                                content = "👋還要我再幫你推薦嗎？",
                                type = "options"
                            )
                        )
                    }
                } else {
                    addMessage(ChatMessage("bot", "⚠️ 系統錯誤，請再試一次。"))
                }
            }
        }
    }
}
