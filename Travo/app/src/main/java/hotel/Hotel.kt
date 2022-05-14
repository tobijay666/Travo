package hotel

import com.google.firebase.firestore.Exclude

class Hotel {

    //variables for hotels
    var name:String?=null
    var star:String?=null
    var price:String?=null
    var image:String?=null
    val description:String?=null
    val address:String?=null
    var phone:String?=null

    @Exclude
    var id: String? = null
    fun <T : Hotel?> withId(id: String): T {
        this.id = id
        return this as T
    }
}
