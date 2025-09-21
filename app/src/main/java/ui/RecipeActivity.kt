package ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import tw.edu.pu.csim.refrigerator.feature.recipe.RecipeListVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePage(
    navController: NavController,
    vm: RecipeListVM = viewModel()   // 使用 ViewModel
) {
    // ✅ 補上 initial 避免型別不明確
    val recipes = vm.filtered()
    val searchText by vm.query.collectAsState(initial = "")
    val loading by vm.loading.collectAsState(initial = false)

    val gridState = rememberLazyGridState()

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
                value = searchText,
                onValueChange = { vm.setQuery(it) }, // ✅ 用 ViewModel 更新
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

        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    Column(
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color(0xFFEAEAEA))
                            .clickable {
                                val encodedId = Uri.encode(recipe.id)
                                navController.navigate("recipeDetailById/$encodedId")
                            }
                    ) {
                        AsyncImage(
                            model = recipe.imageUrl ?: "https://i.imgur.com/zMZxU8v.jpg",
                            contentDescription = recipe.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = recipe.title,
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
}
