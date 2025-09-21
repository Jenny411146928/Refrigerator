@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
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
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.myapplication.IngredientScreen
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.FoodItem
import tw.edu.pu.csim.refrigerator.R
import tw.edu.pu.csim.refrigerator.ui.AddCartIngredientsScreen
import tw.edu.pu.csim.refrigerator.ui.CartPageScreen
import tw.edu.pu.csim.refrigerator.ui.ChatPage
import tw.edu.pu.csim.refrigerator.ui.ChatViewModel
import tw.edu.pu.csim.refrigerator.ui.FridgeCard
import tw.edu.pu.csim.refrigerator.ui.FridgeCardData
import tw.edu.pu.csim.refrigerator.ui.RecipeDetailScreen
import tw.edu.pu.csim.refrigerator.ui.UserPage
import tw.edu.pu.csim.refrigerator.ui.theme.RefrigeratorTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.refrigerator.ui.LoginPage

class MainActivity : ComponentActivity() {

    // 你的 Realtime DB（保留）
    private val database = Firebase.database.reference
    private val chatViewModel: ChatViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 你原本的測試（保留，不影響）
        writeData("message", "Hello, Firebase realdatabase!")
        writeData("user001", mapOf("name" to "Alice", "age" to 25))
        readData("user001")

        setContent {
            RefrigeratorTheme {
                val navController = rememberNavController()
                val fridgeFoodMap = remember { mutableStateMapOf<String, MutableList<FoodItem>>() }
                val cartItems = remember { mutableStateListOf<FoodItem>() }

                // 🔹 這裡新增 FirebaseAuth 與狀態監聽
                val auth = FirebaseAuth.getInstance()
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { fb ->
                        val user = fb.currentUser
                        isLoggedIn = user != null

                        if (user != null) {
                            // ✅ 登入成功後 → 強制跳到首頁
                            navController.navigate("fridge") {
                                popUpTo(0) { inclusive = true } // 清掉返回堆疊，避免回到登入畫面
                                launchSingleTop = true
                            }
                        }
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                // 🔹 判斷是否登入
                if (!isLoggedIn) {
                    // 尚未登入 → 顯示登入頁
                    LoginPage(onLoginSuccess = { /* 不用手動切換，listener 會處理 */ })
                } else {
                    // 已登入 → 顯示主要頁面
                    AppNavigator(
                        navController = navController,
                        fridgeFoodMap = fridgeFoodMap,
                        cartItems = cartItems,
                        chatViewModel = chatViewModel
                    )
                }
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
fun AppNavigator(
    navController: NavHostController,
    fridgeFoodMap: MutableMap<String, MutableList<FoodItem>>,
    cartItems: MutableList<FoodItem>,
    chatViewModel: ChatViewModel
) {
    var selectedFridgeId by rememberSaveable { mutableStateOf("") }
    val notifications = remember { mutableStateListOf<NotificationItem>() } // 你原本的型別
    var topBarTitle by rememberSaveable { mutableStateOf("Refrigerator") }
    var isFabVisible by remember { mutableStateOf(true) }
    val LightBluePressed = Color(0xFFD1DAE6)
    val favoriteRecipes = remember { mutableStateListOf<Pair<String, String>>() }

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
    var fridgeList by rememberSaveable(stateSaver = fridgeCardDataSaver) { mutableStateOf(emptyList()) }
    var selectedFridge by remember { mutableStateOf<FridgeCardData?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = { if (topBarTitle != "通知") CommonAppBar(title = topBarTitle, navController = navController) },
        bottomBar = { BottomNavigationBar(currentRoute = currentRoute, navController = navController) },
        floatingActionButton = {
            if (isFabVisible) {
                FloatingActionButton(
                    onClick = {
                        isFabVisible = false
                        navController.navigate("addfridge")
                    },
                    containerColor = LightBluePressed
                ) { Icon(Icons.Default.Add, contentDescription = "Add Fridge") }
            }
        }
    ) { paddingValues ->
        AnimatedNavHost(
            navController = navController,
            startDestination = "fridge",
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable("fridge") {
                topBarTitle = "首頁"
                isFabVisible = true
                FrontPage(
                    fridgeList = fridgeList,
                    onAddFridge = { fridgeList = fridgeList + it },
                    onDeleteFridge = { fridgeList = fridgeList - it },
                    navController = navController,
                    onFridgeClick = { id ->
                        selectedFridgeId = id
                        if (fridgeFoodMap[id] == null) fridgeFoodMap[id] = mutableStateListOf()
                        navController.navigate("ingredients")
                    }
                )
            }
            composable("recipe") {
                topBarTitle = "食譜"
                isFabVisible = false
                RecipeListPage(navController = navController) // ✅ 列表頁（從 Firestore 讀）
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
                val currentFoodList = fridgeFoodMap.getOrPut(selectedFridgeId) { mutableStateListOf() }
                IngredientScreen(
                    foodList = currentFoodList,
                    navController = navController,
                    onEditItem = { item ->
                        val index = currentFoodList.indexOf(item)
                        if (index != -1) navController.navigate("edit/$index") { launchSingleTop = true }
                    },
                    cartItems = cartItems,
                    notifications = notifications,
                    fridgeId = selectedFridgeId
                )
            }
            composable("add") {
                topBarTitle = "新增食材"
                isFabVisible = false
                AddIngredientScreen(
                    navController = navController,
                    existingItem = null,
                    isEditing = false,
                    fridgeId = selectedFridgeId,
                    onSave = { newItem ->
                        fridgeFoodMap[selectedFridgeId]?.add(newItem)
                        navController.popBackStack()
                    }
                )
            }
            composable("edit/{index}") { backStackEntry ->
                topBarTitle = "編輯食材"
                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                val item = index?.let { fridgeFoodMap[selectedFridgeId]?.getOrNull(it) }
                if (item != null && index != null) {
                    AddIngredientScreen(
                        navController = navController,
                        existingItem = item,
                        isEditing = true,
                        fridgeId = selectedFridgeId,
                        onSave = { updatedItem ->
                            fridgeFoodMap[selectedFridgeId]?.set(index, updatedItem)
                            navController.popBackStack()
                        }
                    )
                } else navController.popBackStack()
            }
            composable("chat") {
                topBarTitle = "FoodieBot Room"
                isFabVisible = false

                // ✅ 使用 ViewModel 保存聊天紀錄，避免切換頁面後清空
                val chatViewModel: ChatViewModel = viewModel()

                ChatPage(
                    foodList = fridgeFoodMap[selectedFridgeId] ?: emptyList(),
                    onAddToCart = { itemName ->
                        // ✅ 新增購物車邏輯
                        val existingItem = cartItems.find { it.name == itemName }
                        if (existingItem != null) {
                            // 已存在 → 數量 +1
                            val newQuantity = (existingItem.quantity.toIntOrNull() ?: 0) + 1
                            cartItems[cartItems.indexOf(existingItem)] =
                                existingItem.copy(quantity = newQuantity.toString())
                        } else {
                            // 不存在 → 新增一筆
                            cartItems.add(
                                FoodItem(
                                    name = itemName,
                                    quantity = "1",
                                    fridgeId = selectedFridgeId
                                )
                            )
                        }
                        notifications.removeAll { it.targetName == itemName }
                    },
                    viewModel = chatViewModel // ✅ 傳入同一個 ViewModel
                )
            }

            composable("user") {
                topBarTitle = "個人檔案"
                isFabVisible = false
                UserPage(navController)
            }
            composable("notification") {
                topBarTitle = "通知"
                isFabVisible = false
                NotificationPage(navController = navController, notifications = notifications)
            }
            composable("cart") {
                topBarTitle = "購物車"
                isFabVisible = false
                CartPageScreen(navController = navController, cartItems = cartItems)
            }
            composable("add_cart_ingredient") {
                topBarTitle = "新增購物食材"
                isFabVisible = false
                AddCartIngredientsScreen(navController = navController) { newItem ->
                    cartItems.add(newItem)
                    navController.navigate("cart") {
                        popUpTo("cart") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            composable("edit_cart_item/{index}") { backStackEntry ->
                topBarTitle = "編輯購物食材"
                val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                val item = index?.let { cartItems.getOrNull(it) }
                if (item != null && index != null) {
                    AddCartIngredientsScreen(
                        navController = navController,
                        existingItem = item,
                        isEditing = true,
                        onSave = { updatedItem ->
                            cartItems[index] = updatedItem
                            navController.popBackStack()
                        }
                    )
                } else navController.popBackStack()
            }

            // ✅ 唯一保留的食譜詳情路由：用 Firestore 的 documentId（recipeId）
            composable(
                route = "recipeDetailById/{recipeId}",
                arguments = listOf(navArgument("recipeId") { defaultValue = "" })
            ) { backStackEntry ->
                topBarTitle = "食譜詳情"
                isFabVisible = false

                val recipeId = backStackEntry.arguments?.getString("recipeId").orEmpty()
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                // 你的 RecipeDetailScreen 現在需要 recipeId + uid（依錯誤訊息）
                RecipeDetailScreen(
                    recipeId = recipeId,
                    uid = uid,
                    onAddToCart = { item ->
                        val existing = cartItems.find { it.name == item.name }
                        if (existing != null) {
                            val newQuantity =
                                (existing.quantity.toIntOrNull() ?: 0) + (item.quantity.toIntOrNull() ?: 0)
                            cartItems[cartItems.indexOf(existing)] =
                                existing.copy(quantity = newQuantity.toString())
                        } else {
                            cartItems.add(item)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("favorite_recipes") {
                topBarTitle = "最愛食譜"
                isFabVisible = false
                FavoriteRecipeScreen(navController = navController, recipes = favoriteRecipes)
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
    onFridgeClick: (String) -> Unit,
    navController: NavController
) {
    val textField1 = remember { mutableStateOf("") }
    var showDeleteFor by remember { mutableStateOf<FridgeCardData?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜尋欄
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(1000.dp))
                .fillMaxWidth()
                .background(Color(0xFFD9D9D9))
                .padding(vertical = 7.dp, horizontal = 13.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = "Search Icon",
                modifier = Modifier
                    .padding(end = 5.dp)
                    .clip(RoundedCornerShape(1000.dp))
                    .size(24.dp),
                tint = Color.Unspecified
            )

            TextField(
                value = textField1.value,
                onValueChange = { textField1.value = it },
                placeholder = { Text("搜尋冰箱") },
                textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
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
                            .clickable { onFridgeClick(fridge.id) }
                    ) {
                        FridgeCard(fridge)
                        if (showDeleteFor == fridge) {
                            TextButton(
                                onClick = { onDeleteFridge(fridge) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) { Text("刪除", color = Color.Red) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun AddFridgePage(onSave: (FridgeCardData) -> Unit, navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0xFFEBEDF2),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSave(
                        FridgeCardData(
                            id = (1000000..9999999).random().toString(),
                            name = name,
                            imageRes = null,
                            imageUri = imageUri
                        )
                    )
                } else {
                    Toast.makeText(context, "請輸入冰箱名稱", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBCC7D7),
                contentColor = Color.Black
            )
        ) { Text("加入冰箱") }
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
        Icon(
            painter = painterResource(R.drawable.bell),
            contentDescription = "通知",
            modifier = Modifier
                .size(23.dp)
                .clickable {
                    navController.navigate("notification") {
                        launchSingleTop = true
                    }
                },
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            painter = painterResource(R.drawable.cart),
            contentDescription = "購物車",
            modifier = Modifier.size(24.dp).clickable { navController.navigate("cart") },
            tint = Color.Unspecified
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    navController: NavController?
) {
    val routes = listOf("fridge", "recipe", "chat", "user")
    val icons = listOf(
        R.drawable.refrigerator,
        R.drawable.recipe,
        R.drawable.recommend,
        R.drawable.account
    )
    val selectedItem = routes.indexOf(currentRoute)

    NavigationBar(containerColor = Color(0xFFF5F0F5)) {
        icons.forEachIndexed { index, iconResId ->
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = {
                    val targetRoute = routes[index]
                    navController?.navigate(targetRoute) {
                        popUpTo("fridge") { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = Color.Unspecified
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFd1dae6),
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.DarkGray
                )
            )
        }
    }
}

/* =========================
  🔹 食譜列表（Firestore → recipes）
  未輸入時顯示隨機 20 筆
  ※ 改名 RecipeListPage 避免與其他檔案衝突
  ========================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListPage(navController: NavController) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var all by remember { mutableStateOf(listOf<RecipeCardItem>()) }
    var featured by remember { mutableStateOf(listOf<RecipeCardItem>()) }


    LaunchedEffect(Unit) {
        loading = true
        val db = FirebaseFirestore.getInstance()
        val snap = db.collection("recipes").limit(200).get().await()
        val list = snap.documents.mapNotNull { d ->
            val title = d.getString("title") ?: return@mapNotNull null
            val img = d.getString("imageUrl")
            @Suppress("UNCHECKED_CAST")
            val ingredients = (d.get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            RecipeCardItem(id = d.id, title = title, imageUrl = img, ingredients = ingredients)
        }
        all = list
        featured = list.shuffled().take(20)
        loading = false
    }


    val items = remember(query, featured, all) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) featured
        else all.filter { r ->
            r.title.lowercase().contains(q) || r.ingredients.any { it.lowercase().contains(q) }
        }.take(100)
    }


    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // 搜尋欄
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
                modifier = Modifier.padding(end = 8.dp).size(22.dp),
                tint = Color.Unspecified
            )
            TextField(
                value = query,
                onValueChange = { query = it },
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items, key = { it.id }) { recipe ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable {
                                val encodedId = Uri.encode(recipe.id)
                                navController.navigate("recipeDetailById/$encodedId")
                            }
                    ) {
                        Column {
                            // 讓每張圖一樣高（可依喜好調整）
                            AsyncImage(
                                model = recipe.imageUrl ?: "https://i.imgur.com/zMZxU8v.jpg",
                                contentDescription = recipe.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)           // ⬅︎ 統一圖片高度
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )


                            val titleBoxHeight = with(LocalDensity.current) {
                                (MaterialTheme.typography.bodyLarge.lineHeight * 2).toDp() + 16.dp // 2 行 + padding(8*2)
                            }
                            // 統一灰色區塊高度
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEAEAEA))
                                    .height(titleBoxHeight)            // ⬅︎ 統一標題容器高度（可調 56~72.dp）
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = recipe.title,
                                    maxLines = 2,              // 最多 2 行
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// 小資料類（公開），避免與其他檔案的資料類名稱/可見性衝突
data class RecipeCardItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val ingredients: List<String>
)