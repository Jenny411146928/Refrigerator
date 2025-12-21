package ui

import java.util.UUID

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val targetName: String = "",
    val daysLeft: Int = 0,
    val imageUrl: String? = null,
    val fridgeId: String = ""
)
