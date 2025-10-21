package tw.edu.pu.csim.refrigerator

fun extractIngredientQuantity(text: String): Int {
    val match = Regex("(\\d+)").find(text)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
}

fun hasEnoughIngredient(ingredient: String, fridgeItems: List<FoodItem>): Boolean {
    val requiredQty = extractIngredientQuantity(ingredient)
    val ingredientName = ingredient.replace(Regex("\\d+"), "").trim()
    val matched = fridgeItems.find { it.name.contains(ingredientName, ignoreCase = true) }
    val fridgeQty = matched?.quantity?.toIntOrNull() ?: 0
    return matched != null && fridgeQty >= requiredQty
}