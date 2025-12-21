package tw.edu.pu.csim.refrigerator.openai

import android.util.Log
import com.github.houbb.opencc4j.util.ZhConverterUtil
import okhttp3.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import tw.edu.pu.csim.refrigerator.BuildConfig
import tw.edu.pu.csim.refrigerator.FoodItem
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
    @SerializedName("message") val message: OpenAIMessage
)

data class OpenAIMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)


data class AIIntentResult(
    val intent: String,
    val include: List<String> = emptyList(),
    val avoid: List<String> = emptyList(),
    val cuisine: String? = null,
    val style: String? = null,
    val spiciness: String? = null,
    val reply: String? = null
)

object OpenAIClient {

  
    private const val MODEL = "gpt-3.5-turbo"

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
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()


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
                    "bot" -> "assistant"   
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

                        callback(cleaned?.let { normalizeTaiwan(it) })
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
所有食材名稱請使用台灣常用詞彙（例：馬鈴薯，不用土豆；花椰菜，不用西蘭花；青蔥，不用小蔥）。
並且永遠使用繁體中文，不得出現任何簡體字。

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
你是一個語意解析器，只負責「將使用者輸入轉換成乾淨、可被 JSON 解析的格式」。  
禁止聊天、禁止補充、禁止加任何一句多餘的話、禁止給例子、禁止猜測、禁止Markdown。

你的輸出格式永遠只能是：

{
  "intent": "find_recipe" | "chat" | "ask",
  "include": [字串...],
  "avoid": [字串...],
  "cuisine": 字串 或 null,
  "style": 字串 或 null,
  "spiciness": "mild" | "spicy" | null,
  "reply": 字串 或 null
}

⚠ 規則（務必遵守，不能違反）：

1. intent 判斷：
   - 若使用者輸入與料理完全無關 → intent = "chat"，並填 reply（只能一句，不能多）。
   - 若使用者有在找料理但資訊不足 → intent = "ask"，reply 需提示要更多描述。
   - 只要確定是在找料理 → intent = "find_recipe"。

2. include：輸入中出現的食材、料理名稱、關鍵字，只能列原字串，不能重寫、不能發揮。

3. avoid：只放使用者明確說「不要、排除、不吃」的詞。

4. cuisine：只能是單一類別（台式、日式、西式、韓式、中式、美式…）  
   若沒有就回 null，不能回多個。

5. style：健康、減脂、高蛋白、清爽、家常、油炸、湯類… 若沒有就回 null。

6. spiciness：
   - 若看到「辣、微辣、辣一點」→ spicy
   - 若看到「不辣、無辣、給小孩吃」→ mild
   - 其他 → null

7. reply：
   - intent = chat → reply 需要（只能一句自然的話）
   - intent = ask → reply 需要（只能一句提示性問題）
   - intent = find_recipe → reply = null

8. 禁止：
   ❌ 不可以多講一句話  
   ❌ 不可以加 Markdown  
   ❌ 不可以加 ```  
   ❌ 不可以加解釋  
   ❌ 不可以加範例  
   ❌ 不可以轉換語氣  
   ❌ 不可以介紹食材  
   ❌ 不可以亂補文字  
   ❌ 不可以生成跟 JSON 無關的內容  

只要回傳乾淨、單純、可以被解析的 JSON。
""".trimIndent()
        )


        val user = ChatMessage(role = "user", content = userInput)

        val bodyJson = Gson().toJson(
            mapOf(
                "model" to MODEL,
                "temperature" to 0.3, 
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
                   
                    val cleaned = raw
                        .removePrefix("```json")
                        .removePrefix("```JSON")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val result = gson.fromJson(cleaned, AIIntentResult::class.java)
                    callback(result)
                } catch (e: Exception) {
                    Log.e(
                        "OpenAI",
                        "❌ analyzeUserIntent JSON 解析錯誤: ${e.message}\nbody=$bodyStr"
                    )
                    callback(null)
                }
            }
        })
    }
    suspend fun detectFoodFromImage(base64Image: String): FoodDetectResult? {

        val requestJson = """
    {
      "model": "gpt-4o-mini",
      "input": [
        {
          "role": "user",
          "content": [
            {
              "type": "input_text",
              "text": "請辨識圖片中的食材，只回傳 JSON：{\"name\":\"食材名\",\"category\":\"蔬菜/水果/肉類/海鮮/其他\"}"
            },
            {
              "type": "input_image",
              "image_url": "data:image/jpeg;base64,$base64Image"
            }
          ]
        }
      ]
    }
    """.trimIndent()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            Log.e("VisionRaw", body)

         
            val root = JSONObject(body)
            val outputArray = root.optJSONArray("output") ?: return null

            
            var outputText: String? = null

            for (i in 0 until outputArray.length()) {
                val outputItem = outputArray.getJSONObject(i)
                val contentArray = outputItem.optJSONArray("content") ?: continue

                for (j in 0 until contentArray.length()) {
                    val contentItem = contentArray.getJSONObject(j)
                    if (contentItem.optString("type") == "output_text") {
                        outputText = contentItem.optString("text")
                        break
                    }
                }
            }

            if (outputText == null) return null

            
            val cleaned = outputText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.e("VisionCleaned", cleaned)

            return gson.fromJson(cleaned, FoodDetectResult::class.java)

        } catch (e: Exception) {
            Log.e("Vision", "❌ detectFoodFromImage 錯誤：${e.message}")
            null
        }
    }


    private val ingredientCache = mutableMapOf<Pair<String, String>, Boolean>()

    fun isSameIngredientAI(
        ownedName: String,
        recipeName: String,
        callback: (Boolean) -> Unit
    ) {
        val key = ownedName to recipeName
        ingredientCache[key]?.let {
            callback(it)
            return
        }

        
        val cleanOwned = ownedName
            .replace(Regex("[\\(（\\[\\{].*?[\\)）\\]\\}]"), "") 
            .replace(Regex("^\\[.*?\\]"), "")                   
            .replace(Regex("\\d+[\\u4e00-\\u9fa5a-zA-Z]*"), "")
            .replace(Regex("(少許|適量|些許|一點點|適可而止)"), "")
            .replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z]"), "")
            .trim()

        val cleanRecipe = recipeName
            .replace(Regex("[\\(（\\[\\{].*?[\\)）\\]\\}]"), "")
            .replace(Regex("^\\[.*?\\]"), "")
            .replace(Regex("\\d+[\\u4e00-\\u9fa5a-zA-Z]*"), "")
            .replace(Regex("(少許|適量|些許|一點點|適可而止)"), "")
            .replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z]"), "")
            .trim()


        val commonChars = cleanOwned.toSet().intersect(cleanRecipe.toSet())
        if (commonChars.isEmpty() && cleanOwned.length > 2 && cleanRecipe.length > 2) {

            callback(false)
            return
        }


        val shortWordExceptions = listOf("蔥", "青蔥", "大蔥", "蔥花", "細蔥", "三星蔥", "宜蘭蔥")


        val synonymGroups = listOf(
            listOf("蔥", "青蔥", "大蔥", "蔥花", "細蔥", "三星蔥", "宜蘭蔥"),
            listOf("番茄", "蕃茄"),
            listOf("胡蘿蔔", "紅蘿蔔"),
            listOf("白蘿蔔", "蘿蔔"),
            listOf("地瓜", "番薯"),
            listOf("馬鈴薯", "洋芋"),
            listOf("香菇", "冬菇", "乾香菇"),
            listOf("蛋", "雞蛋", "土雞蛋", "新鮮雞蛋"),
            listOf("牛奶", "鮮奶"),
            listOf("糖", "白糖", "砂糖"),
            listOf("鹽", "鹽巴", "食鹽"),
            listOf("白米", "米", "生米"),
            listOf("糯米", "糯米飯"),
            listOf("豆干", "豆乾"),
            listOf("蒜", "大蒜", "蒜頭", "蒜末", "蒜泥", "蒜粒", "蒜蓉"),
            listOf("薑", "老薑", "嫩薑", "薑絲", "薑末", "薑片", "薑蓉"),
            listOf("番茄醬", "蕃茄醬"),
            listOf("沙茶醬", "沙茶")
        )

        if ((cleanOwned.length <= 2 || cleanRecipe.length <= 2)
            && cleanOwned != cleanRecipe
            && !(shortWordExceptions.contains(cleanOwned) && shortWordExceptions.contains(cleanRecipe))
            && !(synonymGroups.any { it.contains(cleanOwned) && it.contains(cleanRecipe) })
        ) {

            callback(false)
            return
        }


        if (kotlin.math.abs(cleanOwned.length - cleanRecipe.length) >= 3 && commonChars.size <= 1) {
            callback(false)
            return
        }


        val seasoningKeywords = listOf("水", "鹽", "鹽巴", "糖", "油", "醬油", "胡椒", "味精", "醋", "酒", "米酒", "香油", "麻油", "辣椒")
        if (seasoningKeywords.contains(cleanOwned) && seasoningKeywords.contains(cleanRecipe) && cleanOwned != cleanRecipe) {
            callback(false)
            return
        }


        val tooShort = cleanOwned.length <= 1 || cleanRecipe.length <= 1
        val trivialWords = listOf("水", "油", "鹽", "糖", "醋", "粉", "汁")


        val alwaysAllowGroups = listOf("蔥", "青蔥", "大蔥", "蔥花", "細蔥")

        if (!tooShort &&
            (cleanOwned.contains(cleanRecipe) || cleanRecipe.contains(cleanOwned)) &&
            (

                    (!trivialWords.contains(cleanOwned) && !trivialWords.contains(cleanRecipe)) ||
                            (alwaysAllowGroups.contains(cleanOwned) && alwaysAllowGroups.contains(cleanRecipe))
                    )
        ) {
            callback(true)
            return
        }

        if (synonymGroups.any { group ->
                group.contains(cleanOwned) && group.contains(cleanRecipe)
            }) {
            callback(true)
            return
        }


        val prompt = """
        判斷以下兩個食材名稱是否表示同一種食材：
        1. 冰箱食材：$cleanOwned
        2. 食譜食材：$cleanRecipe
    
        請根據「是否為同一原料」做出嚴謹判斷，而不是單純語意相似。

        ✅ 請回答「是」僅在以下情況：
        - 它們是同一原料或同一食物（例如「青蔥」與「蔥」、「糖」與「砂糖」、「雞蛋」與「新鮮雞蛋」）。
        - 或只是描述性形容詞不同（如「新鮮雞蛋」與「雞蛋」）。
            
        🚫 請回答「否」若出現以下情況：
        - 不同品種、部位、部件（如「雞胸肉」與「雞腿肉」、「牛肉」與「牛絞肉」）。
        - 加工狀態不同（如「辣椒」與「乾辣椒」、「蒜頭」與「蒜粉」、「豆腐」與「豆干」）。
        - 完全不同原料或味道（如「水」與「水梨」、「醬油」與「味噌」、「鹽」與「糖」）。
            
        ⚠️ 只回答「是」或「否」，不要包含其他文字或解釋。
        """.trimIndent()

        val messages = listOf(ChatMessage("user", prompt))
        askChatGPT(messages) { result ->
            val isSame = result?.trim()?.startsWith("是") == true
            ingredientCache[key] = isSame
            callback(isSame)
        }
    }
    fun normalizeTaiwan(text: String): String {

        var t = text


        t = ZhConverterUtil.toTraditional(t)

        val replaceMap = mapOf(
            "西兰花" to "花椰菜",
            "西蘭花" to "花椰菜",

            "胡萝卜" to "紅蘿蔔",

            "土豆" to "馬鈴薯",
            "洋芋" to "馬鈴薯",

            "鸡蛋" to "雞蛋",
            "猪肉" to "豬肉",

            "洋葱" to "洋蔥",

            "小葱" to "青蔥",
            "大葱" to "青蔥",
            "香葱" to "青蔥"
        )

        replaceMap.forEach { (cn, tw) ->
            t = t.replace(cn, tw)
        }

        return t
    }


    data class ResponseC(
        val choices: List<ResponseChoice>?
    )

    data class ResponseChoice(
        val message: ResponseMessage?
    )

    data class ResponseMessage(
        val content: List<ResponseContent>?
    )

    data class ResponseContent(
        val type: String?,
        val text: String?
    )

    data class FoodDetectResult(
        val name: String,
        val category: String
    )


    data class ResponseNew(
        val output: List<ResponseOutput>?
    )

    data class ResponseOutput(
        val type: String?,
        val text: String?
    )

}