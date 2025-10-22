/*@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
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

/*@Composable
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
}*/

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

/* ==================== FrontPage ====================
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
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("搜尋冰箱") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50.dp)),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = "Search Icon",
                        tint = Color.Gray
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color(0xFFF2F2F2),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color.Black
                )
            )

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
}*/



@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FrontPage(
    fridgeList: List<FridgeCardData>,
    onAddFridge: (FridgeCardData) -> Unit,
    onDeleteFridge: (FridgeCardData) -> Unit,
    onFridgeClick: (String) -> Unit,
    navController: NavController
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var searchText by remember { mutableStateOf("") }

    // 🔹 分類：主冰箱 vs 好友冰箱
    val myFridges = fridgeList.filter { it.ownerId == currentUserId }
    val friendFridges = fridgeList.filter { it.ownerId != currentUserId }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔍 搜尋框
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("搜尋冰箱") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(50.dp)),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search Icon",
                    tint = Color.Gray
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF2F2F2),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // ======================== ⭐ 主冰箱 ========================
        Text(
            text = "⭐ 主冰箱",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A6B87),
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
        )

        if (myFridges.isEmpty()) {
            // 🧊 沒有主冰箱 → 顯示灰色新增框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD9D9D9))
                    .height(180.dp)
                    .clickable { navController.navigate("addfridge") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增主要冰箱",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            // ✅ 顯示主冰箱卡片
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                myFridges.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onFridgeClick(fridge.id) }
                    ) {
                        FridgeCard(fridge)
                        // 標籤
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color(0xFFFFD700), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("主", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        // 刪除按鈕
                        TextButton(
                            onClick = { onDeleteFridge(fridge) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text("刪除", color = Color.Red)
                        }
                    }
                }
            }
        }

        // ======================== 👥 好友冰箱 ========================
        Text(
            text = "👥 好友冰箱",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A6B87),
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
        )

        if (friendFridges.isEmpty()) {
            Text(
                text = "目前沒有好友冰箱",
                color = Color.Gray,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                friendFridges.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onFridgeClick(fridge.id) }
                    ) {
                        FridgeCard(fridge)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color(0xFFB0C4DE), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("好友", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
*/

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
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
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FrontPage(
    fridgeList: List<FridgeCardData>,
    onAddFridge: (FridgeCardData) -> Unit,
    onDeleteFridge: (FridgeCardData) -> Unit,
    onFridgeClick: (String) -> Unit,
    navController: NavController
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var searchText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<FridgeCardData?>(null) } // ✅ 控制彈出對話框

    val myFridges = fridgeList.filter { it.ownerId == currentUserId }
    val friendFridges = fridgeList.filter { it.ownerId != currentUserId }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔍 搜尋框
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("搜尋冰箱") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(50.dp)),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search Icon",
                    tint = Color.Gray
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF2F2F2),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // ======================== ⭐ 主冰箱 ========================
        Text(
            text = "⭐ 主冰箱",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A6B87),
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
        )

        if (myFridges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD9D9D9))
                    .height(180.dp)
                    .clickable { navController.navigate("addfridge") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增主要冰箱",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                myFridges.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onFridgeClick(fridge.id) }
                    ) {
                        FridgeCard(fridge)

                        // ⭐ 標籤
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color(0xFFFFD700), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("主", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 🗑️ 垃圾桶刪除 ICON（右下角）
                        IconButton(
                            onClick = { showDeleteConfirm = fridge },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "刪除冰箱",
                                tint = Color.Red,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // ======================== 👥 好友冰箱 ========================
        Text(
            text = "👥 好友冰箱",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A6B87),
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
        )

        if (friendFridges.isEmpty()) {
            Text(
                text = "目前沒有好友冰箱",
                color = Color.Gray,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                friendFridges.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onFridgeClick(fridge.id) }
                    ) {
                        FridgeCard(fridge)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color(0xFFB0C4DE), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("好友", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ⚠️ 刪除前警告對話框
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("確認刪除") },
            text = { Text("確定要刪除此冰箱嗎？此動作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFridge(showDeleteConfirm!!)
                    showDeleteConfirm = null
                }) {
                    Text("刪除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
    }
}
