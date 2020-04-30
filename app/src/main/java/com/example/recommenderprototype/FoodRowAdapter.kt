package com.example.recommenderprototype

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.io.Serializable

//lambda listener
class FoodRowAdapter(private var allFood: List<Food>) :
    RecyclerView.Adapter<FoodRowAdapter.GridViewHolder>() {

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val title : TextView = itemView.findViewById(R.id.gridFoodName)
        val price : TextView = itemView.findViewById(R.id.gridFoodPrice)
        val image : ImageView = itemView.findViewById(R.id.gridFoodImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.food_row, parent, false)
        return GridViewHolder(view)
    }

    override fun getItemCount() = allFood.size

    override fun onBindViewHolder(holder: FoodRowAdapter.GridViewHolder, position: Int) {
        holder.itemView.setOnClickListener{
            Toast.makeText(holder.itemView.context, "Finally Available!", Toast.LENGTH_SHORT).show()
            //Bundle
            val bundle = Bundle()
            bundle.putParcelable("selectedFood", allFood[position])
            val foodDetailsFragment = FoodDetailsFragment()
            foodDetailsFragment.arguments = bundle

            //Update user weight on Firestore after click
            val odb = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null){
                val docRef = odb.collection("user").document(currentUser.email!!)
                val user = User()
                docRef.get().addOnSuccessListener { document ->
                    if (document.exists()){
                        //If userDocument exists
                        val staple_options = listOf<String>("飯", "麵", "餃", "餅", "湯", "麵包", "其他")
                        val protein_options = listOf<String>("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
                        user.staple_weight = document["staple_weight"].toString().split(",").map{it.toFloat()}.toMutableList()
                        user.protein_weight = document["protein_weight"].toString().split(",").map{it.toFloat()}.toMutableList()
                        staple_options.forEachIndexed { index, option ->
                            if (allFood[position].staple.contains(option))
                            {
                                if (user.staple_weight[index] < 5)
                                    user.staple_weight[index] += 0.1F
                            }
                        }
                        protein_options.forEachIndexed { index, option ->
                            if (allFood[position].main_ingredient.contains(option))
                            {
                                if (user.protein_weight[index] < 5)
                                    user.protein_weight[index] += 0.1F
                            }
                        }
                        odb.collection("user").document(currentUser.email!!).update("staple_weight", user.staple_weight.joinToString(separator = ","))
                        odb.collection("user").document(currentUser.email!!).update("protein_weight", user.protein_weight.joinToString(separator = ","))
                    }
                }
            }

            // load fragment
            val transaction = (holder.itemView.context as FragmentActivity).supportFragmentManager.beginTransaction()
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            transaction.replace(R.id.containerFullscreen, foodDetailsFragment, "FOOD_DETAILS_FRAGMENT_TAG")
            transaction.addToBackStack("FOOD_DETAILS_FRAGMENT_TAG")
            transaction.commit()

        }
        holder.title.text = allFood[position].name
        holder.price.text = allFood[position].price.toString()
        if (allFood[position].imgurl != "")
        {Picasso.get().load(allFood[position].imgurl).into(holder.image)}
    }
}
