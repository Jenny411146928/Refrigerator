package tw.edu.pu.csim.refrigerator.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class ChatMessage(
    val role: String, // "user" 或 "assistant"
    val content: String,
    val visible: MutableState<Boolean> = mutableStateOf(false)
)
