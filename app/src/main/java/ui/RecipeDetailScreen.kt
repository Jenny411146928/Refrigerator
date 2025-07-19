package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R

@Composable
fun RecipeDetailScreen(
    recipeName: String,
    imageUrl: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    ingredients: List<Pair<String, Boolean>>,
    steps: List<String>,
    onAddToCart: (FoodItem) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.height(250.dp)) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .padding(16.dp)
                        .size(36.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White, shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite() },
                    modifier = Modifier
                        .padding(16.dp)
                        .size(36.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.White, shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = Color(0xFFDC143C)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp)
            ) {
                Text(recipeName, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp), // 整體左右留空
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // 每個卡片間距 8.dp
                ) {
                    InfoCard(drawableResId = R.drawable.restaurantmenu, label = "簡單", modifier = Modifier.weight(1f))
                    InfoCard(drawableResId = R.drawable.schedule, label = "40 分鐘", modifier = Modifier.weight(1f))
                    InfoCard(drawableResId = R.drawable.fire, label = "4 人份", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("食材", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ingredients.forEach { (name, owned) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontSize = 16.sp)
                        Icon(
                            imageVector = if (owned) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.clickable(enabled = !owned) {
                                onAddToCart(FoodItem(name = name))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("作法", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                steps.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", fontSize = 16.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun InfoCard(drawableResId: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F2))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = drawableResId),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Black, fontSize = 14.sp)
    }
}
