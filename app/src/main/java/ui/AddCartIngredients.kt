package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.Ingredient


@Composable
fun AddCartIngredientsScreen(
    navController: NavController,
    item: Ingredient? = null,
    index: Int = -1,
    onSave: (Ingredient, Int) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "") }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(item?.imageUri) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F1F1))
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/1d7dab96-10ed-43d6-a0e9-9cb957a53673",
                        contentDescription = "加號",
                        modifier = Modifier.size(52.dp)
                    )
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("請輸入食材名稱") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("數量", fontSize = 20.sp)
            TextField(
                value = quantity,
                onValueChange = { quantity = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("備註", fontSize = 20.sp)
            TextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("返回", fontSize = 20.sp)
                }

                Button(
                    onClick = {
                        val qty = quantity.toIntOrNull()
                        if (qty != null && name.isNotBlank()) {
                            val newItem = Ingredient(name, qty, note, imageUri)
                            onSave(newItem, index)
                            navController.navigate("cart") {
                                popUpTo("cart") { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            Toast.makeText(context, "請輸入有效的數量與名稱", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B61FF))
                ) {
                    Text("儲存", fontSize = 20.sp)
                }
            }
        }
    }
}
