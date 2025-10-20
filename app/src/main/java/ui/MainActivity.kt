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
import tw.edu.pu.csim.refrigerator.ui.FavoriteRecipeScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
//import androidx.compose.foundation.layout.ColumnScopeInstance.weight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.refrigerator.ui.AddID
import tw.edu.pu.csim.refrigerator.ui.ChatHistoryPage
import tw.edu.pu.csim.refrigerator.ui.LoginPage
import tw.edu.pu.csim.refrigerator.ui.RecipeListPage
import tw.edu.pu.csim.refrigerator.ui.RegisterPage

class MainActivity : ComponentActivity() {
    private val database = Firebase.database.reference
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RefrigeratorTheme {
                val fridgeFoodMap = remember { mutableStateMapOf<String, MutableList<FoodItem>>() }
                val cartItems = remember { mutableStateListOf<FoodItem>() }

                val auth = FirebaseAuth.getInstance()
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null && auth.currentUser?.isEmailVerified == true) }

                // 🔹 監聽登入狀態
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { fb ->
                        val user = fb.currentUser
                        isLoggedIn = user != null && user.isEmailVerified
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                if (!isLoggedIn) {
                    AuthNavHost()
                } else {
                    // ✅ 一進入主畫面時，載入 Firestore 聊天紀錄
                    LaunchedEffect(Unit) {
                        chatViewModel.loadMessagesFromFirestore()
                    }

                    MainNavHost(
                        fridgeFoodMap = fridgeFoodMap,
                        cartItems = cartItems,
                        chatViewModel = chatViewModel
                    )
                }

            }
        }
    }
}

/** 登入/註冊流程 */
@Composable
fun AuthNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginPage(
                onLoginSuccess = { /* listener 自動處理 */ },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterPage(
                onRegisterSuccess = { /* 不需要用 */ },
                onBackToLogin = { navController.popBackStack() }
            )
        }
    }
}

/** 主頁流程 */
@Composable
fun MainNavHost(
    fridgeFoodMap: MutableMap<String, MutableList<FoodItem>>,
    cartItems: MutableList<FoodItem>,
    chatViewModel: ChatViewModel
) {
    val navController = rememberNavController()
    AppNavigator(
        navController = navController,
        fridgeFoodMap = fridgeFoodMap,
        cartItems = cartItems,
        chatViewModel = chatViewModel
    )
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
    val favoriteRecipes = remember { mutableStateListOf<Triple<String, String, String?>>() }

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
    var showAddFriendSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { if (topBarTitle != "通知") CommonAppBar(title = topBarTitle, navController = navController) },
        bottomBar = { BottomNavigationBar(currentRoute = currentRoute, navController = navController) },
        floatingActionButton = {
            if (isFabVisible) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // 🔹 上面：新增好友 FAB
                    FloatingActionButton(
                        onClick = {
                            showAddFriendSheet = true
                        },
                        containerColor = LightBluePressed
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.account),
                            contentDescription = "Add Friend"
                        )
                    }

                    // 🔹 下面：新增冰箱 FAB（原本的）
                    FloatingActionButton(
                        onClick = {
                            isFabVisible = false
                            navController.navigate("addfridge")
                        },
                        containerColor = LightBluePressed
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Fridge")
                    }
                }
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
            // 在 AnimatedNavHost { ... } 內，和其他 composable 平行新增：
            composable("chat_history") {
                ChatHistoryPage(
                    navController = navController,
                    onSelectDate = { date ->
                        // 點選某天後載入該日訊息
                        chatViewModel.loadMessagesFromFirestore(date)
                        // 回到聊天頁
                        navController.popBackStack()
                    }
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
                // val chatViewModel: ChatViewModel = viewModel()

                ChatPage(
                    navController = navController, // ✅ 加上這一行
                    viewModel = chatViewModel,     // ✅ 確保傳入 ViewModel（若前面有宣告）
                    foodList = fridgeFoodMap[selectedFridgeId] ?: emptyList(),
                    fridgeList = fridgeList,
                    fridgeFoodMap = fridgeFoodMap,
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
                    }
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
                    onBack = { navController.popBackStack() },
                    favoriteRecipes = favoriteRecipes
                )
            }

            composable("favorite_recipes") {
                topBarTitle = "最愛食譜"
                isFabVisible = false
                FavoriteRecipeScreen(
                    navController = navController,
                    recipes = favoriteRecipes // ⬅ Triple 型別
                )
            }
        }
        if (showAddFriendSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddFriendSheet = false },
                containerColor = Color.White, // 背景色
                modifier = Modifier.fillMaxHeight(0.8f) // 覆蓋螢幕約 4/5
            ) {
                AddID(
                    onClose = { showAddFriendSheet = false },
                    onSearch = { query ->
                        Log.d("AddID", "搜尋好友ID: $query")
                        // TODO: Firestore 搜尋好友邏輯
                    }
                )
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
    var searchText by remember { mutableStateOf("") }
    var showDeleteFor by remember { mutableStateOf<FridgeCardData?>(null) }

    // ✅ 加入篩選邏輯
    val filteredList = fridgeList.filter {
        it.name.contains(searchText.trim(), ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 🔍 搜尋框
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("搜尋冰箱") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .clip(RoundedCornerShape(50.dp)),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search Icon",
                    tint = Color.Gray
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF2F2F2),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 冰箱清單
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (filteredList.isEmpty()) {
                Text("找不到符合的冰箱", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                filteredList.forEach { fridge ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.dp)
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