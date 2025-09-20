@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import tw.edu.pu.csim.refrigerator.R

// ==================== 冰箱卡片 ====================
@Composable
fun FridgeCard(
    fridge: FridgeCardData,
    onEdit: (FridgeCardData) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0E0E0))
    ) {
        // 背景圖片
        if (fridge.imageUri != null || fridge.imageRes != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fridge.imageUri ?: fridge.imageRes)
                    .crossfade(true)
                    .build(),
                contentDescription = fridge.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            // 半透明遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.White.copy(alpha = 0.35f))
            )

            // ID 文字
            Text(
                text = "ID：${fridge.id}",
                fontSize = 12.sp,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // 冰箱名稱（點擊可修改）
        Text(
            text = fridge.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 8.dp)
                .clickable { onEdit(fridge) }
        )
    }
}

// ==================== BottomNavigation ====================
@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.account), contentDescription = "Account") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.refrigerator), contentDescription = "Home") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.recommend), contentDescription = "Recommand") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.recipe), contentDescription = "Recipe") },
            selected = false,
            onClick = {}
        )
    }
}

@Composable
fun FridgeCardList(
    fridges: List<FridgeCardData>,
    onEdit: (FridgeCardData) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(fridges, key = { it.id }) { fridge ->
            FridgeCard(fridge = fridge, onEdit = onEdit)
        }
    }
}

// ==================== AppBar ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar() {
    TopAppBar(
        title = { Text("Refrigerator", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        actions = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(painterResource(R.drawable.cart), contentDescription = "購物車")
            }
        }
    )
}

// ==================== FrontPage ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrontPage() {
    var fridgeCards by remember {
        mutableStateOf(
            listOf(
                FridgeCardData(name = "蔡譯嫺's fridge", imageRes = R.drawable.refrigerator),
                FridgeCardData(name = "家庭冰箱", imageRes = R.drawable.refrigerator),
                FridgeCardData(name = "辦公室冰箱", imageRes = R.drawable.refrigerator)
            )
        )
    }

    var searchText by rememberSaveable { mutableStateOf("") }
    var editingFridge by rememberSaveable { mutableStateOf<FridgeCardData?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }

    val filteredFridges by remember(searchText, fridgeCards) {
        derivedStateOf {
            if (searchText.isBlank()) fridgeCards
            else fridgeCards.filter { it.name.contains(searchText, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = { AppBar() },
        bottomBar = { BottomNavigationBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 🔍 搜尋欄
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .fillMaxWidth()
                    .background(Color(0xFFD9D9D9))
                    .padding(vertical = 7.dp, horizontal = 13.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search Icon",
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .clip(RoundedCornerShape(1000.dp))
                        .size(24.dp),
                    tint = Color.Unspecified
                )

                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("搜尋冰箱", color = Color.Gray) },
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.Black, // 強制輸入文字黑色
                        fontSize = 15.sp
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color(0xFFD9D9D9),  // 跟外層背景一致
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // 🧊 冰箱卡片列表
            if (filteredFridges.isEmpty()) {
                Text(
                    "沒有找到符合的冰箱",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Gray
                )
            } else {
                filteredFridges.forEach { fridge ->
                    FridgeCardList(fridges = filteredFridges, onEdit = { selected ->
                        println("點擊到冰箱：${selected.name}")
                        editingFridge = selected
                        newName = selected.name
                    })
                }
            }
        }
    }

    // ✏️ 修改名稱對話框
    if (editingFridge != null) {
        AlertDialog(
            onDismissRequest = { editingFridge = null },
            title = { Text("修改冰箱名稱") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("輸入新名稱") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    fridgeCards = fridgeCards.map {
                        if (it.id == editingFridge!!.id) {
                            it.copy(name = newName)
                        } else it
                    }
                    editingFridge = null
                }) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFridge = null }) {
                    Text("取消")
                }
            }
        )
    }
}
