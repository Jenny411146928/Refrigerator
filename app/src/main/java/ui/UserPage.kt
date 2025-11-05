package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.data.UserPreferences
import android.widget.Toast
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPage(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val defaultImageUrl = "https://i.imgur.com/1Z3ZKpP.png"

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val user = auth.currentUser

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var userName by remember { mutableStateOf("姓名") }
    var isEditingName by remember { mutableStateOf(false) }
    val userEmail = user?.email ?: "信箱未設定"

    // 🔹 初始載入使用者資料
    LaunchedEffect(user?.uid) {
        val uid = user?.uid ?: return@LaunchedEffect
        try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                userName = doc.getString("name") ?: userName
                doc.getString("imageUrl")?.let { imageUrl ->
                    selectedImageUri = Uri.parse(imageUrl)
                }
            } else {
                db.collection("users").document(uid).set(
                    mapOf(
                        "name" to userName,
                        "email" to userEmail,
                        "imageUrl" to (selectedImageUri?.toString() ?: "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "無法載入使用者資料", Toast.LENGTH_SHORT).show()
        }
    }

    // 🔹 圖片選擇器＋上傳 Firebase Storage
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                coroutineScope.launch {
                    try {
                        // ✅ 先上傳圖片到 Firebase Storage
                        val uid = user?.uid ?: return@launch
                        val storageRef = storage.reference.child("profile_images/${uid}.jpg")
                        storageRef.putFile(it).await()
                        val downloadUrl = storageRef.downloadUrl.await()

                        // ✅ 儲存到 Firestore
                        db.collection("users").document(uid)
                            .update("imageUrl", downloadUrl.toString())
                            .addOnSuccessListener {
                                Toast.makeText(context, "✅ 頭像已更新", Toast.LENGTH_SHORT).show()
                            }

                        // ✅ 同步更新 UserPreferences
                        UserPreferences.saveImageUri(context, downloadUrl.toString())

                        // 更新目前顯示的圖片
                        selectedImageUri = Uri.parse(downloadUrl.toString())

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "❌ 上傳頭像失敗：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    if (isEditingName) {
                        isEditingName = false
                        focusManager.clearFocus()
                        coroutineScope.launch {
                            UserPreferences.saveUserName(context, userName)
                            user?.uid?.let { uid ->
                                db.collection("users").document(uid)
                                    .update("name", userName)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "✅ 名稱已更新", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }
                })
            }
    ) {
        // 上方背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.39f)
                .background(
                    color = Color(0xFFD7E0E5),
                    shape = RoundedCornerShape(bottomStart = 60.dp, bottomEnd = 60.dp)
                )
                .align(Alignment.TopCenter)
        )

        // 個人資料頭像＋名稱區
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .zIndex(2f)
        ) {
            // 頭像
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "使用者頭像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        placeholder = rememberAsyncImagePainter(defaultImageUrl),
                        error = rememberAsyncImagePainter(defaultImageUrl)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "預設人像頭貼",
                        tint = Color(0xFFB0B7BE),
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 名稱欄位
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isEditingName) {
                    Box(contentAlignment = Alignment.Center) {
                        BasicTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .padding(end = 36.dp),
                            decorationBox = { innerTextField ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box {
                                        if (userName.isEmpty()) {
                                            Text(
                                                text = "請輸入姓名",
                                                color = Color.Gray,
                                                fontSize = 16.sp,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                        innerTextField()
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color.Gray)
                                    )
                                }
                            }
                        )

                        Text(
                            text = "儲存",
                            color = Color(0xFF4B5E72),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clickable {
                                    isEditingName = false
                                    focusManager.clearFocus()
                                    coroutineScope.launch {
                                        UserPreferences.saveUserName(context, userName)
                                        user?.uid?.let { uid ->
                                            db.collection("users").document(uid)
                                                .update("name", userName)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "✅ 名稱已更新", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                }
                                .padding(end = 4.dp)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = userName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier
                                .clickable { isEditingName = true }
                                .padding(end = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "編輯名字",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { isEditingName = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFFE9ECF1), RoundedCornerShape(12.dp))
                    .width(380.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 下方選單
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 280.dp)
                .zIndex(1f)
        ) {
            OptionItem(Icons.Default.Favorite, "最愛食譜") {
                navController.navigate("favorite_recipes")
            }
            OptionItem(Icons.Default.Notifications, "通知") {
                navController.navigate("notification")
            }
            OptionItem(Icons.Default.Info, "簡介") {
                navController.navigate("about")
            }

            Spacer(modifier = Modifier.height(120.dp))

            Button(
                onClick = { FirebaseAuth.getInstance().signOut() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF2F2F2),
                    contentColor = Color.Red
                ),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(0.4f)
                    .height(46.dp)
            ) {
                Text("登出", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 36.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = text, tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Text(text, fontSize = 16.sp, color = Color.Black)
    }
}
