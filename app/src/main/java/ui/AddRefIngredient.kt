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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import tw.edu.pu.csim.refrigerator.openai.OpenAIClient
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@Composable
fun AddIngredientScreen(
    navController: NavController,
    onSave: (FoodItem) -> Unit,
    existingItem: FoodItem?,
    fridgeId: String,
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

    val nonFrozenCategories = listOf("蔬菜", "水果", "海鮮", "肉類", "其他", "自選")
    val frozenCategories = listOf("冷凍肉類", "冷凍海鮮", "冷凍加工食品", "其他", "自選")

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImageUri = it
    }

    fun updateDateBasedOnCategory() {
        val days = when (foodCategory) {
            "蔬菜" -> 3
            "水果" -> 5
            "海鮮" -> 4
            "肉類", "冷凍肉類" -> 30
            "冷凍加工食品" -> 45
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

    LaunchedEffect(nameText) {
        if (nameText.trim().length in 2..12) {
            val prompt = listOf(
                ChatMessage("system", "你是冰箱幫手，會根據食材名稱判斷類別，只回覆「肉類、蔬菜、水果、海鮮、其他」之一"),
                ChatMessage("user", "食材名稱：${nameText.trim()}")
            )
            OpenAIClient.askChatGPT(prompt) { result ->
                result?.let {
                    val clean = it.trim().replace("。", "")
                    foodCategory = when {
                        "肉" in clean -> "肉類"
                        "菜" in clean -> "蔬菜"
                        "果" in clean -> "水果"
                        "海" in clean -> "海鮮"
                        else -> "其他"
                    }
                }
            }
        }
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

                val spacing = Modifier.padding(top = 20.dp)

                InputField("食材名稱", nameText, modifier = spacing) { nameText = it }
                DropdownSelector("儲存方式", listOf("非冷凍", "冷凍"), storageType, spacing) {
                    storageType = it
                    foodCategory = "自選"
                }
                val currentOptions = if (storageType == "冷凍") frozenCategories else nonFrozenCategories
                DropdownSelector("分類", currentOptions, foodCategory, spacing) {
                    foodCategory = it
                }
                DateField(dateText, spacing) { dateText = it }
                InputField("數量", quantityText, KeyboardType.Number, spacing) { quantityText = it }
                InputField("備註", noteText, modifier = spacing) { noteText = it }

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
                                        progressPercent = progress,
                                        fridgeId = fridgeId,
                                        category = foodCategory
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFABB7CD)),
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
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(Color(0xFFE3E6ED)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
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
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.then(Modifier.padding(horizontal = 30.dp))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFE3E6ED))
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
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(Color(0xFFE3E6ED)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(50.dp),
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp)
    )
}
