package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.data.UserPreferences
import androidx.compose.material3.LocalTextStyle
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPage(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("冰擠勒") }
    val defaultImageUrl = "https://i.imgur.com/1Z3ZKpP.png"

    // 載入使用者頭像
    LaunchedEffect(true) {
        val uriStr = UserPreferences.loadImageUri(context)
        if (!uriStr.isNullOrEmpty()) {
            selectedImageUri = Uri.parse(uriStr)
        }
        val name = UserPreferences.loadUserName(context)
        if (!name.isNullOrEmpty()) {
            userName = name
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            coroutineScope.launch { UserPreferences.saveImageUri(context, it.toString()) }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is FocusInteraction.Unfocus) isEditing = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 頭像與名稱區
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .height(150.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F2))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(50))
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "使用者圖片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = rememberAsyncImagePainter(model = defaultImageUrl),
                        error = rememberAsyncImagePainter(model = defaultImageUrl)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "預設圖片",
                        tint = Color(0xFF5C5050),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(
                modifier = Modifier.weight(2f).padding(start = 20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (isEditing) {
                    TextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            coroutineScope.launch { UserPreferences.saveUserName(context, it) }
                        },
                        singleLine = true,
                        interactionSource = interactionSource,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Gray,
                            unfocusedIndicatorColor = Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                } else {
                    Text(
                        text = userName.ifBlank { "請輸入姓名" },
                        fontSize = 24.sp,
                        color = Color.Black,
                        modifier = Modifier.clickable { isEditing = true }.padding(vertical = 4.dp)
                    )
                }

                Text("ID : refrigerator113", fontSize = 14.sp, color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ✅ 各選項
        ShowFavoritesScreen(navController)
        SettingOption(navController)
        AboutOption()

        Spacer(modifier = Modifier.height(36.dp))

        // ✅ 登出
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                // ❌ 不需要自己 navigate("login")，因為 MainActivity 的 listener 會自動把 UI 切回 LoginPage
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF2F2F2),
                contentColor = Color.Black
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 32.dp)
                .height(40.dp)
                .fillMaxWidth(0.5f)
        ) {
            Text("登出", fontSize = 16.sp)
        }

    }
}


@Composable
fun ShowFavoritesScreen(navController: NavHostController) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("favorite_recipes")
            }
            .padding(start = 36.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Icon(Icons.Default.Favorite, contentDescription = "最愛食譜", tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Text("最愛食譜", fontSize = 16.sp, color = Color.Black)
    }
}
@Composable
fun SettingOption(navController: NavHostController) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("setting") }
            .padding(start = 36.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Icon(Icons.Default.Settings, contentDescription = "用戶設定", tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Text("用戶設定", fontSize = 16.sp, color = Color.Black)
    }
}

@Composable
fun AboutOption() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { println("ℹ️ 顯示簡介") }
            .padding(start = 36.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Icon(Icons.Default.Info, contentDescription = "簡介", tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(18.dp))
        Text("簡介", fontSize = 16.sp, color = Color.Black)
    }
}
@Composable
fun FavoriteOption(navController: NavHostController) {
    ShowFavoritesScreen(navController)
}
