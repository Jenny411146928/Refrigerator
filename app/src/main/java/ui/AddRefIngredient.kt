package ui

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
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
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.model.ChatMessage
import tw.edu.pu.csim.refrigerator.openai.OpenAIClient
import tw.edu.pu.csim.refrigerator.firebase.FirebaseManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import tw.edu.pu.csim.refrigerator.openai.OpenAIClient.FoodDetectResult

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
    val coroutineScope = rememberCoroutineScope()

    var nameText by remember { mutableStateOf(existingItem?.name ?: "") }
    var dateText by remember { mutableStateOf(existingItem?.date ?: "請選擇到期日") }
    var quantityText by remember { mutableStateOf(existingItem?.quantity ?: "") }
    var noteText by remember { mutableStateOf(existingItem?.note ?: "") }
    var selectedImageUri by remember { mutableStateOf(existingItem?.imageUrl?.let { Uri.parse(it) }) }

    var storageType by remember { mutableStateOf(existingItem?.storageType ?: "非冷凍") }
    var foodCategory by remember { mutableStateOf(existingItem?.category ?: "自選") }

    val nonFrozenCategories = listOf("蔬菜", "水果", "海鮮", "肉類", "其他", "自選")
    val frozenCategories = listOf("冷凍肉類", "冷凍海鮮", "冷凍加工食品", "其他", "自選")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri

        // ⭐⭐⭐ 就加在這裡：相簿選圖片 → 做圖片辨識
        if (uri != null) {
            coroutineScope.launch {
                Log.e("VisionEntry", "📌 開始圖片辨識（相簿）")

                val bitmap = loadBitmapFromUri(context, uri)
                val base64 = bitmapToBase64(bitmap)

                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    OpenAIClient.detectFoodFromImage(base64)
                }

                Log.e("VisionEntry", "📌 辨識結果：$result")

                if (result != null) {

                    // 1️⃣ 修正名稱（Vision 常回西蘭花 → 改成花椰菜）
                    val fixedName = normalizeFoodName(result.name)

                    // 2️⃣ 自動分類
                    val finalCategory = guessCategoryByName(fixedName)

                    // 3️⃣ 自動推算保存期限
                    val days = guessExpireDays(finalCategory)
                    val today = LocalDate.now()
                    val expire = today.plusDays(days.toLong())
                    val expireDate = "${expire.year}/${expire.monthValue}/${expire.dayOfMonth}"

                    // 🟢 自動寫入畫面欄位
                    nameText = fixedName            // 食材名稱
                    foodCategory = finalCategory    // 食材分類
                    dateText = expireDate           // 食材過期日

                    Log.e("VisionAuto", "✔ 名稱=$fixedName / 分類=$finalCategory / 到期日=$expireDate")
                }

            }

        }
    }

    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = photoUri.value
            selectedImageUri = uri

            // ⭐⭐⭐ 就加在這裡：拍照完 → 做圖片辨識
            if (uri != null) {
                coroutineScope.launch {
                    Log.e("VisionEntry", "📌 開始圖片辨識（相機）")

                    val bitmap = loadBitmapFromUri(context, uri)
                    val base64 = bitmapToBase64(bitmap)

                    val result = withContext(Dispatchers.IO) {
                        OpenAIClient.detectFoodFromImage(base64)
                    }

                    Log.e("VisionEntry", "📌 辨識結果：$result")

                    if (result != null) {
                        nameText = result.name
                        foodCategory = result.category
                    }
                }

            }
        }
    }

    val showDialog = remember { mutableStateOf(false) }

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
        if (!isEditing) updateDateBasedOnCategory()
    }

    LaunchedEffect(nameText) {
        if (!isEditing && nameText.trim().length in 2..12) {
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
                // ✅ 圖片區塊
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
                            .clickable { showDialog.value = true }
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

                // ✅ AlertDialog 選擇來源
                if (showDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showDialog.value = false },
                        confirmButton = {},
                        text = {
                            Column {
                                Text(
                                    "選擇圖片來源",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(
                                    onClick = {
                                        showDialog.value = false
                                        val imageFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            imageFile
                                        )
                                        photoUri.value = uri
                                        takePhotoLauncher.launch(uri)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFABB7CD),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(50.dp)
                                ) { Text("📸 拍照上傳") }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        showDialog.value = false
                                        imagePickerLauncher.launch("image/*")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFABB7CD),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(50.dp)
                                ) { Text("🖼 從相簿選擇") }
                            }
                        }
                    )
                }

                val spacing = Modifier.padding(top = 20.dp)

                InputField("食材名稱", nameText, modifier = spacing) { nameText = it }
                DropdownSelector("儲存方式", listOf("非冷凍", "冷凍"), storageType, spacing) {
                    storageType = it
                    foodCategory = "自選"
                }
                val currentOptions = if (storageType == "冷凍") frozenCategories else nonFrozenCategories
                DropdownSelector("分類", currentOptions, foodCategory, spacing) { foodCategory = it }

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
                    ) { Text("返回食材頁", color = Color.White) }

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
                                val daysRemaining =
                                    ((selectedDate.time - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                                val progress = daysRemaining.coerceAtMost(7) / 7f

                                // ⭐ 新增：處理圖片邏輯，避免編輯時用 http URL 當成要上傳的 Uri
                                val safeImageUrl = selectedImageUri?.toString() ?: (existingItem?.imageUrl ?: "")
                                val uploadImageUri =
                                    if (selectedImageUri != null && selectedImageUri.toString().startsWith("content://")) {
                                        selectedImageUri
                                    } else {
                                        null
                                    }

                                val itemId = existingItem?.id ?: UUID.randomUUID().toString()

                                val item = FoodItem(
                                    id = itemId,  // ⭐ 永遠正確：編輯用舊 ID、新增用新 UUID

                                    name = nameText,
                                    date = dateText,
                                    quantity = quantityText,
                                    note = noteText,
                                    imageUrl = safeImageUrl,
                                    daysRemaining = daysRemaining,
                                    dayLeft = "$daysRemaining day left",
                                    progressPercent = progress,
                                    fridgeId = fridgeId,
                                    category = foodCategory,
                                    storageType = storageType
                                )

                                // ✅ 呼叫 FirebaseManager 上傳食材與圖片
                                coroutineScope.launch {
                                    try {
                                        if (isEditing && existingItem != null) {
                                            // ⭐ 正確：編輯模式 → 更新既有食材
                                            FirebaseManager.updateIngredient(fridgeId, item, uploadImageUri)
                                        } else {
                                            // ⭐ 新增模式 → 新增食材
                                            FirebaseManager.addIngredientToFridge(fridgeId, item, uploadImageUri)
                                        }

                                        Toast.makeText(context, "✅ 已成功儲存！", Toast.LENGTH_SHORT).show()

                                        navController.navigate("ingredients") {
                                            popUpTo("ingredients") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "❌ 上傳失敗：${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            } catch (e: Exception) {
                                Toast.makeText(context, "儲存失敗，請確認資料格式正確", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFABB7CD)),
                        shape = RoundedCornerShape(50.dp)
                    ) { Text("儲存食材", color = Color.White) }
                }
            }
        }
    }
}
fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    val bytes = stream.toByteArray()
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
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
// ===============================
// 🟩 Vision 修正版名稱
// ===============================
fun normalizeFoodName(raw: String): String {
    return when (raw) {
        "西蘭花", "青花菜", "綠花椰" -> "花椰菜"
        "番茄", "蕃茄" -> "番茄"
        else -> raw
    }
}

