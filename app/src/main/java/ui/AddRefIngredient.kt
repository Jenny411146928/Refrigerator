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
import java.time.LocalDate
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
    val today = remember { LocalDate.now() }

    var nameText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("請選擇到期日") }
    var quantityText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var storageType by remember { mutableStateOf("非冷凍") }
    var foodCategory by remember { mutableStateOf("自選") }

    val nonFrozenCategories = listOf("菜葉類（3天）", "根莖類（7天）", "水果類（5天）", "自選")
    val frozenCategories = listOf("冷凍瘦肉類（30天）", "冷凍肥肉類（20天）", "冷凍加工食品（45天）", "自選")

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImageUri = it
    }

    fun updateDateBasedOnCategory() {
        val days = when (foodCategory) {
            "菜葉類（3天）" -> 3
            "根莖類（7天）" -> 7
            "水果類（5天）" -> 5
            "冷凍瘦肉類（30天）" -> 30
            "冷凍肥肉類（20天）" -> 20
            "冷凍加工食品（45天）" -> 45
            else -> null
        }
        days?.let {
            val updatedDate = today.plusDays(it.toLong())
            dateText = "${updatedDate.year}/${updatedDate.monthValue}/${updatedDate.dayOfMonth}"
        }
    }

    LaunchedEffect(foodCategory, storageType) {
        updateDateBasedOnCategory()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 60.dp)
            ) {
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

                InputField("食材名稱", nameText) { nameText = it }

                DropdownSelector("儲存方式", listOf("非冷凍", "冷凍"), storageType) {
                    storageType = it
                    foodCategory = "自選"
                }

                val currentOptions = if (storageType == "冷凍") frozenCategories else nonFrozenCategories
                DropdownSelector("分類", currentOptions, foodCategory) {
                    foodCategory = it
                }

                DateField(dateText) { dateText = it }
                InputField("數量", quantityText, KeyboardType.Number) { quantityText = it }
                InputField("備註", noteText) { noteText = it }

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
                                if (dateText == "請選擇到期日") {
                                    Toast.makeText(context, "請先選擇到期日", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (nameText.isBlank()) {
                                    Toast.makeText(context, "請輸入食材名稱", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (quantityText.isBlank()) {
                                    Toast.makeText(context, "請輸入數量", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val selectedDate = sdf.parse(dateText)
                                val todayCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val daysRemaining = ((selectedDate.time - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
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
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "儲存失敗，請確認資料格式正確", Toast.LENGTH_SHORT).show()
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
            .background(Color(0xFFE9E1F4)),
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
fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 30.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFE9E1F4))
                .clickable { expanded = true }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$label：$selected",
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp
                )
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
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
                    { _, year, month, day ->
                        onDateSelected("$year/${month + 1}/$day")
                    },
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
            .background(Color(0xFFE9E1F4)),
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
