package tw.edu.pu.csim.refrigerator.model

data class ChatMessage(
    var role: String = "",
    var content: String = "",
    var type: String = "text",
    var timestamp: Long = System.currentTimeMillis(),
    var tab: String = "all" // ✅ 新增：訊息所屬分頁（fridge / recipe / all）
) {
    // Firestore 用的空建構子
    constructor() : this("", "", "text", System.currentTimeMillis(), "all")
}