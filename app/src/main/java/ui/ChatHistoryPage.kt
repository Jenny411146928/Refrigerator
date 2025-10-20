@file:OptIn(ExperimentalMaterial3Api::class)
package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatHistoryPage(
    navController: NavController,
    onSelectDate: (String) -> Unit
) {
    // ✅ 以台灣時區生成今天日期
    val tz = TimeZone.getTimeZone("Asia/Taipei")
    val today = remember {
        SimpleDateFormat("yyyyMMdd", Locale.TAIWAN).apply {
            timeZone = tz
        }.format(Date())
    }

    // ✅ 過去 7 天的日期清單（今天 + 前 6 天）
    val dateList = remember {
        (0..6).map { i ->
            val cal = Calendar.getInstance(tz)
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val date = cal.time
            val id = SimpleDateFormat("yyyyMMdd", Locale.TAIWAN).apply {
                timeZone = tz
            }.format(date)
            val label = SimpleDateFormat("MM/dd (E)", Locale.TAIWAN).apply {
                timeZone = tz
            }.format(date)
            id to label
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("歷史聊天紀錄", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE3E6ED))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dateList) { (id, label) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // ✅ 點選日期時呼叫回調並返回 ChatPage
                            onSelectDate(id)
                            navController.navigate("chat") {
                                popUpTo("chat_history") { inclusive = true }
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (id == today) Color(0xFFE3E6ED) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (id == today) "📅 今天 ($label)" else "🗓 $label",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (id == today) Color(0xFF44506E) else Color.Black
                        )
                    }
                }
            }
        }
    }
}
