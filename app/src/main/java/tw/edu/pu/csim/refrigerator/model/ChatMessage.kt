package tw.edu.pu.csim.refrigerator.model

data class ChatMessage(
    var role: String = "",
    var content: String = "",
    var type: String = "text",
    var timestamp: Long = System.currentTimeMillis(),
    var tab: String = "all"
) {
    constructor() : this("", "", "text", System.currentTimeMillis(), "all")
}