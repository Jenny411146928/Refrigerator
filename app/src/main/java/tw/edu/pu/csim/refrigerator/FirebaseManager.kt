package tw.edu.pu.csim.refrigerator

import com.google.firebase.database.*
import tw.edu.pu.csim.refrigerator.models.FridgeItem

object FirebaseManager {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // 取得冰箱內的食材列表
    fun getItems(fridgeID: String, callback: (List<FridgeItem>) -> Unit) {
        database.child("fridges").child(fridgeID).child("items")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = snapshot.children.mapNotNull { it.getValue(FridgeItem::class.java) }
                    callback(items)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList()) // 讀取失敗時回傳空列表
                }
            })
    }
}
