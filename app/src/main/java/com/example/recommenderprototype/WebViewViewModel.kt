package com.example.recommenderprototype

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recommenderprototype.database.Food
import kotlinx.coroutines.launch

class WebViewViewModel : ViewModel() {

    var data = MutableLiveData<String>()
    var imgUrlData = MutableLiveData<String?>()

    fun data (inputAlbumUrl : String){
        data.value = inputAlbumUrl
    }

    fun imgUrlData (inputImgUrl : String?){
        imgUrlData.postValue(inputImgUrl)
    }
}