package tw.edu.pu.csim.refrigerator.model

data class ChatMessage(
    val role: String,          // "user" / "bot"
    val content: String,
    val type: String = "text", // "text" / "options" / "recommendations" / "recipeDetail"
    val timestamp: Long = System.currentTimeMillis(),
    val extra: String? = null  // 🔹 新增，用來存料理名稱
)
