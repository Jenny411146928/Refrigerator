@file:OptIn(ExperimentalMaterial3Api::class)

package tw.edu.pu.csim.refrigerator.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginPage(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // 鍵盤彈出時避免被擋
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,          // ⬅️ 整個置中
        horizontalAlignment = Alignment.CenterHorizontally // ⬅️ 水平置中
    ) {
        Text("登入 / 註冊", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Text("請輸入 Email 與密碼", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("⚠️ 尚未有帳號的使用者，請先註冊", color = Color.Black, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Email 輸入框
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
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 密碼輸入框
        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("密碼") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Checkbox(
                        checked = passwordVisible,
                        onCheckedChange = { passwordVisible = it },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(" 顯示密碼", fontSize = 14.sp, color = Color.Gray)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFFEBF2F6),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 登入 / 註冊按鈕
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            Toast.makeText(context, "✅ 登入成功！", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                        .addOnFailureListener {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: return@addOnSuccessListener
                                    val userData = hashMapOf(
                                        "email" to email,
                                        "createdAt" to System.currentTimeMillis(),
                                        "sharedFridges" to emptyList<String>()
                                    )
                                    db.collection("users").document(uid).set(userData)
                                    Toast.makeText(context, "🎉 註冊成功，已自動登入！", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "❌ 登入/註冊失敗：${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                } else {
                    Toast.makeText(context, "⚠️ 請輸入帳號與密碼", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .wrapContentWidth()
                .padding(top = 12.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD7E0E5),
                contentColor = Color.Black
            )
        ) {
            Text("登入 / 註冊", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
