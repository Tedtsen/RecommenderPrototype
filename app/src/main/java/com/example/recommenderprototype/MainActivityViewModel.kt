package com.example.recommenderprototype

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivityViewModel : ViewModel() {
    val mLiveData = MutableLiveData<ArrayList<ArrayList<Food>>>()
    fun fetchData() {

        viewModelScope.launch(Dispatchers.Default) {

            val odb: FirebaseFirestore = FirebaseFirestore.getInstance()
            val menu = ArrayList<Food>()
            var listOfLists = arrayListOf<ArrayList<Food>>()

            //Firestore database read function
            odb.collection("food_test").get()
                .addOnSuccessListener { results ->
                    var index = 0
                    for (document in results) {
                        menu.add(index, document.toObject(Food::class.java))
                        menu[index].menu_id = document.id
                    }

                    //Create different categories of menus
                    var menuRecommended : ArrayList<Food> = menu.toCollection(ArrayList<Food>())
                    var menuPopular = menu.toCollection(ArrayList<Food>())

                    //Get current user details if logged in
                    //Check if logged in
                    val auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser
                    if (currentUser != null){
                        //If logged in, do recommendation algorithm, do other menus nonetheless
                        val docRef = odb.collection("user").document(currentUser.email!!)
                        val user = User()
                        docRef.get().addOnSuccessListener { document ->
                            if (document.exists()){
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

                                //Start the calculation for Suggestions
                                // //Knowledge Based, to set the CB_score of food to -1 if menu.main_ingredient
                                // //matches user.cant_eat
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

                                // //Content - Based
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

                                // //Collab - Filter
                                //Firestore database read user-item matrix
                                var userItemMatrix = mutableListOf<MutableList<Float>>()
                                var currentUserRow = mutableListOf<Float>()
                                //To store prediction of current user to all food in menu
                                var prediction  = arrayOfNulls<Float>(size = menu.size)
                                prediction.fill(0F,fromIndex = 0, toIndex = menu.size)

                                odb.collection("user_item_matrix").get()
                                    .addOnSuccessListener { results->
                                        for (document in results) {
                                            //Log.d("prediction", document["CF_score"].toString())
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
                                            //Log.d("prediction", "calcTableA[0]["+i+"] " + value)
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
                                                    //Log.d("prediction", "calcTableB[0]["+i+"] " + value)
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
                                                        //Log.d("prediction", "simofA"+index + " " + simOfAB + ", " + RBmean)
                                                    }
                                                }
                                            }
                                            //only update prediction for current user non-rated items (<= 0)
                                            if (currentUserRow[i] <= 0){
                                                prediction[i] = (RAmean + predTop/predBtm).toFloat()
                                                Log.d("prediction", prediction[i].toString() + " " + i)}
                                            //else Log.d("prediction", currentUserRow.to)
                                        }
                                    }

                                // //Recommendation
                                for (i in 0 until menuRecommended.size){
                                    if (CB_score[i]!! >= 0F)
                                    menuRecommended[i].score = ( 0.7*CB_score[i]!! + 0.3*prediction[i]!! ).toFloat()
                                    Log.d("debug", menuRecommended[i].menu_id)
                                }
                                menuRecommended.sortByDescending { it.score }
                            }
                        }
                    }
                    else{
                        //If not logged in, recommended change to login reminder
                        menuRecommended = arrayListOf(Food(name = "Login to view!"))
                    }

                    //Popular
                    menuPopular.sortByDescending { it.clicks }
                    listOfLists.add(menuPopular)

                    //Recommended
                    listOfLists.add(menuRecommended)

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

                    //postValue is used to tell main activity this thread is done
                    mLiveData.postValue(listOfLists)

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