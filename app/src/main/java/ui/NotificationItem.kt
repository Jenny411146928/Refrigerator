package ui

import java.util.UUID

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(), // 唯一識別碼
    val title: String,
    val message: String,
    val targetName: String = "",   // 預設空字串
    val daysLeft: Int = 0,         // 預設 0
    val imageUrl: String? = null   // 可選
)
