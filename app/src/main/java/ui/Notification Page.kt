package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.R

@Composable
fun NotificationPage(
    navController: NavController,
    notifications: List<NotificationItem>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 左上角返回
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close), // 建議改用本地資源
                contentDescription = "Close",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { navController.popBackStack() }
            )
        }

        // 標題
        Text(
            text = "通知",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        Divider(
            color = Color(0xFFDDDDDD), // 淺灰色
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )

        // 通知列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Text("目前沒有通知")
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    NotificationCard(notif) {
                        navController.navigate("ingredients")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    val bgColor = when {
        item.daysLeft <= 0 -> Color(0xFFFFCDD2) // 已過期（紅）
        item.daysLeft <= 3 -> Color(0xFFFFF8E1) // 即將過期（黃）
        else -> Color.White
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ 如果有圖片 → 用 AsyncImage，否則用冰箱 icon
            if (!item.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "Food Image",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.refrigerator),
                    contentDescription = "Food Icon",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(item.message, color = Color.Gray, fontSize = 14.sp)
            }

            Text(
                if (item.daysLeft < 0) "已過期" else "剩 ${item.daysLeft} 天",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.daysLeft < 0) Color.Red else Color.DarkGray
            )
        }
    }
}

