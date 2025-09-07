package tw.edu.pu.csim.refrigerator.feature.recipe

data class Recipe(
    val id: String = "",
    val title: String = "",
    val ingredients: List<String> = emptyList(),
    val link: String = "",
    val source: String = "icook",
    val imageUrl: String? = null
)
