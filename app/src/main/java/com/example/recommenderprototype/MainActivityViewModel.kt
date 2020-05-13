package com.example.recommenderprototype

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.Restaurant
import com.example.recommenderprototype.database.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_profile_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivityViewModel : ViewModel() {
    val mLiveData = MutableLiveData<ArrayList<ArrayList<Food>>>()
    val mUserLiveData = MutableLiveData<User>()
    val mRestaurantListLiveData = MutableLiveData<ArrayList<Restaurant>>()
    val user = User()

    fun fetchData() {

        viewModelScope.launch(Dispatchers.Default) {

            val odb: FirebaseFirestore = FirebaseFirestore.getInstance()
            val menu = ArrayList<Food>()
            var listOfLists = arrayListOf<ArrayList<Food>>()
            val restaurantList = ArrayList<Restaurant>()

            //Firestore restaurant database read function
            odb.collection("restaurant").get()
                .addOnSuccessListener { results ->
                    for (document in results){
                        restaurantList.add(document.toObject(Restaurant::class.java))
                    }
                    mRestaurantListLiveData.postValue(restaurantList)
                }

            //Firestore food database read function
            odb.collection("food_test").get()
                .addOnSuccessListener { results ->

                    /*-- Populate Menu --*/
                    var index = 0
                    for (document in results) {
                        //Push all elements back 1 space and insert new element at 0th index
                        menu.add(index, document.toObject(Food::class.java))
                        menu[index].menu_id = document.id
                    }

                    /*-- Get Bookmark Data --*/
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        //Get user document to check for bookmark
                        val odb = FirebaseFirestore.getInstance()
                        val docRef = odb.collection("user").document(currentUser.email!!)
                        docRef.get().addOnSuccessListener { document ->
                            if (document.exists()) {

                                if (document["bookmark"] != null && document["bookmark"].toString().split(",").toMutableList().isNotEmpty())
                                    user.bookmark = document["bookmark"].toString().split(",").map { it.toInt() }.toMutableList()

                                //Check if bookmark size is up-to-date in case of new food document added
                                while (user.bookmark.size < menu.size)
                                    user.bookmark.add(0)

                                menu.forEachIndexed { index, food ->
                                    if (user.bookmark[food.matrix_index] == 1)
                                        food.bookmark = true
                                }
                            }
                        }
                    }

                    //Create different categories of menus (Extras menu with filter please the code after recommendation algorithm)
                    var menuRecommended : ArrayList<Food> = menu.toCollection(ArrayList<Food>())
                    var menuPopular = menu.toCollection(ArrayList<Food>())

                    /*-- Get User Data and Perform Algorithm --*/
                    //Check if logged in
                    if (currentUser != null){
                        //If logged in, do recommendation algorithm, do other menus nonetheless
                        val docRef = odb.collection("user").document(currentUser.email!!)
                        //val user = User()
                        docRef.get().addOnSuccessListener { document ->
                            if (document.exists()){

                                //Record last online time of user to firestore
                                val tz = TimeZone.getTimeZone("GMT+08:00");
                                val c = Calendar.getInstance(tz);
                                val dateAndTime : String = c.get(Calendar.YEAR).toString()+"_"+(c.get(Calendar.MONTH)+1).toString()+"_"+c.get(
                                    Calendar.DATE).toString()+"_"+c.get(Calendar.HOUR_OF_DAY).toString()+":"+c.get(
                                    Calendar.MINUTE).toString()
                                odb.collection("user").document(currentUser!!.email!!).update("last_online", dateAndTime)

                                //Set user data to local reference
                                user.email = currentUser.email.toString()
                                user.google_account_profile_photo_url = currentUser.photoUrl.toString()
                                user.google_account_name = currentUser.displayName.toString()
                                user.gender = document["gender"].toString()
                                user.age = document["age"].toString().toInt()
                                user.height = document["height"].toString().toInt()
                                user.weight = document["weight"].toString().toInt()
                                user.cant_eat = document["cant_eat"].toString().split(",").toMutableList()
                                user.activity = document["activity"].toString()
                                user.prefer = document["prefer"].toString()
                                user.prefer_not = document["prefer_not"].toString()
                                user.staple_weight = document["staple_weight"].toString().split(",").map{it.toFloat()}.toMutableList()
                                user.protein_weight = document["protein_weight"].toString().split(",").map {it.toFloat()}.toMutableList()
                                if (document["history"].toString() != "" && document["history"] != null)
                                    user.history = document["history"].toString().split(",").map {it.toInt()}.toMutableList()
                                if (document["nutrition_edit_history"].toString() != "" && document["nutrition_edit_history"] != null)
                                    user.nutrition_edit_history = document["nutrition_edit_history"].toString().split(",").toMutableList()
                                if (document["photo_upload_history"].toString() != "" && document["photo_upload_history"] != null)
                                    user.photo_upload_history = document["photo_upload_history"].toString().split(",").toMutableList()

                                /*-- Algorithm --*/
                                //Knowledge - Based
                                //Set the CB_score of food to -1 if menu.main_ingredient matches user.cant_eat
                                var CB_score = arrayOfNulls<Float>(menuRecommended.size)
                                for (i in 0 until menuRecommended.size){
                                    CB_score[i] = 0F
                                    //separate 雞肉豬肉 into "雞肉", "豬肉"
                                    var chunked = listOf<String>()
                                    if (menuRecommended[i].main_ingredient != "") {
                                        chunked = menuRecommended[i].main_ingredient.chunked(2)
                                    }
                                    for (protein_type in chunked)
                                        if (user.cant_eat.contains(protein_type)) {
                                            CB_score[i] = -1F
                                        }
                                }

                                //Content - Based
                                val staple_options = listOf<String>("飯", "麵", "餃", "餅", "湯", "麵包", "其他")
                                val protein_options = listOf<String>("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
                                var staple_vec = arrayOfNulls<Float>(size = 7)
                                var protein_vec = arrayOfNulls<Float>(size = 10)
                                for (i in 0 until menuRecommended.size){

                                    //Init staple vector for every food in menu
                                    staple_options.forEachIndexed { index, option ->
                                        if (menuRecommended[i].staple.contains(option))
                                            staple_vec[index] = 1F
                                        else staple_vec[index] = 0F
                                    }

                                    //Init protein vector for every food in menu
                                    protein_options.forEachIndexed { index, option ->
                                        if (menuRecommended[i].main_ingredient.contains(option))
                                            protein_vec[index] = 1F
                                        else protein_vec[index] = 0F
                                    }

                                    if (CB_score[i]!! >= 0){
                                        for (j in 0 until staple_options.size){
                                            CB_score[i] = CB_score[i]?.plus(user.staple_weight[j]*staple_vec[j]!!)
                                        }
                                        for (k in 0 until protein_options.size){
                                            CB_score[i] = CB_score[i]?.plus(user.protein_weight[k]*protein_vec[k]!!)
                                        }
                                    }
                                }

                                //Collab - Filter
                                //Firestore database read user-item matrix
                                var userItemMatrix = mutableListOf<MutableList<Float>>()
                                var currentUserRow = mutableListOf<Float>()
                                //To store prediction of current user to all food in menu
                                var prediction  = arrayOfNulls<Float>(size = menu.size)
                                prediction.fill(0F,fromIndex = 0, toIndex = menu.size)

                                odb.collection("user_item_matrix").get()
                                    .addOnSuccessListener { results->
                                        for (document in results) {
                                            if (document.id != currentUser.email) {
                                                val index = userItemMatrix.size
                                                userItemMatrix.add(document["CF_score"].toString().split(",").map { it.toFloat() }.toMutableList())
                                                //Check if userItemMatrix size is up-to-date in case of new food document added
                                                while (userItemMatrix[index].size < menu.size)
                                                    userItemMatrix[index].add(0F)

                                            }
                                            else {
                                                currentUserRow = document["CF_score"].toString().split(",").map { it.toFloat() }.toMutableList()
                                                //Check if currentUserRow size is up-to-date in case of new food document added
                                                while (currentUserRow.size < menu.size)
                                                    currentUserRow.add(0F)
                                            }
                                        }

                                        //Calculate usera average of nonzero elements
                                        val currentUserRowNonZero = currentUserRow.filter { it > 0F }
                                        val RAmean  = currentUserRowNonZero.average()
                                        //Log.d("prediction", "mean of A is " + RAmean)

                                        //Columns: (rai-ramean) | (rai-ramean)^2
                                        var calcTableA = mutableListOf(mutableListOf<Float>(),mutableListOf<Float>())
                                        for (i in 0 until menu.size){
                                            //As we need to find co-rated items, non-rated are labelled -100
                                            var value = -100F
                                            if (currentUserRow[i] > 0F) {
                                                value = (currentUserRow[i]-RAmean).toFloat()
                                            }
                                            calcTableA[0].add(value)
                                            calcTableA[1].add(if (value > -50F) value.pow(2) else value)
                                        }

                                        for (i in 0 until menu.size) {
                                            //Prediction equation top & bottom
                                            var predTop = 0F
                                            var predBtm = 0F
                                            userItemMatrix.forEachIndexed { index, nextUser ->

                                                //For every other user in the matrix, calculate its mean and table
                                                val nextUserNonZero = nextUser.filter { it > 0F }
                                                val RBmean = nextUserNonZero.average()

                                                //Columns: (rbi-rbmean) | (rbi-rbmean)^2
                                                var calcTableB = mutableListOf(mutableListOf<Float>(),mutableListOf<Float>())
                                                for (i in 0 until menu.size) {
                                                    //As we need to find co-rated items, non-rated are labelled -100
                                                    var value = -100F
                                                    if (nextUser[i] > 0F) {
                                                        value = (nextUser[i] - RBmean).toFloat()
                                                    }
                                                    calcTableB[0].add(value)
                                                    calcTableB[1].add(
                                                        if (value > -50F) value.pow(
                                                            2
                                                        ) else value
                                                    )
                                                }

                                                //If current user has NO rating for ith item
                                                //& next user HAS rating for ith item then do prediction
                                                if (currentUserRow[i] <= 0F && nextUser[i] > 0F) {
                                                    //Do prediction based on next user only is similarity is high
                                                    if (sim(calcTableA, calcTableB) >= 0.6) {
                                                        val simOfAB = sim(calcTableA, calcTableB)
                                                        predTop += (simOfAB * (nextUser[i] - RBmean)).toFloat()
                                                        predBtm += simOfAB
                                                    }
                                                }
                                            }
                                            //Only update prediction for current user non-rated items (<= 0)
                                            if (currentUserRow[i] <= 0){
                                                prediction[i] = (RAmean + predTop/predBtm).toFloat()
                                            }
                                        }
                                    }

                                //Recommendation
                                for (i in 0 until menuRecommended.size){
                                    if (CB_score[i]!! >= 0F)
                                    menuRecommended[i].score = ( 0.7*CB_score[i]!! + 0.3*prediction[i]!! ).toFloat()
                                }
                                menuRecommended.sortByDescending { it.score }
                            }
                        }
                    }
                    else{
                        //If not logged in, recommended change to login reminder (will not be shown, but there'll be a checking done to show a popup message)
                        menuRecommended = arrayListOf(Food(name = "Login to view!"))
                    }

                    //Popular
                    menuPopular.sortByDescending { it.clicks }
                    listOfLists.add(menuPopular)

                    //Recommended
                    listOfLists.add(menuRecommended)

                    //Nutrition
                    listOfLists.add(menu.filter { it-> it.calorie > 0 || it.starch > 0 || it.protein > 0 || it.fat > 0 } as ArrayList<Food>)

                    //Rice
                    listOfLists.add(menu.filter { it-> it.staple == "飯" } as ArrayList<Food>)

                    //Noodle
                    listOfLists.add(menu.filter { it-> it.staple == "麵" } as ArrayList<Food>)

                    //Dumpling
                    listOfLists.add(menu.filter { it-> it.staple == "餃" } as ArrayList<Food>)

                    //Biscuit
                    listOfLists.add(menu.filter { it-> it.staple == "餅" } as ArrayList<Food>)

                    //Soup
                    listOfLists.add(menu.filter { it-> it.staple == "湯" } as ArrayList<Food>)

                    //Bread
                    listOfLists.add(menu.filter { it-> it.staple == "麵包" } as ArrayList<Food>)

                    //Other
                    listOfLists.add(menu.filter { it-> it.staple == "其他" } as ArrayList<Food>)

                    //PostValue is used to tell main activity this thread is done and return the lists
                    mLiveData.postValue(listOfLists)
                    //Tell main activity that this thread has finished getting data and return the data
                    mUserLiveData.postValue(user)
                }
                .addOnFailureListener { exception ->
                    Log.d("User", "Exception " + exception)
                }

        }
    }

    fun sim(userA : MutableList<MutableList<Float>> , userB : MutableList<MutableList<Float>>)  : Float {
        var top = 0F
        var btmA = 0F
        var btmB = 0F
        for (i in 0 until userA[0].size)
        {
            //Check if greater than -50F, if lesser than -50F then it is (non-rated)
            //If userA ith item is rated && userB as well, calculate top and bottom of equation
            if (userA[0][i] > -50F && userB[0][i] > -50F) {
                top += userA[0][i] * userB[0][i]
                btmA += userA[1][i]
                btmB += userB[1][i]
            }
        }
        return top/sqrt(btmA*btmB)
    }
}