package ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePage(navController: NavController) {
    val searchText = remember { mutableStateOf("") }

    // TODO: 之後改成 Firestore 資料來源
    val recipes = listOf(
        Triple("番茄炒蛋", "https://i.imgur.com/zMZxU8v.jpg", "id1"),
        Triple("義大利麵", "https://i.imgur.com/8QO4YDa.jpg", "id2"),
        Triple("紅燒牛肉", "https://i.imgur.com/9yD1b5r.jpg", "id3"),
        Triple("炒青菜", "https://i.imgur.com/g8Kzp8a.jpg", "id4")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 🔍 搜尋欄（僅顯示 placeholder「搜尋食譜」）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(1000.dp))
                .background(Color(0xFFD9D9D9))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = "Search Icon",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(22.dp),
                tint = Color.Unspecified
            )
            TextField(
                value = searchText.value,
                onValueChange = { searchText.value = it },
                placeholder = { Text("搜尋食譜") },
                textStyle = TextStyle(color = Color(0xFF504848), fontSize = 15.sp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🍽️ 食譜卡片 Grid（只顯示「圖片＋標題」）
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(recipes) { recipe ->
                Column(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color(0xFFEAEAEA))
                        .clickable {
                            // 導向詳情（使用 id）
                            navController.navigate("recipeDetail/${recipe.third}")
                        }
                ) {
                    AsyncImage(
                        model = recipe.second,
                        contentDescription = recipe.first,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = recipe.first,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
