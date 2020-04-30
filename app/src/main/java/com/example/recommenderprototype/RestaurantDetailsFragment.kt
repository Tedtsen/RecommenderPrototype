package com.example.recommenderprototype

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_restaurant_details.*


class RestaurantDetailsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_restaurant_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GoogleMap.setOnClickListener {
            val gmmIntentUri: Uri = Uri.parse("geo:23.9048125,121.5315601?q=吉米餐坊")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(context!!.packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val builder = AlertDialog.Builder(context);
                builder.setMessage("Please install Google Maps");
                builder.setCancelable(false);
                builder.setPositiveButton("Install", getGoogleMapsListener())
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    fun getGoogleMapsListener() : DialogInterface.OnClickListener{
        return object : DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.google.android.apps.maps")
                )
                startActivity(intent)
                //Finish the activity so they can't circumvent the check
                activity?.finish()
            }
        }
    }
}


