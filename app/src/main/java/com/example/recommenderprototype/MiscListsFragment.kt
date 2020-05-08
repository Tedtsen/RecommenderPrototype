package com.example.recommenderprototype

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recommenderprototype.database.User
import kotlinx.android.synthetic.main.fragment_misc_lists.*

class MiscListsFragment : Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_misc_lists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listTitle = arguments!!.getParcelable<ProfileFragment.ListTitleParcel>("listTitle")!!.title
        val listToApply = arguments!!.getParcelable<ProfileFragment.ListParcel>("listToApply")?.list
        val user = arguments!!.getParcelable<User>("user")!!

        miscListsTitle.text = listTitle
        miscListsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            if (listToApply != null)
                adapter = FoodRowAdapter(listToApply, user)
            else adapter = FoodRowAdapter()
        }
    }
}
