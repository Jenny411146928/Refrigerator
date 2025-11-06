package tw.edu.pu.csim.refrigerator.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.firebase.FirebaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddID(
    onClose: () -> Unit,
    onSearch: (String) -> Unit,
    onAdded: () -> Unit,
    existingFridgeIds: List<String>
) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) } // 🔹 搜尋中狀態
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 標題列
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜尋好友冰箱",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                color = Color.Black
            )
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 搜尋框區塊
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFF2F2F2))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = "Search Icon",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )

            TextField(
                value = searchText,
                onValueChange = { text ->
                    searchText = text
                    searchJob?.cancel()

                    if (text.trim().isNotEmpty()) {
                        isSearching = true
                        searchJob = scope.launch {
                            delay(400) // 🔸 防止每次打字立即觸發
                            try {
                                val resultList = FirebaseManager.searchFridgeByEmail(text.trim())
                                searchResults = resultList
                            } catch (e: Exception) {
                                Toast.makeText(context, "搜尋失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSearching = false
                            }
                        }
                    } else {
                        searchResults = emptyList()
                        isSearching = false
                    }
                },
                placeholder = { Text("請輸入好友信箱") },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                )
            )

            Button(
                onClick = {
                    if (searchText.isBlank()) {
                        Toast.makeText(context, "請輸入好友信箱", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSearching = true
                    scope.launch {
                        try {
                            val resultList = FirebaseManager.searchFridgeByEmail(searchText.trim())
                            searchResults = resultList
                        } catch (e: Exception) {
                            Toast.makeText(context, "搜尋失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSearching = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBCC7D7),
                    contentColor = Color.Black
                ),
                modifier = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("搜尋")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 結果顯示區域
        when {
            isSearching -> {
                // 🔹 顯示圓形 Loading 動畫
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFBCC7D7),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            searchResults.isNotEmpty() -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(searchResults) { result ->
                        val name = result["name"]?.toString() ?: "未命名冰箱"
                        val id = result["id"]?.toString() ?: ""
                        val imageUrl = result["imageUrl"]?.toString()
                        val ownerName = result["ownerName"]?.toString() ?: "未知使用者"
                        val isAlreadyAdded = existingFridgeIds.contains(id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF7F7F7))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.LightGray)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("ID：$id", fontSize = 13.sp, color = Color.Gray)
                                Text("擁有者：$ownerName", fontSize = 13.sp, color = Color.Gray)
                            }

                            Button(
                                onClick = {
                                    if (!isAlreadyAdded) {
                                        if (currentUser == null) {
                                            Toast.makeText(context, "請先登入", Toast.LENGTH_SHORT)
                                                .show()
                                            return@Button
                                        }

                                        scope.launch {
                                            try {
                                                val uid = currentUser.uid
                                                val sharedRef = db.collection("users")
                                                    .document(uid)
                                                    .collection("sharedFridges")
                                                    .document(id)

                                                sharedRef.set(result)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(
                                                            context,
                                                            "已成功加入好友冰箱！",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        onAdded()
                                                        onClose()
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(
                                                            context,
                                                            "加入失敗：${it.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "加入失敗：${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isAlreadyAdded,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD1DAE6),
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFFDEE2E6),
                                    disabledContentColor = Color.Gray
                                )
                            ) {
                                Text(if (isAlreadyAdded) "已加入" else "加入")
                            }
                        }
                    }
                }
            }
            searchText.isNotBlank() && !isSearching && searchResults.isEmpty() -> {
                // 搜尋過但沒找到結果 → 顯示提示文字
                Box(
                    modifier = Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "請確認對方已註冊帳號",
                        color = Color.Gray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            else -> {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}

