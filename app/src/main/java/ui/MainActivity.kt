package ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.example.myapplication.IngredientScreen
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import tw.edu.pu.csim.refrigerator.ui.FridgeCard
import tw.edu.pu.csim.refrigerator.ui.theme.RefrigeratorTheme
import tw.edu.pu.csim.refrigerator.ui.FridgeCardData
import tw.edu.pu.csim.refrigerator.ui.UserPage
import androidx.navigation.NavHostController
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.Ingredient
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.ui.AddCartIngredientsScreen
import tw.edu.pu.csim.refrigerator.ui.CartPageScreen
class MainActivity : ComponentActivity() {
    private val database = Firebase.database.reference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        writeData("message", "Hello, Firebase realdatabase!")
        writeData("user001", mapOf("name" to "Alice", "age" to 25))
        readData("user001")
        setContent {
            RefrigeratorTheme {
                val navController = rememberNavController()
                val foodList = remember { mutableStateListOf<FoodItem>() }
                val cartItems = remember { mutableStateListOf<Ingredient>() }
                AppNavigator(navController = navController, foodList = foodList, cartItems = cartItems)
            }
        }
    }

    private fun writeData(path: String, data: Any) {
        database.child(path).setValue(data)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ 資料成功寫入 $path", Toast.LENGTH_SHORT).show()
                Log.d("Firebase", "資料成功寫入到 $path 路徑")
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ 資料寫入失敗: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "資料寫入失敗: ${it.message}")
            }
    }

    private fun readData(path: String) {
        database.child(path).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "✅ 資料：${snapshot.value}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "❌ 未找到資料", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ 資料讀取失敗: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun AppNavigator(navController: NavHostController, foodList: MutableList<FoodItem>, cartItems: MutableList<Ingredient>) {
    val context = LocalContext.current
    var topBarTitle by rememberSaveable { mutableStateOf("Refrigerator") }
    var selectedItem by rememberSaveable { mutableStateOf(0) }
    var isFabVisible by remember { mutableStateOf(true) }

    val fridgeCardDataSaver: Saver<List<FridgeCardData>, Any> = listSaver(
        save = { list -> list.map { listOf(it.name, it.imageUri?.toString() ?: "") } },
        restore = {
            @Suppress("UNCHECKED_CAST")
            val data = it as List<List<String>>
            data.map { item ->
                FridgeCardData(
                    name = item[0],
                    imageRes = null,
                    imageUri = if (item[1].isNotBlank()) Uri.parse(item[1]) else null
                )
            }
        }
    )

    var fridgeList by rememberSaveable(stateSaver = fridgeCardDataSaver) {
        mutableStateOf(emptyList())
    }

    Scaffold(
        topBar = { CommonAppBar(title = topBarTitle, navController = navController) },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                navController = navController,
                onItemSelected = { index ->
                    selectedItem = index
                    when (index) {
                        0 -> {
                            isFabVisible = true
                            navController.navigate("fridge") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        1 -> {
                            isFabVisible = false
                            navController.navigate("recipe")
                        }
                        2 -> {
                            isFabVisible = false
                            navController.navigate("ingredients")
                        }
                        3 -> {
                            isFabVisible = false
                            navController.navigate("user")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isFabVisible) {
                FloatingActionButton(
                    onClick = {
                        isFabVisible = false
                        navController.navigate("addfridge")
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Fridge")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "fridge",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("fridge") {
                topBarTitle = "首頁"
                isFabVisible = true
                FrontPage(
                    fridgeList = fridgeList,
                    onAddFridge = { fridgeList = fridgeList + it },
                    onDeleteFridge = { fridgeList = fridgeList - it },
                    navController = navController
                )
            }
            composable("recipe") {
                topBarTitle = "食譜"
                RecipePage()
            }
            composable("addfridge") {
                topBarTitle = "新增冰箱"
                isFabVisible = false
                AddFridgePage(
                    onSave = {
                        fridgeList = fridgeList + it
                        navController.popBackStack()
                    },
                    navController = navController
                )
            }
            composable("ingredients") {
                topBarTitle = "瀏覽食材"
                isFabVisible = false
                IngredientScreen(foodList = foodList, navController = navController)
            }
            composable("add") {
                topBarTitle = "新增食材"
                AddIngredientScreen(
                    navController = navController,
                    existingItem = null,
                    isEditing = false,
                    onSave = { newItem -> foodList.add(newItem) }
                )
            }
            composable("edit/{index}") { backStackEntry ->
                topBarTitle = "編輯食材"
                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                val item = index?.let { foodList.getOrNull(it) }
                if (item != null && index != null) {
                    AddIngredientScreen(
                        navController = navController,
                        existingItem = item,
                        isEditing = true,
                        onSave = { updatedItem ->
                            foodList[index] = updatedItem
                            navController.popBackStack()
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }
            composable("user") {
                topBarTitle = "我志己"
                UserPage(navController)
            }
            composable("cart") {
                topBarTitle = "購物清單"
                isFabVisible = false
                CartPageScreen(navController = navController, cartItems = cartItems)
            }
            composable("add_cart_ingredient") {
                topBarTitle = "新增購物食材"
                isFabVisible = false
                AddCartIngredientsScreen(navController = navController) { newItem ->
                    cartItems.add(newItem)
                    // ✅ 修正：儲存後直接跳轉到購物清單頁，並避免重複推入 stack
                    navController.navigate("cart") {
                        popUpTo("cart") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun FrontPage(
    fridgeList: List<FridgeCardData>,
    onAddFridge: (FridgeCardData) -> Unit,
    onDeleteFridge: (FridgeCardData) -> Unit,
    navController: NavController
) {
    val textField1 = remember { mutableStateOf("") }
    var showDeleteFor by remember { mutableStateOf<FridgeCardData?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜尋欄 + Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(1000.dp))
                .fillMaxWidth()
                .background(Color(0xFFD9D9D9))
                .padding(vertical = 7.dp, horizontal = 13.dp)
        ) {
            AsyncImage(
                model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/e346ee13-bedc-4716-997c-3021b1c60805",
                contentDescription = "Search Icon",
                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                error = painterResource(R.drawable.ic_launcher_foreground),
                modifier = Modifier
                    .padding(end = 5.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .size(24.dp)
            )
            TextField(
                value = textField1.value,
                onValueChange = { textField1.value = it },
                placeholder = { Text("搜尋冰箱") },
                textStyle = TextStyle(color = Color(0xFF504848), fontSize = 15.sp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fridgeList.isEmpty()) {
                Text("目前尚未有冰箱，請點擊右下角 + 建立")
            } else {
                fridgeList.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                navController.navigate("ingredients")
                            }
                    ) {
                        FridgeCard(fridge)
                        if (showDeleteFor == fridge) {
                            TextButton(
                                onClick = { onDeleteFridge(fridge) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Text("刪除", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddFridgePage(onSave: (FridgeCardData) -> Unit, navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray)
                .clickable { pickImageLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Fridge Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = "https://img.icons8.com/ios-filled/50/plus-math.png",
                    contentDescription = "Add Image",
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("請輸入冰箱名稱") },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        FridgeCardData(
                            name = name,
                            imageRes = null,
                            imageUri = imageUri
                        )
                    )
                    //navController.popBackStack()
                } else {
                    Toast.makeText(context, "請輸入冰箱名稱", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("加入冰箱")
        }
    }
}

@Composable
fun CommonAppBar(title: String, navController: NavController) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD7E0E5))
            .statusBarsPadding()
            .padding(vertical = 11.dp, horizontal = 24.dp)
    ) {
        Text(
            title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9DA5C1),
            modifier = Modifier.weight(1f)
        )
        AsyncImage(
            model = "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/4faebf02-2554-4a05-ac3b-f30f513a28c3",
            contentDescription = "Cart Icon",
            placeholder = painterResource(R.drawable.ic_launcher_foreground),
            error = painterResource(R.drawable.ic_launcher_foreground),
            modifier = Modifier
                .size(31.dp)
                .clickable { navController.navigate("cart") }, // 👉 新增跳轉
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun BottomNavigationBar(
    selectedItem: Int,
    navController: NavController?,
    onItemSelected: (Int) -> Unit
) {
    val routes = listOf("fridge", "recipe", "recommend", "user")
    val icons = listOf(
        "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/05ddb832-37fe-47c3-8220-028ff10b3a3b", // 冰箱
        "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/491f290c-7773-45bc-8cb9-26245e94863c", // 食譜
        "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/af697626-7bdb-47a8-aa3c-f54f66e0eb6a", // 推薦
        "https://figma-alpha-api.s3.us-west-2.amazonaws.com/images/d087a83c-ddf3-4ec4-95b4-c114aa377ef5"  // 個人
    )

    NavigationBar(containerColor = Color(0xFFF5F0F5)) {
        icons.forEachIndexed { index, iconUrl ->
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = {
                    onItemSelected(index)
                    navController?.navigate(routes[index]) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    AsyncImage(
                        model = iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFD6CCEA),
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.DarkGray
                )
            )
        }
    }
}
