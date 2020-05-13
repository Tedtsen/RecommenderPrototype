package com.example.recommenderprototype

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.Restaurant
import com.example.recommenderprototype.database.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_food_details.*
import kotlinx.android.synthetic.main.nutrition_information_edit_dialog.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class FoodDetailsFragment : Fragment() , LifecycleObserver{

    var api_key = ""
    var request_url = ""
    var albumUrl = ""

    @Parcelize
    data class RestaurantListParcel (val restaurantList : ArrayList<Restaurant> = ArrayList<Restaurant>()) : Parcelable

    @Parcelize
    data class MenuParcel (val menu : ArrayList<Food> = ArrayList<Food>()) : Parcelable

    fun createNewIntent (message : String) : Intent {
        val intent = Intent()
        intent.putExtra("message", message)
        return intent
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_food_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uploadProgressBar.visibility = View.INVISIBLE

        //Imgur API
        api_key = getString(R.string.imgur_api_key)
        request_url = getString(R.string.request_url)

        val bundle = arguments!!
        val selectedFood : Food = bundle!!.getParcelable<Food>("selectedFood")!!
        val menuSizeParcel  = bundle.getParcelable<FoodRowAdapter.CountParcel>("menuSize")!!
        val user = bundle.getParcelable<User>("user")!!
        val restaurantList = bundle.getParcelable<FoodRowAdapter.RestaurantListParcel>("restaurantList")!!.restaurantList
        val menu = bundle.getParcelable<FoodRowAdapter.MenuFullParcel>("menuFull")!!.menu
        Picasso.get().load(selectedFood.imgurl).resize(180,180).centerCrop().into(detailsFoodImage)
        detailsFoodName.text = selectedFood.name
        detailsFoodPrice.text = selectedFood.price.toString()
        detailsFoodRestaurant.text = restaurantList.filter { it.restaurant_id ==  selectedFood.restaurant_id}.first().name
        foodTypeValTextView.text = selectedFood.staple
        calorieValTextView.text =  selectedFood.calorie.toString()
        starchValTextView.text = selectedFood.starch.toString()
        proteinValTextView.text = selectedFood.protein.toString()
        fatValTextView.text = selectedFood.fat.toString()

        val mFragment = (context as FragmentActivity).supportFragmentManager.findFragmentByTag("RESTAURANT_DETAILS_FRAGMENT_TAG")
        if (mFragment != null) {
            //if Restaurant fragment was created, take away the drawable and doesn't allow click
            detailsFoodRestaurant.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0)
        }
        //else if Restaurant fragment doesn't exist, allow creation of Restaurant fragment
        detailsFoodRestaurant.setOnClickListener {
            val mFragment = (context as FragmentActivity).supportFragmentManager.findFragmentByTag("RESTAURANT_DETAILS_FRAGMENT_TAG")
            if (mFragment == null) {
                val bundle = Bundle()
                bundle.putParcelable("restaurantList", RestaurantListParcel(restaurantList))
                bundle.putParcelable("selectedRestaurant", restaurantList.filter { it.restaurant_id == selectedFood.restaurant_id }.first())
                bundle.putParcelable("menu", MenuParcel(menu))
                bundle.putParcelable("user", user)
                val restaurantDetailsFragment = RestaurantDetailsFragment()
                restaurantDetailsFragment.arguments = bundle

                val transaction =
                    (context as FragmentActivity).supportFragmentManager.beginTransaction()
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                transaction.add(R.id.containerFullscreen, restaurantDetailsFragment, "RESTAURANT_DETAILS_FRAGMENT_TAG")
                transaction.addToBackStack("RESTAURANT_DETAILS_FRAGMENT_TAG")
                transaction.commit()
            }
        }

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

                        val editHistoryRecord : String  = selectedFood.menu_id+"/"+selectedFood.calorie.toString()+"/"+selectedFood.starch.toString()+"/"+selectedFood.protein.toString()+"/"+selectedFood.fat.toString()
                        user.nutrition_edit_history.add(editHistoryRecord)
                        odb.collection("user").document(userEmail).update("nutrition_edit_history", user.nutrition_edit_history.joinToString(separator = ","))

                        odb.collection("food_test").document(selectedFood.menu_id).update(
                            "calorie" , selectedFood.calorie,
                            "starch" , selectedFood.starch,
                            "protein" , selectedFood.protein,
                            "fat" , selectedFood.fat
                        )
                    })
                builder.show()
            }

            //Start browser intent to upload and detect the album url
            //Then using imgur API, we get the image direct url and add to firestore as well as local food entry
            detailsFoodImage.setOnClickListener{

                //Create new fragment with webView to load imgur upload page
                val webViewFragment = WebViewFragment()
                val transaction =
                    (context as FragmentActivity).supportFragmentManager.beginTransaction()
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                transaction.add(R.id.containerFullscreen, webViewFragment, "WEB_VIEW_FRAGMENT_TAG")
                transaction.addToBackStack("WEB_VIEW_FRAGMENT_TAG")
                transaction.commit()


                //Wait for webView to return the album url of the image uploaded by user
                val viewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)
                viewModel.data.observe(this, Observer {
                    albumUrl = viewModel.data.value!!

                    //Regular expression pattern matching to extract album id from the album url
                    val pattern = "[^\\/]+\$".toRegex()
                    val albumId = pattern.find(albumUrl)!!.value
                    Log.d("album Url and album id", albumUrl + " " + albumId)

                    //Access imgur API with album id to get the image url
                    requestAlbumInfo(request_url = URL(request_url + albumId))

                    //Wait for return of image url data from HTTP connection to imgur API
                    viewModel.imgUrlData.observe(this, Observer {
                        val imageUrl = viewModel.imgUrlData.value
                        if (imageUrl != null){
                            selectedFood.imgurl = imageUrl
                            user.photo_upload_history.add(selectedFood.menu_id+"/"+selectedFood.imgurl)
                            odb.collection("food_test").document(selectedFood.menu_id).update("imgurl", selectedFood.imgurl)
                            odb.collection("user").document(userEmail).update("photo_upload_history", user.photo_upload_history.joinToString(separator = ","))
                            Picasso.get().load(selectedFood.imgurl).resize(180,180).centerCrop().into(detailsFoodImage)
                            Toast.makeText(this.context, getString(R.string.food_details_image_upload_success), Toast.LENGTH_SHORT).show()
                            uploadProgressBar.visibility = View.GONE
                        }
                        else{
                            Toast.makeText(this.context, getString(R.string.food_details_image_upload_failed), Toast.LENGTH_SHORT).show()
                            uploadProgressBar.visibility = View.GONE
                        }
                    })
                })
            }


            //Record the matrix_index of selected food into user history
            user.history.add(selectedFood.matrix_index)
            odb.collection("user").document(userEmail).update("history", user.history.joinToString(separator = ","))
        }
        //If not logged in, show pop up telling login to bookmark
        else{
            detailsFoodImage.setOnClickListener{
                Toast.makeText(context, getString(R.string.food_details_login_to_edit), Toast.LENGTH_SHORT).show()
            }
            bookmarkButton.setOnCheckedChangeListener { buttonView, isChecked ->
                Toast.makeText(context, getString(R.string.food_details_login_to_bookmark), Toast.LENGTH_SHORT).show()
            }
            nutritionInformationTextView.setOnClickListener{
                Toast.makeText(context, getString(R.string.food_details_login_to_edit), Toast.LENGTH_SHORT).show()
            }
        }

        mainIngredientsListView.apply {
            adapter = ArrayAdapter(this.context, android.R.layout.simple_list_item_1, selectedFood.main_ingredient.chunked(size = 2))
        }
    }

    private fun requestAlbumInfo(request_url : URL){

        //Create a background thread to access imgur API to get the uploaded image id
        class asyncCaller() : AsyncTask<Void, Void, String>() {
            override fun doInBackground(vararg params: Void?): String? {

                //Authentication header - Authorization: Client-ID "Client-ID"
                val basicAuth = "Client-ID " + api_key
                val urlConnection : HttpsURLConnection = request_url.openConnection() as HttpsURLConnection
                urlConnection.setRequestProperty("Authorization", basicAuth)
                urlConnection.requestMethod = "GET"
                urlConnection.connect()
                val viewModel = ViewModelProvider(this@FoodDetailsFragment).get(WebViewViewModel::class.java)
                if (urlConnection.responseMessage == "OK"){
                    Log.d("imgur API response", urlConnection.responseMessage)
                    val jsonString = urlConnection.inputStream.bufferedReader().readText()
                    Log.d("imgur API response data", jsonString)
                    val jsonObject = JSONObject(jsonString)
                    val data  = JSONObject(jsonObject.get("data").toString())
                    val imageArray  = JSONArray(data.get("images").toString())
                    //Sometimes upload too slow or imgur doesn't have time to generate album cover
                    if (imageArray.isNull(0) ){
                        urlConnection.disconnect()
                        viewModel.imgUrlData(inputImgUrl = null)
                        return null
                    }
                    val firstImage  = JSONObject(imageArray.getJSONObject(0).toString())
                    val imageUrl = firstImage.get("link") as String
                    urlConnection.disconnect()
                    viewModel.imgUrlData(inputImgUrl = imageUrl)
                    return imageUrl
                }
                else {
                    Log.d("imgur API response", urlConnection.responseMessage)
                    urlConnection.disconnect()
                    viewModel.imgUrlData(inputImgUrl = null)
                    return null
                }
            }
        }
        asyncCaller().execute()
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

