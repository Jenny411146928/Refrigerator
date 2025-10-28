package tw.edu.pu.csim.refrigerator.openai

import android.util.Log
import okhttp3.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import tw.edu.pu.csim.refrigerator.BuildConfig
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// 🔹 OpenAI API 回傳格式
data class ChatResponse(
    @SerializedName("choices") val choices: List<Choice>
)
data class Choice(
    @SerializedName("message") val message: OpenAIMessage
)
// 🔹 對應 OpenAI API 的 message 格式
data class OpenAIMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

// ✅ 語意分析結果（只用來決策，不生成食譜）
data class AIIntentResult(
    val intent: String,                  // "find_recipe" | "chat" | "ask"
    val include: List<String> = emptyList(),   // 想要的食材/關鍵字
    val avoid: List<String> = emptyList(),     // 排除的食材/關鍵字（含辣等）
    val cuisine: String? = null,               // 台式/西式/日式/韓式/中式/美式...
    val style: String? = null,                 // 健康/減脂/低卡/家常/清爽...
    val spiciness: String? = null,             // "mild" | "spicy" | null
    val reply: String? = null                  // 若 intent=chat/ask 用這個回覆
)

object OpenAIClient {

    // 👉 你可以換成更好的模型（若你的 key 有權限）：
    // 建議："gpt-4o" 或 "gpt-4.1"；無法使用時退回 "gpt-3.5-turbo"
    private const val MODEL = "gpt-3.5-turbo"

    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private const val apiKey = BuildConfig.OPENAI_API_KEY

