package com.example.recommenderprototype

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recommenderprototype.database.Food
import kotlinx.android.synthetic.main.category_row.view.*
import kotlinx.android.synthetic.main.content_main.*

class HorizontalRecyclerViewAdapter(val categories: List<String>, foodGridRecyclerView: RecyclerView, listOfLists : List<List<Food>>) :
    RecyclerView.Adapter<HorizontalRecyclerViewAdapter.ViewHolder>() {

    val mainRV = foodGridRecyclerView
    val lists = listOfLists
    var selectedPosition = 0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val categoryName = view.categoryName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.category_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoriesStrings = holder.itemView.resources.getStringArray(R.array.categories).toList()
        holder.categoryName.text = categories[position]
        holder.itemView.setOnClickListener {
            if (categories[position] == categoriesStrings[0]) {
                changeRecyclerViewList(holder, 0, position)
            }
            else if (categories[position] == categoriesStrings[1]) {
                changeRecyclerViewList(holder, 1, position)
            }
            else if (categories[position] == categoriesStrings[2]) {
                changeRecyclerViewList(holder, 2, position)
            }
            else if (categories[position] == categoriesStrings[3]) {
                changeRecyclerViewList(holder, 3, position)
            }
            else if (categories[position] == categoriesStrings[4]) {
                changeRecyclerViewList(holder, 4, position)
            }
            else if (categories[position] == categoriesStrings[5]) {
                changeRecyclerViewList(holder, 5, position)
            }
            else if (categories[position] == categoriesStrings[6]) {
                changeRecyclerViewList(holder, 6, position)
            }
            else if (categories[position] == categoriesStrings[7]) {
                changeRecyclerViewList(holder, 7, position)
            }
            else if (categories[position] == categoriesStrings[8]) {
                changeRecyclerViewList(holder, 8, position)
            }
        }
        if (position == selectedPosition)
            holder.itemView.setBackgroundResource(R.drawable.rounded_corners_selected)
        else
            holder.itemView.setBackgroundResource(R.drawable.rounded_corners)
    }

    fun changeRecyclerViewList (holder : ViewHolder, listIndex : Int, position : Int){
        mainRV.apply {
            selectedPosition = position
            layoutManager = LinearLayoutManager(holder.itemView.context)
            adapter = FoodRowAdapter(lists[listIndex])
        }
        notifyDataSetChanged()
    }
}