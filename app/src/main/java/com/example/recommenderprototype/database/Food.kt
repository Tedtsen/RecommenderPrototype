package com.example.recommenderprototype.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
@Entity
data class Food(
    val restaurant_id: String = "",
    var menu_id: String = "",
    val name: String = "",
    val price: Int = -1,
    val staple: String = "",
    val main_ingredient: String = "",
    var calorie: Int = -1,
    var starch: Int = -1,
    var protein: Int = -1,
    var fat: Int = -1,
    val tag: String = "",
    val clicks: Int = -1,
    var imgurl: String? = "https://via.placeholder.com/128.png",
    var score: Float = 0F,
    val matrix_index: Int = -1,
    var bookmark: Boolean = false
) : Parcelable

