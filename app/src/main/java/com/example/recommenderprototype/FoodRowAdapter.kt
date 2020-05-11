package com.example.recommenderprototype

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.Restaurant
import com.example.recommenderprototype.database.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.parcel.Parcelize

class FoodRowAdapter() :
    RecyclerView.Adapter<FoodRowAdapter.GridViewHolder>(), Filterable {
    private var menuFull = ArrayList<Food>()
    private var menu = ArrayList<Food>()
    private var user = User()
    private var restaurantList = ArrayList<Restaurant>()

    @Parcelize
    data class CountParcel (val size : Int = -1) : Parcelable

    @Parcelize
    data class RestaurantListParcel (val restaurantList : ArrayList<Restaurant> = ArrayList<Restaurant>()) : Parcelable

    @Parcelize
    data class MenuFullParcel (val menu : ArrayList<Food> = ArrayList<Food>()) : Parcelable

    constructor( originalMenu: List<Food>, inputUser : User, inputRestaurantList: List<Restaurant>,
                 //For category selection
                 categoryIndex : Int? = null, listOfLists : ArrayList<ArrayList<Food>>? = null,
                 //For misc lists
                 listToApply : ArrayList<Food>? = null) : this() {

        this.user = inputUser
        this.restaurantList = ArrayList<Restaurant>(inputRestaurantList)
        //If this is not category selection from HorizontalRecyclerViewAdapter
        if (categoryIndex == null) {
            if (listToApply == null)
                this.menu = ArrayList<Food>(originalMenu)
            else this.menu = listToApply
        }
        else this.menu = listOfLists!![categoryIndex]
        menuFull = ArrayList<Food>(originalMenu)
    }

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val title : TextView = itemView.findViewById(R.id.gridFoodName)
        val price : TextView = itemView.findViewById(R.id.gridFoodPrice)
        val restaurantName : TextView = itemView.findViewById(R.id.gridFoodRestaurant)
        val image : ImageView = itemView.findViewById(R.id.gridFoodImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.food_row, parent, false)
        return GridViewHolder(view)
    }

    override fun getItemCount() = menu.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: FoodRowAdapter.GridViewHolder, position: Int) {

        holder.itemView.setOnClickListener{
            //Update user weight on Firestore after click
            val odb = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null){
                val docRef = odb.collection("user").document(currentUser.email!!)
                docRef.get().addOnSuccessListener { document ->
                    if (document.exists()){
                        //If userDocument exists
                        val staple_options = listOf<String>("飯", "麵", "餃", "餅", "湯", "麵包", "其他")
                        val protein_options = listOf<String>("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
                        user.staple_weight = document["staple_weight"].toString().split(",").map{it.toFloat()}.toMutableList()
                        user.protein_weight = document["protein_weight"].toString().split(",").map{it.toFloat()}.toMutableList()
                        user.history = document["history"].toString().split(",").map { it.toInt() }.toMutableList()
                        staple_options.forEachIndexed { index, option ->
                            if (menu[position].staple.contains(option) && menu[position].staple == option)
                            {
                                if (user.staple_weight[index] < 5)
                                    user.staple_weight[index] += 0.1F
                            }
                        }
                        protein_options.forEachIndexed { index, option ->
                            if (menu[position].main_ingredient.contains(option))
                            {
                                if (user.protein_weight[index] < 5)
                                    user.protein_weight[index] += 0.1F
                            }
                        }
                        //Increase the weights of the selected staple and main ingredient under the user profile
                        odb.collection("user").document(currentUser.email!!).update("staple_weight", user.staple_weight.joinToString(separator = ","))
                        odb.collection("user").document(currentUser.email!!).update("protein_weight", user.protein_weight.joinToString(separator = ","))

                    }
                }
                val matrixDocRef = odb.collection("user_item_matrix").document(currentUser.email!!)
                matrixDocRef.get().addOnSuccessListener { document ->
                    if (document.exists()){
                        var currentUserRow  = document["CF_score"].toString().split(",").map{it.toFloat()}.toMutableList()
                        //Check if currentUserRow size is up-to-date in case of new food document added
                        while (currentUserRow.size < menu.size)
                            currentUserRow.add(0F)
                        //Increase the food score in user-item matrix
                        if (currentUserRow[menu[position].matrix_index] < 5)
                            currentUserRow[menu[position].matrix_index] = currentUserRow[menu[position].matrix_index] + 1F
                        odb.collection("user_item_matrix").document(currentUser.email!!).update("CF_score", currentUserRow.joinToString (separator = ","))
                    }
                }
            }

            //Increase the "clicks" field of the selected food
            odb.collection("food_test").document(menu[position].menu_id).update("clicks", FieldValue.increment(1))

            //Bundle
            val bundle = Bundle()
            bundle.putParcelable("selectedFood", menu[position])
            bundle.putParcelable("menuSize", CountParcel(size = menu.size))
            bundle.putParcelable("user", user)
            bundle.putParcelable("restaurantList", RestaurantListParcel(restaurantList = restaurantList))
            bundle.putParcelable("menuFull", MenuFullParcel(menu = menuFull))
            val foodDetailsFragment = FoodDetailsFragment()
            foodDetailsFragment.arguments = bundle

            // load fragment of the selected food
            //REDUNDANT
            val transaction = (holder.itemView.context as FragmentActivity).supportFragmentManager.beginTransaction()
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            transaction.add(R.id.containerFullscreen, foodDetailsFragment, "FOOD_DETAILS_FRAGMENT_TAG")
            transaction.addToBackStack("FOOD_DETAILS_FRAGMENT_TAG")
            transaction.commit()
        }

        //Fill the text of TextViews in each row
        holder.title.text = menu[position].name
        holder.price.text = menu[position].price.toString()
        holder.restaurantName.text = restaurantList.filter { it.restaurant_id == menu[position].restaurant_id }.first().name
        Picasso.get().load(menu[position].imgurl).resize(180,180).centerCrop().into(holder.image)
    }

    override fun getFilter(): Filter {
        return menuFilter
    }

    val menuFilter = object : Filter(){
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredMenu = arrayListOf<Food>()

            if (constraint == null || constraint.isEmpty()){
                filteredMenu.addAll(menuFull)
            }
            else{
                val filterPattern = constraint.toString().trim()

                for (food in menu){
                    if (food.name.toString().contains(filterPattern)
                        || food.staple.toString().contains(filterPattern)
                        || food.main_ingredient.toString().contains(filterPattern)
                        || food.tag.toString().contains(filterPattern)
                        || restaurantList.filter { it.restaurant_id == food.restaurant_id }.first().name.contains(filterPattern)  )
                    {
                        filteredMenu.add(food)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredMenu
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            menu.clear()
            menu.addAll(results!!.values as ArrayList<Food>)
            notifyDataSetChanged()
        }

    }
}

