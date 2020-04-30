package com.example.recommenderprototype.database

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
    var staple_weight: MutableList<Float> = mutableListOf<Float>()
)