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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePage(navController: NavController) {
    val searchText = remember { mutableStateOf("") }

    val recipes = listOf(
        Pair("番茄炒蛋", "https://i.imgur.com/zMZxU8v.jpg"),
        Pair("義大利麵", "https://i.imgur.com/8QO4YDa.jpg"),
        Pair("紅燒牛肉", "https://i.imgur.com/9yD1b5r.jpg"),
        Pair("炒青菜", "https://i.imgur.com/g8Kzp8a.jpg")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 🔍 搜尋欄
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

        // 🍽️ 食譜卡片 Grid
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
                            // ✅ 導向 RecipeDetail 畫面（帶入參數）
                            val encodedUrl = Uri.encode(recipe.second)
                            navController.navigate("recipeDetail/${recipe.first}/$encodedUrl")
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(recipe.first)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.heart),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Unspecified
                            )
                            Text(" 503", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
