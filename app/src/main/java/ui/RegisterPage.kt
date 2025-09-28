//RegisterPage.kt

@file:OptIn(ExperimentalMaterial3Api::class)
package tw.edu.pu.csim.refrigerator.ui

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterPage(onRegisterSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 標題
        Text("註冊", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Text("建立新帳號以使用所有功能", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("⚠️ 註冊後會寄送驗證信，請先完成驗證再登入", color = Color.Black, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // 姓名
        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("姓名") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFFEBF2F6),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFFEBF2F6),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 密碼
        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("密碼") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFFEBF2F6),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 註冊按鈕（保留原本邏輯）
        Button(
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "請輸入姓名", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "請輸入正確的 Email 格式", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.length < 6) {
                    Toast.makeText(context, "密碼至少需要 6 碼", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        val uid = user?.uid ?: return@addOnSuccessListener
                        val userData = mapOf(
                            "name" to name,
                            "email" to email,
                            "createdAt" to System.currentTimeMillis(),
                            "sharedFridges" to emptyList<String>()
                        )

                        db.collection("users").document(uid).set(userData)
                            .addOnSuccessListener {
                                user?.sendEmailVerification()
                                    ?.addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "📩 驗證信已寄出，請至信箱確認",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        auth.signOut()
                                        onBackToLogin()
                                    }
                                    ?.addOnFailureListener { e ->
                                        Toast.makeText(
                                            context,
                                            "❌ 驗證信失敗：${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "❌ 使用者資料儲存失敗：${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        val message = if (e.message?.contains("already in use") == true) {
                            "⚠️ 這個 Email 已經被註冊過了，請直接登入"
                        } else {
                            "❌ 註冊失敗：${e.message}"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD7E0E5),
                contentColor = Color.Black
            )
        ) {
            Text("註冊", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text("已經有帳號？去登入 →", fontSize = 14.sp)
        }
    }
}
