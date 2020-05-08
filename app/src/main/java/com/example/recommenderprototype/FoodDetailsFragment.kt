package com.example.recommenderprototype

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_food_details.*

class FoodDetailsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_food_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments!!
        val selectedFood : Food = bundle!!.getParcelable<Food>("selectedFood")!!
        val menuSizeParcel  = bundle.getParcelable<FoodRowAdapter.CountParcel>("menuSize")!!
        val user = bundle.getParcelable<User>("user")!!
        detailsFoodName.text = selectedFood.name
        detailsFoodPrice.text = selectedFood.price.toString()
        //detailsFoodRestaurant.text = selectedFood.rest_id
        foodTypeValTextView.text = selectedFood.staple

        //Init bookmark drawable first
        if (selectedFood.bookmark == true) {
            bookmarkButton.isChecked = true
            bookmarkButton.setBackgroundResource(R.drawable.ic_bookmark_yellow_24dp)
        }
        else {
            bookmarkButton.isChecked = false
            bookmarkButton.setBackgroundResource(R.drawable.ic_bookmark_border_grey_24dp)
        }
        bookmarkButton.text = null
        bookmarkButton.textOn = null
        bookmarkButton.textOff = null

        //Check login status
        val loginStatus = FirebaseAuth.getInstance().currentUser
        //If logged in
        if (loginStatus != null){
            //Get user document to check for bookmark
            val odb = FirebaseFirestore.getInstance()
            val userEmail = loginStatus.email!!

            //Check if bookmark size is up-to-date in case of new food document added
            while (user.bookmark.size < menuSizeParcel.size)
                user.bookmark.add(0)

            //If toggle button changed to check, then update firestore user bookmark data and local bookmark data for every food
            bookmarkButton.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isChecked == true) {
                    bookmarkButton.setBackgroundResource(R.drawable.ic_bookmark_yellow_24dp)
                    user.bookmark[selectedFood.matrix_index] = 1
                    odb.collection("user").document(userEmail).update("bookmark", user.bookmark.joinToString(","))
                    selectedFood.bookmark = true
                    Toast.makeText(context, getString(R.string.food_details_bookmarked), Toast.LENGTH_SHORT).show()
                }
                else if (buttonView.isChecked == false){
                    bookmarkButton.setBackgroundResource( R.drawable.ic_bookmark_border_grey_24dp)
                    user.bookmark[selectedFood.matrix_index] = 0
                    odb.collection("user").document(userEmail).update("bookmark", user.bookmark.joinToString(","))
                    selectedFood.bookmark = false
                    Toast.makeText(context, getString(R.string.food_details_remove_bookmark), Toast.LENGTH_LONG).show()
                }
            }

            //Record the matrix_index of selected food into user history
            user.history.add(selectedFood.matrix_index)
            odb.collection("user").document(userEmail).update("history", user.history.joinToString(separator = ","))
        }
        //If not logged in, show pop up telling login to bookmark
        else{
            bookmarkButton.setOnCheckedChangeListener { buttonView, isChecked ->
                Toast.makeText(context, getString(R.string.food_details_login_to_bookmark), Toast.LENGTH_SHORT).show()
            }
        }

        val mFragment = (context as FragmentActivity).supportFragmentManager.findFragmentByTag("RESTAURANT_DETAILS_FRAGMENT_TAG")
        if (mFragment != null) {
            //if Restaurant fragment was created, take away the drawable and doesn't allow click
            detailsFoodRestaurant.setCompoundDrawables(null, null, null, null)
        }
            //else if Restaurant fragment doesn't exist, allow creation of Restaurant fragment
            detailsFoodRestaurant.setOnClickListener {
                val mFragment = (context as FragmentActivity).supportFragmentManager.findFragmentByTag("RESTAURANT_DETAILS_FRAGMENT_TAG")
                if (mFragment == null) {
                    val transaction =
                        (context as FragmentActivity).supportFragmentManager.beginTransaction()
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    transaction.add(R.id.containerFullscreen, RestaurantDetailsFragment(), "RESTAURANT_DETAILS_FRAGMENT_TAG")
                    transaction.addToBackStack("RESTAURANT_DETAILS_FRAGMENT_TAG")
                    transaction.commit()
                }
            }

        mainIngredientsListView.apply {
            adapter = ArrayAdapter(this.context, android.R.layout.simple_list_item_1, selectedFood.main_ingredient.chunked(size = 2))
        }
    }

    override fun onResume() {
        detailsFoodRestaurant.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_assignment_soft_blue_24dp, 0)
        super.onResume()
    }
}

