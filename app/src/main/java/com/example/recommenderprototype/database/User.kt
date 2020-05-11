package com.example.recommenderprototype.database

import android.os.Parcelable
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User (
    var activity: String = "",
    var age: Int = -1,
    var gender: String = "",
    var height: Int = -1,
    var weight: Int = -1,
    var cant_eat: MutableList<String> = mutableListOf<String>(),
    var prefer: String = "",
    var prefer_not: String = "",
    var protein_weight: MutableList<Float> = mutableListOf<Float>(),
    var staple_weight: MutableList<Float> = mutableListOf<Float>(),
    var bookmark : MutableList<Int> = mutableListOf<Int>(),
    var history : MutableList<Int> = mutableListOf<Int>(),
    var nutrition_edit_history : MutableList<String> = mutableListOf<String>(),
    var photo_upload_history : MutableList<String> = mutableListOf<String>()
) : Parcelable