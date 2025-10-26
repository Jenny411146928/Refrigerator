@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
//package tw.edu.pu.csim.refrigerator.ui
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
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

// ✅ 補：你專案內定義的項目，維持你的命名空間
//import tw.edu.pu.csim.refrigerator.NotificationItem
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavType
import com.google.firebase.auth.FirebaseAuth
import tw.edu.pu.csim.refrigerator.feature.recipe.RecipeNavRoot
import tw.edu.pu.csim.refrigerator.ui.AddID
import tw.edu.pu.csim.refrigerator.ui.ChatHistoryPage
import tw.edu.pu.csim.refrigerator.ui.LoginPage
import tw.edu.pu.csim.refrigerator.ui.RecipeListPage
import tw.edu.pu.csim.refrigerator.ui.RegisterPage
// ✅ 補：你在 routes "add" / "edit/{index}" 使用的畫面
//import tw.edu.pu.csim.refrigerator.ui.AddIngredientScreen
import tw.edu.pu.csim.refrigerator.ui.FrontPage

import tw.edu.pu.csim.refrigerator.firebase.FirebaseManager
// import tw.edu.pu.csim.refrigerator.ui.BottomNavigationBar // ✅ 修正：這個 import 造成簽名衝突，先註解掉，使用本檔案的 BottomNavigationBar

