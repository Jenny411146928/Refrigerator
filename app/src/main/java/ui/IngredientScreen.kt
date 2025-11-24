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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.firebase.FirebaseManager
import ui.NotificationItem
import java.text.SimpleDateFormat
import java.util.*

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
    val foodListState = remember { mutableStateListOf<FoodItem>() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<FoodItem?>(null) }
    val searchText = remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf("全部") }

    var ingredientRef by remember { mutableStateOf<com.google.firebase.firestore.CollectionReference?>(null) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    val expiredCount = remember { mutableStateOf(0) }
    val categoryList = listOf(
        "全部",
        "肉類",
        "海鮮",
        "蔬菜",
        "水果",
        "蛋類",
        "豆製品",
        "乳製品",
        "調味料",
        "過期"
    )

    // 🔸 新增：是否為共享冰箱
    var isSharedFridge by remember { mutableStateOf(false) }

    LaunchedEffect(fridgeId) {
        try {
            isLoading = true
            listenerRegistration?.remove()

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(uid)
            val fridgeDoc = userDoc.collection("fridge").document(fridgeId)

            val sharedDoc = userDoc.collection("sharedFridges").document(fridgeId).get().await()
            isSharedFridge = sharedDoc.exists()

            ingredientRef = if (isSharedFridge) {
                val mirrorPath = sharedDoc.getString("mirrorFridgePath")
                val parts = mirrorPath?.split("/") ?: emptyList()
                if (parts.size >= 4) {
                    db.collection("users").document(parts[1])
                        .collection("fridge").document(parts[3])
                        .collection("Ingredient")
                } else {
                    fridgeDoc.collection("Ingredient")
                }
            } else {
                fridgeDoc.collection("Ingredient")
            }

            ingredientRef?.let { ref ->
                listenerRegistration = ref.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("IngredientScreen", "❌ 即時監聽錯誤：${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val loadedList = snapshot.documents.mapNotNull { doc ->
                            val data = doc.toObject(FoodItem::class.java)
                            data?.copy(id = doc.id)
                        }
                        foodList.clear()
                        foodList.addAll(loadedList)
                        foodListState.clear()
                        foodListState.addAll(loadedList)
                        Log.d("IngredientScreen", "👂 即時更新 ${loadedList.size} 筆食材資料")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("IngredientScreen", "Load error: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    DisposableEffect(fridgeId) {
        onDispose {
            listenerRegistration?.remove()
            Log.d("IngredientScreen", "🧹 已移除即時監聽")
        }
    }

    // ⭐ 過期通知
    LaunchedEffect(foodListState) {
        var expiredCounter = 0
        foodList.forEach { food ->
            if (food.fridgeId == fridgeId) {
                val days = calculateDaysRemainingSafely(food.date, food.daysRemaining)
                if (days < 0) expiredCounter++
                val title = when {
                    days < 0 -> "❌ 食材已過期"
                    days <= 3 -> "⚠️ 食材即將過期"
                    days <= 4 -> "⏰ 食材保存期限提醒"
                    else -> null
                }
                title?.let {
                    val msg = "「${food.name}」只剩 $days 天，請儘快使用！"
                    if (notifications.none { it.message == msg }) {
                        notifications.add(
                            NotificationItem(
                                title = it,
                                message = msg,
                                targetName = food.name,
                                daysLeft = days,
                                imageUrl = food.imageUrl
                            )
                        )
                    }
                }
            }
        }
        expiredCount.value = expiredCounter
    }

    // ⭐ 搜尋與篩選
    val filtered = foodListState.filter { item ->
        val matchesName = item.name.contains(searchText.value.trim(), ignoreCase = true)
        val matchesCategory = when (selectedCategory.value) {

            "海鮮" -> item.category.contains("海鮮")   // 冷凍海鮮 / 海鮮 / 冷藏海鮮 都會顯示
            "肉類" -> item.category.contains("肉")     // 冷凍肉類 / 肉類 / 冷藏肉類 都會顯示
            "蔬菜" -> item.category.contains("蔬菜")
            "水果" -> item.category.contains("水果")

            else -> selectedCategory.value == "全部" ||
                    item.category == selectedCategory.value
        }


        val days = calculateDaysRemainingSafely(item.date, item.daysRemaining)
        val matchesExpired = selectedCategory.value == "過期" && days < 0
        item.fridgeId == fridgeId && matchesName && (matchesCategory || matchesExpired)
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFABB7CD))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {

            //搜尋列
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchText.value,
                    onValueChange = { searchText.value = it },
                    placeholder = { Text("請輸入想搜尋的食材") },
                    singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF444B61), fontSize = 15.sp),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50.dp)),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "search",
                            tint = Color.Gray
                        )
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color(0xFFF2F2F2),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                //僅主冰箱顯示新增按鈕
                if (!isSharedFridge) {
                    IconButton(
                        onClick = { navController.navigate("add") },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFABB7CD), RoundedCornerShape(100))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新增", tint = Color.White)
                    }
                }
            }

            // 分類列
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
            ) {
                categoryList.forEach { category ->
                    val isSelected = selectedCategory.value == category
                    TextButton(
                        onClick = { selectedCategory.value = category },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (isSelected) Color(0xFFABB7CD) else Color(0xFFE3E6ED),
                            contentColor = if (isSelected) Color.White else Color(0xFF444B61)
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(category)
                    }
                }
            }

            //共享冰箱提示文字
            if (isSharedFridge) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "（此為共享冰箱，僅可查看內容，無法編輯或刪除）",
                    color = Color(0xFF7A869A),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 1.dp, bottom = 6.dp)
                )
            }

            // 食材卡
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                itemsIndexed(filtered) { _, item ->
                    FoodCard(
                        item = item,
                        onDelete = { if (!isSharedFridge) showDialog = true; itemToDelete = item },
                        onEdit = { onEditItem(item) },
                        disableDelete = isSharedFridge,
                        disableEdit = isSharedFridge   // ✅ 共享冰箱禁用編輯
                    )
                }
            }
        }
    }

    // 刪除對話框（僅主冰箱可用）
    if (showDialog && itemToDelete != null && !isSharedFridge) {
        AlertDialog(
            onDismissRequest = { showDialog = false; itemToDelete = null },
            title = { Text("刪除食材") },
            text = { Text("你要將「${itemToDelete!!.name}」加入購物車，還是直接刪除？") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        try {
                            cartItems.add(itemToDelete!!.copy(quantity = "1"))
                            FirebaseManager.deleteIngredient(fridgeId, itemToDelete!!.name)
                            foodList.remove(itemToDelete)
                            foodListState.remove(itemToDelete)
                            notifications.removeAll { it.targetName == itemToDelete!!.name }
                        } catch (_: Exception) {}
                        showDialog = false
                        itemToDelete = null
                    }
                }) { Text("加入購物車") }
            },
            dismissButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        try {
                            FirebaseManager.deleteIngredient(fridgeId, itemToDelete!!.name)
                            foodList.remove(itemToDelete)
                            foodListState.remove(itemToDelete)
                            notifications.removeAll { it.targetName == itemToDelete!!.name }
                        } catch (_: Exception) {}
                        showDialog = false
                        itemToDelete = null
                    }
                }) { Text("直接刪除") }
            }
        )
    }
}

