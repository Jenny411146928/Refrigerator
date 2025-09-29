package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecipeRecommendationCard(
    recipeName: String,
    detail: String?,
    onViewDetail: () -> Unit,
    onCollapse: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFD9DEE8)) // 灰藍色背景
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                recipeName,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFE082)) // 鵝黃色按鈕
                    .clickable {
                        if (expanded) {
                            expanded = false
                            onCollapse()
                        } else {
                            expanded = true
                            onViewDetail()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (expanded) "收合" else "查看",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }

        if (expanded && detail != null) {
            Spacer(Modifier.height(8.dp))
            Text(detail, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}
