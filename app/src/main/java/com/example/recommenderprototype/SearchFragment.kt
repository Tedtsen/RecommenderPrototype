package com.example.recommenderprototype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderprototype.database.Food
import com.example.recommenderprototype.database.User
import kotlinx.android.synthetic.main.fragment_search.*

class SearchFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Replace the magnifying glass action button
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        //Default results
        val listOfLists = arguments!!.getParcelable<MainActivity.ListsParcel>("menu")!!.listOfLists
        val user = arguments!!.getParcelable<User>("user")!!
        val restaurantList = arguments!!.getParcelable<MainActivity.RestaurantListParcel>("restaurantList")!!.restaurantList
        val mAdapter = FoodRowAdapter(listOfLists[0], user, restaurantList)
        searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        //Not setting up search submit button
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                mAdapter.filter.filter(newText)
                searchRecyclerView.apply {
                    adapter = mAdapter
                }
                return false
            }

        })
        arguments!!.clear()
    }
}

