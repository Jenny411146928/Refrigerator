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

    // ===============================================================
    // ✅ 新增主冰箱（含圖片上傳）
    // ===============================================================
    suspend fun createMainFridge(name: String, imageUri: String?) {
        val uid = currentUserId ?: return
        val fridgeId = (100000..999999).random().toString()
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        var uploadedImageUrl: String? = null

        try {
            // 🔹 若使用者選了圖片 URI，就上傳到 Firebase Storage
            if (!imageUri.isNullOrEmpty() && imageUri.startsWith("content://")) {
                val fileRef = storage.reference.child("fridgeImages/$uid/$fridgeId.jpg")
                Log.d("FirebaseManager", "📤 開始上傳主冰箱圖片：$fileRef")
                fileRef.putFile(Uri.parse(imageUri)).await()
                uploadedImageUrl = fileRef.downloadUrl.await().toString()
                Log.d("FirebaseManager", "✅ 主冰箱圖片上傳完成：$uploadedImageUrl")
            } else if (!imageUri.isNullOrEmpty()) {
                // 若 imageUri 已經是網址（例如之前上傳過）
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

            // 更新主冰箱 ID
            db.collection("users").document(uid)
                .update("mainFridgeId", fridgeId).await()

            Log.d("FirebaseManager", "✅ 已建立主冰箱 $name（ID: $fridgeId）")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 建立主冰箱失敗: ${e.message}")
        }
    }

    // ===============================================================
    // ✅ 更新冰箱資訊（修改名稱 / 圖片）
    // ===============================================================
    suspend fun updateFridgeInfo(fridgeId: String, newName: String?, newImageUri: Uri?) {
        val uid = currentUserId ?: return
        val fridgeRef = db.collection("users").document(uid)
            .collection("fridge").document(fridgeId)

        try {
            val updates = mutableMapOf<String, Any>()

            // 🔹 若修改名稱
            if (!newName.isNullOrBlank()) {
                updates["name"] = newName
                Log.d("FirebaseManager", "📝 名稱更新為：$newName")
            }

            // 🔹 若修改圖片
            if (newImageUri != null) {
                try {
                    val fileRef = storage.reference.child("fridgeImages/$uid/$fridgeId.jpg")
                    Log.d("FirebaseManager", "📤 開始上傳更新後冰箱圖片：$fileRef")
                    fileRef.putFile(newImageUri).await()
                    val downloadUrl = fileRef.downloadUrl.await().toString()
                    updates["imageUrl"] = downloadUrl
                    Log.d("FirebaseManager", "✅ 冰箱圖片已成功上傳並更新網址：$downloadUrl")
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "❌ 冰箱圖片上傳失敗: ${e.message}")
                }
            }

            // ✅ 同步更新 Firestore（只要有變更就更新）
            if (updates.isNotEmpty()) {
                fridgeRef.update(updates).await()
                Log.d("FirebaseManager", "✅ 冰箱 $fridgeId 更新成功：$updates")
            } else {
                Log.d("FirebaseManager", "⚠️ 沒有要更新的欄位")
            }

        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ 更新冰箱資料失敗: ${e.message}")
        }
    }

    // ===============================================================
    // ✅ 分享冰箱給好友
    // ===============================================================
    suspend fun shareFridgeWithFriend(fridgeId: String, friendUid: String) {
        val uid = currentUserId ?: return
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
            "mirrorFridgePath" to "users/$uid/fridge/$fridgeId"
        )

        db.collection("users").document(friendUid)
            .collection("sharedFridges").document(fridgeId)
            .set(sharedData).await()

        fridgeRef.update("members", FieldValue.arrayUnion(friendUid)).await()
        Log.d("FirebaseManager", "🤝 已分享冰箱 $fridgeId 給好友 $friendUid")
    }

    // ===============================================================
    // ✅ 讀取所有冰箱（分為主冰箱與好友冰箱）
    // ===============================================================
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

    // ===============================================================
    // 🔍 透過冰箱 ID 搜尋好友冰箱
    // ===============================================================
    // 🔍 模糊搜尋好友冰箱（email 關鍵字）
    suspend fun searchFridgeByEmail(keyword: String): List<Map<String, Any>> {
        val keywordLower = keyword.trim().lowercase()
        val allUsersSnapshot = db.collection("users").get().await()

        // 🔎 篩選出 email 含有關鍵字的使用者
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

    // ===============================================================
    // 🛒 購物清單功能
    // ===============================================================

    /** ✅ 新增購物清單項目（含圖片上傳） */
    suspend fun addCartItem(item: FoodItem) {
        val uid = currentUserId ?: throw Exception("使用者尚未登入")
        val cartRef = db.collection("users").document(uid).collection("cart")
        val itemId = UUID.randomUUID().toString()

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

    /** ✅ 讀取購物清單 */
    suspend fun getCartItems(): List<FoodItem> {
        val uid = currentUserId ?: return emptyList()
        val snapshot = db.collection("users").document(uid)
            .collection("cart").get().await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                FoodItem(
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

    /** ✅ 刪除購物清單項目 */
    suspend fun deleteCartItem(name: String) {
        val uid = currentUserId ?: return
        val cartRef = db.collection("users").document(uid).collection("cart")
        val snapshot = cartRef.whereEqualTo("name", name).get().await()
        for (doc in snapshot.documents) {
            cartRef.document(doc.id).delete().await()
        }
        Log.d("FirebaseManager", "🗑 已刪除購物清單項目：$name")
    }

    /** ✅ 更新購物清單數量 */
    suspend fun updateCartQuantity(name: String, qty: Int) {
        val uid = currentUserId ?: return
        val cartRef = db.collection("users").document(uid).collection("cart")
        val snapshot = cartRef.whereEqualTo("name", name).get().await()
        for (doc in snapshot.documents) {
            try {
                cartRef.document(doc.id).update("quantity", qty.toString()).await()
                Log.d("FirebaseManager", "🔄 已更新 $name 的數量為 $qty")
            } catch (e: Exception) {
                Log.e("FirebaseManager", "❌ 更新數量失敗: ${e.message}")
            }
        }
    }
    // ===============================================================
// ❤️ 最愛食譜功能（收藏、移除、讀取）
// ===============================================================
    suspend fun addFavoriteRecipe(
        recipeId: String,
        title: String,
        imageUrl: String?,
        link: String?
    ) {
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
                .set(favoriteData)
                .await()

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
                .orderBy("timestamp") // 🔹 依收藏時間排序
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


}


