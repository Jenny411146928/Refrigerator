package tw.edu.pu.csim.refrigerator

import android.net.Uri

data class Ingredient(
    val name: String,
    var quantity: Int,
    val imageUri: Uri?,
    val note: String = ""
)
