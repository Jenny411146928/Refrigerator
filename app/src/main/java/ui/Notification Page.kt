package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@Composable
fun NotificationPage(navController: NavController, notifications: List<NotificationItem>) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("全部", "個人通知")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 左上角關閉
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp)
        ) {
            AsyncImage(
                model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/a3ae89c3-ad58-4d75-a179-fab7f702d326",
                contentDescription = "Close Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        navController.navigate("fridge") {
                            popUpTo("fridge") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
            )
        }

        // 標題
        Text(
            text = "通知",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        // Tab
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = Color(0xFF9DA5C1),
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTabIndex])
                        .height(3.dp)
                        .background(Color(0xFF9DA5C1))
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) Color.Black else Color.Gray
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    if (notifications.isEmpty()) {
                        Text("目前沒有通知")
                    } else {
                        Column {
                            notifications.forEach {
                                Text("🔔 ${it.title}", fontWeight = FontWeight.Bold)
                                Text(it.message, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
                            }
                        }
                    }
                }
                1 -> Text("這裡是個人通知")
            }
        }
    }
}
