package com.example.recommenderprototype

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

        //Need to record previous Prefer and Prefer Not as they are not saved to hint (3 bugs known not patched, if updating prefernot, and leave prefer the same,
        // there will be duplicate items that will occur in both prefernot and prefer)(WEIGHT BUG, if resetting of profile doesnt touch prefer/notprefer, weight resetted)
        //ZH ENG TRANSLATION ARGHHHHHHHH!!!!
        var previousPreferString : String = ""
        var previousPreferNotString : String = ""
        var previousStapleWeight : String = ""
        var previousProteinWeight : String = ""
        val initProteinWeight = mutableListOf<Float>(0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F,0.5F)

        //Show the details user already submitted before when creating the profile for the first time
        val odb = FirebaseFirestore.getInstance()
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
        }

        //Setting the autocomplete list and spinners
        val genderList = listOf("Male", "Female")
        genderAutoCompleteTextView.setAdapter(createAdapter(genderList, dropDownDesign = android.R.layout.simple_list_item_1))
        genderAutoCompleteTextView.showSelections()

        //Cant Eat
        val cantEatListZH = listOf("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
        val cantEatList = listOf("Chicken", "Pork", "Beef", "Mutton", "Fish", "Seafood", "Egg", "Vege", "Tofu")
        val cantEatBuilder = AlertDialog.Builder(this.context)
        cantEatBuilder.setTitle("Can't Eat")
        cantEatBuilder.setMessage("Select the options below that you can't consume")
        val cantEatListView =  ListView(this.context)
        cantEatListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        cantEatListView.adapter = createAdapter(cantEatList, dropDownDesign = android.R.layout.simple_list_item_multiple_choice)
        cantEatBuilder.setView(cantEatListView)
        cantEatBuilder.setPositiveButton("Done"){ dialog, which ->
            var cantEatInputString : String = ""
            val checked: SparseBooleanArray = cantEatListView.getCheckedItemPositions()
            for (i in 0 until cantEatListView.getAdapter().getCount()) {
                if (checked[i]) {
                    if (cantEatInputString.length == 0)
                        cantEatInputString += cantEatListZH[i]
                else cantEatInputString += ("," + cantEatListZH[i])
                }
            }
            cantEatMultiAutoCompleteTextView.setText(cantEatInputString)
        }
        cantEatBuilder.setNegativeButton("Clear selections"){dialog, which ->
            cantEatMultiAutoCompleteTextView.setText("Nothing")
        }
        cantEatMultiAutoCompleteTextView.isEnabled = false
        val cantEatDialog = cantEatBuilder.create()
        cantEatTextView.setOnClickListener{cantEatDialog.show()}
        cantEatListView.setOnItemClickListener { parent, view, position, id ->
        }

        //Activity selections
        val activityList = listOf("Rare", "Few", "Moderate", "Frequent", "Extreme")
        activityAutoCompleteTextView.setAdapter(createAdapter(activityList, dropDownDesign = android.R.layout.simple_list_item_1))
        activityAutoCompleteTextView.showSelections()

        //Prefer List
        var preferInputString: String = ""
        val preferList = listOf("Chicken", "Pork", "Beef", "Mutton", "Fish", "Seafood", "Egg", "Vege", "Tofu")
        val preferListZH = listOf("雞肉", "豬肉", "牛肉", "羊肉", "魚肉", "海鮮", "雞蛋", "蔬果", "豆腐")
        //Prefer Not List is declared here, to exclude selected preferred items
        var preferNotList = mutableListOf<String>()
        var preferNotListZH = mutableListOf<String>()
        var preferSetFlag : Boolean = false
        val preferBuilder = AlertDialog.Builder(this.context)
        preferBuilder.setTitle("Prefer")
        preferBuilder.setMessage("Select the options below that you prefer")
        val preferListView =  ListView(this.context)
        preferListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        preferListView.adapter = createAdapter(preferList, dropDownDesign = android.R.layout.simple_list_item_multiple_choice)
        preferBuilder.setView(preferListView)
        preferBuilder.setPositiveButton("Done"){dialog, which ->
            preferInputString = ""
            preferNotList.clear()
            val checked: SparseBooleanArray = preferListView.getCheckedItemPositions()
            for (i in 0 until cantEatListView.getAdapter().getCount()) {
                if (checked[i]) {
                    initProteinWeight[i] = 1F
                    if (preferInputString.length == 0)
                        preferInputString += preferListZH[i]
                    else preferInputString += ("," + preferListZH[i])

                }
                else{
                    preferNotList.add(preferList[i])
                    preferNotListZH.add(preferListZH[i])
                }
            }
            preferMultiAutoCompleteTextView.setText(preferInputString)
            preferSetFlag = true
        }
        preferMultiAutoCompleteTextView.isEnabled = false
        val preferDialog = preferBuilder.create()
        preferTextView.setOnClickListener{preferDialog.show()}

        //Prefer Not List 
        var preferNotInputString: String = ""
        val preferNotBuilder = AlertDialog.Builder(this.context)
        preferNotBuilder.setTitle("Prefer Not")
        preferNotBuilder.setMessage("Select the options below that you not prefer")
        val preferNotListView =  ListView(this.context)
        preferNotListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        preferNotListView.adapter = createAdapter(preferNotList, dropDownDesign = android.R.layout.simple_list_item_multiple_choice)
        preferNotBuilder.setView(preferNotListView)
        preferNotBuilder.setPositiveButton("Done"){dialog, which ->
            preferNotInputString = ""
            val checked: SparseBooleanArray = preferNotListView.getCheckedItemPositions()
            for (i in 0 until preferNotListView.getAdapter().getCount()) {
                if (checked[i]) {
                    //get index from prefer list as it is original, while prefer not list is modified
                    initProteinWeight[preferList.indexOf(preferNotList[i])] = 0F
                    if (preferNotInputString.length == 0)
                        preferNotInputString += preferNotListZH[i]
                    else preferNotInputString += ("," + preferNotListZH[i])
                }
            }
            preferNotMultiAutoCompleteTextView.setText(preferNotInputString)
        }
        preferNotMultiAutoCompleteTextView.isEnabled = false
        val dialog = preferNotBuilder.create()
        preferNotTextView.setOnClickListener{
            if (preferSetFlag)
                dialog.show()
            else Toast.makeText(context, "Please Set Preferred Items First!", Toast.LENGTH_LONG).show()
        }
        
        //Submission
        submitProfileSettingsButton.setOnClickListener{

            //If user profile already exists, then allow empty fields
            val odb = FirebaseFirestore.getInstance()
            val docRef = odb.collection("user").document(FirebaseAuth.getInstance().currentUser!!.email!!)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()){
                    val gender = genderAutoCompleteTextView.getTextThenHint()
                    val age = AgeEditText.getTextThenHint()
                    val height = heightEditText.getTextThenHint().toInt()
                    val weight = weightEditText.getTextThenHint().toInt()
                    val cantEat = cantEatMultiAutoCompleteTextView.getTextThenHint()
                    val activity = activityAutoCompleteTextView.getTextThenHint()

                    val details  = HashMap<String, kotlin.Any>()
                    details["gender"] = gender
                    details["age"] = age
                    details["height"] = height
                    details["weight"] = weight
                    details["cant_eat"] = cantEat
                    details["prefer"] = if (preferInputString != "") preferInputString else previousPreferString
                    details["prefer_not"] = if (preferNotInputString != "") preferNotInputString else previousPreferNotString
                    details["activity"] = activity
                    details["staple_weight"] = previousStapleWeight
                    //If new protein weight is different and not default, then upload new protein weight
                    details["protein_weight"] = if (initProteinWeight.joinToString(separator = ",") != previousProteinWeight
                                                    && initProteinWeight.joinToString (separator = ",") != "0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5")
                                                        initProteinWeight.joinToString (separator = ",")
                                                else previousProteinWeight

                    //Debugging Lines Below
                    //details["staple_weight"] = "1,1,1,1,1,1,1"
                    //details["protein_weight"] = initProteinVec.joinToString (separator = ",")

                    val cantEatInputList = details["cant_eat"].toString().split(",")
                    val preferInputList = details["prefer"].toString().split(",")
                    val preferNotInputList = details["prefer_not"].toString().split(",")
                    if (checkIfConflict(cantEatInputList, preferInputList, preferNotInputList) == false){
                        odb.collection("user").document(FirebaseAuth.getInstance().currentUser!!.email!!).set(details)
                        Toast.makeText(this.context, getString(R.string.profile_settings_submitted_message), Toast.LENGTH_SHORT).show()
                        fragmentManager!!.popBackStack()
                    }
                    else
                        Snackbar.make(view, getString(R.string.profile_settings_conflict_warning_message), Snackbar.LENGTH_SHORT).show()
                }
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
                    if (cantEatMultiAutoCompleteTextView.checkIfEmpty() == false)
                        notEmptyCheckBoxes[4] = true
                    if (activityAutoCompleteTextView.checkIfEmpty() == false)
                        notEmptyCheckBoxes[5] = true
                    if (preferInputString != "")
                        notEmptyCheckBoxes[6] = true
                    if (preferNotInputString != "")
                        notEmptyCheckBoxes[7] = true
                    var passed = true
                   for (status in notEmptyCheckBoxes)
                   {
                       if (status != true)
                           passed = false
                   }
                    if (passed){
                                val gender = genderAutoCompleteTextView.getTextThenHint()
                                val age = AgeEditText.getTextThenHint()
                                val height = heightEditText.getTextThenHint().toInt()
                                val weight = weightEditText.getTextThenHint().toInt()
                                val cantEat = cantEatMultiAutoCompleteTextView.getTextThenHint()
                                val activity = activityAutoCompleteTextView.getTextThenHint()

                                val details  = HashMap<String, kotlin.Any>()
                                details["gender"] = gender
                                details["age"] = age
                                details["height"] = height
                                details["weight"] = weight
                                details["cant_eat"] = cantEat
                                details["prefer"] = preferInputString
                                details["prefer_not"] =  preferNotInputString
                                details["activity"] = activity
                                details["staple_weight"] = "1,1,1,1,1,1,1"
                                details["protein_weight"] = initProteinWeight.joinToString (separator = ",")

                                val cantEatInputList = details["cant_eat"].toString().split(",")
                                val preferInputList = details["prefer"].toString().split(",")
                                val preferNotInputList = details["prefer_not"].toString().split(",")

                                if (checkIfConflict(cantEatInputList, preferInputList, preferNotInputList) == false){

                                    //Set above details
                                    odb.collection("user").document(FirebaseAuth.getInstance().currentUser!!.email!!).set(details)

                                    //Set user entry in user-item matrix (get foodCount from parcel from MainActivity)
                                    val foodCount = arguments!!.getParcelable<MainActivity.countParcel>("foodCount")!!.foodCount
                                    val userEntry = hashMapOf("CF_score" to IntArray(foodCount){_-> 0}.joinToString(separator = ","))
                                    odb.collection("user_item_matrix").document(FirebaseAuth.getInstance().currentUser!!.email!!).set(userEntry)

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
