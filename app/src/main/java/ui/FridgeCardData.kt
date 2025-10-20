package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri

data class FridgeCardData(
    val id: String = (100000..999999).random().toString(),
    val name: String = "",
    val ownerName: String? = null,
    val imageRes: Int? = null,
    val imageUri: Uri? = null,
    val imageUrl: String? = null // ✅ 一定要有這行
)
