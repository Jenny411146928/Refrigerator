package tw.edu.pu.csim.refrigerator

import android.net.Uri
import java.util.UUID

data class FoodItem(
    val id: String = UUID.randomUUID().toString(),

    // ⭐⭐⭐ 新增欄位（依你排序功能所需，完全不影響既有資料）
    val createdAt: Long = System.currentTimeMillis(),

    val createdTime: Long = System.currentTimeMillis(),
    val name: String = "",
    val quantity: String = "",
    val note: String = "",
    val imageUri: Uri? = null,
    val imageUrl: String = "",
    val date: String = "",
    val daysRemaining: Int = 0,
    val dayLeft: String = "",
    val progressPercent: Float = 0f,
    var fridgeId: String = "",
    val category: String = "",
    var storageType: String = ""
)
