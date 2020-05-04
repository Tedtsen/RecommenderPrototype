package com.example.recommenderprototype.database

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
@Entity
data class Food(
    val rest_id: String = "",
    var menu_id: String = "",
    val name: String = "",
    val price: Int = -1,
    val staple: String = "",
    val main_ingredient: String = "",
    val tag: String = "",
    val clicks: Int = -1,
    val imgurl : String? = "https://via.placeholder.com/128.png",
    var score : Float = 0F,
    val matrix_index : Int = -1
) : Parcelable

