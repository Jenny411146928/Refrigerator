package tw.edu.pu.csim.refrigerator.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

@Composable
fun AddCartIngredientsScreen(
    navController: NavController,
    existingItem: FoodItem? = null,
    isEditing: Boolean = false,
    onSave: (FoodItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var quantity by remember { mutableStateOf(existingItem?.quantity ?: "") }
    var note by remember { mutableStateOf(existingItem?.note ?: "") }

    // ✅ 原本的相簿選擇器
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    // ✅ 改良版 createImageFile()：使用 externalCacheDir 以避免 MIUI 拒寫
    fun createImageFile(): Uri {
        val directory = context.externalCacheDir ?: context.cacheDir
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    // ✅ 改良版拍照啟動器
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri = capturedImageUri
            Toast.makeText(context, "📸 拍照完成", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "❌ 拍照取消或失敗", Toast.LENGTH_SHORT).show()
        }
    }

    val buttonColor = Color(0xFFABB7CD)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 圖片選擇區
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "圖片預覽",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "新增圖片",
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        // ✅ 新增：兩個按鈕（相簿選擇 / 拍照）
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { launcher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("從相簿選擇", fontSize = 16.sp)
            }

            // ✅ 拍照按鈕加上 try-catch + 權限授予
            Button(
                onClick = {
                    try {
                        val uri = createImageFile()
                        capturedImageUri = uri
                        // 🔹 授權給相機寫入
                        context.grantUriPermission(
                            "com.android.camera",
                            uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        takePictureLauncher.launch(uri)
                    } catch (e: Exception) {
                        Toast.makeText(context, "開啟相機失敗：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("開啟相機拍照", fontSize = 16.sp)
            }
        }

        // 三個輸入欄位
        CustomInputField(value = name, onValueChange = { name = it }, placeholder = "名稱")
        CustomInputField(value = quantity, onValueChange = { quantity = it }, placeholder = "數量")
        CustomInputField(value = note, onValueChange = { note = it }, placeholder = "備註")

        // 按鈕區
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 返回食材頁
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3D4D3)),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("返回食材頁", fontSize = 16.sp)
            }

            // 加入購物清單
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "請填寫名稱", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val newItem = FoodItem(
                        name = name,
                        quantity = quantity,
                        note = note,
                        imageUri = imageUri,
                        imageUrl = imageUri?.toString() ?: existingItem?.imageUrl ?: "",
                        date = "",
                        daysRemaining = 0,
                        dayLeft = "",
                        progressPercent = 0f
                    )

                    // ✅ 呼叫 FirebaseManager 寫入購物清單
                    scope.launch {
                        try {
                            tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.addCartItem(newItem)
                            Toast.makeText(context, "成功新增至購物清單", Toast.LENGTH_SHORT).show()
                            onSave(newItem)

                            // ✅ 導回購物車頁面
                            navController.navigate("cart") {
                                launchSingleTop = true
                                popUpTo("cart") { inclusive = false }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "寫入失敗：${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(if (isEditing) "儲存變更" else "加入購物清單", fontSize = 16.sp)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color(0xFFE3E6ED),
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}
