package com.example.recommenderprototype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.example.recommenderprototype.database.Food
import kotlinx.android.synthetic.main.fragment_food_details.*
import kotlinx.android.synthetic.main.fragment_restaurant_details.*

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

        val bundle = arguments
        val selectedFood : Food = bundle!!.getParcelable<Food>("selectedFood")!!
        detailsFoodName.text = selectedFood.name
        detailsFoodPrice.text = selectedFood.price.toString()
        //detailsFoodRestaurant.text = selectedFood.rest_id
        foodTypeValTextView.text = selectedFood.staple

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
    }

    override fun onResume() {
        detailsFoodRestaurant.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_assignment_soft_blue_24dp, 0)
        super.onResume()
    }
}