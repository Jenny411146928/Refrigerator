package tw.edu.pu.csim.refrigerator.model


data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)
