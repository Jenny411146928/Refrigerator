package tw.edu.pu.csim.refrigerator.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import tw.edu.pu.csim.refrigerator.FoodItem
import java.util.Date
import java.util.UUID

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUserId get() = FirebaseAuth.getInstance().currentUser?.uid


    suspend fun createMainFridge(name: String, imageUri: String?) {
        val uid = currentUserId ?: return
        val fridgeId = (100000..999999).random().toString()
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        var uploadedImageUrl: String? = null

        try {
            if (!imageUri.isNullOrEmpty() && imageUri.startsWith("content://")) {
                val fileRef = storage.reference.child("fridgeImages/$uid/$fridgeId.jpg")
                Log.d("FirebaseManager", "📤 開始上傳主冰箱圖片：$fileRef")
                fileRef.putFile(Uri.parse(imageUri)).await()
                uploadedImageUrl = fileRef.downloadUrl.await().toString()
                Log.d("FirebaseManager", "✅ 主冰箱圖片上傳完成：$uploadedImageUrl")
            } else if (!imageUri.isNullOrEmpty()) {
                uploadedImageUrl = imageUri
                Log.d("FirebaseManager", "ℹ️ 使用現有圖片 URL：$uploadedImageUrl")
            } else {
                Log.d("FirebaseManager", "⚠️ 未選擇圖片，使用 null 圖片網址")
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 主冰箱圖片上傳失敗: ${e.message}")
        }

        val fridgeData = mapOf(
            "id" to fridgeId,
            "name" to name,
            "ownerId" to uid,
            "ownerName" to userEmail,
            "imageUrl" to uploadedImageUrl,
            "editable" to true,
            "isMain" to true,
            "members" to emptyList<String>(),
            "createdAt" to Date()
        )

        try {
            db.collection("users").document(uid)
                .collection("fridge").document(fridgeId)
                .set(fridgeData).await()

            db.collection("users").document(uid)
                .update("mainFridgeId", fridgeId).await()

            Log.d("FirebaseManager", "✅ 已建立主冰箱 $name（ID: $fridgeId）")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 建立主冰箱失敗: ${e.message}")
        }
    }


    suspend fun updateFridgeInfo(fridgeId: String, newName: String?, newImageUri: Uri?) {
        val uid = currentUserId ?: return
        val db = FirebaseFirestore.getInstance()

        try {
            val updates = mutableMapOf<String, Any>()
            if (!newName.isNullOrBlank()) {
                updates["name"] = newName
                Log.d("FirebaseManager", "📝 名稱更新為：$newName")
            }

            if (newImageUri != null) {
                try {
                    val fileRef = storage.reference.child("fridgeImages/$uid/$fridgeId.jpg")
                    Log.d("FirebaseManager", "📤 開始上傳更新後冰箱圖片：$fileRef")
                    fileRef.putFile(newImageUri).await()
                    val downloadUrl = fileRef.downloadUrl.await().toString()
                    updates["imageUrl"] = downloadUrl
                    Log.d("FirebaseManager", "✅ 冰箱圖片已成功上傳並更新網址：$downloadUrl")
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "❌ 圖片上傳失敗（不影響名稱更新）：${e.message}")
                }
            }

            if (updates.isEmpty()) {
                Log.d("FirebaseManager", "⚠️ 沒有要更新的欄位")
                return
            }


            val mainRef = db.collection("users").document(uid)
                .collection("fridge").document(fridgeId)
            mainRef.update(updates).await()
            Log.d("FirebaseManager", "✅ 主冰箱更新完成：$updates")


            try {
                val usersSnapshot = db.collection("users").get().await()
                var updatedCount = 0

                for (user in usersSnapshot.documents) {
                    val sharedRef = user.reference
                        .collection("sharedFridges")
                        .document(fridgeId)
                    val sharedSnap = sharedRef.get().await()
                    if (sharedSnap.exists()) {
                        sharedRef.update(updates).await()
                        updatedCount++
                        Log.d("FirebaseManager", "🔄 已同步更新 ${user.id} 的 sharedFridge：$fridgeId")
                    }
                }
                if (updatedCount > 0) {
                    Log.d("FirebaseManager", "🎉 已同步更新 $updatedCount 位好友的 sharedFridge 資料")
                } else {
                    Log.d("FirebaseManager", "ℹ️ 沒有好友持有該冰箱，無需同步")
                }
            } catch (e: Exception) {
                Log.w("FirebaseManager", "⚠️ 主冰箱更新成功，但同步好友失敗：${e.message}")
            }

        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 更新冰箱資料發生錯誤：${e.message}")
        }
    }


    suspend fun deleteFridgeAndSync(fridgeId: String) {
        val uid = currentUserId ?: return
        val db = FirebaseFirestore.getInstance()

        try {
            val fridgeRef = db.collection("users").document(uid)
                .collection("fridge").document(fridgeId)

            val snapshot = fridgeRef.get().await()
            if (!snapshot.exists()) {
                Log.w("FirebaseManager", "⚠️ 冰箱不存在，無法刪除 ID=$fridgeId")
                return
            }

            val members = (snapshot.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            fridgeRef.delete().await()
            Log.d("FirebaseManager", "✅ 已刪除主帳號的冰箱 $fridgeId")

            for (friendUid in members) {
                try {
                    db.collection("users").document(friendUid)
                        .collection("sharedFridges")
                        .document(fridgeId)
                        .delete()
                        .await()
                    Log.d("FirebaseManager", "🧹 已同步刪除好友 $friendUid 的 sharedFridge $fridgeId")
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "⚠️ 刪除好友 $friendUid 的 sharedFridge 失敗：${e.message}")
                }
            }

            try {
                val ingredientSnap = db.collection("users").document(uid)
                    .collection("fridge").document(fridgeId)
                    .collection("Ingredient").get().await()

                for (doc in ingredientSnap.documents) {
                    doc.reference.delete().await()
                }
                Log.d("FirebaseManager", "🍎 已刪除冰箱 $fridgeId 內的所有食材")
            } catch (e: Exception) {
                Log.w("FirebaseManager", "⚠️ 刪除冰箱食材時發生錯誤：${e.message}")
            }

            Log.d("FirebaseManager", "🎉 冰箱 $fridgeId 刪除同步完成")

        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 刪除冰箱失敗：${e.message}")
        }
    }


    fun listenToFridgeChanges(
        userId: String,
        fridgeId: String,
        onUpdate: (Map<String, Any>?) -> Unit
    ): () -> Unit {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users")
            .document(userId)
            .collection("fridge")
            .document(fridgeId)


        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseManager", "❌ 冰箱即時監聽錯誤：${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data
                Log.d("FirebaseManager", "👂 冰箱資料更新：$data")
                onUpdate(data)
            } else {
                Log.w("FirebaseManager", "⚠️ 冰箱文件不存在（可能被刪除）")
                onUpdate(null)
            }
        }

        return { registration.remove() }
    }


    suspend fun shareFridgeWithFriend(fridgeId: String, friendUid: String) {
        val uid = currentUserId ?: return
        val db = FirebaseFirestore.getInstance()
        val fridgeRef = db.collection("users").document(uid)
            .collection("fridge").document(fridgeId)
        val fridgeSnapshot = fridgeRef.get().await()
        val fridgeData = fridgeSnapshot.data ?: return

        val sharedData = mapOf(
            "id" to fridgeId,
            "name" to fridgeData["name"],
            "imageUrl" to fridgeData["imageUrl"],
            "ownerId" to uid,
            "ownerName" to fridgeData["ownerName"],
            "editable" to false,
            "mirrorFridgePath" to "users/$uid/fridge/$fridgeId",
            "createdAt" to com.google.firebase.Timestamp.now()
        )


        db.collection("users").document(friendUid)
            .collection("sharedFridges").document(fridgeId)
            .set(sharedData).await()

        fridgeRef.update(
            mapOf(
                "members" to FieldValue.arrayUnion(friendUid),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
        ).await()

        Log.d("FirebaseManager", "🤝 已分享冰箱 $fridgeId 給好友 $friendUid 並同步更新時間")
    }


    suspend fun getUserFridges(): Pair<List<Map<String, Any>>, List<Map<String, Any>>> {
        val uid = currentUserId ?: return Pair(emptyList(), emptyList())
        val myFridgesSnapshot = db.collection("users").document(uid)
            .collection("fridge").get().await()
        val sharedFridgesSnapshot = db.collection("users").document(uid)
            .collection("sharedFridges").get().await()
        val myFridges = myFridgesSnapshot.documents.mapNotNull { it.data }
        val sharedFridges = sharedFridgesSnapshot.documents.mapNotNull { it.data }
        return Pair(myFridges, sharedFridges)
    }


    fun listenToUserFridges(
        onUpdate: (myFridges: List<Map<String, Any>>, sharedFridges: List<Map<String, Any>>) -> Unit
    ): () -> Unit {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return { }

        val db = FirebaseFirestore.getInstance()
        val myRef = db.collection("users").document(uid).collection("fridge")
        val sharedRef = db.collection("users").document(uid).collection("sharedFridges")


        val myListener = myRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("FirebaseManager", "❌ 監聽主冰箱錯誤：${e.message}")
                return@addSnapshotListener
            }
            val myList = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            val sharedList = sharedRef.get().result?.documents?.mapNotNull { it.data } ?: emptyList()
            onUpdate(myList, sharedList)
        }

        val sharedListener = sharedRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("FirebaseManager", "❌ 監聽好友冰箱錯誤：${e.message}")
                return@addSnapshotListener
            }
            val sharedList = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            val myList = myRef.get().result?.documents?.mapNotNull { it.data } ?: emptyList()
            onUpdate(myList, sharedList)
        }


        return {
            myListener.remove()
            sharedListener.remove()
        }
    }


    suspend fun searchFridgeByEmail(keyword: String): List<Map<String, Any>> {
        val keywordLower = keyword.trim().lowercase()
        val allUsersSnapshot = db.collection("users").get().await()
        val matchedUsers = allUsersSnapshot.documents.filter { doc ->
            val email = doc.getString("email")?.lowercase() ?: ""
            email.contains(keywordLower)
        }
        if (matchedUsers.isEmpty()) {
            Log.d("FirebaseManager", "❌ 找不到含關鍵字 '$keyword' 的使用者")
            return emptyList()
        }
        val resultList = mutableListOf<Map<String, Any>>()
        for (userDoc in matchedUsers) {
            val email = userDoc.getString("email") ?: "未知"
            val userId = userDoc.id
            val fridgeSnapshot = db.collection("users")
                .document(userId)
                .collection("fridge")
                .get()
                .await()
            for (doc in fridgeSnapshot.documents) {
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["ownerId"] = userId
                data["ownerName"] = email
                data["editable"] = false
                resultList.add(data)
            }
        }
        Log.d("FirebaseManager", "✅ 找到 ${resultList.size} 個冰箱符合關鍵字 '$keyword'")
        return resultList
    }


    suspend fun addCartItem(item: FoodItem) {
        val uid = currentUserId ?: throw Exception("使用者尚未登入")
        val cartRef = db.collection("users").document(uid).collection("cart")


        val itemId = item.id.ifBlank { UUID.randomUUID().toString() }

        var imageUrl = item.imageUrl

        if (item.imageUri != null) {
            try {
                val fileRef = storage.reference.child("cartImages/$uid/$itemId.jpg")
                Log.d("FirebaseManager", "📤 開始上傳購物清單圖片：$fileRef")
                fileRef.putFile(item.imageUri!!).await()
                imageUrl = fileRef.downloadUrl.await().toString()
                Log.d("FirebaseManager", "✅ 購物清單圖片上傳完成：$imageUrl")
            } catch (e: Exception) {
                Log.e("FirebaseManager", "❌ 上傳購物清單圖片失敗: ${e.message}")
            }
        }

        val data = mapOf(
            "id" to itemId,
            "name" to item.name,
            "quantity" to item.quantity,
            "note" to item.note,
            "imageUrl" to imageUrl,
            "createdAt" to Date()
        )

        cartRef.document(itemId).set(data).await()
        Log.d("FirebaseManager", "✅ 已新增購物清單項目：${item.name}")
    }

    suspend fun getCartItems(): List<FoodItem> {
        val uid = currentUserId ?: return emptyList()
        val snapshot = db.collection("users").document(uid)
            .collection("cart").get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {


                val itemId = doc.getString("id") ?: doc.id

                FoodItem(
                    id = itemId,
                    name = doc.getString("name") ?: "",
                    quantity = doc.getString("quantity") ?: "",
                    note = doc.getString("note") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    date = "",
                    daysRemaining = 0,
                    dayLeft = "",
                    progressPercent = 0f
                )
            } catch (e: Exception) {
                Log.e("FirebaseManager", "❌ 載入購物清單失敗：${e.message}")
                null
            }
        }
    }


    suspend fun deleteCartItem(itemId: String) {
        val uid = currentUserId ?: return
        val cartRef = db.collection("users").document(uid).collection("cart")
        try {
            cartRef.document(itemId).delete().await()
            Log.d("FirebaseManager", "🗑 已刪除購物清單項目 id=$itemId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 刪除購物清單失敗：${e.message}")
        }
    }

    suspend fun updateCartQuantity(itemId: String, qty: Int) {
        val uid = currentUserId ?: return
        val cartRef = db.collection("users").document(uid).collection("cart")
        try {
            cartRef.document(itemId).update("quantity", qty.toString()).await()
            Log.d("FirebaseManager", "🔄 已更新 id=$itemId 的數量為 $qty")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 更新數量失敗: ${e.message}")
        }
    }



    suspend fun addFavoriteRecipe(recipeId: String, title: String, imageUrl: String?, link: String?) {
        val uid = currentUserId ?: run {
            Log.e("FirebaseManager", "❌ 無法收藏：尚未登入使用者")
            return
        }
        try {
            val favoriteData = hashMapOf(
                "title" to title.ifBlank { "未命名食譜" },
                "imageUrl" to (imageUrl ?: ""),
                "link" to (link ?: ""),
                "timestamp" to Date()
            )
            db.collection("users").document(uid)
                .collection("favorites").document(recipeId)
                .set(favoriteData).await()
            Log.d("FirebaseManager", "✅ 收藏成功：$title (ID: $recipeId)")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 收藏食譜失敗：${e.message}", e)
        }
    }

    suspend fun removeFavoriteRecipe(recipeId: String) {
        val uid = currentUserId ?: run {
            Log.e("FirebaseManager", "❌ 無法取消收藏：尚未登入使用者")
            return
        }
        try {
            val favoriteRef = db.collection("users").document(uid)
                .collection("favorites").document(recipeId)
            favoriteRef.delete().await()
            Log.d("FirebaseManager", "🗑 已取消收藏食譜：$recipeId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 移除收藏食譜失敗：${e.message}", e)
        }
    }

    suspend fun getFavoriteRecipes(): List<Triple<String, String, String?>> {
        val uid = currentUserId ?: run {
            Log.e("FirebaseManager", "❌ 無法讀取收藏：尚未登入使用者")
            return emptyList()
        }
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("favorites")
                .orderBy("timestamp")
                .get()
                .await()
            val result = snapshot.documents.map {
                Triple(
                    it.id,
                    it.getString("title") ?: "未命名食譜",
                    it.getString("imageUrl")
                )
            }
            Log.d("FirebaseManager", "📥 讀取到 ${result.size} 筆收藏食譜")
            result
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 讀取收藏清單失敗：${e.message}", e)
            emptyList()
        }
    }

    suspend fun addIngredientToFridge(fridgeId: String, foodItem: FoodItem, imageUri: Uri?) {
        try {
            val uid = currentUserId ?: throw Exception("尚未登入使用者")
            var uploadedUrl = foodItem.imageUrl
            if (imageUri != null) {
                val imageRef = storage.reference.child("ingredientImages/$uid/${UUID.randomUUID()}.jpg")
                Log.d("FirebaseManager", "📤 開始上傳食材圖片：$imageRef")
                imageRef.putFile(imageUri).await()
                uploadedUrl = imageRef.downloadUrl.await().toString()
                Log.d("FirebaseManager", "✅ 圖片上傳成功：$uploadedUrl")
            }
            val ingredientRef = db.collection("users").document(uid)
                .collection("fridge").document(fridgeId)
                .collection("Ingredient").document(foodItem.id)
            val newItem = foodItem.copy(imageUrl = uploadedUrl)
            ingredientRef.set(newItem).await()
            Log.d("FirebaseManager", "✅ 已新增食材 ${newItem.name} 至冰箱 $fridgeId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 上傳食材失敗：${e.message}")
            throw e
        }
    }

    suspend fun getIngredients(fridgeId: String): List<FoodItem> {
        val uid = currentUserId ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("fridge").document(fridgeId)
                .collection("Ingredient").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    FoodItem(
                        name = doc.getString("name") ?: "",
                        date = doc.getString("date") ?: "",
                        quantity = doc.getString("quantity") ?: "",
                        note = doc.getString("note") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        daysRemaining = (doc.getLong("daysRemaining") ?: 0L).toInt(),
                        dayLeft = doc.getString("dayLeft") ?: "",
                        progressPercent = (doc.getDouble("progressPercent") ?: 0.0).toFloat(),
                        fridgeId = fridgeId,
                        category = doc.getString("category") ?: "",
                        storageType = doc.getString("storageType") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "❌ 食材解析失敗：${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 讀取食材失敗：${e.message}")
            emptyList()
        }
    }

    suspend fun deleteIngredient(fridgeId: String, ingredientName: String) {
        val uid = currentUserId ?: return
        try {
            val colRef = db.collection("users").document(uid)
                .collection("fridge").document(fridgeId)
                .collection("Ingredient")
            val snapshot = colRef.whereEqualTo("name", ingredientName).get().await()
            for (doc in snapshot.documents) {
                colRef.document(doc.id).delete().await()
                Log.d("FirebaseManager", "🗑 已刪除食材：${doc.getString("name")}")
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 刪除食材失敗：${e.message}")
        }
    }
    suspend fun getFoodsByFridgeId(fridgeId: String): List<FoodItem> {
        val db = FirebaseFirestore.getInstance()
        val foods = mutableListOf<FoodItem>()

        try {
            val snapshot = db.collectionGroup("food")
                .whereEqualTo("fridgeId", fridgeId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.toObject(FoodItem::class.java)?.let { foods.add(it) }
            }

        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 無法取得冰箱食材：${e.message}")
        }

        return foods
    }

    suspend fun updateIngredient(fridgeId: String, foodItem: FoodItem, newImageUri: Uri?) {
        val uid = currentUserId ?: throw Exception("尚未登入使用者")

        try {
            val ingredientRef = db.collection("users").document(uid)
                .collection("fridge").document(fridgeId)
                .collection("Ingredient").document(foodItem.id)


            val oldData = ingredientRef.get().await()
            val oldImageUrl = oldData.getString("imageUrl") ?: ""

            var finalImageUrl = oldImageUrl


            if (newImageUri != null && newImageUri.toString().startsWith("content://")) {
                val imageRef = storage.reference.child("ingredientImages/$uid/${foodItem.id}.jpg")
                Log.d("FirebaseManager", "📤 正在更新食材圖片：$imageRef")
                imageRef.putFile(newImageUri).await()
                finalImageUrl = imageRef.downloadUrl.await().toString()
                Log.d("FirebaseManager", "✅ 圖片更新完成：$finalImageUrl")
            }


            val updatedItem = foodItem.copy(imageUrl = finalImageUrl)


            ingredientRef.set(updatedItem).await()

            Log.d("FirebaseManager", "🔄 已成功更新食材：${foodItem.name} (ID: ${foodItem.id})")

        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 更新食材失敗：${e.message}")
            throw e
        }
    }

    suspend fun getIngredientsByOwner(ownerId: String, fridgeId: String): List<FoodItem> {
        return try {
            val snapshot = db.collection("users").document(ownerId)
                .collection("fridge").document(fridgeId)
                .collection("Ingredient").get().await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    FoodItem(
                        name = doc.getString("name") ?: "",
                        date = doc.getString("date") ?: "",
                        quantity = doc.getString("quantity") ?: "",
                        note = doc.getString("note") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        daysRemaining = (doc.getLong("daysRemaining") ?: 0L).toInt(),
                        dayLeft = doc.getString("dayLeft") ?: "",
                        progressPercent = (doc.getDouble("progressPercent") ?: 0.0).toFloat(),
                        fridgeId = fridgeId,
                        category = doc.getString("category") ?: "",
                        storageType = doc.getString("storageType") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "❌ 食材解析失敗：${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 讀取朋友冰箱食材失敗：${e.message}")
            emptyList()
        }
    }
}
