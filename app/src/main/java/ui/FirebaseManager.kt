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
    private val currentUserId get() = FirebaseAuth.getInstance().currentUser?.uid
    private val storage = FirebaseStorage.getInstance()

    /** ✅ 新增主冰箱 */
    suspend fun createMainFridge(name: String, imageUrl: String?) {
        val uid = currentUserId ?: return
        val fridgeId = (100000..999999).random().toString()
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        val fridgeData = mapOf(
            "id" to fridgeId,
            "name" to name,
            "ownerId" to uid,
            "ownerName" to userEmail,
            "imageUrl" to imageUrl,
            "editable" to true,
            "isMain" to true,
            "members" to emptyList<String>(),
            "createdAt" to Date()
        )

        db.collection("users").document(uid)
            .collection("fridge").document(fridgeId)
            .set(fridgeData).await()

        db.collection("users").document(uid)
            .update("mainFridgeId", fridgeId)
            .await()
    }

    /** ✅ 分享冰箱給好友（建立好友端 sharedFridge） */
    suspend fun shareFridgeWithFriend(fridgeId: String, friendUid: String) {
        val uid = currentUserId ?: return
        val fridgeRef = db.collection("users").document(uid)
            .collection("fridge").document(fridgeId)

        val fridgeSnapshot = fridgeRef.get().await()
        val fridgeData = fridgeSnapshot.data ?: return

        // 在好友端建立一份只讀 sharedFridge 文件
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

        // 更新原始冰箱成員列表
        fridgeRef.update("members", FieldValue.arrayUnion(friendUid)).await()
    }

    /** ✅ 讀取所有冰箱（自動分主冰箱與好友冰箱） */
    suspend fun getUserFridges(): Pair<List<Map<String, Any>>, List<Map<String, Any>>> {
        val uid = currentUserId ?: return Pair(emptyList(), emptyList())
        val myFridges = db.collection("users").document(uid).collection("fridge").get().await()
        val sharedFridges = db.collection("users").document(uid).collection("sharedFridges").get().await()
        return Pair(
            myFridges.documents.mapNotNull { it.data },
            sharedFridges.documents.mapNotNull { it.data }
        )
    }

    // --------------------------------------------------------------------------
    // 🛒 以下是「購物清單」功能（新增、不動上面任何冰箱程式碼）
    // --------------------------------------------------------------------------

    /** ✅ 新增購物清單項目（含圖片上傳） */
    suspend fun addCartItem(item: FoodItem) {
        val uid = currentUserId ?: throw Exception("使用者尚未登入")
        val cartRef = db.collection("users").document(uid).collection("cart")
        val itemId = UUID.randomUUID().toString()

        var imageUrl = item.imageUrl
        if (item.imageUri != null) {
            try {
                val fileRef = storage.reference.child("cartImages/$itemId.jpg")
                fileRef.putFile(item.imageUri!!).await()
                imageUrl = fileRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e("FirebaseManager", "❌ 上傳圖片失敗: ${e.message}")
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

    /** ✅ 讀取購物清單項目 */
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
}
