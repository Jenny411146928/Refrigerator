package com.example.myapplication

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.FoodItem

@Composable
fun IngredientScreen(
    foodList: MutableList<FoodItem>,
    navController: NavController
) {
    val searchText = remember { mutableStateOf("") }
    val filtered = foodList.filter { it.name.contains(searchText.value.trim(), ignoreCase = true) }

    fun deleteItem(item: FoodItem) {
        foodList.remove(item)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp) // 為底部導覽列預留空間
    ) {
        // Search bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFD9D9D9))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/e6e641b3-d4ed-43ac-8068-2ce5b28df138",
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                TextField(
                    value = searchText.value,
                    onValueChange = { searchText.value = it },
                    placeholder = { Text("請輸入想搜尋的食材") },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 14.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = { navController.navigate("add") },
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black, RoundedCornerShape(100))
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增", tint = Color.White)
            }
        }

        // Grid list
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            itemsIndexed(filtered) { index, item ->
                FoodCard(
                    item = item,
                    onDelete = { deleteItem(item) },
                    onEdit = {
                        val realIndex = foodList.indexOf(item)
                        navController.navigate("edit/$realIndex")
                    }
                )
            }
        }
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