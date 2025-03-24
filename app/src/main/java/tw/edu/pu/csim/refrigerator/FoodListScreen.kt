//package tw.edu.pu.csim.refrigerator


//package tw.edu.pu.csim.refrigerator.screens
package tw.edu.pu.csim.refrigerator.firebase


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import tw.edu.pu.csim.refrigerator.FirebaseManager
//import tw.edu.pu.csim.refrigerator.FirebaseManager.FirebaseManager
//import tw.edu.pu.csim.refrigerator.firebase.FirebaseManager
//import tw.edu.pu.csim.refrigerator.firebase.FirebaseManager


import tw.edu.pu.csim.refrigerator.models.FridgeItem

@Composable
fun FoodListScreen(fridgeID: String) {
    var foodList by remember { mutableStateOf(listOf<FridgeItem>()) }

    LaunchedEffect(Unit) {
        FirebaseManager.getItems(fridgeID) { items ->
            foodList = items
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "冰箱食材列表", style = MaterialTheme.typography.headlineMedium)

        LazyColumn {
            items(foodList) { item ->
                FoodItemCard(item)
            }
        }
    }
}

@Composable
fun FoodItemCard(item: FridgeItem) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "名稱: ${item.name}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "價格: ${item.price} 元")
            Text(text = "到期日: ${item.expirationDate}")
            Text(text = "備註: ${item.note}")
        }
    }
}
