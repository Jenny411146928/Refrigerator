package tw.edu.pu.csim.refrigerator

import android.net.Uri

data class FoodItem(
    val name: String = "",
    val quantity: String = "",
    val note: String = "",
    val imageUri: Uri? = null,
    val imageUrl: String = "",
    val date: String = "",
    val daysRemaining: Int = 0,
    val dayLeft: String = "",
    val progressPercent: Float = 0f,
    var fridgeId: String = "" ,
    val category: String = ""

)
