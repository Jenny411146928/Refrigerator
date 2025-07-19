package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R
import ui.NotificationItem

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun IngredientScreen(
    foodList: MutableList<FoodItem>,
    navController: NavController,
    onEditItem: (FoodItem) -> Unit,
    cartItems: MutableList<FoodItem>,
    notifications: MutableList<NotificationItem>,
    fridgeId: String
) {
    val searchText = remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<FoodItem?>(null) }
    val selectedCategory = remember { mutableStateOf("全部") }
    val categoryList = listOf("全部", "肉類", "蔬菜", "水果", "海鮮", "自選")

    val filtered = foodList.filter {
        it.fridgeId == fridgeId &&
                it.name.contains(searchText.value.trim(), ignoreCase = true) &&
                (selectedCategory.value == "全部" || it.category == selectedCategory.value)
    }

    LaunchedEffect(foodList) {
        foodList.forEach { food ->
            if (food.fridgeId == fridgeId) {
                val title = when {
                    food.daysRemaining <= 2 -> "⚠️ 食材即將過期"
                    food.daysRemaining <= 4 -> "⏰ 食材保存期限提醒"
                    else -> null
                }
                title?.let {
                    val msg = "「${food.name}」只剩 ${food.daysRemaining} 天，請儘快使用！"
                    if (notifications.none { it.message == msg }) {
                        notifications.add(NotificationItem(it, msg))
                    }
                }
            }
        }
    }

    fun confirmDelete(item: FoodItem) {
        itemToDelete = item
        showDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp)
    ) {
        // 🔍 搜尋列
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(1000.dp))
                    .background(Color(0xFFD9D9D9))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.search),
                    contentDescription = "Search Icon",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp),
                    tint = Color.Unspecified
                )
                TextField(
                    value = searchText.value,
                    onValueChange = { searchText.value = it },
                    placeholder = { Text("請輸入想搜尋的食材") },
                    textStyle = TextStyle(color = Color(0xFF504848), fontSize = 15.sp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { navController.navigate("add") },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black, RoundedCornerShape(100))
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增", tint = Color.White)
            }
        }

        // 🔘 分類按鈕列
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            categoryList.forEach { category ->
                val isSelected = selectedCategory.value == category
                TextButton(
                    onClick = { selectedCategory.value = category },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) Color(0xFFD1DAE6) else Color(0xFFF0F0F0),
                        contentColor = if (isSelected) Color(0xFF444B61) else Color.DarkGray
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(category)
                }
            }
        }

        // 🍱 卡片列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            itemsIndexed(filtered) { index, item ->
                FoodCard(
                    item = item,
                    onDelete = { confirmDelete(item) },
                    onEdit = { onEditItem(item) }
                )
            }
        }
    }

    // 🗑️ 刪除對話框
    if (showDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                itemToDelete = null
            },
            title = { Text("刪除食材") },
            text = { Text("你要將「${itemToDelete!!.name}」加入購物車，還是直接刪除？") },
            confirmButton = {
                TextButton(onClick = {
                    cartItems.add(itemToDelete!!.copy(quantity = "1"))
                    foodList.remove(itemToDelete)
                    showDialog = false
                    itemToDelete = null
                }) {
                    Text("加入購物車")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    foodList.remove(itemToDelete)
                    showDialog = false
                    itemToDelete = null
                }) {
                    Text("直接刪除")
                }
            }
        )
    }
}
@Composable
fun FoodCard(
    item: FoodItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val progressColor = when {
        item.daysRemaining <= 2 -> Color(0xFFE53935)
        item.daysRemaining <= 4 -> Color(0xFFFF9432)
        else -> Color(0xFF22C97D)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFEDF1F9))
            .clickable { onEdit() }
            .padding(12.dp)
    ) {
        Column {
            Box {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(90.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .height(90.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F2F2))
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "編輯", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFFF2F3F8))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressPercent.coerceAtLeast(0.05f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(25.dp))
                        .background(progressColor)
                )
            }

            Text(
                text = item.dayLeft,
                fontSize = 12.sp,
                color = Color(0xFF9DA5C1),
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "到期日：${item.date}",
                fontSize = 13.sp,
                color = Color(0xFF9DA5C1),
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "數量：${item.quantity}",
                fontSize = 13.sp,
                color = Color(0xFF9DA5C1),
                modifier = Modifier.padding(top = 2.dp)
            )

            if (item.note.isNotBlank()) {
                Text(
                    text = "備註：${item.note}",
                    fontSize = 13.sp,
                    color = Color(0xFF9DA5C1),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            TextButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = Color.Gray)
            }
        }
    }
}