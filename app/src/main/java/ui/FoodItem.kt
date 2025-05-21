package ui

import android.net.Uri

data class FoodItem(
    val name: String,
    var quantity: String = "1", // 注意：購物車會用到數量加減
    var note: String = "",
    var imageUri: Uri? = null,
    var imageUrl: String = "",

    // 以下是 AddRefIngredient 頁面會用到的欄位
    var date: String = "",
    var daysRemaining: Int = 0,
    var dayLeft: String = "",
    var progressPercent: Float = 0f
)
