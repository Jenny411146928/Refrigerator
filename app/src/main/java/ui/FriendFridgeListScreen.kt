package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.ui.AddID

data class FriendFridge(
    val id: String = "",
    val name: String = "",
    val ownerName: String = "",
    val imageUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendFridgeListScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val friendFridges = remember { mutableStateListOf<FriendFridge>() }
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showAddFriendSheet by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        try {
            val docs = db.collection("users")
                .document(uid ?: return@LaunchedEffect)
                .collection("sharedFridges")
                .get()
                .await()

            val list = docs.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: "好友冰箱"
                val owner = doc.getString("ownerName") ?: "未知"
                val image = doc.getString("imageUrl")
                FriendFridge(doc.id, name, owner, image)
            }
            friendFridges.clear()
            friendFridges.addAll(list)
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFriendSheet = true },
                containerColor = Color(0xFFD1DAE6),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(60.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_add_24),
                    contentDescription = "新增好友",
                    tint = Color(0xFF444B61),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        ,
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFABB7CD))
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = 12.dp,
                            start = 16.dp,
                            end = 16.dp
                        )                ) {
                    // 返回鍵
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(top = 1.dp, bottom = 6.dp)
                            .size(30.dp)
                            .align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF444B61)
                        )
                    }

                    // ✅ 搜尋框
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("搜尋冰箱") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF444B61)),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "search",
                                tint = Color.Gray
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color(0xFFF2F2F2),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50.dp))
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    val filteredList = friendFridges.filter {
                        searchText.isBlank() ||
                                it.name.contains(searchText, ignoreCase = true) ||
                                it.ownerName.contains(searchText, ignoreCase = true)
                    }

                    if (filteredList.isEmpty()) {
                        Text(
                            "目前沒有好友冰箱",
                            color = Color(0xFF7A869A),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        filteredList.forEach { fridge ->
                            FriendFridgeCard(
                                fridge = fridge,
                                onClick = {
                                    navController.navigate("friendFridgeIngredients/${fridge.id}")
                                },
                                onDelete = { fridgeId ->
                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(uid!!)
                                        .collection("sharedFridges")
                                        .document(fridgeId)
                                        .delete()
                                        .addOnSuccessListener {
                                            refreshTrigger++     // 🔥 立即刷新
                                        }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
    // ========================================================
    // 👥 加好友 BottomSheet（由下往上彈出）
    // ========================================================
    if (showAddFriendSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddFriendSheet = false },
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight(0.85f) // 彈出的高度
        ) {

            AddID(
                onClose = { showAddFriendSheet = false },
                onSearch = { /* 不用處理，AddID 內有處理 */ },
                onAdded = {
                    refreshTrigger++
                    // 加完好友後自動刷新並關閉 BottomSheet
                    showAddFriendSheet = false
                },
                existingFridgeIds = friendFridges.map { it.id }
            )
        }
    }
}

@Composable
fun FriendFridgeCard(fridge: FriendFridge, onClick: () -> Unit,onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE3E6ED))
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fridge.imageUrl != null) {
                AsyncImage(
                    model = fridge.imageUrl,
                    contentDescription = "Fridge",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDADFE8))
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(fridge.name, fontWeight = FontWeight.Bold, color = Color(0xFF444B61))
                Text("擁有者：${fridge.ownerName}", color = Color(0xFF7A869A), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_delete_24),
                    contentDescription = "Delete Friend Fridge",
                    tint = Color(0xFFE57373) // 🔥 更柔和的紅色（不刺眼）
                )
            }
        }
    }

    // 🔥 Alert Dialog（確認刪除）
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(fridge.id)
                    showDeleteDialog = false
                }) {
                    Text("確定刪除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text("刪除好友冰箱") },
            text = { Text("你確定要刪除此冰箱嗎？") }
        )
    }
}