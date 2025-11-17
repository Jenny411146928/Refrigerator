package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateOf
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

    /** 新增：搜尋文字（跨頁記住） */
    var searchQuery = mutableStateOf("")

    /** 是否為使用者手動修改搜尋文字（避免返回時跳到頂部） */
    var isUserChangingQuery = mutableStateOf(false)

    /** featured 清單的滑動記憶 */
    val featuredState = LazyGridState()

    /** 搜尋清單的滑動記憶 */
    val searchState = LazyGridState()

    /** 新增：保留 LazyGrid 滑動位置 */
    val listState = LazyGridState()

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
                if (_featured.value.isEmpty()) {
                    _featured.value = list.shuffled().take(20)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
                onLoaded()
            }
        }
    }
}