package com.example.recommenderprototype

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipData.newIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.recommenderprototype.database.Food
import com.firebase.ui.auth.AuthUI.getApplicationContext
import kotlinx.android.synthetic.main.fragment_imgur_webview.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest


class WebViewFragment : Fragment(){

    //Reference: https://github.com/mgks/Os-FileUp/blob/master/app/src/main/java/mgks/os/fileup/MainActivity.java
    /*-- CUSTOMIZE --*/ /*-- you can customize these options for your convenience --*/
    private val webview_url = "https://imgur.com/upload" // web address or local file location you want to open in webview
    private val file_type = "image/*" // file types to be allowed for upload
    private val multiple_files = false // allowing multiple file upload

    private val TAG = MainActivity::class.java.simpleName

    private var cam_file_data: String? = null // for storing camera file information

    private var file_data // data/header received after file selection
            : ValueCallback<Uri>? = null
    private var file_path // received file(s) temp. location
            : ValueCallback<Array<Uri>>? = null

    private val file_req_code = 1

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (Build.VERSION.SDK_INT >= 21) {
            var results: ArrayList<Uri>? = null

            /*-- if file request cancelled; exited camera. we need to send null value to make future attempts workable --*/if (resultCode == Activity.RESULT_CANCELED) {
                if (requestCode == file_req_code) {
                    file_path!!.onReceiveValue(null)
                    return
                }
            }

            /*-- continue if response is positive --*/if (resultCode == Activity.RESULT_OK) {
                if (requestCode == file_req_code) {
                    if (null == file_path) {
                        return
                    }
                    var clipData: ClipData?
                    var stringData: String?
                    try {
                        clipData = intent!!.clipData
                        stringData = intent.dataString
                    } catch (e: Exception) {
                        clipData = null
                        stringData = null
                    }
                    if (clipData == null && stringData == null && cam_file_data != null) {
                        results = arrayListOf(Uri.parse(cam_file_data))
                    } else {
                        if (clipData != null) { // checking if multiple files selected or not
                            val numSelectedFiles = clipData.itemCount
                            for (i in 0 until clipData.itemCount) {
                                results!!.add(clipData.getItemAt(i).uri)
                            }
                        } else {
                            results = arrayListOf(Uri.parse(stringData))
                        }
                    }
                }
            }
            file_path!!.onReceiveValue(results!!.toTypedArray())
            file_path = null
        } else {
            if (requestCode == file_req_code) {
                if (null == file_data) return
                val result =
                    if (intent == null || resultCode != RESULT_OK) null else intent.data
                file_data!!.onReceiveValue(result)
                file_data = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_imgur_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgurWebView.settings.javaScriptEnabled = true
        imgurWebView.settings.useWideViewPort = true
        imgurWebView.settings.loadWithOverviewMode = true
        imgurWebView.settings.allowFileAccess = true

        if (Build.VERSION.SDK_INT >= 21) {
            imgurWebView.settings.mixedContentMode = 0
            imgurWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else if (Build.VERSION.SDK_INT >= 19) {
            imgurWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            imgurWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        imgurWebView.webViewClient = Callback()
        imgurWebView.loadUrl(webview_url)
        imgurWebView.setWebChromeClient(object : WebChromeClient() {

            /*-- handling input[type="file"] requests for android API 21+ --*/
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                return if (file_permission() && Build.VERSION.SDK_INT >= 21) {
                    file_path = filePathCallback
                    var takePictureIntent: Intent? = null
                    var takeVideoIntent: Intent? = null
                    var includeVideo = false
                    var includePhoto = false

                    /*-- checking the accept parameter to determine which intent(s) to include --*/paramCheck@ for (acceptTypes in fileChooserParams.acceptTypes) {
                        val splitTypes = acceptTypes.split(", ?+")
                            .toTypedArray() // although it's an array, it still seems to be the whole value; split it out into chunks so that we can detect multiple values
                        for (acceptType in splitTypes) {
                            when (acceptType) {
                                "*/*" -> {
                                    includePhoto = true
                                    includeVideo = true
                                    break@paramCheck
                                }
                                "image/*" -> includePhoto = true
                                "video/*" -> includeVideo = true
                            }
                        }
                    }
                    if (fileChooserParams.acceptTypes.size == 0) {   //no `accept` parameter was specified, allow both photo and video
                        includePhoto = true
                        includeVideo = true
                    }
                    if (includePhoto) {
                        takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        if (takePictureIntent.resolveActivity(context!!.getPackageManager()) != null) {
                            var photoFile: File? = null
                            try {
                                photoFile = create_image()
                                takePictureIntent.putExtra("PhotoPath", cam_file_data)
                            } catch (ex: IOException) {
                                Log.e(TAG, "Image file creation failed", ex)
                            }
                            if (photoFile != null) {
                                cam_file_data = "file:" + photoFile.getAbsolutePath()
                                takePictureIntent.putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile)
                                )
                            } else {
                                cam_file_data = null
                                takePictureIntent = null
                            }
                        }
                    }
                    if (includeVideo) {
                        takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        if (takeVideoIntent.resolveActivity(context!!.getPackageManager()) != null) {
                            var videoFile: File? = null
                            try {
                                videoFile = create_video()
                            } catch (ex: IOException) {
                                Log.e(TAG, "Video file creation failed", ex)
                            }
                            if (videoFile != null) {
                                cam_file_data = "file:" + videoFile.getAbsolutePath()
                                takeVideoIntent.putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(videoFile)
                                )
                            } else {
                                cam_file_data = null
                                takeVideoIntent = null
                            }
                        }
                    }
                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    contentSelectionIntent.type = file_type
                    if (multiple_files) {
                        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    val intentArray: Array<Intent?>
                    if (takePictureIntent != null && takeVideoIntent != null) {
                        intentArray = arrayOf(takePictureIntent, takeVideoIntent)
                    } else if (takePictureIntent != null) {
                        intentArray = arrayOf(takePictureIntent)
                    } else if (takeVideoIntent != null){
                        intentArray = arrayOf(takeVideoIntent)
                    }else intentArray = arrayOfNulls(0)



                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser")
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                    startActivityForResult(chooserIntent, file_req_code)
                    true
                } else {
                    false
                }
            }
        })


    }

