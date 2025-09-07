package tw.edu.pu.csim.refrigerator.feature.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RecipeListVM(
    private val repo: RecipeRepository = RecipeRepository()
) : ViewModel() {

    private val _all = MutableStateFlow<List<Recipe>>(emptyList())
    private val _featured = MutableStateFlow<List<Recipe>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _loading = MutableStateFlow(false)

    val featured: StateFlow<List<Recipe>> = _featured
    val query: StateFlow<String> = _query
    val loading: StateFlow<Boolean> = _loading

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        val bulk = repo.loadBulk(limit = 200)
        _all.value = bulk
        _featured.value = bulk.shuffled(Random(System.currentTimeMillis())).take(20) // 推薦 20 筆
        _loading.value = false
    }

    fun setQuery(q: String) { _query.value = q }

    fun filtered(): List<Recipe> {
        val q = _query.value.trim()
        if (q.isEmpty()) return _featured.value
        val lower = q.lowercase()
        return _all.value.filter { r ->
            r.title.lowercase().contains(lower) || r.ingredients.any { it.lowercase().contains(lower) }
        }.take(100)
    }
}
