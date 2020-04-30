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
        holder.categoryName.text = categories[position]
        holder.itemView.setOnClickListener {
            if (categories[position] == "Popular") {
                changeRecyclerViewList(holder, 0, position)
            }
            else if (categories[position] == "Suggestions") {
                changeRecyclerViewList(holder, 1, position)
            }
            else if (categories[position] == "Rice") {
                changeRecyclerViewList(holder, 2, position)
            }
            else if (categories[position] == "Noodle") {
                changeRecyclerViewList(holder, 3, position)
            }
            else if (categories[position] == "Dumpling") {
                changeRecyclerViewList(holder, 4, position)
            }
            else if (categories[position] == "Biscuit") {
                changeRecyclerViewList(holder, 5, position)
            }
            else if (categories[position] == "Soup") {
                changeRecyclerViewList(holder, 6, position)
            }
            else if (categories[position] == "Bread") {
                changeRecyclerViewList(holder, 7, position)
            }
            else if (categories[position] == "Other") {
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