package tw.edu.pu.csim.refrigerator.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tw.edu.pu.csim.refrigerator.R

@Composable
fun FrontPage() {
    val fridgeCards = listOf(
        FridgeCardData("蔡譯嫺's fridge", R.drawable.hsfridgebg),
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
            Frame10() // 使用 Figma 設計的 UI 來取代 JoinFridgeSection
            FridgeCardList(fridgeCards)
        }
    }
}

@Composable
fun Frame10() {
    //TODO("Not yet implemented")
    Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp)) {
        Text("此區域待實作：Figma設計")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar() {
    TopAppBar(
        title = { Text("Refrigerator", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        actions = {
            IconButton(onClick = { /* TODO: 購物車功能 */ }) {
                Icon(painterResource(R.drawable.shoppingcarticon), contentDescription = "購物車")
            }
        }
    )
}

@Composable
fun JoinFridgeSection() {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // 修正錯誤
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

@Composable
fun FridgeCardList(fridges: List<FridgeCardData>) {
    Column(modifier = Modifier.padding(16.dp)) {
        fridges.forEach { fridge ->
            FridgeCard(fridge)
        }
    }
}

@Composable
fun FridgeCard(fridge: FridgeCardData) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { /* TODO: 開啟冰箱詳情 */ }
    ) {
        Column {
            Image(
                painter = painterResource(fridge.imageRes),
                contentDescription = fridge.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(fridge.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.accounticon), contentDescription = "Account") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.homeicon), contentDescription = "Home") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.reccomandicon), contentDescription = "Recommand") },
            selected = false,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.recipeicon), contentDescription = "Recipe") },
            selected = false,
            onClick = {}
        )
    }
}

data class FridgeCardData(val name: String, val imageRes: Int)
