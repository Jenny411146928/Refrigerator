@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.R

@Composable
fun FridgeCard(
    fridge: FridgeCardData,
    onEdit: (FridgeCardData) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0E0E0))
            .height(160.dp)
    ) {
        if (fridge.imageUri != null || fridge.imageRes != null || fridge.imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fridge.imageUri ?: fridge.imageUrl ?: fridge.imageRes)
                    .crossfade(true)
                    .build(),
                contentDescription = fridge.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFD9D9D9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.refrigerator),
                    contentDescription = "Default Fridge",
                    tint = Color(0xFF9DA5C1),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.35f))
        )

        Text(
            text = "ID：${fridge.id}",
            fontSize = 12.sp,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Text(
            text = fridge.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 8.dp)
                .clickable { onEdit(fridge) }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FrontPage(
    fridgeList: List<FridgeCardData>,
    onAddFridge: (FridgeCardData) -> Unit,
    onDeleteFridge: (FridgeCardData) -> Unit,
    onFridgeClick: (String) -> Unit,
    navController: NavController
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf<FridgeCardData?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && showEditDialog != null) {
            showEditDialog = showEditDialog!!.copy(imageUri = uri)
        }
    }

    val myFridges = fridgeList.filter { it.ownerId == currentUserId }
    val friendFridges = fridgeList.filter { it.ownerId != currentUserId }

    Column(modifier = Modifier.fillMaxSize()) {

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("搜尋冰箱") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(50.dp)),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search Icon",
                    tint = Color.Gray
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF2F2F2),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Text(
            text = "⭐ 主冰箱",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A6B87),
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
        )

        if (myFridges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD9D9D9))
                    .height(180.dp)
                    .clickable { navController.navigate("addfridge") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新增主要冰箱",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                myFridges.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onFridgeClick(fridge.id) }
                    ) {
                        FridgeCard(fridge)

                        IconButton(
                            onClick = { showEditDialog = fridge },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "編輯冰箱",
                                tint = Color(0xFF5A6B87),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "👥 好友冰箱",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A6B87),
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
        )

        if (friendFridges.isEmpty()) {
            Text(
                text = "目前沒有好友冰箱",
                color = Color.Gray,
                modifier = Modifier.padding(start = 24.dp, top = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                friendFridges.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onFridgeClick(fridge.id) }
                    ) {
                        FridgeCard(fridge)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color(0xFFB0C4DE), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("好友", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog != null) {
        var editedName by remember { mutableStateOf(showEditDialog!!.name) }
        var showConfirmDelete by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("編輯冰箱", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3E6ED))
                            .clickable { pickImageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (showEditDialog!!.imageUri != null || showEditDialog!!.imageUrl != null) {
                            AsyncImage(
                                model = showEditDialog!!.imageUri ?: showEditDialog!!.imageUrl,
                                contentDescription = "Fridge Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.refrigerator),
                                contentDescription = "Default Image",
                                tint = Color(0xFF9DA5C1),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("冰箱名稱") },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color(0xFFF4F5F7),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showConfirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "刪除冰箱", tint = Color.Red)
                        Text("刪除冰箱", color = Color.Red)
                    }

                    if (showConfirmDelete) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDelete = false },
                            title = { Text("確認刪除") },
                            text = { Text("確定要刪除這個冰箱嗎？此動作會同步刪除好友的共享副本！") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showConfirmDelete = false
                                    showEditDialog?.let { fridgeToDelete ->
                                        scope.launch {
                                            try {
                                                tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.deleteFridgeAndSync(fridgeToDelete.id)
                                                Toast.makeText(context, "冰箱已刪除並同步好友端", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "刪除失敗：${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        onDeleteFridge(fridgeToDelete)
                                        showEditDialog = null
                                    }
                                }) {
                                    Text("確定刪除", color = Color.Red)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmDelete = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val updatedFridge = showEditDialog!!.copy(name = editedName)

                    val index = fridgeList.indexOfFirst { it.id == updatedFridge.id }
                    if (index != -1) {
                        (fridgeList as MutableList)[index] = updatedFridge
                    }

                    scope.launch {
                        try {
                            tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.updateFridgeInfo(
                                fridgeId = updatedFridge.id,
                                newName = editedName,
                                newImageUri = updatedFridge.imageUri
                            )

                            val (myData, friendData) =
                                tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.getUserFridges()
                            val allFridges = (myData + friendData).map {
                                FridgeCardData(
                                    id = it["id"].toString(),
                                    name = it["name"].toString(),
                                    imageUrl = it["imageUrl"]?.toString(),
                                    ownerId = it["ownerId"]?.toString(),
                                    ownerName = it["ownerName"]?.toString(),
                                    editable = it["editable"] as? Boolean ?: true
                                )
                            }
                            (fridgeList as MutableList).clear()
                            (fridgeList as MutableList).addAll(allFridges)

                            Toast.makeText(context, "✅ 冰箱資料已同步更新", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "❌ 更新失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    showEditDialog = null
                }) {
                    Text("儲存", color = Color(0xFF5A6B87))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}