package tw.edu.pu.csim.refrigerator.openai

import android.util.Log
import okhttp3.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import tw.edu.pu.csim.refrigerator.BuildConfig
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
            .build()
    }

    private val gson = Gson()

    fun askChatGPT(messages: List<ChatMessage>, callback: (String?) -> Unit) {
        if (apiKey.isBlank()) {
            Log.e("OpenAI", "❌ API Key 為空，請確認 local.properties")
            callback(null)
            return
        }

        // 🔹 把你的 ChatMessage 轉成 OpenAI 支援的格式
        val formattedMessages = messages.map {
            OpenAIMessage(
                role = when (it.role) {
                    "user" -> "user"
                    "bot" -> "assistant" // ✅ 修正：把 bot 轉成 assistant
                    else -> "system"
                },
                content = it.content
            )
        }

        val bodyJson = gson.toJson(
            mapOf(
                "model" to "gpt-3.5-turbo",
                "messages" to formattedMessages
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
                Log.e("OpenAI", "❌ 網路錯誤: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    try {
                        val chatResponse = gson.fromJson(bodyStr, ChatResponse::class.java)
                        val rawReply = chatResponse.choices.firstOrNull()?.message?.content

                        // ✅ 過濾：避免出現兩段重複的推薦
                        val cleaned = rawReply
                            ?.lines()
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?.distinct() // 去掉重複
                            ?.joinToString("\n")
                            ?.ifEmpty { rawReply }

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
}
