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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteRecipeScreen(
    navController: NavController,
    recipes: List<Triple<String, String, String?>>
) {
    var query by remember { mutableStateOf("") }
    var recipeList by remember { mutableStateOf(recipes) } // ✅ 新增：可即時更新列表
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    // ✅ 新增：從 Firebase 載入收藏食譜
    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val snapshot = db.collection("users").document(uid)
                    .collection("favorites").get().await()

                val fetched = snapshot.documents.map {
                    Triple(
                        it.id,
                        it.getString("title") ?: "",
                        it.getString("imageUrl")
                    )
                }

                recipeList = fetched
                android.util.Log.d("FavoriteRecipeScreen", "✅ 已從 Firebase 讀取收藏 ${fetched.size} 筆")
            } else {
                android.util.Log.e("FavoriteRecipeScreen", "❌ 尚未登入，無法載入收藏")
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteRecipeScreen", "❌ 載入收藏失敗: ${e.message}")
        }
    }

    // 🔹 過濾最愛食譜（沿用原本邏輯）
    val filtered = remember(query, recipeList) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) recipeList
        else recipeList.filter { (_, title, _) ->
            title.lowercase().contains(q)
        }
    }

    // LazyGrid 狀態與回頂部控制
    val listState = rememberLazyGridState()
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 } // 超過3張卡才顯示
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            val focusManager = LocalFocusManager.current
            // 搜尋欄（和食譜頁一樣）
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
                    placeholder = { Text("搜尋最愛食譜") },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                        }
                    ),
                    textStyle = TextStyle(color = Color(0xFF504848), fontSize = 15.sp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                // 清除按鈕（右側 X）
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除搜尋",
                            tint = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("尚未收藏任何食譜", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = listState, // 🔹 綁定 state，讓 FAB 能控制滾動
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.first }) { (id, title, imageUrl) ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .padding(6.dp)
                                .clickable {
                                    val encodedId = Uri.encode(id)
                                    navController.navigate("recipeDetail/$encodedId")   // ✅ 改這裡
                                }

                        ) {
                            Column {
                                // 圖片（統一高度）
                                AsyncImage(
                                    model = imageUrl ?: "https://i.imgur.com/zMZxU8v.jpg",
                                    contentDescription = title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                // 灰色標題框（固定高度，最多兩行字）
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
                                    Text(
                                        text = title,
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

        // 🔹 回頂部按鈕（與食譜頁一致）
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
