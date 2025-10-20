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
            // ✅ 延長 Timeout 設定
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    // ================================================================
    // 🧠 原始 API 呼叫：askChatGPT（保留）
    // ================================================================
    fun askChatGPT(messages: List<ChatMessage>, callback: (String?) -> Unit) {
        if (apiKey.isBlank()) {
            Log.e("OpenAI", "❌ API Key 為空，請確認 local.properties")
            callback(null)
            return
        }

        // ✅ 原本的 system 指令（保持不變）
        val systemInstruction = OpenAIMessage(
            role = "system",
            content = """
你是一位美食推薦助手，請根據使用者輸入的食材或問題，推薦數道食譜。
請以 **JSON 格式** 回覆，格式如下：

[
  {
    "title": "料理名稱 by 作者",
    "ingredients": ["食材1", "食材2", "食材3"],
    "steps": ["步驟1", "步驟2", "步驟3"]
  },
  ...
]

注意事項：
1. 不要加上任何多餘說明、問候或文字。
2. 只輸出 JSON 陣列，保持標準格式。
3. 所有文字使用繁體中文。
"""
        )

        // 🔹 把 ChatMessage 轉成 OpenAI 格式
        val formattedMessages = messages.map {
            OpenAIMessage(
                role = when (it.role) {
                    "user" -> "user"
                    "bot" -> "assistant"
                    else -> "system"
                },
                content = it.content
            )
        }

        // ✅ 在最前插入 system 指令
        val finalMessages = listOf(systemInstruction) + formattedMessages

        val bodyJson = gson.toJson(
            mapOf(
                "model" to "gpt-3.5-turbo",
                "temperature" to 1.5,
                "top_p" to 0.9,
                "messages" to finalMessages
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
    // 🌟 新增：智慧 FoodieBot 模式（更聰明、更貼心）
    // ================================================================
    fun askSmartBot(
        messages: List<ChatMessage>,
        foodList: List<FoodItem>,
        mode: String = "fridge",
        callback: (String?) -> Unit
    ) {
        // 🧠 FoodieBot 的行為定義
        val systemPrompt = """
            你是 FoodieBot，一個貼心又細心的料理助理。
            請根據使用者目前的冰箱狀況、輸入內容與心情，產生自然、友善、有人味的回覆。

            🎯 回覆邏輯：
            1️⃣ 若冰箱沒有任何食材，請不要推薦料理，改提醒使用者「新增食材」或「使用今天想吃什麼料理」。
            2️⃣ 若食材太少或關鍵食材缺乏，請禮貌地說明不足，並給出幾道「可簡單完成」的家常料理。
            3️⃣ 若食材齊全，請推薦 2–3 道適合台灣人口味的料理（要列出【名稱】【食材】【步驟】）。
            4️⃣ 若使用者輸入模糊（如「好餓」、「推薦一下」），請主動引導：「想吃台式、日式還是西式呢？」。
            5️⃣ 回覆語氣自然、有溫度，不要太機械。
            6️⃣ 若要推薦料理，請使用此格式：
               【名稱】：○○料理
               【食材】：A、B、C...
               【步驟】：
               1. ...
               2. ...
        """.trimIndent()

        // 🧩 判斷冰箱狀況
        val hasFood = foodList.isNotEmpty()
        val contextMessage = if (hasFood) {
            "目前冰箱內有：${foodList.joinToString("、") { it.name }}。請根據這些食材提供貼心建議。"
        } else {
            "冰箱目前是空的。請不要推薦料理，改提醒使用者新增食材或切換模式。"
        }

        // 🧩 組合完整的 system prompt
        val systemMsg = ChatMessage(role = "system", content = systemPrompt + "\n\n" + contextMessage)
        val finalMessages = listOf(systemMsg) + messages

        // ✅ 呼叫原本的 askChatGPT 發送
        askChatGPT(finalMessages, callback)
    }
}
