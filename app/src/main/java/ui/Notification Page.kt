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
    notifications: List<NotificationItem>,
    selectedFridgeId: String
) {

    var localList by remember { mutableStateOf(listOf<NotificationItem>()) }
    LaunchedEffect(notifications) {
        localList = notifications
    }

    val filtered = localList.filter { it.fridgeId == selectedFridgeId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { navController.popBackStack() }
            )
        }

        Text(
            text = "通知",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        Divider(color = Color(0xFFDDDDDD), thickness = 1.dp)

        val sorted = filtered.sortedWith(
            compareBy<NotificationItem> { it.daysLeft < 0 }
                .thenBy { it.daysLeft == 0 }
                .thenBy { it.daysLeft }
        ).reversed()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (sorted.isEmpty()) {
                item { Text("目前沒有通知") }
            } else {
                items(sorted, key = { it.id }) { notif ->
                    NotificationCard(notif) {
                        navController.navigate("ingredients?highlight=${notif.targetName}")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    val bgColor = when {
        item.daysLeft < 0 -> Color(0xFFFFE5E5)
        item.daysLeft <= 3 -> Color(0xFFFFF6D8)
        else -> Color(0xFFE3E6ED)
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
                when {
                    item.daysLeft < 0 -> "已過期"
                    item.daysLeft == 0 -> "今天到期"
                    else -> "剩 ${item.daysLeft} 天"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.daysLeft < 0) Color.Red else Color.DarkGray
            )
        }
    }
}

