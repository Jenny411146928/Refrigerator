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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginPage(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize().imePadding().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("登入", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("請輸入 Email 與密碼", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("⚠️ 尚未有帳號？請點擊下方去註冊", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Email
        TextField(
            value = email, onValueChange = { email = it }, placeholder = { Text("Email") },
            singleLine = true, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(containerColor = Color(0xFFEBF2F6))
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 密碼
        TextField(
            value = password, onValueChange = { password = it }, placeholder = { Text("密碼") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Checkbox(checked = passwordVisible, onCheckedChange = { passwordVisible = it })
            },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(containerColor = Color(0xFFEBF2F6))
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 登入按鈕
        Button(
            onClick = {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "請輸入正確 Email", Toast.LENGTH_SHORT).show(); return@Button
                }
                if (email.isNotBlank() && password.isNotBlank()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val user = result.user
                            if (user != null && user.isEmailVerified) {
                                Toast.makeText(context, "✅ 登入成功！", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "⚠️ 請先驗證信箱", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "❌ 登入失敗：${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD7E0E5))
        ) { Text("登入", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 重新寄送驗證信
        TextButton(
            onClick = {
                val user = auth.currentUser
                if (user != null && !user.isEmailVerified) {
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            Toast.makeText(context, "📩 驗證信已重新寄送", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "❌ 發送失敗：${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "請先登入帳號才能重新寄送驗證信", Toast.LENGTH_SHORT).show()
                }
            }
        ) { Text("重新寄送驗證信", fontSize = 14.sp, color = Color.Gray) }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("還沒有帳號？去註冊 →", fontSize = 14.sp)
        }
    }
}
