package tw.edu.pu.csim.refrigerator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 食譜卡片資料類
data class RecipeCardItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val ingredients: List<String>
)

class RecipeViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _all = MutableStateFlow<List<RecipeCardItem>>(emptyList())
    val all: StateFlow<List<RecipeCardItem>> = _all

    private val _featured = MutableStateFlow<List<RecipeCardItem>>(emptyList())
    val featured: StateFlow<List<RecipeCardItem>> = _featured

    /** 從 Firestore 載入食譜 */
    fun loadRecipes(force: Boolean = false, onLoaded: () -> Unit = {}) {
        // 如果已經有資料，且不是強制刷新，就直接回傳
        if (!force && _all.value.isNotEmpty()) {
            _loading.value = false
            onLoaded()
            return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                val db = FirebaseFirestore.getInstance()
                val snap = db.collection("recipes").limit(200).get().await()
                val list = snap.documents.mapNotNull { d ->
                    val title = d.getString("title") ?: return@mapNotNull null
                    val img = d.getString("imageUrl")
                    @Suppress("UNCHECKED_CAST")
                    val ingredients = (d.get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    RecipeCardItem(id = d.id, title = title, imageUrl = img, ingredients = ingredients)
                }
                _all.value = list
                _featured.value = list.shuffled().take(20)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
                onLoaded()
            }
        }
    }
}