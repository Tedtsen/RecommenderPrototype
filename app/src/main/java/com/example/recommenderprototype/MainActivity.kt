package com.example.recommenderprototype

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.Restaurant
import com.example.recommenderprototype.database.User
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private var user = User()
    val MY_REQUEST_CODE : Int = 1212
    lateinit var mViewModel: MainActivityViewModel
    private lateinit var auth: FirebaseAuth

    @Parcelize
    data class RestaurantListParcel (var restaurantList : ArrayList<Restaurant> = ArrayList<Restaurant>()) : Parcelable
    private var mRestaurantListParcel = RestaurantListParcel()

    //global firestore food collection count var
    @Parcelize
    data class CountParcel (var foodCount : Int = -1) : Parcelable
    private var mCountParcel = CountParcel()

    @Parcelize
    data class ListsParcel (var listOfLists : ArrayList<ArrayList<Food>> = ArrayList<ArrayList<Food>>()) : Parcelable
    private var mListsParcel = ListsParcel()

    private fun showsplash() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.activity_splash_screen)
        dialog.setCancelable(true)
        dialog.show()

        val handler = Handler()
        val runnable = Runnable() {
            kotlin.run { dialog.dismiss() }
        }
        handler.postDelayed(runnable, 5000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showsplash()

        //Coroutine thread to get data and calculate recommendation
        mViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        mViewModel.fetchData()
        //Observe if user personal info document reading from firestore is done
        mViewModel.mUserLiveData.observe(this, Observer {
            user = mViewModel.mUserLiveData.value!!

            //Observe if all restaurant data is obtained
            mViewModel.mRestaurantListLiveData.observe(this, Observer {
                mRestaurantListParcel.restaurantList = mViewModel.mRestaurantListLiveData.value!!

                //If user personal info reading is done, observe if the calculation of recommendation is done
                mViewModel.mLiveData.observe(this, Observer {
                    val listOfLists = mViewModel.mLiveData.value!!
                    //Update foodCount, so we can use in profile settings to create user-matrix columns
                    mCountParcel.foodCount = listOfLists[0].size
                    mListsParcel.listOfLists = listOfLists

                    //Food Grid, populate the first recycler view
                    foodGridRecyclerView.apply {
                        layoutManager = LinearLayoutManager(this@MainActivity)
                        adapter = FoodRowAdapter(mViewModel.mLiveData.value!![0], user, mRestaurantListParcel.restaurantList)
                    }

                    val categories = resources.getStringArray(R.array.categories).toList()
                    horizontalRecyclerView.apply {
                        layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
                        adapter = HorizontalRecyclerViewAdapter(categories, foodGridRecyclerView, listOfLists, user, mRestaurantListParcel.restaurantList)
                    }

                    //Remove loading icon
                    progressBar2.visibility = View.GONE
                })
            })
        })


        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_home -> {
                    supportFragmentManager.popBackStack()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_search -> {
                    val mFragment = supportFragmentManager.findFragmentByTag("SEARCH_FRAGMENT_TAG")
                    if (mFragment == null && progressBar2.visibility == View.GONE) {
                        supportFragmentManager.popBackStack()
                        val bundle = Bundle()
                        bundle.putParcelable("menu", mListsParcel)
                        bundle.putParcelable("user", user)
                        bundle.putParcelable("restaurantList", mRestaurantListParcel)
                        val searchFragment = SearchFragment()
                        searchFragment.arguments = bundle
                        loadFragment(searchFragment, "SEARCH_FRAGMENT_TAG", fullscreen = false)
                    }
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_profile -> {
                    //Check if logged in
                    auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser
                    if (currentUser == null && progressBar2.visibility == View.GONE){
                        //if not logged in, go to Google sign-in (check onActivity Result)
                    startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                        .build(), MY_REQUEST_CODE)
                        }
                    else {
                        //if logged in (goto profile page)
                        val mFragment = supportFragmentManager.findFragmentByTag("PROFILE_FRAGMENT_TAG")
                        if (mFragment == null && progressBar2.visibility == View.GONE) {
                            val bundle = Bundle()
                            bundle.putParcelable("listOfLists", mListsParcel)
                            bundle.putParcelable("user", user)
                            bundle.putParcelable("restaurantList", mRestaurantListParcel)
                            val profileFragment = ProfileFragment()
                            profileFragment.arguments = bundle
                            supportFragmentManager.popBackStack()
                            loadFragment(profileFragment, "PROFILE_FRAGMENT_TAG", fullscreen = false)
                        }
                    }
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK)
            {
                //Signed in successfully, check if profile details are set
                auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser!!
                Toast.makeText(this, getString(R.string.profile_signed_in_as)+ " " + currentUser.email, Toast.LENGTH_SHORT).show()
                val odb = FirebaseFirestore.getInstance()
                val docRef = odb.collection("user").document(currentUser.email!!)
                docRef.get()
                    .addOnSuccessListener {document->
                        if (document.exists()) {
                            //if profile details are already SET (goto profile page)
                            val mFragment = supportFragmentManager.findFragmentByTag("PROFILE_FRAGMENT_TAG")
                            if (mFragment == null) {

                                //Needs to reassign all local reference of user info after login successful
                                user.bookmark = document["bookmark"].toString().split(",").map { it.toInt() }.toMutableList()

                                //Check if bookmark length is up-to-date
                                while (user.bookmark.size < mListsParcel.listOfLists[0].size)
                                    user.bookmark.add(0)

                                mListsParcel.listOfLists[0].forEachIndexed { index, food ->
                                    if (user.bookmark[food.matrix_index] == 1)
                                        food.bookmark = true
                                }

                                //Get google provider data
                                //Sometimes currentUser.displayName returns null and causes the app to crash, get data directly from auth provider (Google)
                                //If Google doesn't have it, take from firebase,
                                //If that fails, don't set anything and use default
                                currentUser.let{
                                    for (profile in it.providerData){
                                        if (profile.providerId == GoogleAuthProvider.PROVIDER_ID) {
                                            Log.d("google user", profile.photoUrl.toString() + " " + profile.displayName)
                                            Log.d("firebase user", currentUser.photoUrl.toString() + " " + currentUser.displayName)
                                            if (profile.photoUrl != null && profile.displayName != null){
                                                user.google_account_profile_photo_url = profile.photoUrl.toString()
                                                user.google_account_name = profile.displayName!!
                                            }
                                            else if (currentUser.photoUrl != null && currentUser.displayName != null){
                                                user.google_account_profile_photo_url = currentUser.photoUrl.toString()
                                                user.google_account_name = currentUser.displayName!!
                                            }
                                        }
                                    }
                                }
                                user.email = currentUser.email.toString()
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


                                val bundle = Bundle()
                                bundle.putParcelable("listOfLists", mListsParcel)
                                bundle.putParcelable("user", user)
                                bundle.putParcelable("restaurantList", mRestaurantListParcel)
                                val profileFragment = ProfileFragment()
                                profileFragment.arguments = bundle
                                supportFragmentManager.popBackStack()
                                loadFragment(profileFragment, "PROFILE_FRAGMENT_TAG", fullscreen = false)
                            }
                        }
                        else {
                            //if profile details NOT set (goto profile settings)
                            val mFragment = supportFragmentManager.findFragmentByTag("PROFILE_SETTINGS_FRAGMENT_TAG")
                            if (mFragment==null){
                                supportFragmentManager.popBackStack()

                                //First-time login and profile NOT set, so we need to add entry in user-item matrix (need foodCount for column width)
                                val bundle = Bundle()
                                bundle.putParcelable("foodCount", mCountParcel)
                                bundle.putParcelable("user", user)
                                val profileSettingsFragment = ProfileSettingsFragment()
                                profileSettingsFragment.arguments = bundle

                                // load fragment
                                /*val container = R.id.containerFullscreen
                                val transaction = supportFragmentManager.beginTransaction()
                                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                                transaction.replace(container, profileSettingsFragment, "PROFILE_SETTINGS_FRAGMENT_TAG")
                                transaction.addToBackStack("PROFILE_SETTINGS_FRAGMENT_TAG")
                                transaction.commit()*/
                                loadFragment(profileSettingsFragment, "PROFILE_SETTINGS_FRAGMENT_TAG", fullscreen = true)
                            }
                        }
                }
            }else Log.d("Login", "Login Result Not Successful")
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String, fullscreen : Boolean) {
        // load fragment
        val container = if (fullscreen) R.id.containerFullscreen else R.id.container
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
        transaction.replace(container, fragment, tag)
        transaction.addToBackStack(tag)
        transaction.commit()
    }

    override fun onBackPressed() {
        val selectedItemId: Int = bottomNavigationView.selectedItemId
        val profileSettingsFragmentSelected = supportFragmentManager.findFragmentByTag("PROFILE_SETTINGS_FRAGMENT_TAG")
        val foodDetailsFragmentSelected = supportFragmentManager.findFragmentByTag("FOOD_DETAILS_FRAGMENT_TAG")
        val miscListsFragmentSelected = supportFragmentManager.findFragmentByTag("MISC_LISTS_FRAGMENT_TAG")
        if (selectedItemId != R.id.menu_home && profileSettingsFragmentSelected == null
            && foodDetailsFragmentSelected == null && miscListsFragmentSelected == null) {
            //if current fragment NOT HOME, profile SETTINGS fragment NOT SELECTED,
            //food details in search fragment NOT SELECTED, misc lists fragment NOT SELECTED go back to home
            bottomNavigationView.selectedItemId = R.id.menu_home
            supportFragmentManager.popBackStack()
        }
        else if (profileSettingsFragmentSelected != null) {
            //BackButton Do nothing when pressed (profile NOT SET)
            //Else if profile fragment selected, then back to profile (because profile already set,
            //so that user was able to enter profile fragment in the first place)
            val profileFragmentSelected = supportFragmentManager.findFragmentByTag("PROFILE_FRAGMENT_TAG")
            if (profileFragmentSelected != null)
                supportFragmentManager.popBackStack()
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        val profileSettingsFragmentSelected = supportFragmentManager.findFragmentByTag("PROFILE_SETTINGS_FRAGMENT_TAG")
        if (profileSettingsFragmentSelected != null){
            //If user logged in and profile NOT SET
            //Check if user came from profile, if NOT from profile, home button logs user out
            val profileFragmentSelected = supportFragmentManager.findFragmentByTag("PROFILE_FRAGMENT_TAG")
            if (profileFragmentSelected == null) {
                AuthUI.getInstance().signOut(this)
            }
        }
        super.onPause()
    }
}
