package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.R

@Composable
fun RecipePage(navController: NavController) {
    val recipes = listOf(
        Pair("番茄炒蛋", "https://i.imgur.com/zMZxU8v.jpg"),
        Pair("義大利麵", "https://i.imgur.com/0MZ3Uac.jpg"),
        Pair("炒青菜", "https://i.imgur.com/hz5Q9uG.jpg"),
        Pair("燒肉飯", "https://i.imgur.com/IEphKyr.jpg")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("推薦食譜", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recipes) { (title, imageUrl) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 假設 title 當作 recipeId 傳入詳情頁
                            navController.navigate("recipeDetail/${title}")
                        }
                ) {
                    Column {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Text(
                            text = title,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
