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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
    var query by remember { mutableStateOf("") }

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
            r.title.lowercase().contains(q) || r.ingredients.any { it.lowercase().contains(q) }
        }.take(100)
    }

    // 狀態：LazyGrid + CoroutineScope
    val listState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // 搜尋欄
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(1000.dp))
                    .background(Color(0xFFD9D9D9))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search Icon",
                    modifier = Modifier.padding(end = 8.dp).size(22.dp),
                    tint = Color.Unspecified
                )
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜尋食譜") },
                    textStyle = TextStyle(color = Color(0xFF504848), fontSize = 15.sp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // SwipeRefresh 包住內容
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = {
                    viewModel.loadRecipes(force = true) // 強制刷新
                },
                modifier = Modifier.weight(1f), // 讓清單填滿
                indicator = { _, _ -> } // 不顯示 SwipeRefresh 的圈圈
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color(0xFFABB7CD), // 灰藍色
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
                                        navController.navigate("recipeDetailById/$encodedId"
                                        )
                                    }
                            ){
                                Column {
                                    // 圖片
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

                                    // 標題區塊
                                    val titleBoxHeight = with(LocalDensity.current) {
                                        (MaterialTheme.typography.bodyLarge.lineHeight * 2).toDp() + 16.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFEAEAEA))
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
                    }
                }
            }
        }
        //  一鍵回頂部按鈕
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