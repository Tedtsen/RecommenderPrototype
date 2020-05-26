package com.example.recommenderprototype

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.Restaurant
import com.example.recommenderprototype.database.User
import kotlinx.android.synthetic.main.fragment_restaurant_details.*


class RestaurantDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_restaurant_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments!!
        val restaurantList =
            bundle.getParcelable<FoodDetailsFragment.RestaurantListParcel>("restaurantList")!!.restaurantList
        val selectedRestaurant = bundle.getParcelable<Restaurant>("selectedRestaurant")!!
        val menu = bundle.getParcelable<FoodDetailsFragment.MenuParcel>("menu")!!.menu
        val user = bundle.getParcelable<User>("user")!!

        detailsRestaurantName.text = selectedRestaurant.name
        val operatingHoursFormatted = selectedRestaurant.operating_hours.split("_")
        var operatingHoursFinal : String = ""
        if (selectedRestaurant.operating_hours != "") {
            operatingHoursFormatted.forEachIndexed { index, it ->
                if (index == 0)
                    operatingHoursFinal += it[0].toString() + it[1].toString() + ":" + it[2].toString() + it[3].toString() + "-" + it[4].toString() + it[5].toString() + ":" + it[6].toString() + it[7].toString()
                else
                    operatingHoursFinal += " " + it[0].toString() + it[1].toString() + ":" + it[2].toString() + it[3].toString() + "-" + it[4].toString() + it[5].toString() + ":" + it[6].toString() + it[7].toString()
            }
        }
        detailsRestaurantOperatingHours.text = operatingHoursFinal

        GoogleMap.setOnClickListener {
            val gmmIntentUri: Uri =
                Uri.parse("geo:" + selectedRestaurant.coordinates + "?q=" + selectedRestaurant.name)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(context!!.packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val builder = AlertDialog.Builder(context);
                builder.setMessage("Please install Google Maps");
                builder.setCancelable(false);
                builder.setPositiveButton("Install", getGoogleMapsListener())
                val dialog = builder.create()
                dialog.show()
            }
        }

        val filteredMenu = menu.filter { it.restaurant_id == selectedRestaurant.restaurant_id}
        val madapter = FoodRowAdapter(menu, user, restaurantList, listToApply = filteredMenu as ArrayList<Food>)
        restaurantMenuRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = madapter
        }
        arguments!!.clear()
    }

    fun getGoogleMapsListener(): DialogInterface.OnClickListener {
        return object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.google.android.apps.maps")
                )
                startActivity(intent)
                //Finish the activity so they can't circumvent the check
                activity?.finish()
            }
        }
    }
}


