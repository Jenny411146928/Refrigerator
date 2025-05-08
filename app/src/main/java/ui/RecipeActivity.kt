package ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.ui.theme.RefrigeratorTheme

class RecipeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefrigeratorTheme {
                RecipePage()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePage() {
    val textField1 = remember { mutableStateOf("") }

    val recipes = listOf(
        Pair("番茄炒蛋", "https://i.imgur.com/zMZxU8v.jpg"),
        Pair("義大利麵", "https://i.imgur.com/8QO4YDa.jpg"),
        Pair("番茄炒蛋", "https://i.imgur.com/zMZxU8v.jpg"),
        Pair("義大利麵", "https://i.imgur.com/8QO4YDa.jpg")
    )

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // 搜尋欄
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .fillMaxWidth()
                .background(Color(0xFFD9D9D9))
                .padding(vertical = 7.dp, horizontal = 13.dp)
        ) {
            AsyncImage(
                model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/e346ee13-bedc-4716-997c-3021b1c60805",
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            TextField(
                value = textField1.value,
                onValueChange = { textField1.value = it },
                placeholder = { Text("搜尋食譜") },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(fontSize = 15.sp, color = Color.Black)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 食譜卡片 Grid
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
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(" 503", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}