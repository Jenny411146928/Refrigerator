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

object OpenAIClient {
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
    // 🧠 原始 API 呼叫：askChatGPT
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
                    "bot" -> "assistant"   // 🔥 關鍵修改在這行
                    else -> "system"
                },
                content = it.content
            )
        }

        val bodyJson = gson.toJson(
            mapOf(
                "model" to "gpt-3.5-turbo",
                "temperature" to 1.2,
                "top_p" to 0.9,
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
    // 🌟 新版 FoodieBot：能理解自然語句、口味、食材、甚至閒聊
    // ================================================================
    fun askSmartBot(
        messages: List<ChatMessage>,
        foodList: List<FoodItem>,
        mode: String = "fridge",
        callback: (String?) -> Unit
    ) {
        // 🧠 智慧行為設定
        val systemPrompt = """
你是一位智慧料理助理 FoodieBot，能理解自然語句並推薦料理。
使用者的輸入可能是：
- 食材（例如：「雞肉」、「豆腐」、「牛奶」）
- 多個食材（例如：「我有雞蛋和洋蔥」）
- 口味偏好（例如：「想吃無辣的」、「我想吃台式的」）
- 模糊語句（例如：「好餓」、「推薦一下」）
- 非料理話題（例如：「你好嗎」、「你幾歲」）

🎯 請遵守以下規則：
1️⃣ 若使用者輸入與食物無關，請回「我只懂料理喔～🍳」。
2️⃣ 若提到食材或口味，請推薦 2~3 道料理。
   每道料理請使用這個格式：
   【名稱】：○○料理
   【食材】：A、B、C...
   【步驟】：
   1. ...
   2. ...
3️⃣ 若使用者輸入模糊（例如「推薦一下」、「好餓」），請引導提問：
   「想吃台式、日式還是西式呢？」
4️⃣ 若使用者說「我有雞蛋和牛奶」，請推薦能同時使用這些食材的料理。
5️⃣ 回覆請使用自然、友善、溫暖的語氣，不要太制式。
6️⃣ 不要輸出 JSON，不要加多餘標題或 Markdown 標記。
7️⃣ 所有文字使用繁體中文。

""".trimIndent()

        // 🧩 加入冰箱狀況描述
        val contextMessage = if (foodList.isEmpty()) {
            "冰箱目前是空的，請提醒使用者新增食材或切換到『今天想吃什麼料理』模式。"
        } else {
            "目前冰箱內的食材有：${foodList.joinToString("、") { it.name }}。"
        }

        // 🧩 組合完整的 system 訊息
        val systemMsg = ChatMessage(role = "system", content = "$systemPrompt\n\n$contextMessage")
        val finalMessages = listOf(systemMsg) + messages

        // ✅ 呼叫共用的 API 方法
        askChatGPT(finalMessages, callback)
    }
}
