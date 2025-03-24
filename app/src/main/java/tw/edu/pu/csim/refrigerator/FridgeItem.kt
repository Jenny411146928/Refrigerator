//package tw.edu.pu.csim.refrigerator



/*data class FridgeItem(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val expirationDate: String = "",
    val note: String = ""
)*/
package tw.edu.pu.csim.refrigerator.models

data class FridgeItem(
    val name: String = "",
    val price: Double = 0.0,
    val expirationDate: String = "",
    val note: String = ""
)
