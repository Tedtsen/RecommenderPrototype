package com.example.recommenderprototype

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.res.ComplexColorCompat.inflate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_food_details.*
import kotlinx.android.synthetic.main.nutrition_information_edit_dialog.*
import kotlinx.android.synthetic.main.nutrition_information_edit_dialog.view.*
import java.util.zip.Inflater

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
        detailsFoodRestaurant.text = selectedFood.rest_id
        foodTypeValTextView.text = selectedFood.staple
        calorieValTextView.text =  selectedFood.calorie.toString()
        starchValTextView.text = selectedFood.starch.toString()
        proteinValTextView.text = selectedFood.protein.toString()
        fatValTextView.text = selectedFood.fat.toString()

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

            //Edit button on click listener, show dialog
            nutritionInformationTextView.setOnClickListener {
                //Edit nutrition information
                val builder = AlertDialog.Builder(this.context)
                val view = layoutInflater.inflate(R.layout.nutrition_information_edit_dialog,null)
                builder.setView(view)
                    .setTitle(getString(R.string.food_details_edit_title))
                    .setMessage(getString(R.string.food_details_edit_description))
                    .setNegativeButton(getString(R.string.profile_feedback_cancel), DialogInterface.OnClickListener { dialog, which ->  })
                    .setPositiveButton(getString(R.string.profile_settings_submit), DialogInterface.OnClickListener { dialog, which ->
                        if (view.calorieEditText.checkIfEmpty() == false)
                            selectedFood.calorie = view.calorieEditText.text.toString().toInt()
                        if (view.starchEditText.checkIfEmpty() == false)
                            selectedFood.starch = view.starchEditText.text.toString().toInt()
                        if (view.proteinEditText.checkIfEmpty() == false)
                            selectedFood.protein = view.proteinEditText.text.toString().toInt()
                        if (view.fatEditText.checkIfEmpty() == false)
                            selectedFood.fat = view.fatEditText.text.toString().toInt()

                        calorieValTextView.text = selectedFood.calorie.toString()
                        starchValTextView.text = selectedFood.starch.toString()
                        proteinValTextView.text = selectedFood.protein.toString()
                        fatValTextView.text = selectedFood.fat.toString()

                        odb.collection("food_test").document(selectedFood.menu_id).update(
                            "calorie" , selectedFood.calorie,
                            "starch" , selectedFood.starch,
                            "protein" , selectedFood.protein,
                            "fat" , selectedFood.fat
                        )
                    })
                builder.show()
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
            nutritionInformationTextView.setOnClickListener{
                Toast.makeText(context, getString(R.string.food_details_login_to_edit), Toast.LENGTH_SHORT).show()
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

    private fun EditText.checkIfEmpty() : Boolean{
        var isEmpty = false
        if (this@checkIfEmpty.text.toString().trim().equals("",ignoreCase = true)){
            this@checkIfEmpty.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_soft_red_24dp, 0)
            isEmpty = true
        }
        this.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (this@checkIfEmpty.text.toString().trim().equals("",ignoreCase = true)){
                    this@checkIfEmpty.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_soft_red_24dp, 0)
                    isEmpty = true
                }
                else{
                    this@checkIfEmpty.setCompoundDrawables(null,null,null,null)
                    isEmpty = false
                }
            }
        })
        return isEmpty
    }
}

