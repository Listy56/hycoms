package com.example.hycoms

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ControlActivity : AppCompatActivity() {

    private lateinit var mode: TextView
    private lateinit var tvStatusMode: TextView
    private lateinit var btnBack: ImageView
    private lateinit var layoutWarning: LinearLayout

    // MODE
    private lateinit var switchMode: LinearLayout
    private lateinit var circleMode: View

    // POMPA AIR
    private lateinit var pumpLayout: CardView
    private lateinit var switchPump: LinearLayout
    private lateinit var circlePump: View
    private lateinit var statusSwitch: TextView

    // PH UP
    private lateinit var phUpLayout: CardView
    private lateinit var switchPhUp: LinearLayout
    private lateinit var circlePhUp: View
    private lateinit var statusPhUp: TextView

    // PH DOWN
    private lateinit var phDownLayout: CardView
    private lateinit var switchPhDown: LinearLayout
    private lateinit var circlePhDown: View
    private lateinit var statusPhDown: TextView

    // NUTRISI UP
    private lateinit var nutrisiUpLayout: CardView
    private lateinit var switchNutrisiUp: LinearLayout
    private lateinit var circleNutrisiUp: View
    private lateinit var statusNutrisiUp: TextView

    // NUTRISI DOWN
    private lateinit var nutrisiDownLayout: CardView
    private lateinit var switchNutrisiDown: LinearLayout
    private lateinit var circleNutrisiDown: View
    private lateinit var statusNutrisiDown: TextView

    private var deviceID: String? = ""
    private var firebaseDatabase = FirebaseDatabase.getInstance()

    // STATUS
    private var modeStatus = false
    private var pumpOn = false
    private var phUpOn = false
    private var phDownOn = false
    private var nutrisiUpOn = false
    private var nutrisiDownOn = false

    private var online = false

    private fun saveLastState() {

        val pref = getSharedPreferences("CONTROL_STATE", MODE_PRIVATE)

        pref.edit()
            .putBoolean("mode", modeStatus)
            .putBoolean("pump", pumpOn)
            .putBoolean("phUp", phUpOn)
            .putBoolean("phDown", phDownOn)
            .putBoolean("nutrisiA", nutrisiUpOn)
            .putBoolean("nutrisiB", nutrisiDownOn)
            .apply()
    }
    private fun loadLastState() {

        val pref = getSharedPreferences("CONTROL_STATE", MODE_PRIVATE)

        modeStatus = pref.getBoolean("mode", false)
        pumpOn = pref.getBoolean("pump", false)
        phUpOn = pref.getBoolean("phUp", false)
        phDownOn = pref.getBoolean("phDown", false)
        nutrisiUpOn = pref.getBoolean("nutrisiA", false)
        nutrisiDownOn = pref.getBoolean("nutrisiB", false)
    }
    private fun updateControlUI() {

        if (modeStatus) {

            mode.text = "Auto"
            modeSwitchUI(true)

            tvStatusMode.visibility = View.VISIBLE

            pumpLayout.visibility = View.GONE
            phUpLayout.visibility = View.GONE
            phDownLayout.visibility = View.GONE
            nutrisiUpLayout.visibility = View.GONE
            nutrisiDownLayout.visibility = View.GONE

        } else {

            mode.text = "Manual"
            modeSwitchUI(false)

            tvStatusMode.visibility = View.GONE

            pumpLayout.visibility = View.VISIBLE
            phUpLayout.visibility = View.VISIBLE
            phDownLayout.visibility = View.VISIBLE
            nutrisiUpLayout.visibility = View.VISIBLE
            nutrisiDownLayout.visibility = View.VISIBLE

            pumpSwitchUI(pumpOn)
            phUpSwitchUI(phUpOn)
            phDownSwitchUI(phDownOn)
            nutrisiUpSwitchUI(nutrisiUpOn)
            nutrisiDownSwitchUI(nutrisiDownOn)
        }
    }
    private fun setControlEnabled(enable: Boolean) {

        switchMode.isEnabled = enable
        switchPump.isEnabled = enable
        switchPhUp.isEnabled = enable
        switchPhDown.isEnabled = enable
        switchNutrisiUp.isEnabled = enable
        switchNutrisiDown.isEnabled = enable

        switchMode.alpha = if (enable) 1f else 0.5f
        switchPump.alpha = if (enable) 1f else 0.5f
        switchPhUp.alpha = if (enable) 1f else 0.5f
        switchPhDown.alpha = if (enable) 1f else 0.5f
        switchNutrisiUp.alpha = if (enable) 1f else 0.5f
        switchNutrisiDown.alpha = if (enable) 1f else 0.5f

        pumpLayout.alpha = if (enable) 1f else 0.5f
        phUpLayout.alpha = if (enable) 1f else 0.5f
        phDownLayout.alpha = if (enable) 1f else 0.5f
        nutrisiUpLayout.alpha = if (enable) 1f else 0.5f
        nutrisiDownLayout.alpha = if (enable) 1f else 0.5f

        mode.alpha = if (enable) 1f else 0.5f
        tvStatusMode.alpha = if (enable) 1f else 0.5f

        layoutWarning.visibility =
            if (enable) View.GONE else View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_control)
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        // MODE
        mode = findViewById(R.id.mode)
        switchMode = findViewById(R.id.switchMode)
        circleMode = findViewById(R.id.circleMode)
        tvStatusMode = findViewById(R.id.tvStatusMode)

        // POMPA
        pumpLayout = findViewById(R.id.pumpLayout)
        switchPump = findViewById(R.id.switchPump)
        circlePump = findViewById(R.id.circlePump)
        statusSwitch = findViewById(R.id.statusSwitch)

        // PH UP
        phUpLayout = findViewById(R.id.phUpLayout)
        switchPhUp = findViewById(R.id.switchPhUp)
        circlePhUp = findViewById(R.id.circlePhUp)
        statusPhUp = findViewById(R.id.statusPhUp)

        // PH DOWN
        phDownLayout = findViewById(R.id.phDownLayout)
        switchPhDown = findViewById(R.id.switchPhDown)
        circlePhDown = findViewById(R.id.circlePhDown)
        statusPhDown = findViewById(R.id.statusPhDown)

        // NUTRISI UP
        nutrisiUpLayout = findViewById(R.id.nutrisiUpLayout)
        switchNutrisiUp = findViewById(R.id.switchNutrisiUp)
        circleNutrisiUp = findViewById(R.id.circleNutrisiUp)
        statusNutrisiUp = findViewById(R.id.statusNutrisiUp)

        // NUTRISI DOWN
        nutrisiDownLayout = findViewById(R.id.nutrisiDownLayout)
        switchNutrisiDown = findViewById(R.id.switchNutrisiDown)
        circleNutrisiDown = findViewById(R.id.circleNutrisiDown)
        statusNutrisiDown = findViewById(R.id.statusNutrisiDown)
        layoutWarning = findViewById(R.id.layoutWarning)

        val accPref =
            getSharedPreferences(
                "ACCOUNT",
                MODE_PRIVATE
            )

        deviceID = accPref.getString("deviceID", "")

        Log.d("ControlFragment", "DeviceID: $deviceID")

        val baseFirebase = firebaseDatabase.getReference("hycoms")

        // ================= GET DATA FIREBASE =================
        if (!deviceID.isNullOrEmpty()) {

            baseFirebase.child(deviceID!!)
                .child("control")
                .addValueEventListener(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {

                            online = true

                            modeStatus = snapshot.child("mode").value.toString().toBoolean()
                            pumpOn = snapshot.child("waterPump").value.toString().toBoolean()
                            phUpOn = snapshot.child("phUp").value.toString().toBoolean()
                            phDownOn = snapshot.child("phDown").value.toString().toBoolean()
                            nutrisiUpOn = snapshot.child("nutrisiA").value.toString().toBoolean()
                            nutrisiDownOn = snapshot.child("nutrisiB").value.toString().toBoolean()

                            saveLastState()
                            updateControlUI()
                            setControlEnabled(true)
                        } else {

                            online = false

                            loadLastState()
                            updateControlUI()
                            setControlEnabled(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                        online = false

                        loadLastState()
                        updateControlUI()
                        setControlEnabled(false)

                        Toast.makeText(
                            this@ControlActivity,
                            "Error: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } else {

            online = false

            loadLastState()
            updateControlUI()
            setControlEnabled(false)
        }

        // ================= MODE =================
        switchMode.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    this,
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            modeStatus = !modeStatus
            updateControlUI()

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("mode")
                .setValue(modeStatus)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    modeStatus = !modeStatus
                    updateControlUI()

                    Toast.makeText(
                        this,
                        "Gagal mengubah mode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        // ================= POMPA =================
        switchPump.setOnClickListener {

            if (!online) {

                Toast.makeText(
                    this,
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            vibratePhone()

            pumpOn = !pumpOn
            pumpSwitchUI(pumpOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("waterPump")
                .setValue(pumpOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    pumpOn = !pumpOn
                    pumpSwitchUI(pumpOn)

                    Toast.makeText(
                        this,
                        "Gagal mengubah status pompa",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        // ================= PH UP =================
        switchPhUp.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    this,
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            phUpOn = !phUpOn
            phUpSwitchUI(phUpOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("phUp")
                .setValue(phUpOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    phUpOn = !phUpOn
                    phUpSwitchUI(phUpOn)

                    Toast.makeText(
                        this,
                        "Gagal mengubah pH Up",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // ================= PH DOWN =================
        switchPhDown.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    this,
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            phDownOn = !phDownOn
            phDownSwitchUI(phDownOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("phDown")
                .setValue(phDownOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    phDownOn = !phDownOn
                    phDownSwitchUI(phDownOn)

                    Toast.makeText(
                        this,
                        "Gagal mengubah pH Down",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // ================= NUTRISI UP =================
        switchNutrisiUp.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    this,
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            nutrisiUpOn = !nutrisiUpOn
            nutrisiUpSwitchUI(nutrisiUpOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("nutrisiA")
                .setValue(nutrisiUpOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    nutrisiUpOn = !nutrisiUpOn
                    nutrisiUpSwitchUI(nutrisiUpOn)

                    Toast.makeText(
                        this,
                        "Gagal mengubah Nutrisi A",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // ================= NUTRISI DOWN =================
        switchNutrisiDown.setOnClickListener {

            if (!online) {
                Toast.makeText(
                    this,
                    "Hubungkan Device ID terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            vibratePhone()

            nutrisiDownOn = !nutrisiDownOn
            nutrisiDownSwitchUI(nutrisiDownOn)

            baseFirebase.child(deviceID!!)
                .child("control")
                .child("nutrisiB")
                .setValue(nutrisiDownOn)
                .addOnSuccessListener {
                    saveLastState()
                }
                .addOnFailureListener {

                    nutrisiDownOn = !nutrisiDownOn
                    nutrisiDownSwitchUI(nutrisiDownOn)

                    Toast.makeText(
                        this,
                        "Gagal mengubah Nutrisi B",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // ================= MODE UI =================
    private fun modeSwitchUI(isOn: Boolean) {

        switchMode.post {

            if (isOn) {

                switchMode.setBackgroundResource(R.drawable.bg_switch_on)

                circleMode.translationX =
                    (switchMode.width - circleMode.width - 12).toFloat()

            } else {

                switchMode.setBackgroundResource(R.drawable.bg_switch_off)

                circleMode.translationX = 0f
            }
        }
    }

    // ================= POMPA UI =================
    private fun pumpSwitchUI(isOn: Boolean) {

        switchPump.post {

            if (isOn) {

                switchPump.setBackgroundResource(R.drawable.bg_switch_on)

                circlePump.translationX =
                    (switchPump.width - circlePump.width - 12).toFloat()

                statusSwitch.text = "ON"

            } else {

                switchPump.setBackgroundResource(R.drawable.bg_switch_off)

                circlePump.translationX = 0f

                statusSwitch.text = "OFF"
            }
        }
    }
    // ================= PH UP UI =================
    private fun phUpSwitchUI(isOn: Boolean) {

        switchPhUp.post {

            if (isOn) {

                switchPhUp.setBackgroundResource(R.drawable.bg_switch_on)

                circlePhUp.translationX =
                    (switchPhUp.width - circlePhUp.width - 12).toFloat()

                statusPhUp.text = "ON"

            } else {

                switchPhUp.setBackgroundResource(R.drawable.bg_switch_off)

                circlePhUp.translationX = 0f

                statusPhUp.text = "OFF"
            }
        }
    }

    // ================= PH DOWN UI =================
    private fun phDownSwitchUI(isOn: Boolean) {

        switchPhDown.post {

            if (isOn) {

                switchPhDown.setBackgroundResource(R.drawable.bg_switch_on)

                circlePhDown.translationX =
                    (switchPhDown.width - circlePhDown.width - 12).toFloat()

                statusPhDown.text = "ON"

            } else {

                switchPhDown.setBackgroundResource(R.drawable.bg_switch_off)

                circlePhDown.translationX = 0f

                statusPhDown.text = "OFF"
            }
        }
    }
    // ================= NUTRISI UP UI =================
    private fun nutrisiUpSwitchUI(isOn: Boolean) {

        switchNutrisiUp.post {

            if (isOn) {

                switchNutrisiUp.setBackgroundResource(R.drawable.bg_switch_on)

                circleNutrisiUp.translationX =
                    (switchNutrisiUp.width - circleNutrisiUp.width - 12).toFloat()

                statusNutrisiUp.text = "ON"

            } else {

                switchNutrisiUp.setBackgroundResource(R.drawable.bg_switch_off)

                circleNutrisiUp.translationX = 0f

                statusNutrisiUp.text = "OFF"
            }
        }
    }

    // ================= NUTRISI DOWN UI =================
    private fun nutrisiDownSwitchUI(isOn: Boolean) {

        switchNutrisiDown.post {

            if (isOn) {

                switchNutrisiDown.setBackgroundResource(R.drawable.bg_switch_on)

                circleNutrisiDown.translationX =
                    (switchNutrisiDown.width - circleNutrisiDown.width - 12).toFloat()

                statusNutrisiDown.text = "ON"

            } else {

                switchNutrisiDown.setBackgroundResource(R.drawable.bg_switch_off)

                circleNutrisiDown.translationX = 0f

                statusNutrisiDown.text = "OFF"
            }
        }
    }
    // ================= GETAR =================
    private fun vibratePhone() {

        val vibrator =
            getSystemService(
                Context.VIBRATOR_SERVICE
            ) as Vibrator

        // Android baru
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    40, // lama getar
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } else {

            // Android lama
            vibrator.vibrate(80)
        }
    }
}