package tw.edu.pu.csim.refrigerator.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.firebase.FirebaseManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddID(
    onClose: () -> Unit,
    onSearch: (String) -> Unit,
    onAdded: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<Map<String, Any>?>(null) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f) // 佔螢幕一半高度
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

        // 搜尋框 + 按鈕 一體式
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

            Spacer(modifier = Modifier.height(6.dp))

            TextField(
                value = searchText,
                onValueChange = { searchText = it
                    // ✅ 即時搜尋：輸入文字時立刻觸發
                    scope.launch {
                        if (searchText.trim().isNotEmpty()) {
                            try {
                                val resultList = FirebaseManager.searchFridgeByEmail(searchText.trim())
                                searchResult = if (resultList.isNotEmpty()) resultList.first() else null
                            } catch (e: Exception) {
                                Toast.makeText(context, "搜尋失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            searchResult = null
                        }
                    }
                },
                placeholder = { Text("請輸入好友冰箱ID") },
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
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
            )
            Button(
                onClick = {
                    if (searchText.isBlank()) {
                        Toast.makeText(context, "請輸入好友信箱", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // ✅ 這行是用來驗證按鈕有被點擊
                    Log.d("AddID", "🔍 搜尋開始：$searchText")

                    scope.launch {
                        try {
                            val resultList = FirebaseManager.searchFridgeByEmail(searchText)
                            if (resultList.isNotEmpty()) {
                                searchResult = resultList.first()
                            } else {
                                searchResult = null
                                Toast.makeText(context, "查無此冰箱 ID", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "搜尋失敗: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBCC7D7),
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text("搜尋")
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 顯示搜尋結果
        if (searchResult != null) {
            val name = searchResult?.get("name")?.toString() ?: "未命名冰箱"
            val id = searchResult?.get("id")?.toString() ?: ""
            val imageUrl = searchResult?.get("imageUrl")?.toString()
            val ownerName = searchResult?.get("ownerName")?.toString() ?: "未知使用者"

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
                        if (currentUser == null) {
                            Toast.makeText(context, "請先登入", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            try {
                                val uid = currentUser.uid
                                val sharedRef = db.collection("users")
                                    .document(uid)
                                    .collection("sharedFridges")
                                    .document(id)

                                sharedRef.set(searchResult!!)
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
                                Toast.makeText(context, "加入失敗：${e.message}", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD1DAE6),
                        contentColor = Color.Black
                    )
                ) {
                    Text("加入")
                }
            }
        } else {
            Text(
                text = if (searchText.isEmpty()) "請輸入好友冰箱ID以搜尋" else "未找到符合的冰箱",
                style = TextStyle(color = Color.Gray, fontSize = 14.sp),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}