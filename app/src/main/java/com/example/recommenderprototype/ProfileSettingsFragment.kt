package com.example.recommenderprototype

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.recommenderprototype.database.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.fragment_profile_settings.*
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.listOf
import kotlin.collections.set


class ProfileSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var previousCantEatString : String = ""
        var previousPreferString : String = ""
        var previousPreferNotString : String = ""
        var previousStapleWeight : String = ""
        var previousProteinWeight : String = ""
        val initProteinWeight = mutableListOf<Float>(0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F)
        val user = arguments!!.getParcelable<User>("user")!!

        //Use details got in mainActivityViewModel instead of accessing database again
        if (user.age != -1) {
            genderAutoCompleteTextView.hint = user.gender
            AgeEditText.hint = user.age.toString()
            heightEditText.hint = user.height.toString()
            weightEditText.hint = user.weight.toString()
            cantEatMultiAutoCompleteTextView.hint = user.cant_eat.joinToString(",")
            activityAutoCompleteTextView.hint = user.activity.toString()
            preferMultiAutoCompleteTextView.hint = user.prefer
            preferNotMultiAutoCompleteTextView.hint = user.prefer_not
            previousCantEatString = user.cant_eat.joinToString(",")
            previousPreferString = user.prefer.toString()
            previousPreferNotString = user.prefer_not.toString()
            previousStapleWeight = user.staple_weight.joinToString(",")
            previousProteinWeight = user.protein_weight.joinToString(",")
        }
        //Obsolete, check above
        //Show the details user already submitted before, when they created the profile for the first time
        /*val odb = FirebaseFirestore.getInstance()
        val docRef = odb.collection("user").document(FirebaseAuth.getInstance().currentUser!!.email!!)
        docRef.get().addOnSuccessListener { document ->
            if (document.exists()){
                genderAutoCompleteTextView.hint = document["gender"].toString()
                AgeEditText.hint = document["age"].toString()
                heightEditText.hint = document["height"].toString()
                weightEditText.hint = document["weight"].toString()
                cantEatMultiAutoCompleteTextView.hint = document["cant_eat"].toString()
                activityAutoCompleteTextView.hint = document["activity"].toString()
                preferMultiAutoCompleteTextView.hint = document["prefer"].toString()
                preferNotMultiAutoCompleteTextView.hint = document["prefer_not"].toString()
                previousPreferString = document["prefer"].toString()
                previousPreferNotString = document["prefer_not"].toString()
                previousStapleWeight = document["staple_weight"].toString()
                previousProteinWeight = document["protein_weight"].toString()
            }
        }*/

        //Setting the autocomplete list and spinners
        val genderList = listOf(getString(R.string.profile_settings_gender_male), getString(R.string.profile_settings_gender_female))
        genderAutoCompleteTextView.setAdapter(createAdapter(genderList, dropDownDesign = android.R.layout.simple_list_item_1))
        genderAutoCompleteTextView.showSelections()

        //Cant Eat
        //Hidden is for the Chinese version, as we will only record in chinese
        var cantEatHiddenInputString : String = ""
        var cantEatInputString : String = ""
        val cantEatListZH = listOf("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
        val cantEatList = view.resources.getStringArray(R.array.profile_settings_preference_items).toList()
        val cantEatBuilder = AlertDialog.Builder(this.context)
        cantEatBuilder.setTitle(getString(R.string.profile_settings_cant_eat))
        cantEatBuilder.setMessage(getString(R.string.profile_settings_cant_eat_hint))
        val cantEatListView =  ListView(this.context)
        cantEatListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        cantEatListView.adapter = createAdapter(cantEatList, dropDownDesign = android.R.layout.simple_list_item_multiple_choice)
        cantEatBuilder.setView(cantEatListView)
        cantEatBuilder.setPositiveButton(getString(R.string.profile_settings_done)){ dialog, which ->
            cantEatHiddenInputString = ""
            cantEatInputString = ""
            val checked: SparseBooleanArray = cantEatListView.checkedItemPositions
            for (i in 0 until cantEatListView.adapter.count) {
                if (checked[i]) {
                    if (cantEatHiddenInputString.length == 0) {

                        cantEatHiddenInputString += cantEatListZH[i]
                        //cantEatList can be in English or Chinese according to user's locale
                        //cantEatInputString is only for the sake of displaying
                        cantEatInputString += cantEatList[i]
                    }
                else {
                        cantEatHiddenInputString += ("," + cantEatListZH[i])
                        cantEatInputString += ("," + cantEatList[i])
                    }
                }
            }
            //cantEatInputString is only for the sake of displaying
            cantEatMultiAutoCompleteTextView.setText(cantEatInputString)
        }
        cantEatBuilder.setNegativeButton(getString(R.string.profile_settings_nothing_clear)){dialog, which ->
            cantEatMultiAutoCompleteTextView.setText(getString(R.string.profile_settings_nothing))
            cantEatInputString = getString(R.string.profile_settings_nothing)
            cantEatHiddenInputString = "沒有"
        }
        cantEatMultiAutoCompleteTextView.isEnabled = false
        val cantEatDialog = cantEatBuilder.create()
        cantEatTextView.setOnClickListener{cantEatDialog.show()}

        //Activity selections
        val activityList = view.resources.getStringArray(R.array.profile_setting_activity_items).toList()
        activityAutoCompleteTextView.setAdapter(createAdapter(activityList, dropDownDesign = android.R.layout.simple_list_item_1))
        activityAutoCompleteTextView.showSelections()

        //Prefer List
        //This is for the sake of display only
        var preferInputString: String = ""
        //Hidden one is to record
        var preferHiddenInputString: String = ""
        val preferList = view.resources.getStringArray(R.array.profile_settings_preference_items).toList()
        val preferListZH = listOf("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
        //Prefer Not List is declared here
        var preferNotList = view.resources.getStringArray(R.array.profile_settings_preference_items).toList()
        var preferNotListZH = listOf("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
        var preferSetFlag : Boolean = false
        val preferBuilder = AlertDialog.Builder(this.context)
        preferBuilder.setTitle(getString(R.string.profile_settings_prefer))
        preferBuilder.setMessage(getString(R.string.profile_settings_prefer_hint))
        val preferListView =  ListView(this.context)
        preferListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        preferListView.adapter = createAdapter(preferList, dropDownDesign = android.R.layout.simple_list_item_multiple_choice)
        preferBuilder.setView(preferListView)
        preferBuilder.setPositiveButton(getString(R.string.profile_settings_done)){dialog, which ->
            preferInputString = ""
            preferHiddenInputString = ""
            val checked: SparseBooleanArray = preferListView.checkedItemPositions
            for (i in 0 until cantEatListView.adapter.count) {
                if (checked[i]) {
                    if (preferHiddenInputString.length == 0){
                        preferHiddenInputString += preferListZH[i]
                        preferInputString += preferList[i]
                    }
                    else {
                        preferInputString += ("," + preferList[i])
                        preferHiddenInputString += ("," + preferListZH[i])
                    }
                }
            }
            preferMultiAutoCompleteTextView.setText(preferInputString)
            if (preferInputString != "")
                preferSetFlag = true
            else preferSetFlag = false
        }
        preferMultiAutoCompleteTextView.isEnabled = false
        val preferDialog = preferBuilder.create()
        preferTextView.setOnClickListener{preferDialog.show()}

        //After setting prefer, clear prefer not textview

        //Prefer Not List 
        var preferNotHiddenInputString: String = ""
        var preferNotInputString: String = ""
        val preferNotBuilder = AlertDialog.Builder(this.context)
        preferNotBuilder.setTitle(getString(R.string.profile_settings_prefer_not))
        preferNotBuilder.setMessage(getString(R.string.profile_settings_prefer_not_hint))
        val preferNotListView =  ListView(this.context)
        preferNotListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        preferNotListView.adapter = createAdapter(preferNotList, dropDownDesign = android.R.layout.simple_list_item_multiple_choice)
        preferNotBuilder.setView(preferNotListView)
        preferNotBuilder.setPositiveButton(getString(R.string.profile_settings_done)){dialog, which ->
            preferNotHiddenInputString = ""
            preferNotInputString = ""
            val checked: SparseBooleanArray = preferNotListView.checkedItemPositions
            for (i in 0 until preferNotListView.adapter.count) {
                if (checked[i]) {
                    if (preferNotHiddenInputString.length == 0){
                        preferNotHiddenInputString += preferNotListZH[i]
                        preferNotInputString += preferNotList[i]
                    }
                    else {
                        preferNotHiddenInputString += ("," + preferNotListZH[i])
                        preferNotInputString += ("," + preferNotList[i])
                    }
                }
            }
            preferNotMultiAutoCompleteTextView.setText(preferNotInputString)
        }
        preferNotMultiAutoCompleteTextView.isEnabled = false
        val dialog = preferNotBuilder.create()
        preferNotTextView.setOnClickListener{
            if (preferSetFlag)
                dialog.show()
            else Toast.makeText(context, getString(R.string.profile_settings_set_preferred_first), Toast.LENGTH_LONG).show()
        }
        
        /* Submission : 2 parts */
        submitProfileSettingsButton.setOnClickListener{

            //If user profile already exists, then allow empty fields
            val odb = FirebaseFirestore.getInstance()
            val docRef = odb.collection("user").document(FirebaseAuth.getInstance().currentUser!!.email!!)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()){
                    val gender = genderAutoCompleteTextView.getTextThenHint().translateToChinese()
                    val age = AgeEditText.getTextThenHint().toInt()
                    val height = heightEditText.getTextThenHint().toInt()
                    val weight = weightEditText.getTextThenHint().toInt()
                    val activity = activityAutoCompleteTextView.getTextThenHint().translateToChinese()

                    val details  = HashMap<String, kotlin.Any>()
                    details["gender"] = gender
                    details["age"] = age
                    details["height"] = height
                    details["weight"] = weight
                    details["cant_eat"] = if (cantEatHiddenInputString != "") cantEatHiddenInputString else previousCantEatString
                    details["prefer"] = if (preferHiddenInputString != "") preferHiddenInputString else previousPreferString
                    details["prefer_not"] = if (preferNotHiddenInputString != "") preferNotHiddenInputString else previousPreferNotString
                    details["activity"] = activity
                    details["staple_weight"] = previousStapleWeight

                    //Take data and check for conflict
                    val cantEatInputList = details["cant_eat"].toString().split(",")
                    val preferInputList = details["prefer"].toString().split(",")
                    val preferNotInputList = details["prefer_not"].toString().split(",")
                    if (checkIfConflict(cantEatInputList, preferInputList, preferNotInputList) == false){

                        //Protein weight is calculated here to ensure no conflict
                        //First calculate initProteinWeight
                        preferListZH.forEachIndexed{ index: Int, element: String ->
                            if (preferHiddenInputString.contains(element)){
                                initProteinWeight[index] = 1F
                            }
                            else if (preferNotHiddenInputString.contains(element)){
                                initProteinWeight[index] = 0F
                            }
                        }
                        //If new protein weight is different and not default, then upload new protein weight
                        details["protein_weight"] = if (initProteinWeight.joinToString(separator = ",") != previousProteinWeight
                            && initProteinWeight.joinToString (separator = ",") != "0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5")
                            initProteinWeight.joinToString (separator = ",")
                        else previousProteinWeight

                        val currentUser = FirebaseAuth.getInstance().currentUser!!
                        //Set above details if no conflict
                        odb.collection("user").document(currentUser.email!!).set(details, SetOptions.merge())

                        //RMB Set details to local reference too, if user is able to come to this state, that means user already has
                        //bookmark, history data, name, profile photo, email set, so we don't have to set anything for those attributes
                        user.gender = gender
                        user.age = age
                        user.height = height
                        user.weight = weight
                        user.cant_eat = details["cant_eat"].toString().split(",").toMutableList()
                        user.prefer = details["prefer"].toString()
                        user.prefer_not = details["prefer_not"].toString()
                        user.activity = activity
                        user.staple_weight = previousStapleWeight.split(",").map { it.toFloat() }.toMutableList()
                        user.protein_weight = details["protein_weight"].toString().split(",").map { it.toFloat() }.toMutableList()

                        Toast.makeText(this.context, getString(R.string.profile_settings_submitted_message), Toast.LENGTH_SHORT).show()
                        fragmentManager!!.popBackStack()
                    }
                    else
                        Snackbar.make(view, getString(R.string.profile_settings_conflict_warning_message), Snackbar.LENGTH_SHORT).show()
                }
                //If user profile doesn't exist, doesn't allow empty fields
                else{
                    var notEmptyCheckBoxes = BooleanArray(8){false}
                    if (genderAutoCompleteTextView.checkIfEmpty() == false)
                        notEmptyCheckBoxes[0] = true
                    if (AgeEditText.checkIfEmpty() == false)
                        notEmptyCheckBoxes[1] = true
                    if (heightEditText.checkIfEmpty() == false)
                        notEmptyCheckBoxes[2] = true
                    if (weightEditText.checkIfEmpty() == false)
                        notEmptyCheckBoxes[3] = true
                    if (cantEatHiddenInputString != "" || cantEatMultiAutoCompleteTextView.checkIfEmpty() == false)
                        notEmptyCheckBoxes[4] = true
                    if (activityAutoCompleteTextView.checkIfEmpty() == false)
                        notEmptyCheckBoxes[5] = true
                    if (preferHiddenInputString != "" || preferMultiAutoCompleteTextView.checkIfEmpty() == false)
                        notEmptyCheckBoxes[6] = true
                    //If user selects all prefer options, we set number 7 criterion, which is prefer not as true (not empty) to allow special case
                    if (preferHiddenInputString.length >= 25)
                        notEmptyCheckBoxes[7] = true
                    else if (preferNotHiddenInputString != "" || preferNotMultiAutoCompleteTextView.checkIfEmpty() == false)
                        notEmptyCheckBoxes[7] = true
                    var passed = true
                   for (status in notEmptyCheckBoxes)
                   {
                       if (status != true)
                           passed = false
                   }
                    //If no empty fields
                    if (passed){
                                val gender = genderAutoCompleteTextView.getTextThenHint().translateToChinese()
                                val age = AgeEditText.getTextThenHint().toInt()
                                val height = heightEditText.getTextThenHint().toInt()
                                val weight = weightEditText.getTextThenHint().toInt()
                                val activity = activityAutoCompleteTextView.getTextThenHint().translateToChinese()
                                val foodCount = arguments!!.getParcelable<MainActivity.CountParcel>("foodCount")!!.foodCount

                                //Package all details into a hashmap to upload to firestore
                                val details  = HashMap<String, kotlin.Any>()
                                details["gender"] = gender
                                details["age"] = age
                                details["height"] = height
                                details["weight"] = weight
                                details["cant_eat"] = cantEatHiddenInputString
                                details["prefer"] = preferHiddenInputString
                                details["prefer_not"] =  preferNotHiddenInputString
                                details["activity"] = activity
                                details["staple_weight"] = "1,1,1,1,1,1,1"
                                details["bookmark"] = IntArray(foodCount){_ -> 0}.joinToString(separator = ",")
                                details["history"] = ""
                                details["nutrition_edit_history"] = ""
                                details["photo_upload_history"] = ""

                                //Take data and check for conflict
                                val cantEatInputList = details["cant_eat"].toString().split(",")
                                val preferInputList = details["prefer"].toString().split(",")
                                val preferNotInputList = details["prefer_not"].toString().split(",")
                                if (checkIfConflict(cantEatInputList, preferInputList, preferNotInputList) == false){

                                    //Protein weight is calculated here to ensure no conflict
                                    //Calculate initial protein weight
                                    preferListZH.forEachIndexed{ index: Int, element: String ->
                                        if (preferHiddenInputString.contains(element)){
                                            initProteinWeight[index] = 1F
                                        }
                                        else if (preferNotHiddenInputString.contains(element)){
                                            initProteinWeight[index] = 0F
                                        }
                                    }
                                    details["protein_weight"] = initProteinWeight.joinToString (separator = ",")

                                    val currentUser = FirebaseAuth.getInstance().currentUser!!
                                    //Set above details if no conflict
                                    odb.collection("user").document(currentUser.email!!).set(details)

                                    //Initialise user entry in user-item matrix (get foodCount from parcel from MainActivity)
                                    val userEntry = hashMapOf("CF_score" to IntArray(foodCount){_-> 0}.joinToString(separator = ","))
                                    odb.collection("user_item_matrix").document(currentUser.email!!).set(userEntry)

                                    //RMB Set the details to local reference too
                                    user.email = currentUser.email!!
                                    user.google_account_profile_photo_url = currentUser.photoUrl.toString()
                                    user.google_account_name = currentUser.displayName!!
                                    user.gender = gender
                                    user.age = age
                                    user.height = height
                                    user.weight = weight
                                    user.cant_eat = cantEatHiddenInputString.split(",").toMutableList()
                                    user.prefer = details["prefer"].toString()
                                    user.prefer_not = details["prefer_not"].toString()
                                    user.activity = activity
                                    user.staple_weight = details["staple_weight"].toString().split(",").map { it.toFloat() }.toMutableList()
                                    user.protein_weight = details["protein_weight"].toString().split(",").map { it.toFloat() }.toMutableList()

                                    Toast.makeText(this.context, getString(R.string.profile_settings_submitted_message), Toast.LENGTH_SHORT).show()
                                    fragmentManager!!.popBackStack()
                                }
                                else
                                    Snackbar.make(view, getString(R.string.profile_settings_conflict_warning_message), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun String.translateToChinese() : String{
        var returnString = this
        when (this){
            "Male" -> returnString = "男"
            "Female" -> returnString = "女"
            "Rare" -> returnString = "極少"
            "Few" -> returnString = "少"
            "Moderate" -> returnString = "適中"
            "Frequent" -> returnString = "多"
            "Extreme" -> returnString = "極多"
        }
        return returnString
    }

    private fun EditText.getTextThenHint() : String{
        if (this.text.toString().trim().equals("",ignoreCase = true)){
            return this.hint.toString()
        }
        else
            return this.text.toString()
    }

    private fun EditText.checkIfEmpty() : Boolean{
        var isEmpty = false
        if (this@checkIfEmpty.text.toString().trim().equals("",ignoreCase = true)){
            this@checkIfEmpty.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_soft_red_24dp, 0)
            isEmpty = true
        }
        this.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (this@checkIfEmpty.text.toString().trim().equals("",ignoreCase = true)){
                    this@checkIfEmpty.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_error_soft_red_24dp, 0)
                    isEmpty = true
                }
                else{
                    this@checkIfEmpty.setCompoundDrawables(null,null,null,null)
                    isEmpty = false
                }
            }
        })
        return isEmpty
    }

    private fun checkIfConflict(listA : List<String>, listB : List<String>, listC : List<String>) : Boolean{
        listA.forEach { element ->
            if (listB.contains(element) || listC.contains(element))
                return true
        }
        listB.forEach { element ->
            if (listC.contains(element))
                return true
        }
        return false
    }

    private fun createAdapter(list : List<String>, dropDownDesign : Int) : ArrayAdapter<Any>{
        return ArrayAdapter(this.context!!, dropDownDesign, list)
    }

    private fun AutoCompleteTextView.showSelections(){
        this.setOnFocusChangeListener { v, hasFocus ->
            this.showDropDown()
            this.showSoftInputOnFocus = false
            this.hideKeyboard()
        }
        this.setOnClickListener {
            this.showDropDown()
        }
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}
