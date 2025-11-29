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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
            cartItems.forEachIndexed { index, item ->
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
                        scope.launch {
                            try {
                                tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.deleteCartItem(item.id)
                            } catch (_: Exception) {
                            }
                        }
                        cartItems.removeAt(index)
                    },

                    onEdit = { navController.navigate("edit_cart_item/$index") }
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
    onEdit: () -> Unit
) {
    var count by remember { mutableStateOf(quantity.coerceAtLeast(1)) }
    var checked by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(count) { onQuantityChange(count) }

    LaunchedEffect(checked) {
        if (checked) {
            visible = false
            delay(300)
            onDelete()
        }
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
                onCheckedChange = { checked = it },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
