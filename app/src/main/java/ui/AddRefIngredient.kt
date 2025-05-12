package ui

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.FoodItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddIngredientScreen(
    navController: NavController,
    onSave: (FoodItem) -> Unit,
    existingItem: FoodItem?,
    isEditing: Boolean = false
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy/M/d", Locale.getDefault()) }

    var nameText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("請選擇到期日") }
    var quantityText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // 編輯模式時初始化欄位資料
    LaunchedEffect(existingItem) {
        existingItem?.let {
            nameText = it.name
            dateText = it.date
            quantityText = it.quantity
            noteText = it.note
            if (it.imageUrl.isNotBlank()) {
                selectedImageUri = Uri.parse(it.imageUrl)
            }
        }
    }



    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImageUri = it
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 主內容區
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 60.dp)
            ) {
                // 圖片上傳區
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                        .wrapContentSize(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri == null) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // 輸入欄位
                InputField("食材名稱", nameText) { nameText = it }
                DateField(dateText) { dateText = it }
                InputField("數量", quantityText, KeyboardType.Number) { quantityText = it }
                InputField("備註", noteText) { noteText = it }

                // 按鈕區
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("返回食材頁", color = Color.White)
                    }

                    Button(
                        onClick = {
                            try {
                                val selectedDate = sdf.parse(dateText)
                                val today = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val daysRemaining = ((selectedDate.time - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                                val progress = daysRemaining.coerceAtMost(7) / 7f

                                onSave(
                                    FoodItem(
                                        name = nameText,
                                        date = dateText,
                                        quantity = quantityText,
                                        note = noteText,
                                        imageUrl = selectedImageUri?.toString() ?: "",
                                        daysRemaining = daysRemaining,
                                        dayLeft = "$daysRemaining day left",
                                        progressPercent = progress
                                    )
                                )
                                navController.navigate("ingredients") {
                                    popUpTo("ingredients") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "請選擇正確到期日", Toast.LENGTH_SHORT).show()
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

@Composable
fun InputField(
    placeholder: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun DateField(
    dateText: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    TextField(
        value = dateText,
        onValueChange = {},
        readOnly = true,
        placeholder = { Text("請點擊右側圖示選擇日期") },
        trailingIcon = {
            IconButton(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day -> onDateSelected("$year/${month + 1}/$day") },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
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
}