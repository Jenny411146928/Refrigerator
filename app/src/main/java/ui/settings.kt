package ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.ui.FridgeCardData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settings(navController: NavController, fridgeList: List<FridgeCardData>) {

    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    val mainFridge = fridgeList.firstOrNull()

    var fridgeName by remember { mutableStateOf(mainFridge?.name ?: "我的冰箱") }
    var fridgeImageUri by remember { mutableStateOf<Uri?>(mainFridge?.imageUri) }

    val scope = rememberCoroutineScope()
    val storage = FirebaseStorage.getInstance()
    val db = FirebaseFirestore.getInstance()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let { uri ->
            fridgeImageUri = uri
            scope.launch {
                try {
                    val ref = storage.reference.child("fridge_images/${uid}.jpg")
                    ref.putFile(uri).await()
                    val downloadUrl = ref.downloadUrl.await()

                    db.collection("users").document(uid)
                        .collection("fridge").document(mainFridge!!.id)
                        .update("imageUrl", downloadUrl.toString())

                } catch (e: Exception) {
                    Toast.makeText(context, "更新圖片失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {


            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE3E6ED))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val displayImage = when {
                    fridgeImageUri != null -> fridgeImageUri
                    mainFridge?.imageUrl != null -> mainFridge.imageUrl
                    else -> null
                }

                if (displayImage != null) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Edit, contentDescription = "edit")
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("冰箱名稱", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = fridgeName,
                onValueChange = { fridgeName = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color(0xFFF2F2F2),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val fridgeId = mainFridge?.id ?: run {
                                Toast.makeText(context, "找不到冰箱ID", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            val finalImageUrl =
                                fridgeImageUri?.toString()
                                    ?: mainFridge.imageUrl
                                    ?: ""


                            db.collection("users").document(uid)
                                .collection("fridge").document(fridgeId)
                                .update(
                                    mapOf(
                                        "name" to fridgeName,
                                        "imageUrl" to finalImageUrl
                                    )
                                )


                            val usersSnapshot = db.collection("users").get().await()

                            for (userDoc in usersSnapshot.documents) {
                                val friendUid = userDoc.id


                                val sharedDoc = db.collection("users").document(friendUid)
                                    .collection("sharedFridges").document(fridgeId)
                                    .get()
                                    .await()

                                if (sharedDoc.exists()) {
                                    db.collection("users").document(friendUid)
                                        .collection("sharedFridges").document(fridgeId)
                                        .update(
                                            mapOf(
                                                "name" to fridgeName,
                                                "imageUrl" to finalImageUrl
                                            )
                                        )
                                }
                            }

                            Toast.makeText(context, "已更新（所有使用者同步）", Toast.LENGTH_SHORT).show()

                        } catch (e: Exception) {
                            Toast.makeText(context, "更新失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(Color(0xFFABB7CD))
            ) {
                Text("儲存設定")
            }
        }
    }
}
