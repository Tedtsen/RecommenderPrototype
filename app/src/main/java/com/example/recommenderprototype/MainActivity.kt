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
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.content_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    val MY_REQUEST_CODE : Int = 1212
    lateinit var mViewModel: MainActivityViewModel
    private lateinit var auth: FirebaseAuth
    //global firestore food collection count var
    @Parcelize
    data class countParcel (var foodCount : Int = -1) : Parcelable
    var mParcel = countParcel()



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
        handler.postDelayed(runnable, 2000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showsplash()

        mViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        mViewModel.fetchData()
        mViewModel.mLiveData.observe(this, Observer {
            //Food Grid
            foodGridRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = FoodRowAdapter(mViewModel.mLiveData.value!![0])
            }

            val categories = listOf("Popular", "Suggestions", "Rice", "Noodle", "Dumpling", "Biscuit", "Soup", "Bread", "Other")
            horizontalRecyclerView.apply {
                var listOfLists = mViewModel.mLiveData.value!!
                //Update foodCount, so we can use in profile settings to create user-matrix columns
                mParcel.foodCount = listOfLists[0].size
                layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
                adapter = HorizontalRecyclerViewAdapter(categories, foodGridRecyclerView, listOfLists)
            }

            //Remove loading icon
            progressBar2.visibility = View.GONE
        })

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_home -> {
                    supportFragmentManager.popBackStack()
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_search -> {
                    val mFragment = supportFragmentManager.findFragmentByTag("SEARCH_FRAGMENT_TAG")
                    if (mFragment == null) {
                        supportFragmentManager.popBackStack()
                        loadFragment(SearchFragment(), "SEARCH_FRAGMENT_TAG", fullscreen = false)
                    }
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.menu_profile -> {
                    //Check if logged in
                    auth = FirebaseAuth.getInstance()
                    val currentUser = auth.currentUser
                    if (currentUser == null){
                        //if not logged in, go to Google sign-in (check onActivity Result)
                    startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build()))
                        .build(), MY_REQUEST_CODE)
                        }
                    else {
                        //if logged in (goto profile page)
                        val mFragment = supportFragmentManager.findFragmentByTag("PROFILE_FRAGMENT_TAG")
                        if (mFragment == null) {
                            supportFragmentManager.popBackStack()
                            loadFragment(ProfileFragment(), "PROFILE_FRAGMENT_TAG", fullscreen = false)
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
                val currentUserEmail = auth.currentUser!!.email!!
                Toast.makeText(this, "Signed in as ${currentUserEmail}", Toast.LENGTH_SHORT).show()
                val odb = FirebaseFirestore.getInstance()
                val docRef = odb.collection("user").document(currentUserEmail)
                docRef.get()
                    .addOnSuccessListener {document->
                        if (document.exists()) {
                            //if profile details are already SET (goto profile page)
                            val mFragment = supportFragmentManager.findFragmentByTag("PROFILE_FRAGMENT_TAG")
                            if (mFragment == null) {
                                supportFragmentManager.popBackStack()
                                loadFragment(ProfileFragment(), "PROFILE_FRAGMENT_TAG", fullscreen = false)
                            }
                        }
                        else {
                            //if profile details NOT set (goto profile settings)
                            val mFragment = supportFragmentManager.findFragmentByTag("PROFILE_SETTINGS_FRAGMENT_TAG")
                            if (mFragment==null){
                                supportFragmentManager.popBackStack()
                                //First-time login and profile NOT set, so we need to add entry in user-item matrix (need foodCount for column width)
                                val bundle = Bundle()
                                bundle.putParcelable("foodCount", mParcel)
                                val profileSettingsFragment = ProfileSettingsFragment()
                                profileSettingsFragment.arguments = bundle
                                // load fragment
                                val container = R.id.containerFullscreen
                                val transaction = supportFragmentManager.beginTransaction()
                                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                                transaction.replace(container, profileSettingsFragment, "PROFILE_SETTINGS_FRAGMENT_TAG")
                                transaction.addToBackStack("PROFILE_SETTINGS_FRAGMENT_TAG")
                                transaction.commit()
                            }
                        }
                }
            }else Log.d("Login", "Login result not successful")
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
        if (selectedItemId != R.id.menu_home && profileSettingsFragmentSelected == null) {
            //if current fragment NOT HOME, profile SETTINGS fragment NOT SELECTED, go back to home
            bottomNavigationView.selectedItemId = R.id.menu_home
            supportFragmentManager.popBackStack()
        }
        else if (profileSettingsFragmentSelected != null) {
            //BackButton Do nothing when pressed (profile NOT SET)
            //Else if profile fragment selected, then back to profile (because profile already set,
            // so that user was able to enter profile fragment in the first place)
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