// ✅ 修正：缺少 coroutine import（對應錯誤 line 740 的 launch 未解析）
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.storage.FirebaseStorage

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
                onLoginSuccess = { },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterPage(
                onRegisterSuccess = { },
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
/*
@Composable
fun AppNavigator(
    navController: NavHostController,
    fridgeFoodMap: MutableMap<String, MutableList<FoodItem>>,
    cartItems: MutableList<FoodItem>,
    chatViewModel: ChatViewModel
) {
    var selectedFridgeId by rememberSaveable { mutableStateOf("") }
    val notifications = remember { mutableStateListOf<NotificationItem>() } // ✅ 仍用你原本的型別（含 targetName）
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
                    // 🔹 上面：新增好友 FAB（保留）
                    FloatingActionButton(
                        onClick = { showAddFriendSheet = true },
                        containerColor = LightBluePressed
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.account),
                            contentDescription = "Add Friend"
                        )
                    }
                    /*
                    // 🔹 下面：新增冰箱 FAB（保留）
                    FloatingActionButton(
                        onClick = {
                            isFabVisible = false
                            navController.navigate("addfridge")
                        },
                        containerColor = LightBluePressed
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Fridge")
                    }
                    */

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

                // ✅ 使用 RecipeNavRoot 管理食譜清單與詳情導航
                RecipeNavRoot(
                    uid = FirebaseAuth.getInstance().currentUser?.uid,
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
                    favoriteRecipes = favoriteRecipes,
                    fridgeFoodMap = fridgeFoodMap,       // ✅ 傳入所有冰箱資料
                    selectedFridgeId = selectedFridgeId  // ✅ 傳入目前使用的冰箱 ID
                )
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
            // ✅ 歷史聊天
            composable("chat_history") {
                ChatHistoryPage(
                    navController = navController,
                    onSelectDate = { date ->
                        chatViewModel.loadMessagesFromFirestore(date)
                        navController.popBackStack()
                    }
                )
            }
            // ✅ 食材瀏覽
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


 */

@Composable
fun AppNavigator(
    navController: NavHostController,
    fridgeFoodMap: MutableMap<String, MutableList<FoodItem>>,
    cartItems: MutableList<FoodItem>,
    chatViewModel: ChatViewModel
) {
    var selectedFridgeId by rememberSaveable { mutableStateOf("") }
    val notifications = remember { mutableStateListOf<NotificationItem>() }
    var topBarTitle by rememberSaveable { mutableStateOf("Refrigerator") }
    var isFabVisible by remember { mutableStateOf(true) }
    val LightBluePressed = Color(0xFFD1DAE6)
    val favoriteRecipes = remember { mutableStateListOf<Triple<String, String, String?>>() }

    var fridgeList by remember { mutableStateOf<List<FridgeCardData>>(emptyList()) }
    var selectedFridge by remember { mutableStateOf<FridgeCardData?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showAddFriendSheet by remember { mutableStateOf(false) }

    // ✅ 修正 component1() 錯誤，用明確變數命名
    LaunchedEffect(Unit) {
        try {
            val result = tw.edu.pu.csim.refrigerator.firebase.FirebaseManager.getUserFridges()
            val myFridges = result.first
            val sharedFridges = result.second

            // 🔹 主冰箱（可編輯）
            val mainFridges = myFridges.map {
                FridgeCardData(
                    id = it["id"].toString(),
                    name = it["name"].toString(),
                    ownerName = it["ownerName"]?.toString(),
                    imageUrl = it["imageUrl"]?.toString(),
                    ownerId = it["ownerId"]?.toString(),
                    editable = (it["editable"] as? Boolean) ?: true
                )
            }

            // 🔹 好友冰箱（唯讀）
            val friendFridges = sharedFridges.map {
                FridgeCardData(
                    id = it["id"].toString(),
                    name = it["name"].toString(),
                    ownerName = it["ownerName"]?.toString(),
                    imageUrl = it["imageUrl"]?.toString(),
                    ownerId = it["ownerId"]?.toString(),
                    editable = false
                )
            }

            fridgeList = mainFridges + friendFridges
            Log.d("Firestore", "✅ 成功載入冰箱，共 ${fridgeList.size} 個")

            // ✅ 若目前沒有選擇冰箱，自動設定第一個
            if (selectedFridgeId.isBlank() && fridgeList.isNotEmpty()) {
                selectedFridgeId = fridgeList.first().id
                Log.d("AppNavigator", "🔹 自動設定主冰箱 ID = $selectedFridgeId")
            }

            // ✅ 若該冰箱沒有食材資料，先建立空清單（避免空指標）
            if (fridgeFoodMap[selectedFridgeId] == null) {
                fridgeFoodMap[selectedFridgeId] = mutableStateListOf()
            }

        } catch (e: Exception) {
            Log.e("Firestore", "❌ 載入冰箱失敗: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            // ✅ 修正：CommonAppBar 未解析的根因是下方 AddFridgePage 少了一個大括號，已在檔案後面補上
            if (topBarTitle != "通知") CommonAppBar(title = topBarTitle, navController = navController)
        },
        bottomBar = {
            // ✅ 修正：改用本檔案定義的 BottomNavigationBar（上面已註解掉外部 import）
            BottomNavigationBar(currentRoute = currentRoute, navController = navController)
        },
        floatingActionButton = {
            if (isFabVisible) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // ✅ 新增好友 FAB（保留）
                    FloatingActionButton(
                        onClick = { showAddFriendSheet = true },
                        containerColor = LightBluePressed
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.account),
                            contentDescription = "Add Friend"
                        )
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

            /** 🧊 冰箱首頁 **/
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

            /** 🍽 食譜 **/
            composable("recipe") {
                topBarTitle = "食譜"
                isFabVisible = false

                // ✅ 使用 RecipeNavRoot 管理食譜清單與詳情導航
                RecipeNavRoot(
                    uid = FirebaseAuth.getInstance().currentUser?.uid,
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
                    favoriteRecipes = favoriteRecipes,
                    fridgeFoodMap = fridgeFoodMap,       // ✅ 傳入所有冰箱資料
                    selectedFridgeId = selectedFridgeId  // ✅ 傳入目前使用的冰箱 ID
                )
            }

            /** ➕ 新增冰箱 **/
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

            /** 💬 聊天歷史 **/
            composable("chat_history") {
                ChatHistoryPage(
                    navController = navController,
                    onSelectDate = { date ->
                        chatViewModel.loadMessagesFromFirestore(date)
                        navController.popBackStack()
                    }
                )
            }

            /** 🥕 食材列表 **/
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

            /** ➕ 新增食材 **/
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

            /** ✏️ 編輯食材 **/
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

            /** 🤖 聊天室 **/
            composable("chat") {
                topBarTitle = "FoodieBot Room"
                isFabVisible = false
                ChatPage(
                    navController = navController,
                    viewModel = chatViewModel,
                    foodList = fridgeFoodMap[selectedFridgeId] ?: emptyList(),
                    fridgeList = fridgeList,
                    fridgeFoodMap = fridgeFoodMap,
                    onAddToCart = { itemName ->
                        val existingItem = cartItems.find { it.name == itemName }
                        if (existingItem != null) {
                            val newQuantity = (existingItem.quantity.toIntOrNull() ?: 0) + 1
                            cartItems[cartItems.indexOf(existingItem)] =
                                existingItem.copy(quantity = newQuantity.toString())
                        } else {
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

            /** 👤 個人頁 **/
            composable("user") {
                topBarTitle = "個人檔案"
                isFabVisible = false
                UserPage(navController)
            }

            /** 🔔 通知頁 **/
            composable("notification") {
                topBarTitle = "通知"
                isFabVisible = false
                NotificationPage(navController = navController, notifications = notifications)
            }

            /** 🛒 購物車 **/
            composable("cart") {
                topBarTitle = "購物車"
                isFabVisible = false
                CartPageScreen(navController = navController, cartItems = cartItems)
            }

            /** ➕ 新增購物食材 **/
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

            /** ✏️ 編輯購物清單食材 **/
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

            /** 📖 食譜詳情 **/
            composable(
                route = "recipeDetail/{recipeId}",
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                Log.d("CartDebug", "🚀 已進入 recipeDetail composable，ID=${backStackEntry.arguments?.getString("recipeId")}")

                topBarTitle = "食譜詳情"
                isFabVisible = false

                val recipeId = backStackEntry.arguments?.getString("recipeId").orEmpty()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val context = LocalContext.current

                // ✅ 取得目前冰箱的食材清單
                val currentFoodList = fridgeFoodMap[selectedFridgeId] ?: mutableListOf()

                RecipeDetailScreen(
                    recipeId = recipeId,
                    uid = uid,

                    // ✅ 傳入目前冰箱的食材（用來判斷 ✔／＋）
                    foodList = currentFoodList,

                    onAddToCart = { item ->
                        val safeItem = if (item.quantity.isBlank()) item.copy(quantity = "1") else item
                        val existing = cartItems.find { it.name.equals(safeItem.name, ignoreCase = true) }

                        if (existing != null) {
                            val oldQty = existing.quantity.toIntOrNull() ?: 0
                            val newQty = safeItem.quantity.toIntOrNull() ?: 0
                            val total = oldQty + newQty
                            val updated = existing.copy(quantity = total.toString())
                            cartItems[cartItems.indexOf(existing)] = updated
                        } else {
                            cartItems.add(safeItem)
                        }

                        Toast.makeText(context, "${safeItem.name} 已加入購物車！", Toast.LENGTH_SHORT).show()

                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            val db = FirebaseFirestore.getInstance()
                            Log.d("CartDebug", "👉 準備寫入 Firestore")
                            Log.d("CartDebug", "當前 UID: $uid")
                            Log.d("CartDebug", "項目資料: ${safeItem.name}, 數量=${safeItem.quantity}")

                            val cartData = hashMapOf(
                                "name" to safeItem.name,
                                "quantity" to safeItem.quantity,
                                "note" to safeItem.note,
                                "imageUrl" to safeItem.imageUrl,
                                "fridgeId" to safeItem.fridgeId
                            )

                            db.collection("users").document(uid)
                                .collection("cart")
                                .document(safeItem.name)
                                .set(cartData)
                                .addOnSuccessListener {
                                    Log.d("CartDebug", "✅ 已寫入 Firestore 購物車: ${safeItem.name}")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("CartDebug", "❌ Firestore 寫入失敗: ${e.message}")
                                    Toast.makeText(context, "寫入 Firestore 失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Log.e("CartDebug", "❌ UID 為 null，未登入")
                        }

                    }
                    ,

                    onBack = { navController.popBackStack() },
                    favoriteRecipes = favoriteRecipes,
                    navController = navController
                )
            }

            /** ❤️ 最愛食譜 **/
            composable("favorite_recipes") {
                topBarTitle = "最愛食譜"
                isFabVisible = false
                FavoriteRecipeScreen(
                    navController = navController,
                    recipes = favoriteRecipes
                )
            }
            /** ℹ️ 關於我們（簡介頁） **/
            composable("about") {
                topBarTitle = "簡介"
                isFabVisible = false
                AboutPage(navController = navController)
            }
        }

        /** 👥 加好友 BottomSheet **/
        if (showAddFriendSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddFriendSheet = false },
                containerColor = Color.White,
                modifier = Modifier.fillMaxHeight(0.8f)
            ) {
                AddID(
                    onClose = { showAddFriendSheet = false },
                    onSearch = { query ->
                        Log.d("AddID", "搜尋好友ID: $query")
                    }
                )
            }
        }
    }
}

/*@Composable
// 你註解保留的 FrontPage（不要刪，我照原樣留著）
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun FrontPage( ... ) { ... }
*/

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun AddFridgePage(onSave: (FridgeCardData) -> Unit, navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid ?: ""
    val email = currentUser?.email ?: ""
    val scope = rememberCoroutineScope()

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

        // 圖片區塊
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

        // 名稱輸入框
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

        // 儲存按鈕
        Button(
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "請輸入冰箱名稱", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch {
                    try {
                        // ======================================================
                        // ✅ 【新增】Firebase Storage 上傳圖片邏輯
                        // ======================================================
                        var uploadedImageUrl: String? = null
                        if (imageUri != null) {
                            try {
                                val storageRef = FirebaseStorage.getInstance().reference
                                val fileRef = storageRef.child("fridgeImages/$uid/${System.currentTimeMillis()}.jpg")
                                fileRef.putFile(imageUri!!).await()
                                uploadedImageUrl = fileRef.downloadUrl.await().toString()
                                Log.d("AddFridgePage", "✅ 圖片已上傳：$uploadedImageUrl")
                            } catch (e: Exception) {
                                Log.e("AddFridgePage", "❌ 圖片上傳失敗: ${e.message}")
                            }
                        }

                        // ======================================================
                        // ✅ Firestore 寫入邏輯（保留你原始的）
                        // ======================================================
                        val db = FirebaseFirestore.getInstance()
                        val fridgeRef = db.collection("users")
                            .document(uid)
                            .collection("fridge")
                            .document() // ✅ 自動生成唯一 ID

                        val fridgeId = fridgeRef.id
                        val newFridge = hashMapOf(
                            "id" to fridgeId,
                            "name" to name,
                            "imageUrl" to (uploadedImageUrl ?: imageUri?.toString()), // ✅ 優先使用上傳後的網址
                            "ownerId" to uid,
                            "ownerName" to email,
                            "editable" to true,
                            "isMain" to true,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        fridgeRef.set(newFridge).await()
                        Toast.makeText(context, "成功新增冰箱到雲端", Toast.LENGTH_SHORT).show()

                        // ✅ 將 Firestore ID 同步回畫面顯示
                        onSave(
                            FridgeCardData(
                                id = fridgeId, // ✅ Firestore 真實 ID
                                name = name,
                                imageRes = null,
                                imageUri = imageUri,
                                ownerId = uid,
                                ownerName = email,
                                editable = true
                            )
                        )

                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("Firestore", "❌ 寫入失敗: ${e.message}")
                        Toast.makeText(context, "建立冰箱失敗", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBCC7D7),
                contentColor = Color.Black
            )
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
            modifier = Modifier
                .size(24.dp)
                .clickable { navController.navigate("cart") },
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
