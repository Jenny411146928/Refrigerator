package tw.edu.pu.csim.refrigerator

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddIngredientScreen(
    navController: NavController,
    onSave: (FoodItem) -> Unit,
    existingItem: FoodItem?,
    isEditing: Boolean
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy/M/d", Locale.getDefault())
    val nameText = remember { mutableStateOf(existingItem?.name ?: "") }
    val dateText = remember { mutableStateOf(existingItem?.date ?: "請選擇日期") }
    val quantityText = remember { mutableStateOf(existingItem?.quantity ?: "") }
    val noteText = remember { mutableStateOf(existingItem?.note ?: "") }
    var selectedImageUri by remember { mutableStateOf(existingItem?.imageUrl?.let { Uri.parse(it) }) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImageUri = it
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD7E0E5))
                    .padding(16.dp)
            ) {
                Text(
                    "Refrigerator",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9DA5C1),
                    modifier = Modifier.weight(1f)
                )
                AsyncImage(
                    model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/4faebf02-2554-4a05-ac3b-f30f513a28c3",
                    contentDescription = null,
                    modifier = Modifier.size(31.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                        .wrapContentSize(align = Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { launcher.launch("image/*") }
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri == null) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "選擇圖片",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            val painter = rememberAsyncImagePainter(model = selectedImageUri)
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                TextField(
                    value = nameText.value,
                    onValueChange = { nameText.value = it },
                    placeholder = { Text("食材名稱") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, start = 30.dp, end = 30.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xEEE9E1F4)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(50.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )

                TextField(
                    value = dateText.value,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("請點擊右側圖示選擇日期") },
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day -> dateText.value = "$year/${month + 1}/$day" },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "選擇日期")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, start = 30.dp, end = 30.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xEEE9E1F4)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(50.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )

                TextField(
                    value = quantityText.value,
                    onValueChange = { quantityText.value = it },
                    placeholder = { Text("數量") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, start = 30.dp, end = 30.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xEEE9E1F4)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(50.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                TextField(
                    value = noteText.value,
                    onValueChange = { noteText.value = it },
                    placeholder = { Text("備註") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp, start = 30.dp, end = 30.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xEEE9E1F4)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(50.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("返回食材頁面", color = Color.White)
                    }

                    Button(
                        onClick = {
                            try {
                                val selectedDate = sdf.parse(dateText.value)
                                val todayCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val diffInMillis = selectedDate.time - todayCal.timeInMillis
                                val daysRemaining = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                                val progress = daysRemaining.coerceAtMost(7) / 7f

                                onSave(
                                    FoodItem(
                                        name = nameText.value,
                                        date = dateText.value,
                                        quantity = quantityText.value,
                                        note = noteText.value,
                                        imageUrl = selectedImageUri?.toString() ?: "",
                                        daysRemaining = daysRemaining,
                                        dayLeft = "$daysRemaining day left",
                                        progressPercent = progress
                                    )
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "日期格式錯誤或未選擇，請重新輸入", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6658A0)),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("儲存食材", color = Color.White)
                    }
                }
            }
        }
    }
}