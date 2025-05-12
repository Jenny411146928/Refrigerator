package tw.edu.pu.csim.refrigerator

import android.net.Uri

data class Ingredient(
    val name: String,
    val quantity: Int,
    val note: String,
    val imageUri: Uri?
)