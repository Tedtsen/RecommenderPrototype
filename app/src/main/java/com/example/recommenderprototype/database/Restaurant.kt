package com.example.recommenderprototype.database

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Restaurant (
    val name : String = "",
    val restaurant_id : String = "",
    val coordinates : String = "",
    val operating_hours : String = ""
) : Parcelable