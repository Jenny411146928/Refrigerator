package tw.edu.pu.csim.refrigerator.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import coil.compose.AsyncImage
import tw.edu.pu.csim.refrigerator.R

@Composable
fun FridgeCard(fridge: FridgeCardData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0E0E0))
    ) {
        if (fridge.imageUri != null) {
            AsyncImage(
                model = fridge.imageUri,
                contentDescription = fridge.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        } else if (fridge.imageRes != null) {
            AsyncImage(
                model = fridge.imageRes,
                contentDescription = fridge.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }

        Text(
            text = fridge.name,
            modifier = Modifier.padding(12.dp),
            fontSize = 18.sp
        )
    }
}


@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.account), contentDescription = "Account") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.refrigerator), contentDescription = "Home") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.recipe), contentDescription = "Recommand") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.recipe), contentDescription = "Recipe") },
            selected = false,
            onClick = {}
        )
    }
}


@Composable
fun FridgeCardList(fridges: List<FridgeCardData>) {
    Column(modifier = Modifier.padding(16.dp)) {
        fridges.forEach { fridge ->
            FridgeCard(fridge)
        }
    }
}

@Composable
fun JoinFridgeSection() {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("輸入想加入的冰箱ID") },
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = { /* TODO: 加入冰箱功能 */ },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("加入")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar() {
    TopAppBar(
        title = { Text("Refrigerator", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        actions = {
            IconButton(onClick = { /* TODO: 購物車功能 */ }) {
                Icon(painterResource(R.drawable.cart), contentDescription = "購物車")
            }
        }
    )
}

@Composable
fun Frame10() {
    Box(modifier = Modifier.fillMaxWidth().height(60.dp).padding(16.dp)) {
        Text("此區域待實作：Figma設計")
    }
}

@Composable
fun FrontPage() {
    val fridgeCards = listOf(
        FridgeCardData("蔡譯嫺's fridge", R.drawable.refrigerator),
    )
    Scaffold(
        topBar = { AppBar() },
        bottomBar = { BottomNavigationBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Frame10()
            FridgeCardList(fridgeCards)
        }
    }
}

data class FridgeCardData(
    val name: String,
    val imageRes: Int? = null,
    val imageUri: Uri? = null
)
