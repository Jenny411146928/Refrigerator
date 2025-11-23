package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListPage(
    navController: NavController,
    viewModel: RecipeViewModel = viewModel()
) {
    var query by viewModel.searchQuery

    // 🔹 用 collectAsState 觀察 StateFlow
    val loading by viewModel.loading.collectAsState()
    val all by viewModel.all.collectAsState()
    val featured by viewModel.featured.collectAsState()

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = loading)

    // 第一次進來載入
    LaunchedEffect(Unit) {
        viewModel.loadRecipes()
    }

    // 🔹 搜尋邏輯
    val items = remember(query, featured, all) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) featured
        else all.filter { r ->
            r.title.lowercase().contains(q) ||
                    r.ingredients.any { it.lowercase().contains(q) }
        }.take(100)
    }

    // 狀態：LazyGrid + CoroutineScope
    val listState =
        if (query.isBlank()) viewModel.featuredState
        else viewModel.searchState

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(query) {
        if (viewModel.isUserChangingQuery.value) {
            if (query.isNotBlank()) {
                coroutineScope.launch {
                    viewModel.searchState.scrollToItem(0)
                }
            }

            if (query.isBlank()) {
                coroutineScope.launch {
                    viewModel.featuredState.scrollToItem(0)
                }
            }
        }
        // Reset flag
        viewModel.isUserChangingQuery.value = false
    }


    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            val focusManager = LocalFocusManager.current
            // ⭐ 搜尋欄（與首頁一致的搜尋框）
            OutlinedTextField(
                value = query,
                onValueChange = {
                    viewModel.isUserChangingQuery.value = true
                    query = it
                },

                placeholder = {
                    Text(
                        "搜尋食譜",
                        color = Color(0xFF6D6D6D),
                        fontSize = 16.sp
                    )
                },

                singleLine = true,

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(40.dp)),

                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(22.dp)
                    )
                },

                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.isUserChangingQuery.value = true
                                query = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除搜尋",
                                tint = Color.Gray
                            )
                        }
                    }
                },

                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color(0xFFF2F2F2),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color(0xFF424242),
                    unfocusedTextColor = Color(0xFF424242),
                    focusedPlaceholderColor = Color(0xFF9E9E9E),
                    unfocusedPlaceholderColor = Color(0xFF9E9E9E)
                ),

                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.isUserChangingQuery.value = true
                    }
                )
            )

            Spacer(Modifier.height(8.dp))

            // SwipeRefresh 包住內容
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    viewModel.loadRecipes(force = true)
                },
                modifier = Modifier.weight(1f),
                indicator = { _, _ -> }
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color(0xFFABB7CD),
                            strokeWidth = 4.dp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = listState,
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(items, key = { it.id }) { recipe ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clickable {
                                        val encodedId = Uri.encode(recipe.id)
                                        navController.navigate("recipeDetail/$encodedId")
                                    }
                            ) {
                                Column {
                                    AsyncImage(
                                        model = recipe.imageUrl
                                            ?: "https://i.imgur.com/zMZxU8v.jpg",
                                        contentDescription = recipe.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp
                                                )
                                            ),
                                        contentScale = ContentScale.Crop
                                    )

                                    val titleBoxHeight = with(LocalDensity.current) {
                                        (MaterialTheme.typography.bodyLarge.lineHeight * 2).toDp() + 16.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF2F2F2))
                                            .height(titleBoxHeight)
                                            .padding(8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        val cleanTitle = recipe.title.substringBefore(" by ").trim()
                                        Text(
                                            text = cleanTitle,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "※ 食譜資訊來源：愛料理 iCook",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 16.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showButton,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = Color(0xFFABB7CD)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "回到頂部",
                    tint = Color.White
                )
            }
        }
    }
}