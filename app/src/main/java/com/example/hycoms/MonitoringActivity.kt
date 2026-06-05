package com.example.hycoms

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MonitoringActivity : AppCompatActivity() {

    private lateinit var etPhMin : EditText
    private lateinit var etPhMax : EditText
    private lateinit var etPpmMin : EditText
    private lateinit var etPpmMax : EditText

    private lateinit var switchNotif : LinearLayout
    private lateinit var circleNotif : View
    private lateinit var save : CardView

    private lateinit var spinnerInterval : Spinner
    private lateinit var spinnerSuhu : Spinner

    private var firebaseDatabase = FirebaseDatabase.getInstance()
    private var indexAcc: Int?    = 0
    private var isOn: Boolean     = false
    private var deviceID: String? = ""

    private var online: Boolean = false

    var phMin: String? = ""
    var phMax: String? = ""
    var ppmMin: String? = ""
    var ppmMax: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.setting_monitoring)

        etPhMin = findViewById(R.id.phMin)
        etPhMax = findViewById(R.id.phMax)
        etPpmMin = findViewById(R.id.ppmMin)
        etPpmMax = findViewById(R.id.ppmMax)

        switchNotif = findViewById(R.id.switchNotif)
        circleNotif = findViewById(R.id.circleNotif)

        spinnerInterval = findViewById(R.id.spinnerInterval)
        spinnerSuhu = findViewById(R.id.spinnerSuhu)

        save = findViewById(R.id.save)

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        val intervalList = listOf("10", "30", "60", "120")
        val suhuList     = listOf("C", "F")

        spinnerInterval.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                intervalList
            )

        spinnerSuhu.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                suhuList
            )

        val accPref =
            getSharedPreferences(
                "ACCOUNT",
                MODE_PRIVATE
            )
        deviceID         = accPref.getString("deviceID", "")

        Log.d("MonitoringFragment", "DeviceID: $deviceID")

        var baseFirebase = firebaseDatabase.getReference("hycoms")

        if (deviceID != null && deviceID.toString().isNotEmpty()) {
            baseFirebase.child(deviceID.toString()).child("setting").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        spinnerInterval.visibility = View.VISIBLE
                        spinnerSuhu.visibility     = View.VISIBLE

                        phMin = snapshot.child("phMin").value.toString()
                        phMax = snapshot.child("phMax").value.toString()
                        ppmMin  = snapshot.child("ppmMin").value.toString()
                        ppmMax  = snapshot.child("ppmMax").value.toString()
                        val notifValue = snapshot.child("notifAlert").value

                        val notif = when (notifValue) {
                            is Boolean -> notifValue
                            is String -> notifValue.toBoolean()
                            else -> false
                        }
                        val tUnit  = snapshot.child("tempUnit").value.toString()
                        val nUnit  = snapshot.child("ppmUnit").value.toString()
                        val interval = snapshot.child("intervalUpdate").value.toString()

                        val intervalIndex = when(interval) {
                            "10"  -> 0
                            "30"  -> 1
                            "60"  -> 2
                            "120" -> 3
                            else  -> 0
                        }

                        val suhuIndex = when(tUnit) {
                            "C" -> 0
                            "F" -> 1
                            else -> 0
                        }

                        etPhMin.hint = phMin
                        etPhMax.hint = phMax
                        etPpmMin.hint = ppmMin
                        etPpmMax.hint = ppmMax

                        isOn = notif
                        updateSwitchUI(isOn)

                        spinnerInterval.setSelection(intervalIndex)
                        spinnerSuhu.setSelection(suhuIndex)

                        online = true

                    } else {
                        spinnerInterval.visibility = View.GONE
                        spinnerSuhu.visibility     = View.GONE

                        online = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MonitoringActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        }

        switchNotif.setOnClickListener {
            isOn = !isOn
            updateSwitchUI(isOn)
        }
        save.setOnClickListener {
            if(online) {
                val newPhMax = etPhMax.text.toString().ifEmpty { phMax }
                val newPhMin = etPhMin.text.toString().ifEmpty { phMin }
                val newPpmMax = etPpmMax.text.toString().ifEmpty { ppmMax }
                val newPpmMin = etPpmMin.text.toString().ifEmpty { ppmMin }

                if (deviceID != null && deviceID.toString().isNotEmpty()) {
                    baseFirebase.child(deviceID!!)
                        .child("setting")
                        .updateChildren(
                            mapOf(
                                "phMax" to newPhMax,
                                "phMin" to newPhMin,
                                "ppmMax" to newPpmMax,
                                "ppmMin" to newPpmMin,
                                "notifAlert" to isOn,
                                "tempUnit" to spinnerSuhu.selectedItem.toString(),
                                "intervalUpdate" to spinnerInterval.selectedItem.toString()
                            )
                        )
                }

                Toast.makeText(
                    this@MonitoringActivity,
                    "Data Updated",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
            }
        }
    }

    fun updateSwitchUI(isOn: Boolean) {
        switchNotif.post {
            if (isOn) {
                switchNotif.setBackgroundResource(R.drawable.bg_switch_on)
                circleNotif.animate().translationX(
                    (switchNotif.width - circleNotif.width - 12).toFloat()
                ).setDuration(200).start()
            } else {
                switchNotif.setBackgroundResource(R.drawable.bg_switch_off)
                circleNotif.animate().translationX(0f).setDuration(200).start()
            }
        }
    }
}