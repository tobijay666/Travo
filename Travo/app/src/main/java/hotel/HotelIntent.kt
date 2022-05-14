package hotel

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.example.travo.R
import com.example.travo.Users
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import comment.Comment
import comment.CommentAdapter
import java.util.*
import kotlin.collections.ArrayList

class HotelIntent : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val PERMISSION_ID=44

    private lateinit var name: TextView
    private lateinit var star: TextView
    private lateinit var phone:TextView
    private lateinit var price: TextView
    private lateinit var description: TextView
    private lateinit var hotelLocation: TextView
    //private lateinit var image: ImageView
    private lateinit var btnDirection: Button
    private lateinit var edtComment: EditText
    private lateinit var docId: String
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var commentArrayList: ArrayList<Comment>
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hotel_intent)

        recyclerView=findViewById(R.id.rvComment)
        recyclerView.layoutManager= LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        commentArrayList= arrayListOf()
        commentAdapter= CommentAdapter(this,commentArrayList)
        recyclerView.adapter=commentAdapter

        edtComment=findViewById(R.id.edtComment)
        //docId = intent.getStringExtra("id").toString().trim()

        name=findViewById(R.id.txtName)
        phone=findViewById(R.id.txtPhone)
        star=findViewById(R.id.txtStar)
        price=findViewById(R.id.txtPrice)
        description=findViewById(R.id.txtDescription)
        hotelLocation=findViewById(R.id.txtLocation)
       // image=findViewById(R.id.imgHotelDetails)
        btnDirection=findViewById(R.id.btnGps)

        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this)

        btnDirection.setOnClickListener {
            // Toast.makeText(this,"hello world",Toast.LENGTH_SHORT).show()
            checkGPS()
            getLocation()
        }

        setData()
        setCommentData()
        setImages()
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==PERMISSION_ID){
            if (grantResults[0]== PackageManager.PERMISSION_GRANTED){
                getLocation()
            }
        }
    }

    override fun onPause() {

        super.onPause()
        if(fusedLocationClient!=null){
            fusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }

    private fun setData(){

        val bundle:Bundle?=intent.extras
        val bName=bundle!!.getString("hotelName")
        val bStar= bundle!!.getString("hotelStar")
        val bPhone= bundle!!.getString("phone")
        val bDescription= bundle!!.getString("hotelDescription")
        val bPrice= bundle!!.getString("hotelPrice")
        val bLocation= bundle!!.getString("hotelAddress")
        //val bImage= bundle!!.getString("hotelImage")
        val bDoc=bundle!!.getString("docId")

        name.text=bName
        star.text=bStar
        phone.text=bPhone
        price.text=bPrice
        description.text=bDescription
        hotelLocation.text=bLocation
        docId=bDoc.toString()
        //Glide.with(this).load(bImage).into(image)

    }

    fun addComment(view: View) {

        val commentTxt = edtComment.text.toString()


        FirebaseAuth.getInstance().currentUser?.uid?.let {
            db.collection("Users").document(it)
                .get().addOnSuccessListener { document->
                    if(document!=null){

                        val users=document.toObject(Users::class.java)
                        val name= users?.name.toString()

                        if (commentTxt.isNotEmpty()) {

                            FirebaseFirestore.getInstance().runTransaction { transition ->


                                val newComRef = FirebaseFirestore.getInstance()
                                    .collection("Hotel")
                                    .document(docId).collection("Comments").document()
                                val data = HashMap<String, Any>()
                                data.put("Uname", name)
                                data.put("comm", commentTxt)
                                data.put("time",FieldValue.serverTimestamp())

                                transition.set(newComRef, data)
                            }
                                .addOnSuccessListener {
                                    edtComment.setText("")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("Exception", "not add ${exception.localizedMessage}")
                                }
                        }else{
                            Toast.makeText(this,"Enter Comment", Toast.LENGTH_SHORT).show()
                        }


                    }else {
                        Log.d(TAG, "No such document")
                    }
                }.addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }


    }

    private fun setCommentData() {

        db = FirebaseFirestore.getInstance()
        db.collection("Hotel").document(docId).collection("Comments")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {

                    if (error != null) {
                        Log.e("fireStore error", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {

                        if (dc.type == DocumentChange.Type.ADDED) {
                            commentArrayList.add(dc.document.toObject(Comment::class.java))
                        }
                    }
                    commentAdapter.notifyDataSetChanged()
                }
            })
    }

    private fun setImages(){

        val imageSlider=findViewById<ImageSlider>(R.id.image_slider)

        val imageList=ArrayList<SlideModel>()


        db.collection("Hotel").document("$docId").collection("Images")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("fireStore error", error.message.toString())
                        return
                    }
                    for (dc: DocumentChange in value?.documentChanges!!) {

                        imageList.add(SlideModel(dc.document.getString("url"), ScaleTypes.FIT))
                        imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP)

                    }

                }
            })
    }

    private fun getLocation(){

        if (checkPermission()){
            updateLocation()
        }else{
            requestPermission()
        }
    }

    @SuppressLint("VisibleForTests", "MissingPermission")
    fun updateLocation(){

        val locationRequest= LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        /*locationRequest.priority= LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        locationRequest.interval=1000
        locationRequest.fastestInterval=2000*/

        fusedLocationClient= FusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest,mLocationCallback, Looper.myLooper()!!)

    }

    private var mLocationCallback=object : LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            val location: Location =p0.lastLocation
            addressUi(location)
        }
    }

    private fun addressUi(location: Location){

        val addressList: ArrayList<Address>
        val geocoder = Geocoder(this, Locale.getDefault())

        addressList=geocoder.getFromLocation(location.latitude,location.longitude,1) as ArrayList<Address>

        val currentLoc= addressList[0].getAddressLine(0)
        val destination=hotelLocation.text.toString().trim()
        val userLocation=currentLoc.toString().trim()
        displayTrack(userLocation, destination)

    }

    private fun checkPermission():Boolean{
        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                  == PackageManager.PERMISSION_GRANTED)
            return true

        return false
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )

    }

    private fun checkGPS() {

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result=LocationServices.getSettingsClient(this)
            .checkLocationSettings(builder.build())

        result.addOnCompleteListener{task->
            try {
                val response =task.getResult(
                    ApiException::class.java
                )

            }catch (e: ApiException){
                e.printStackTrace()
                when(e.statusCode){
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED->try {
                        val resolvableApiException=e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(this,200)

                    }catch (sendIntentException: IntentSender.SendIntentException){
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE->{

                    }
                }

            }
        }


    }

    private fun displayTrack(s: String, d: String) {
        try {

            
            //val gmmIntentUri = Uri.parse("https://www.google.co.in/maps/dir/$s/$d")-->get directions
            val gmmIntentUri = Uri.parse("https://www.google.co.in/maps/dir/$s/$d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(mapIntent)

        }catch (e: ActivityNotFoundException){

            val gmmIntentUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(mapIntent)

        }
    }



}

