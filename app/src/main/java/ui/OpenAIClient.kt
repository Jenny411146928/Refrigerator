package tw.edu.pu.csim.refrigerator.openai

import android.util.Log
import okhttp3.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import tw.edu.pu.csim.refrigerator.BuildConfig
import tw.edu.pu.csim.refrigerator.model.ChatMessage

data class ChatResponse(
    @SerializedName("choices") val choices: List<Choice>
)

data class Choice(
    @SerializedName("message") val message: ChatMessage
)

object OpenAIClient {
    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private val apiKey = BuildConfig.OPENAI_API_KEY

    private val client = OkHttpClient()
    private val gson = Gson()

    fun askChatGPT(messages: List<ChatMessage>, callback: (String?) -> Unit) {
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

        Log.d("OpenAI", "目前讀到的 API Key: $apiKey")
        Log.e("OpenAI", "API Error")

        if (apiKey.isBlank()) {
            Log.e("OpenAI", "❌ API Key 為空，請檢查 local.properties 設定")
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpenAI", "❌ 網路連線錯誤: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
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
                    Log.e("OpenAI", "❌ 錯誤 ${response.code}: $bodyStr")
                    callback(null)
                }
            }
        })
    }
}
