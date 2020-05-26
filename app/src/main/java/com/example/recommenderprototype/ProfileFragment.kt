package com.example.recommenderprototype

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.Restaurant
import com.example.recommenderprototype.database.User
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_food_details.*
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.bookmarkButton
import kotlinx.android.synthetic.main.fragment_profile_settings.*
import kotlinx.coroutines.internal.artificialFrame
import java.util.*
import kotlin.collections.ArrayList

class ProfileFragment : Fragment() {

    @Parcelize
    class ListParcel (val list : ArrayList<Food> = arrayListOf(Food())) : Parcelable
    @Parcelize
    class ListTitleParcel (val title : String = "") : Parcelable
    @Parcelize
    class RestaurantListParcel (val restaurantList : ArrayList<Restaurant> = ArrayList()) : Parcelable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listOfLists = arguments!!.getParcelable<MainActivity.ListsParcel>("listOfLists")!!.listOfLists
        val user = arguments!!.getParcelable<User>("user")!!
        val restaurantList = arguments!!.getParcelable<MainActivity.RestaurantListParcel>("restaurantList")!!.restaurantList

        if (user.google_account_profile_photo_url != "")
            Picasso.get().load(user.google_account_profile_photo_url).resize(512, 512).centerCrop().into(userPhotoImageView)
        if (user.google_account_name != "")
            usernameTextView.text = user.google_account_name
        else usernameTextView.text = user.email

        logoutButton.setOnClickListener {
            AuthUI.getInstance().signOut(this.context!!).addOnSuccessListener {
                Toast.makeText(this.context, getString(R.string.profile_loggedout_message), Toast.LENGTH_SHORT).show()
                fragmentManager!!.popBackStack()
            }
        }
        profileSettingsButton.setOnClickListener {
            //Link to Profile Settings fragment (Back pressed able)
            //No pop stack, Profile Settings fragment on top of Profile fragment
            val transaction = fragmentManager!!.beginTransaction()
            val profileSettingsFragment = ProfileSettingsFragment()
            val bundle = Bundle()
            bundle.putParcelable("user", user)
            profileSettingsFragment.arguments = bundle
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
            transaction.replace(R.id.containerFullscreen, profileSettingsFragment, "PROFILE_SETTINGS_FRAGMENT_TAG")
            transaction.addToBackStack("PROFILE_SETTINGS_FRAGMENT_TAG")
            transaction.commit()
        }
        bookmarkButton.setOnClickListener {

            val bookmarkList = listOfLists[0].filter { it.bookmark == true } as ArrayList<Food>
            val bundle = Bundle()
            bundle.putParcelable("originalList", ListParcel(listOfLists[0]))
            bundle.putParcelable("listToApply", ListParcel(bookmarkList))
            bundle.putParcelable("listTitle", ListTitleParcel(getString(R.string.profile_bookmark)))
            bundle.putParcelable("user", user)
            bundle.putParcelable("restaurantList", RestaurantListParcel(restaurantList = restaurantList))
            val miscListsFragment = MiscListsFragment()
            miscListsFragment.arguments = bundle

            val mFragment = fragmentManager!!.findFragmentByTag("MISC_LISTS_FRAGMENT_TAG")
            if (mFragment == null) {
                val transaction = fragmentManager!!.beginTransaction()
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                transaction.add(R.id.containerFullscreen, miscListsFragment, "MISC_LISTS_FRAGMENT_TAG")
                transaction.addToBackStack("MISC_LISTS_FRAGMENT_TAG")
                transaction.commit()
            }
        }
        historyButton.setOnClickListener {

            val historyList = arrayListOf<Food>()
            val userHistoryReversed = user.history.reversed()

            if (user.history.isNotEmpty()) {
                //Take the matrix_index in reversed order from user
                for (record in userHistoryReversed){
                    //Add the food in listOfLists where its matrix_index matches the one from the user
                    //Take the first result returned and there will always be one result as matrix_index is unique
                    historyList.add(listOfLists[0].filter { it.matrix_index == record }.first())
                }
            }

            val bundle = Bundle()
            bundle.putParcelable("originalList", ListParcel(listOfLists[0]))
            bundle.putParcelable("listToApply", ListParcel(historyList))
            bundle.putParcelable("listTitle", ListTitleParcel(getString(R.string.profile_history)))
            bundle.putParcelable("user", user)
            bundle.putParcelable("restaurantList", RestaurantListParcel(restaurantList))
            val miscListsFragment = MiscListsFragment()
            miscListsFragment.arguments = bundle

            val mFragment = fragmentManager!!.findFragmentByTag("MISC_LISTS_FRAGMENT_TAG")
            if (mFragment == null) {
                val transaction = fragmentManager!!.beginTransaction()
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                transaction.add(R.id.containerFullscreen, miscListsFragment, "MISC_LISTS_FRAGMENT_TAG")
                transaction.addToBackStack("MISC_LISTS_FRAGMENT_TAG")
                transaction.commit()
            }
        }
        feedbackButton.setOnClickListener {
            val input: EditText = EditText(this.context)
            val builder = AlertDialog.Builder(this.context)
            builder.setTitle(getString(R.string.profile_feedback_and_suggestions))
            builder.setMessage(getString(R.string.profile_feedback_message))
            builder.setView(input)
            builder.setPositiveButton(getString(R.string.profile_settings_submit)) { dialog, which ->
                val tz = TimeZone.getTimeZone("GMT+08:00");
                val c = Calendar.getInstance(tz);
                val dateAndTime : String = c.get(Calendar.YEAR).toString()+"_"+(c.get(Calendar.MONTH)+1).toString()+"_"+c.get(Calendar.DATE).toString()+"_"+c.get(Calendar.HOUR_OF_DAY).toString()+":"+c.get(Calendar.MINUTE).toString()
                val odb = FirebaseFirestore.getInstance()
                val data = hashMapOf(
                    "user" to user.email,
                    "feedback" to input.text.toString()
                )
                odb.collection("user_feedback").document(dateAndTime).set(data)
                Toast.makeText(this.context, getString(R.string.profile_feedback_submitted), Toast.LENGTH_LONG).show()
            }
            builder.setNegativeButton(getString(R.string.profile_feedback_cancel)) { dialog, which ->
                dialog.dismiss()
            }
            builder.show()
        }
        arguments!!.clear()
    }
}
