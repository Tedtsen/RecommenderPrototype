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
            //val settings = FirebaseFirestoreSettings.Builder()
                //.setPersistenceEnabled(true).build()
            val odb: FirebaseFirestore = FirebaseFirestore.getInstance()
            val menu = ArrayList<Food>()
            var listOfLists = arrayListOf<ArrayList<Food>>()

            //Firestore database read function
            odb.collection("food_test").get()
                .addOnSuccessListener { results ->
                    var index = 0
                    for (document in results) {
                        //need to declr docObj here?
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
                                /*var userItemMatrix = mutableListOf<MutableList<Float>>()
                                var currentUserRow = mutableListOf<Float>()
                                //To store prediction of current user to all food in menu
                                var prediction  = mutableListOf<Float>()

                                odb.collection("user_item_matrix").get()
                                    .addOnSuccessListener { results->
                                        for (document in results){
                                            if (document.id != currentUser.email)
                                                userItemMatrix.add(document.toString().split(",").map{it.toFloat()}.toMutableList())
                                            else currentUserRow = document.toString().split(",").map{it.toFloat()}.toMutableList()
                                        }

                                        val ramean  = currentUserRow.average()
                                        //Columns: (rai-ramean) | (rai-ramean)^2 Calculate this first, as it is reusable
                                        var calcTableA = mutableListOf<MutableList<Float>>()
                                        for (i in 0 until menu.size){
                                            var value : Float = (currentUserRow[i] - ramean).toFloat()
                                            calcTableA[0].add(value)
                                            calcTableA[1].add(value.pow(2))
                                        }

                                        userItemMatrix.forEachIndexed { index, user ->
                                            //For every other user in the matrix, calculate its table
                                            val rbmean = user.average()
                                            //Columns: (rbi-rbmean) | (rbi-rbmean)^2
                                            var calcTableB = mutableListOf<MutableList<Float>>()
                                            for (i in 0 until menu.size){
                                                var value : Float = (user[i] - rbmean).toFloat()
                                                calcTableB[0].add(value)
                                                calcTableB[1].add(value.pow(2))
                                            }
                                            for (i in 0 until menu.size){
                                                if (sim )
                                            }
                                        }
                                    }*/

                                // //Recommendation
                                for (i in 0 until menuRecommended.size){
                                    menuRecommended[i].score = CB_score[i]!!
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
                    Log.d("deb", listOfLists.size.toString())
                    mLiveData.postValue(listOfLists)

                }
                .addOnFailureListener { exception ->
                    Log.d("User", "Exception " + exception)
                }

        }
    }

    fun sim(userA : MutableList<MutableList<Float>> , userB : MutableList<MutableList<Float>>)  : Float {
        var top = 0F
        var btm = 0F
        for (i in 0 until userA[0].size)
        {
            top += userA[0][i]*userB[0][i]
            btm += userB[1][i]*userB[1][i]
        }
        return top/sqrt(btm)
    }
}