// ===============================
// 🟦 自動分類規則
// ===============================
fun guessCategoryByName(name: String): String {
    return when {
        listOf("花椰菜", "番茄", "玉米", "高麗菜", "菠菜", "蔥", "茄子").any { name.contains(it) } -> "蔬菜"
        listOf("蘋果", "香蕉", "葡萄", "芒果").any { name.contains(it) } -> "水果"
        listOf("雞", "豬", "牛", "羊").any { name.contains(it) } -> "肉類"
        listOf("蝦", "魚", "鮭", "鯛", "魷", "章魚").any { name.contains(it) } -> "海鮮"
        else -> "其他"
    }
}

// ===============================
// 🟥 自動到期日（保存天數）
// ===============================
fun guessExpireDays(category: String): Int {
    return when (category) {
        "蔬菜" -> 3
        "水果" -> 5
        "海鮮" -> 4
        "肉類" -> 30
        else -> 5
    }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFE3E6ED))
                .clickable { expanded = true }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label：$selected",
                modifier = Modifier.weight(1f),
                fontSize = 16.sp
            )
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
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

    Column(modifier = modifier.then(Modifier.padding(horizontal = 30.dp))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFE3E6ED))
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onDateSelected("$year/${month + 1}/$day")
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (dateText.isBlank() || dateText == "請選擇到期日")
                    "請選擇到期日" else "到期日：$dateText",
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                color = Color.Black
            )
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
    }
}
