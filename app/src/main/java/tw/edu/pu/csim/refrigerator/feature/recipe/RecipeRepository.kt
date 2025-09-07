package tw.edu.pu.csim.refrigerator.feature.recipe

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RecipeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val collection: String = "recipes"
) {
    suspend fun loadBulk(limit: Long = 200): List<Recipe> {
        val snap = db.collection(collection).limit(limit).get().await()
        return snap.documents.mapNotNull { it.toRecipe() }
    }

    suspend fun loadById(id: String): Recipe? {
        val doc = db.collection(collection).document(id).get().await()
        return doc.toRecipe()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toRecipe(): Recipe? {
        val title = getString("title") ?: return null
        val ingredients = (get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        return Recipe(
            id = id,
            title = title,
            ingredients = ingredients,
            link = getString("link") ?: "",
            source = getString("source") ?: "icook",
            imageUrl = getString("imageUrl")
        )
    }
}
