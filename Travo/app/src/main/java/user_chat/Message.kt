package user_chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

//initializing the class Message
class Message {
    var message: String? = null
    var senderId: String? = null
    var time:Timestamp?=null
}