    // ✅ HTTP Client 設定
    private val client: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    // ================================================================
    // 🧠 原始 API 呼叫：askChatGPT（保留不刪）
    // ================================================================
    fun askChatGPT(messages: List<ChatMessage>, callback: (String?) -> Unit) {
        if (apiKey.isBlank()) {
            Log.e("OpenAI", "❌ API Key 為空，請確認 local.properties")
            callback(null)
            return
        }

        val formattedMessages = messages.map {
            OpenAIMessage(
                role = when (it.role) {
                    "user" -> "user"
                    "bot" -> "assistant"   // 保持你原本的 mapping
                    else -> "system"
                },
                content = it.content
            )
        }

        val bodyJson = gson.toJson(
            mapOf(
                "model" to MODEL,
                "temperature" to 0.9,
                "top_p" to 0.95,
                "messages" to formattedMessages
            )
        )

        val requestBody =
            bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpenAI", "❌ 網路錯誤: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    try {
                        val chatResponse = gson.fromJson(bodyStr, ChatResponse::class.java)
                        val rawReply = chatResponse.choices.firstOrNull()?.message?.content
                        Log.d("OpenAI_RawReply", "💬 $rawReply")

                        val cleaned = rawReply
                            ?.replace("\r\n", "\n")
                            ?.replace(Regex("\n{3,}"), "\n\n")
                            ?.trim()

                        callback(cleaned)
                    } catch (e: Exception) {
                        Log.e("OpenAI", "❌ JSON 解析失敗: ${e.message}")
                        callback(null)
                    }
                } else {
                    Log.e("OpenAI", "❌ 回應錯誤 ${response.code} | body=$bodyStr")
                    callback(null)
                }
            }
        })
    }

    // ================================================================
    // 🌟 新版 FoodieBot（保留不刪）— 但你之後會改用 analyzeUserIntent
    // ================================================================
    fun askSmartBot(
        messages: List<ChatMessage>,
        foodList: List<FoodItem>,
        mode: String = "fridge",
        callback: (String?) -> Unit
    ) {
        val systemPrompt = """
你是一位智慧料理助理 FoodieBot，能理解自然語句並推薦料理。

使用者的輸入可能是：
- 食材（例如：「雞肉」、「豆腐」、「牛奶」）
- 多個食材（例如：「我有雞蛋和洋蔥」）
- 口味偏好（例如：「想吃無辣的」、「我想吃台式的」）
- 模糊語句（例如：「好餓」、「推薦一下」）
- 非料理話題（例如：「你好嗎」、「你幾歲」）

🎯 請遵守以下規則：
1️⃣ 若使用者輸入與料理無關，請回「我只懂料理喔～🍳」。
2️⃣ 若提到食材或口味，請推薦 2~3 道料理。
   - 每道請盡量不同。
   - 若多次詢問相似食材，也要嘗試推薦不同菜色。
3️⃣ 若使用者輸入模糊（例如「推薦一下」、「好餓」），請引導提問：「想吃台式、日式還是西式呢？」。
4️⃣ 若使用者說「我有雞蛋和牛奶」，請推薦能同時使用這些食材的料理。
5️⃣ 若出現「無辣」、「不辣」、「清淡」、「小孩吃」、「健康」，請排除辛辣、麻辣、辣椒、咖哩等料理。
6️⃣ 若使用者沒有明確說「甜點」或「餅乾」，請只推薦鹹食、主餐或家常料理，不能推薦甜點、餅乾、飲品。
7️⃣ 回覆語氣要自然、溫暖、親切。
8️⃣ 所有文字使用繁體中文。
9️⃣ 請在每次回覆時「隨機」從你所知的料理中挑選不同菜名，避免重複。

10️⃣ 每道料理請附上簡短說明，例如：
【名稱】：三杯雞
【食材】：雞腿肉、九層塔、醬油...
【步驟】：簡單三步內即可完成。
⚠️ 若使用者多次詢問相似問題，請隨機挑選不同的菜名或變化版本（例如：上次推薦三杯雞，下次可推薦鹽酥雞或蔥爆雞丁）。

""".trimIndent()

        val contextMessage = if (foodList.isEmpty()) {
            "冰箱目前是空的，請提醒使用者新增食材或切換到『今天想吃什麼料理』模式。"
        } else {
            "目前冰箱內的食材有：${foodList.joinToString("、") { it.name }}。"
        }

        val systemMsg = ChatMessage(role = "system", content = "$systemPrompt\n\n$contextMessage")
        val finalMessages = listOf(systemMsg) + messages

        askChatGPT(finalMessages, callback)
    }

    // ================================================================
    // 🧠（新增）只做「語意→條件」分析，不產生食譜
    // ================================================================
    fun analyzeUserIntent(
        userInput: String,
        callback: (AIIntentResult?) -> Unit
    ) {
        if (apiKey.isBlank()) {
            Log.e("OpenAI", "❌ API Key 為空，請確認 local.properties")
            callback(null)
            return
        }

        val system = ChatMessage(
            role = "system",
            content = """
你是一個語意解析器，只輸出 JSON。不要多餘文字。

請將使用者對話解析為以下 JSON 格式：
{
  "intent": "find_recipe" | "chat" | "ask",
  "include": [字串...],     // 想要包含的食材或關鍵字（例如 "雞肉", "義大利麵"）
  "avoid": [字串...],       // 想要排除的食材或關鍵字（例如 "辣", "海鮮"）
  "cuisine": "台式|西式|日式|韓式|中式|美式|其他|null",
  "style": "健康|減脂|低卡|家常|清爽|null",
  "spiciness": "mild|spicy|null",
  "reply": "若 intent=chat/ask 時給一段自然中文回覆"
}

規則：
- 若使用者只閒聊或與料理無關：intent="chat"，給 reply。
- 若使用者要你推薦或描述偏好但沒有明確條件：intent="ask"，給 reply 引導他（例如：想吃哪一國料理？要不要無辣？）。
- 只要能從文字推斷出找食譜的需求，就用 intent="find_recipe"，並盡量填 include/avoid/cuisine/style/spiciness。
- 請務必輸出有效 JSON（單行或多行皆可），不要加反引號或多餘說明。
""".trimIndent()
        )
        val user = ChatMessage(role = "user", content = userInput)

        val bodyJson = Gson().toJson(
            mapOf(
                "model" to MODEL,
                "temperature" to 0.3, // 解析型任務，降低發散
                "messages" to listOf(
                    OpenAIMessage("system", system.content),
                    OpenAIMessage("user", user.content)
                )
            )
        )

        val requestBody = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpenAI", "❌ analyzeUserIntent 失敗: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (!response.isSuccessful || bodyStr == null) {
                    Log.e("OpenAI", "❌ analyzeUserIntent 回應錯誤 ${response.code}, body=$bodyStr")
                    callback(null)
                    return
                }
                try {
                    val chatResponse = gson.fromJson(bodyStr, ChatResponse::class.java)
                    val raw = chatResponse.choices.firstOrNull()?.message?.content?.trim()
                    if (raw.isNullOrBlank()) {
                        callback(null); return
                    }
                    // 嘗試清掉可能包起來的 code fence
                    val cleaned = raw
                        .removePrefix("```json")
                        .removePrefix("```JSON")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val result = gson.fromJson(cleaned, AIIntentResult::class.java)
                    callback(result)
                } catch (e: Exception) {
                    Log.e("OpenAI", "❌ analyzeUserIntent JSON 解析錯誤: ${e.message}\nbody=$bodyStr")
                    callback(null)
                }
            }
        })
    }
}
