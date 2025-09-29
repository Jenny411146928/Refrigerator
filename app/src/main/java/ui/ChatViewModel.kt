package tw.edu.pu.csim.refrigerator.ui

import android.util.Log
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
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        Log.d("ChatViewModel", "🚀 init，今天是 $today, uid=$uid")
        if (uid != null) {
            loadTodayMessages()   // ⬅️ 啟動時先載入 Firestore 紀錄
            observeTodayMessages()
            deleteOldMessages()
        } else {
            Log.e("ChatViewModel", "❌ init 失敗，uid=null，使用者未登入")
        }
    }

    /** 確保至少有一次選擇框 */
    fun ensureOptionsMessage() {
        if (uid == null) return
        if (messages.none { it.type == "options" }) {
            addMessage(ChatMessage("bot", "👋嗨！要用哪種方式幫你找料理呢？", "options"))
        }
    }

    /** 新增訊息 + 存入 Firestore */
    fun addMessage(message: ChatMessage) {
        if (uid == null) {
            Log.e("ChatViewModel", "❌ addMessage: uid=null，無法寫入 Firestore")
            return
        }
        if (message.type == "options" && messages.any { it.type == "options" }) return

        // 本地更新
        messages.add(message)
        Log.d("ChatViewModel", "📝 addMessage: 新增訊息=$message")

        val msgData = hashMapOf(
            "role" to message.role,
            "content" to message.content,
            "type" to message.type,
            "timestamp" to message.timestamp,
            "date" to today
        )

        // 用 timestamp 當文件 ID，避免丟失
        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(today)
            .collection("messages")
            .document(message.timestamp.toString())
            .set(msgData)
            .addOnSuccessListener {
                Log.d("ChatViewModel", "✅ Firestore 寫入成功: $msgData")
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "❌ Firestore 寫入失敗: ${e.message}", e)
            }
    }

    /** ➡️ 一次性讀取今天的訊息 */
    private fun loadTodayMessages() {
        if (uid == null) return
        Log.d("ChatViewModel", "📥 loadTodayMessages() 載入 $today 訊息")

        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(today)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                messages.clear()
                for (doc in snapshot.documents) {
                    val role = doc.getString("role") ?: "bot"
                    val content = doc.getString("content") ?: ""
                    val type = doc.getString("type") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    messages.add(ChatMessage(role, content, type, timestamp))
                }
                Log.d("ChatViewModel", "✅ Firestore 載入 ${messages.size} 筆訊息")
                if (messages.isEmpty()) {
                    ensureOptionsMessage()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "❌ Firestore 載入失敗: ${e.message}", e)
            }
    }

    /** 監聽今天的訊息（即時更新） */
    private fun observeTodayMessages() {
        if (uid == null) return
        Log.d("ChatViewModel", "👂 observeTodayMessages() 啟動監聽 $today 訊息")

        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(today)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Log.e("ChatViewModel", "❌ Firestore 監聽錯誤: ${e?.message}")
                    return@addSnapshotListener
                }

                val newMessages = mutableListOf<ChatMessage>()
                for (doc in snapshot.documents) {
                    val role = doc.getString("role") ?: "bot"
                    val content = doc.getString("content") ?: ""
                    val type = doc.getString("type") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    newMessages.add(ChatMessage(role, content, type, timestamp))
                }

                messages.clear()
                messages.addAll(newMessages)
                Log.d("ChatViewModel", "🔄 Firestore 監聽更新，共 ${messages.size} 筆")

                if (messages.isEmpty()) {
                    ensureOptionsMessage()
                }
            }
    }

    /** 刪掉昨天以前的訊息 */
    private fun deleteOldMessages() {
        if (uid == null) return
        val todayInt = today.toInt()
        Log.d("ChatViewModel", "🗑 deleteOldMessages() 清理 $todayInt 之前的訊息")

        db.collection("users")
            .document(uid)
            .collection("chats")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val docDate = doc.id.toIntOrNull()
                    if (docDate != null && docDate < todayInt) {
                        db.collection("users")
                            .document(uid)
                            .collection("chats")
                            .document(doc.id)
                            .delete()
                            .addOnSuccessListener {
                                Log.d("ChatViewModel", "🗑 已刪除舊訊息: ${doc.id}")
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "❌ 清理舊訊息失敗: ${e.message}", e)
            }
    }

    /** 呼叫 OpenAI */
    fun askAI(
        foodList: List<String> = emptyList(),
        checkFridge: Boolean = false,
        customPrompt: String? = null
    ) {
        if (uid == null) {
            Log.e("ChatViewModel", "❌ askAI: uid=null")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (checkFridge && foodList.isEmpty()) {
                addMessage(ChatMessage("bot", "🧊 你的冰箱目前是空的，請先新增食材再試試看！"))
                return@launch
            }

            val systemPrompt = customPrompt ?: """
                你是一個料理推薦助理，根據上下文自動判斷使用者意圖...
            """.trimIndent()

            val context = listOf(ChatMessage("system", systemPrompt)) + messages
            Log.d("ChatViewModel", "🤖 askAI 發送訊息，context size=${context.size}")

            OpenAIClient.askChatGPT(context) { reply ->
                if (reply != null) {
                    Log.d("ChatViewModel", "✅ AI 回覆: $reply")
                    if (reply.contains("【食材清單】")) {
                        val parts = reply.split("【步驟】")
                        val ingredients = parts.getOrNull(0)
                            ?.substringAfter("【食材清單】")
                            ?.lines()
                            ?.map { it.trim().replace(Regex("^[0-9\\.\\s\\-:：]+"), "") }
                            ?.filter { it.isNotBlank() }
                            ?: emptyList()
                        val steps = parts.getOrNull(1)
                            ?.lines()
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() && it != ":" && it != "：" }
                            ?: emptyList()
                        if (ingredients.isNotEmpty()) {
                            addMessage(ChatMessage("bot", ingredients.joinToString(","), "ingredients"))
                        }
                        if (steps.isNotEmpty()) {
                            addMessage(ChatMessage("bot", steps.joinToString("||"), "steps"))
                        }
                    } else {
                        addMessage(ChatMessage("bot", reply.trim()))
                    }
                    val lastUser = messages.lastOrNull { it.role == "user" }
                    if (lastUser != null &&
                        !messages.any { it.type == "options" && it.timestamp > lastUser.timestamp }
                    ) {
                        addMessage(ChatMessage("bot", "👋還要我再幫你推薦嗎？", "options"))
                    }
                } else {
                    Log.e("ChatViewModel", "❌ AI 回覆為 null")
                    addMessage(ChatMessage("bot", "⚠️ 系統錯誤，請再試一次。"))
                }
            }
        }
    }
}
