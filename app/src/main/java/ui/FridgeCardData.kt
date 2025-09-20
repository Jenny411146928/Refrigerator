package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri

data class FridgeCardData(
    val id: String = java.util.UUID.randomUUID().toString(), // ✅ 每個冰箱自動生成唯一 ID
    val name: String,
    val imageRes: Int? = null,
    val imageUri: Uri? = null
)