/* ---------------------------------------------------------
 * 日期計算
 * --------------------------------------------------------- */
fun calculateDaysRemainingSafely(dateString: String, fallbackDaysRemaining: Int): Int {
    if (dateString.isBlank()) return fallbackDaysRemaining
    val today = Calendar.getInstance().time
    val patterns = listOf("yyyy-MM-dd", "yyyy/MM/dd", "yyyy/M/d")
    var expireDate: Date? = null
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.isLenient = false
            expireDate = sdf.parse(dateString)
            if (expireDate != null) break
        } catch (_: Exception) {}
    }
    if (expireDate == null) return fallbackDaysRemaining
    val diff = expireDate.time - today.time
    return (diff / (1000 * 60 * 60 * 24)).toInt()
}

/* ---------------------------------------------------------
 * 食材卡片
 * --------------------------------------------------------- */
@Composable
fun FoodCard(
    item: FoodItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    disableDelete: Boolean = false,
    disableEdit: Boolean = false
) {
    val dynamicDays = calculateDaysRemainingSafely(item.date, item.daysRemaining)
    val cardBackground = when {
        dynamicDays < 0 -> Color(0xFFFFE5E5)
        dynamicDays <= 3 -> Color(0xFFFFF6D8)
        else -> Color(0xFFE3E6ED)
    }
    val borderColor = when {
        dynamicDays < 0 -> Color(0xFFFF5A5A)
        dynamicDays <= 3 -> Color(0xFFFFB84D)
        else -> Color.Transparent
    }

    val totalDays = maxOf(item.daysRemaining, 1)
    val progressPercent = (dynamicDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val progressColor = when {
        dynamicDays < 0 -> Color(0xFFE53935)
        dynamicDays <= 3 -> Color(0xFFFF9432)
        else -> Color(0xFF22C97D)
    }

    val dayLeftText = when {
        dynamicDays < 0 -> "已過期"
        dynamicDays == 0 -> "今天到期"
        else -> "剩 $dynamicDays 天"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .border(2.dp, borderColor, RoundedCornerShape(15.dp))
            .background(cardBackground)
            .then(
                if (!disableEdit) Modifier.clickable { onEdit() } else Modifier
            )
            .padding(12.dp)
    ) {
        Column {
            Box {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(90.dp).fillMaxWidth()
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

                if (!disableEdit) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "編輯",
                            tint = Color(0xFF444B61)
                        )
                    }
                }
            }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0xFFABB7CD).copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(progressPercent.coerceAtLeast(0.05f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(25.dp))
                            .background(progressColor)
                    )
                }

                Text(
                    text = dayLeftText,
                    fontSize = 12.sp,
                    color = Color(0xFF7A869A),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF444B61),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "到期日：${item.date}",
                    fontSize = 13.sp,
                    color = Color(0xFF7A869A),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "數量：${item.quantity}",
                    fontSize = 13.sp,
                    color = Color(0xFF7A869A),
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (item.note.isNotBlank()) {
                    Text(
                        text = "備註：${item.note}",
                        fontSize = 13.sp,
                        color = Color(0xFF7A869A),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!disableDelete) {
                    TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "刪除",
                            tint = Color(0xFF7A869A)
                        )
                    }
                }
            }
        }
    }
