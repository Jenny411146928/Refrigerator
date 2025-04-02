package tw.edu.pu.csim.refrigerator

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tw.edu.pu.csim.refrigerator.ui.theme.RefrigeratorTheme
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import tw.edu.pu.csim.refrigerator.firebase.FoodListScreen
import tw.edu.pu.csim.refrigerator.ui.AppBar
import tw.edu.pu.csim.refrigerator.ui.BottomNavigationBar
import tw.edu.pu.csim.refrigerator.ui.FridgeCardData
import tw.edu.pu.csim.refrigerator.ui.FridgeCardList
import tw.edu.pu.csim.refrigerator.ui.FrontPage

class MainActivity : ComponentActivity() {

    private val database = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 測試：寫入資料
        writeData("message", "Hello, Firebase realdatabase!")
        writeData("user001", mapOf("name" to "Alice", "age" to 25))

        // 測試：讀取資料
        readData("user001")

        setContent {
            RefrigeratorTheme {
                FoodListScreen(fridgeID = "fridge1") // 這裡的 fridgeID 需要對應 Firebase 裡的冰箱ID
                FrontPage()
            }
        }

    }

    // 寫入資料 (可通用)
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

    // 讀取資料
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello jenny cindy!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RefrigeratorTheme {
        Greeting("Android")
    }

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
                // 暫時移除這一行或將其內容實現
                // Frame10()
                FridgeCardList(fridgeCards)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FrontPagePreview() {
    RefrigeratorTheme {
        FrontPage()
    }
}