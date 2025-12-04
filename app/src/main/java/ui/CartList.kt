package tw.edu.pu.csim.refrigerator.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R

@Composable
fun CartPageScreen(
    navController: NavController,
    cartItems: MutableList<FoodItem>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ⭐ 新增：記錄被選取的項目（用 id）
    val selectedItems = remember { mutableStateListOf<String>() }

    // ⭐ 新增：單筆刪除確認 Dialog 對象
    var pendingSingleDeleteId by remember { mutableStateOf<String?>(null) }

    // ⭐ 新增：批次刪除 Dialog 顯示狀態
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val items = tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.getCartItems()

            // ⭐ 使用 item.id，解決重複讀取 + 刪錯食材
            val existingIds = cartItems.map { it.id }
            val newItems = items.filter { it.id !in existingIds }
            cartItems.addAll(newItems)

        } catch (e: Exception) {
            Toast.makeText(context, "載入購物清單失敗：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // ⭐ 新增：列表上方的「全選 / 刪除選取」列
            if (cartItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            selectedItems.clear()
                            selectedItems.addAll(cartItems.map { it.id })
                        }
                    ) {
                        Text(
                            text = "⭐ 全選",    // ⭐ 可愛的小勾勾框 icon
                            color = Color(0xFFABB7CD),   // 統一你的灰藍色系
                            fontSize = 16.sp
                        )
                    }


                    val hasSelection = selectedItems.isNotEmpty()

                    TextButton(
                        onClick = {
                            if (hasSelection) {
                                showBatchDeleteDialog = true
                            }
                        },
                        enabled = hasSelection
                    ) {
                        Text(
                            text = "刪除選取",
                            color = if (hasSelection) Color.Red else Color.Gray
                        )
                    }
                }
            }

            cartItems.forEachIndexed { index, item ->
                val isSelected = selectedItems.contains(item.id)

                CartItem(
                    name = item.name,
                    note = item.note,
                    imageUrl = item.imageUrl.ifBlank {
                        "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/1d7dab96-10ed-43d6-a0e9-9cb957a53673"
                    },
                    quantity = item.quantity.toIntOrNull() ?: 1,

                    // ⭐ 改成用 item.id 做更新與刪除
                    onQuantityChange = { newQty ->
                        if (newQty <= 0) {
                            scope.launch {
                                try {
                                    tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.deleteCartItem(item.id)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "刪除失敗：${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            cartItems.removeAt(index)

                        } else {

                            cartItems[index] = item.copy(quantity = newQty.toString())

                            scope.launch {
                                try {
                                    tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.updateCartQuantity(item.id, newQty)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    },

                    onDelete = {
                        // ⭐ 保留原本的刪除邏輯（目前改由外層 Dialog 呼叫）
                        scope.launch {
                            try {
                                tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.deleteCartItem(item.id)
                            } catch (_: Exception) {
                            }
                        }
                        cartItems.removeAt(index)
                    },

                    onEdit = { navController.navigate("edit_cart_item/$index") },

                    // ⭐ 新增：將選取狀態與勾選邏輯交給外層控制
                    isSelected = isSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (selectedItems.isEmpty()) {
                                // ✅ 單筆勾選，啟動「單筆刪除」流程
                                selectedItems.clear()
                                selectedItems.add(item.id)
                                pendingSingleDeleteId = item.id
                            } else {
                                // ✅ 已經有選取 → 視為批次選取的一部分
                                if (!selectedItems.contains(item.id)) {
                                    selectedItems.add(item.id)
                                }
                            }
                        } else {
                            // ✅ 取消選取
                            selectedItems.remove(item.id)
                        }
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("add_cart_ingredient") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 20.dp),
            containerColor = Color.Black,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
        }

        // ⭐ 單筆刪除確認視窗
        pendingSingleDeleteId?.let { deleteId ->
            AlertDialog(
                onDismissRequest = {
                    // 關閉視窗並取消選取
                    pendingSingleDeleteId = null
                    selectedItems.clear()
                },
                title = { Text(text = "確認刪除") },
                text = { Text(text = "確定要刪除此商品嗎？此操作無法復原。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val targetIndex = cartItems.indexOfFirst { it.id == deleteId }
                                if (targetIndex != -1) {
                                    val targetItem = cartItems[targetIndex]
                                    try {
                                        tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.deleteCartItem(targetItem.id)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "刪除失敗：${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    cartItems.removeAt(targetIndex)
                                }
                                selectedItems.clear()
                                pendingSingleDeleteId = null
                            }
                        }
                    ) {
                        Text(text = "刪除", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingSingleDeleteId = null
                            selectedItems.clear()
                        }
                    ) {
                        Text(text = "取消")
                    }
                }
            )
        }

        // ⭐ 批次刪除確認視窗
        if (showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showBatchDeleteDialog = false
                },
                title = { Text(text = "確認刪除") },
                text = {
                    Text(
                        text = "確定要刪除選取的 ${selectedItems.size} 項商品嗎？此操作無法復原。"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val idsToDelete = selectedItems.toList()
                                idsToDelete.forEach { id ->
                                    val targetIndex = cartItems.indexOfFirst { it.id == id }
                                    if (targetIndex != -1) {
                                        val targetItem = cartItems[targetIndex]
                                        try {
                                            tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.deleteCartItem(targetItem.id)
                                        } catch (_: Exception) {
                                        }
                                    }
                                }
                                cartItems.removeAll { it.id in idsToDelete }
                                selectedItems.clear()
                                showBatchDeleteDialog = false
                            }
                        }
                    ) {
                        Text(text = "刪除", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showBatchDeleteDialog = false
                        }
                    ) {
                        Text(text = "取消")
                    }
                }
            )
        }
    }
}

@Composable
fun CartItem(
    name: String,
    note: String,
    imageUrl: String,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    // ⭐ 新增：由外層控制的「是否被選取」狀態與變更回呼
    isSelected: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    var count by remember { mutableStateOf(quantity.coerceAtLeast(1)) }
    var checked by remember { mutableStateOf(isSelected) }
    var visible by remember { mutableStateOf(true) }

    // ⭐ 同步外部狀態到內部 checked
    LaunchedEffect(isSelected) {
        checked = isSelected
    }

    LaunchedEffect(count) { onQuantityChange(count) }

    // ⭐ 原本：勾選後自動刪除的邏輯改為 no-op，刪除改由外層 Dialog 控制
    LaunchedEffect(checked) {
        // 不再在這裡自動刪除，避免誤刪，刪除由外層確認視窗決定
    }

    AnimatedVisibility(visible = visible, exit = fadeOut()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (note.isNotBlank()) {
                    Text(text = "備註：$note", fontSize = 14.sp, color = Color.Gray)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE3E6ED))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = {
                            count = (count - 1)
                            if (count < 0) count = 0
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.remove),
                            contentDescription = "減少",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                    }

                    Spacer(Modifier.width(6.dp))
                    Text("$count", fontSize = 20.sp)
                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = { count++ },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "增加",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "編輯",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Checkbox(
                checked = checked,
                onCheckedChange = { newChecked ->
                    checked = newChecked
                    onCheckedChange(newChecked)
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
