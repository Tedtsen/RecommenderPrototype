package com.example.recommenderprototype

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile_settings.*
import kotlinx.coroutines.internal.artificialFrame

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logoutButton.setOnClickListener {
            AuthUI.getInstance().signOut(this.context!!).addOnSuccessListener {
                Toast.makeText(this.context, getString(R.string.profile_loggedout_message), Toast.LENGTH_SHORT).show()
                fragmentManager!!.popBackStack()
            }
        }
        profileSettingsButton.setOnClickListener {
            //Link to Profile Settings fragment (Back pressed able)
            //No pop stack, Profile Settings fragment on top of Profile fragment
            val transaction = fragmentManager!!.beginTransaction()
            transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
            transaction.replace(R.id.containerFullscreen, ProfileSettingsFragment(), "PROFILE_SETTINGS_FRAGMENT_TAG")
            transaction.addToBackStack("PROFILE_SETTINGS_FRAGMENT_TAG")
            transaction.commit()
        }
        feedbackButton.setOnClickListener {
            val input: EditText = EditText(this.context)
            val builder = AlertDialog.Builder(this.context)
            builder.setTitle(getString(R.string.profile_feedback_and_suggestions))
            builder.setMessage(getString(R.string.profile_feedback_message))
            builder.setView(input)
            builder.setPositiveButton(getString(R.string.profile_settings_submit)) { dialog, which ->
                Toast.makeText(this.context, getString(R.string.profile_feedback_submitted), Toast.LENGTH_LONG).show()

            }
            builder.setNegativeButton(getString(R.string.profile_feedback_cancel)) { dialog, which ->
                dialog.dismiss()
            }
            builder.show()
        }
    }
}
