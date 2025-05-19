package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.Ingredient

@Composable
fun CartPageScreen(navController: NavController, cartItems: MutableList<FoodItem>) {
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
                    minusUrl = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/fa353850-c736-4665-8c98-e83c4c92eaa0",
                    plusUrl = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/2df9505e-17d0-47b3-9e5e-6d23761e87b1",
                    quantity = item.quantity.toIntOrNull() ?: 1,
                    onQuantityChange = { newQty ->
                        if (newQty <= 0) cartItems.removeAt(index)
                        else cartItems[index] = item.copy(quantity = newQty.toString())
                    },
                    onDelete = { cartItems.removeAt(index) },
                    onEdit = { navController.navigate("edit_cart_item/${index}") }
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
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }
}

@Composable
fun CartItem(
    name: String,
    note: String,
    imageUrl: String,
    minusUrl: String,
    plusUrl: String,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var count by remember { mutableStateOf(quantity) }
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
                    .size(100.dp)
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
                        .background(Color(0xFFD9D9D9))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(modifier = Modifier.size(24.dp).clickable { if (count > 0) count-- }) {
                        AsyncImage(model = minusUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("$count", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(24.dp).clickable { count++ }) {
                        AsyncImage(model = plusUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.padding(start = 12.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
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
