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
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
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

    // ✅ 相簿選擇器
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    // ✅ 建立圖片檔案（拍照用）
    fun createImageFile(): Uri {
        val directory = context.externalCacheDir ?: context.cacheDir
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    // ✅ 拍照啟動器
    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri = capturedImageUri
                Toast.makeText(context, "📸 拍照完成", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "❌ 拍照取消或失敗", Toast.LENGTH_SHORT).show()
            }
        }

    val buttonColor = Color(0xFFABB7CD)
    var showDialog by remember { mutableStateOf(false) } // 控制拍照/相簿選擇彈窗

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ✅ 改成點圖片彈出 AlertDialog
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray)
                .clickable { showDialog = true },
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

        // ✅ 彈出視窗：選擇拍照或相簿
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {},
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "選擇圖片來源",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Button(
                            onClick = {
                                showDialog = false
                                val uri = createImageFile()
                                capturedImageUri = uri
                                context.grantUriPermission(
                                    "com.android.camera",
                                    uri,
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                                takePictureLauncher.launch(uri)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📸 拍照上傳")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                showDialog = false
                                launcher.launch("image/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🖼 從相簿選擇")
                        }
                    }
                }
            )
        }

        // ✅ 三個輸入欄位（不變）
        CustomInputField(value = name, onValueChange = { name = it }, placeholder = "名稱")
        CustomInputField(value = quantity, onValueChange = { quantity = it }, placeholder = "數量")
        CustomInputField(value = note, onValueChange = { note = it }, placeholder = "備註")

        // ✅ 功能按鈕區（保留原有功能）
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

            // 加入購物清單 + 上傳 Storage
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "請填寫名稱", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        try {
                            // ✅ 這裡是新增的 Storage 上傳邏輯
                            var imageUrlFromStorage = existingItem?.imageUrl ?: ""

                            if (imageUri != null) {
                                val storageRef = FirebaseStorage.getInstance()
                                    .reference.child("cart_images/${UUID.randomUUID()}.jpg")

                                // 上傳檔案到 Storage
                                storageRef.putFile(imageUri!!).await()

                                // 取得下載網址
                                imageUrlFromStorage = storageRef.downloadUrl.await().toString()
                            }

                            // ✅ 建立要儲存的物件
                            val newItem = FoodItem(
                                name = name,
                                quantity = quantity,
                                note = note,
                                imageUri = imageUri,
                                imageUrl = imageUrlFromStorage,
                                date = "",
                                daysRemaining = 0,
                                dayLeft = "",
                                progressPercent = 0f
                            )

                            // ✅ 儲存到 Firestore
                            tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.addCartItem(newItem)

                            Toast.makeText(context, "✅ 成功新增至購物清單", Toast.LENGTH_SHORT).show()
                            onSave(newItem)

                            // ✅ 導回購物車頁
                            navController.navigate("cart") {
                                launchSingleTop = true
                                popUpTo("cart") { inclusive = false }
                            }

                        } catch (e: Exception) {
                            Toast.makeText(context, "❌ 上傳失敗：${e.message}", Toast.LENGTH_SHORT).show()
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
