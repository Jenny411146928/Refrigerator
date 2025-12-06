package ui

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// ✅ 統一：料理卡片資料結構與解析方法
data class UiRecipe(
    var name: String,
    var ingredients: MutableList<String>,
    var steps: MutableList<String>,
    var imageUrl: String? = null,
    var servings: String? = null,
    var totalTime: String? = null,
    var id: String? = null,
    val cuisine: String? = null,
    val method: String? = null,
    val mainIngredient: String? = null,
    val dishType: String? = null

)


private const val RECIPE_SEP = "§§"
private const val PART_SEP = "|||"
private const val STEP_SEP = "~~"
private const val ING_SEP = ","
/** ✅ 將 ISO 8601 時間（PT15M / PT1H30M）轉換成人類可讀格式 */
fun formatRecipeDuration(raw: String?): String {
    if (raw.isNullOrBlank()) return "未提供"
    val regex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?""")
    val match = regex.find(raw) ?: return raw
    val hours = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
    val minutes = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
    return when {
        hours > 0 && minutes > 0 -> "${hours}小時${minutes}分鐘"
        hours > 0 -> "${hours}小時"
        minutes > 0 -> "${minutes}分鐘"
        else -> "未提供"
    }
}

/** 🧩 編碼料理清單為字串（Firestore、GPT 共用） */
fun encodeRecipeCards(recipes: List<UiRecipe>): String =
    recipes.joinToString(RECIPE_SEP) { r ->
        "${r.name}$PART_SEP${r.ingredients.joinToString(ING_SEP)}$PART_SEP${r.steps.joinToString(STEP_SEP)}"
    }

/** 🧩 嘗試解析 GPT 回覆或 Firestore 中的字串 */
fun decodeOrParseRecipeCards(content: String): List<UiRecipe> {
    Log.e("RecipeDebug", "🟦 GPT 回傳原始 content：\n$content")
    if (content.isBlank()) return emptyList()

    // ✅ 優先嘗試 JSON 格式
    try {
        val json = Json { ignoreUnknownKeys = true }
        val jsonRecipes = json.decodeFromString<List<JsonRecipe>>(content)
        if (jsonRecipes.isNotEmpty()) {
            Log.d("RecipeParser", "✅ 以 JSON 格式成功解析 ${jsonRecipes.size} 道食譜")
            return jsonRecipes.map {
                UiRecipe(
                    name = it.title,
                    ingredients = it.ingredients.toMutableList(),
                    steps = it.steps.toMutableList(),
                    imageUrl = it.imageUrl,
                    servings = it.yield,
                    totalTime = formatRecipeDuration(it.time)  // ✅ 套用轉換函式
                )
            }

        }
    } catch (e: Exception) {
        Log.w("RecipeParser", "⚠️ JSON 解析失敗 (${e.message})，改用文字模式")
    }

    return runCatching { decodeRecipeCards(content) }.getOrNull().orEmpty().ifEmpty {
        parseRecipesFlexible(content)
    }
}

// ✅ JSON 結構對應
@Serializable
data class JsonRecipe(
    val title: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val imageUrl: String? = null,
    val yield: String? = null,
    val time: String? = null
)

/** 舊格式解碼 */
private fun decodeRecipeCards(content: String): List<UiRecipe> {
    if (content.isBlank()) return emptyList()
    return content.split(RECIPE_SEP).mapNotNull { block ->
        val parts = block.split(PART_SEP)
        val name = parts.getOrNull(0)?.trim().orEmpty()
        val ings = parts.getOrNull(1)?.split(ING_SEP)?.map { it.trim() } ?: emptyList()
        val steps = parts.getOrNull(2)?.split(STEP_SEP)?.map { it.trim() } ?: emptyList()
        if (name.isBlank()) null else UiRecipe(name, ings.toMutableList(), steps.toMutableList())
    }
}

/** ✅ GPT 彈性解析：終極穩定修正版（不需動 UI） */
private fun parseRecipesFlexible(raw: String): List<UiRecipe> {
    if (raw.isBlank()) return emptyList()
    val blocks = raw.split(Regex("==料理|【名稱】|【食材】")).filter { it.isNotBlank() }

    return blocks.mapIndexed { i, text ->
        val name = Regex("【名稱】[:：]?\\s*(.+?)\\n").find(text)?.groupValues?.get(1)?.trim()
            ?: "未命名料理 $i"

        val ingredients = Regex("【食材】[:：]?(.*?)【步驟】", RegexOption.DOT_MATCHES_ALL)
            .find(text)?.groupValues?.get(1)
            ?.split("、", "，", ",", "\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toMutableList()
            ?: mutableListOf("（AI 未提供內容）")

        // ✅ 提取步驟文字
        var stepText = Regex("【步驟】[:：]?(.*)", RegexOption.DOT_MATCHES_ALL)
            .find(text)?.groupValues?.get(1)?.trim() ?: ""

        // ✅ 統一全形 → 半形（例如 １→1、．→.）
        stepText = stepText.replace(Regex("[０-９]")) {
            (it.value[0] - '０' + '0'.code).toChar().toString()
        }.replace('．', '.').replace('、', '.').replace('）', ')')

        // ✅ 分割步驟並去除所有多餘編號
        val steps = stepText
            .split(Regex("\\n+|[0-9]+[\\.．、\\)]\\s*")) // 按換行或數字加點切割
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { s ->
                s.replace(Regex("^[0-9]+[\\.．、\\)]\\s*"), "")  // 去掉開頭數字
                    .replace(Regex("^\\p{Punct}+"), "")             // 去掉多餘標點
                    .trim()
            }
            .toMutableList()
            .ifEmpty { mutableListOf("（AI 未提供步驟）") }

        UiRecipe(name, ingredients, steps)
    }
}