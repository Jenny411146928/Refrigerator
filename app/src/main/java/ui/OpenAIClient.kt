package tw.edu.pu.csim.refrigerator.ui

import android.util.Log
import okhttp3.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import tw.edu.pu.csim.refrigerator.BuildConfig
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import tw.edu.pu.csim.refrigerator.model.ChatResponse

object OpenAIClient {
    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private val API_KEY = BuildConfig.OPENAI_API_KEY

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
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpenAIClient", "Request failed: ${e.message}", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                Log.d("OpenAIClient", "Response code: ${response.code}")
                Log.d("OpenAIClient", "Response body: $bodyString")

                if (response.isSuccessful && bodyString != null) {
                    try {
                        val chatResponse = gson.fromJson(bodyString, ChatResponse::class.java)
                        val reply = chatResponse.choices.firstOrNull()?.message?.content
                        callback(reply)
                    } catch (e: Exception) {
                        Log.e("OpenAIClient", "JSON parse error: ${e.message}", e)
                        callback(null)
                    }
                } else {
                    Log.e("OpenAIClient", "Unsuccessful response: ${response.code}")
                    callback(null)
                }
            }
        })
    }
}
