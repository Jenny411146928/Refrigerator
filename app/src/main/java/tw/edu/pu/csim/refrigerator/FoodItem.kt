package tw.edu.pu.csim.refrigerator

import java.util.UUID

data class FoodItem(
    val name: String,
    val date: String,
    val quantity: String,
    val note: String,
    val imageUrl: String,
    val dayLeft: String = "",
    val daysRemaining: Int,
    val progressPercent: Float = 0.0f,
    val id: String = UUID.randomUUID().toString()
)
