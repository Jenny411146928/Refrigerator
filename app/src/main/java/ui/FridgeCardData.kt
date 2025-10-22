package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri

/**
 * ✅ 冰箱資料類別（加上 ownerId 與 editable）
 * 完全相容你原本的結構，不破壞任何欄位
 */
data class FridgeCardData(
    val id: String = (100000..999999).random().toString(),
    val name: String = "",
    val ownerName: String? = null,
    val imageRes: Int? = null,
    val imageUri: Uri? = null,
    val imageUrl: String? = null,
    val ownerId: String? = null, // 🔹 新增：冰箱擁有者 ID
    val editable: Boolean = true // 🔹 新增：是否可編輯（好友冰箱 false）
)
