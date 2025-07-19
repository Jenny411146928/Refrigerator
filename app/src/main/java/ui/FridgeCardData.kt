package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import java.util.UUID

data class FridgeCardData(
    val id: String = (1000000..9999999).random().toString(),
    val name: String,
    val imageRes: Int? = null,
    val imageUri: Uri? = null
)
