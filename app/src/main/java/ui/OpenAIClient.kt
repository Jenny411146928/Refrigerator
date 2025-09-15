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

data class ChatResponse(
    @SerializedName("choices") val choices: List<Choice>
)

data class Choice(
    @SerializedName("message") val message: ChatMessage
)

object OpenAIClient {
    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private const val apiKey = BuildConfig.OPENAI_API_KEY

    // ⚠️ 測試用：信任所有 SSL 憑證
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

        val requestBodyJson = gson.toJson(
            mapOf(
                "model" to "gpt-3.5-turbo",
                "messages" to messages
            )
        )
        val requestBody = requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d("OpenAI", "目前讀到的 API Key: ${apiKey.take(15)}...（隱藏其餘）")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpenAI", "❌ 網路連線錯誤: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                Log.d("OpenAI", "🌐 回傳狀態碼: ${response.code}")
                Log.d("OpenAI", "🌐 回傳內容: $bodyStr")

                if (response.isSuccessful) {
                    try {
                        val chatResponse = gson.fromJson(bodyStr, ChatResponse::class.java)
                        val reply = chatResponse.choices.firstOrNull()?.message?.content
                        Log.d("OpenAI", "✅ GPT 回覆: $reply")
                        callback(reply)
                    } catch (e: Exception) {
                        Log.e("OpenAI", "❌ JSON 解析失敗: ${e.message}")
                        callback(null)
                    }
                } else {
                    Log.e("OpenAI", "❌ 回應錯誤 ${response.code}")
                    callback(null)
                }
            }
        })
    }
}
