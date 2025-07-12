// ChatModels.kt
package tw.edu.pu.csim.refrigerator.model  // 請依照你實際使用的 package


data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)