    /*-- callback reporting if error occurs --*/
    class Callback : WebViewClient() {
        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            // your code here
            if (url != "https://imgur.com/upload") {
                Log.d("upload album url", url)
                Toast.makeText(view!!.context, view!!.context.getString(R.string.food_details_image_upload_uploading), Toast.LENGTH_LONG).show()
                val uploadProgressBar = (view!!.context as FragmentActivity).supportFragmentManager.findFragmentByTag("FOOD_DETAILS_FRAGMENT_TAG")!!.view!!.findViewById<ProgressBar>(R.id.uploadProgressBar)
                uploadProgressBar.visibility = View.VISIBLE
                val viewModel = ViewModelProvider((view!!.context as FragmentActivity).supportFragmentManager.findFragmentByTag("FOOD_DETAILS_FRAGMENT_TAG")!!).get(WebViewViewModel::class.java)
                Handler().postDelayed(
                    {
                        // This method will be executed once the timer is over
                        (view!!.context as FragmentActivity).supportFragmentManager.popBackStack()
                        Toast.makeText(view!!.context, view!!.context.getString(R.string.food_details_image_upload_still_uploading), Toast.LENGTH_LONG).show()
                    },
                    3500 // value in milliseconds
                )
                Handler().postDelayed({
                    viewModel.data(inputAlbumUrl = url!!)
                }, 10000)
            }
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        //Doesn't detect javascript change
        /*override fun shouldOverrideUrlLoading(view: WebView?, url : String): Boolean {
            Log.d("url", url)
            if (url != "https://imgur.com/upload") {
                (view!!.context as FragmentActivity).supportFragmentManager.popBackStack()
                return true
            }
            return false
        }*/

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            Toast.makeText(view.context, "Failed loading app!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /*-- checking and asking for required file permissions --*/
    fun file_permission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(
                context!!,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context!!,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.CAMERA
                ),
                1
            )
            false
        } else {
            true
        }
    }

    /*-- creating new image file here --*/
    @Throws(IOException::class)
    private fun create_image(): File? {
        @SuppressLint("SimpleDateFormat") val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /*-- creating new video file here --*/
    @Throws(IOException::class)
    private fun create_video(): File? {
        @SuppressLint("SimpleDateFormat") val file_name: String =
            SimpleDateFormat("yyyy_mm_ss").format(Date())
        val new_name = "file_" + file_name + "_"
        val sd_directory: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(new_name, ".3gp", sd_directory)
    }
}
