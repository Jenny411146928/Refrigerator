package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AboutPage(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "關於我們",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B5E72),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Refrigerator 是一款智慧冰箱管理應用程式，\n" +
                        "協助你輕鬆管理冰箱中的食材，避免浪費食物，\n" +
                        "並提供食譜推薦、購物清單與 AI 聊天助理等功能，\n" +
                        "讓料理變得更簡單、更有樂趣！",
                fontSize = 16.sp,
                color = Color.Black,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "開發團隊",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4B5E72),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "👩‍💻 成員：冰箱專題團隊\n📍 技術：Kotlin · Jetpack Compose · Firebase · OpenAI API",
                fontSize = 15.sp,
                color = Color.DarkGray,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "© 2025 Refrigerator Project",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
