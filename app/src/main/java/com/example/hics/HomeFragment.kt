package com.example.hics

import android.animation.ValueAnimator
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    // ================= UI =================
    private lateinit var phTextView: TextView
    private lateinit var nutrisiTextView: TextView
    private lateinit var statusSwitch: TextView
    private lateinit var intensitas: TextView
    private lateinit var airTemp: TextView
    private lateinit var waterTemp: TextView
    private lateinit var waterLevelPercent: TextView
    private lateinit var waterLevel: LinearLayout
    private lateinit var baseWaterLevel: FrameLayout
    private var lastWaterTempStatus = Status.NORMAL
    private var lastAirTempStatus = Status.NORMAL

    private var deviceID: String? = ""
    private var firebaseDatabase = FirebaseDatabase.getInstance()

    // ================= DATA =================
    var suhuUdara = 0.0
    var suhuAir = 0.0
    var pH = 0.0
    var nutrisi = 0
    var intensitasCahaya = 0
    var level = 0.0
    var isOn = true

    // ================= SETTING =================
    private var phMinSetting = 5.5
    private var phMaxSetting = 7.5
    private var ppmMinSetting = 800
    private var ppmMaxSetting = 1500
    private var notifAlert = true
    private var tempUnit = "C"

    // ================= STATUS =================
    enum class Status { LOW, NORMAL, HIGH }

    private var phStatus = Status.NORMAL
    private var ppmStatus = Status.NORMAL
    private var airTempStatus = Status.NORMAL
    private var waterTempStatus = Status.NORMAL
    private var waterLevelStatus = Status.NORMAL

    private var lastPhValue = 0.0
    private var lastPpmValue = 0
    private var lastAirTemp = 0.0
    private var lastWaterTemp = 0.0
    private var lastLevel = 0.0

    // ================= ANIMASI =================
    var waterAnimator: ValueAnimator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init UI
        phTextView = view.findViewById(R.id.tvPh)
        nutrisiTextView = view.findViewById(R.id.tvNutrisi)
        statusSwitch = view.findViewById(R.id.statusSwitch)
        intensitas = view.findViewById(R.id.tvIntensitas)
        airTemp = view.findViewById(R.id.airTemp)
        waterTemp = view.findViewById(R.id.waterTemp)
        waterLevel = view.findViewById(R.id.waterLevel)
        baseWaterLevel = view.findViewById(R.id.baseWaterLevel)
        waterLevelPercent = view.findViewById(R.id.waterLevelPercent)

        val accPref = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        deviceID = accPref.getString("deviceID", "")

        loadState()

        val baseFirebase = firebaseDatabase.getReference("Hics")

        baseFirebase.child(deviceID!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // ===== REVISI =====
                if (!isAdded || view == null) return

                if (!snapshot.exists()) return

                // ===== SETTING =====
                phMinSetting = snapshot.child("setting/phMin").value.toString().toDoubleOrNull() ?: 5.5
                phMaxSetting = snapshot.child("setting/phMax").value.toString().toDoubleOrNull() ?: 7.5
                ppmMinSetting = snapshot.child("setting/ppmMin").value.toString().toIntOrNull() ?: 800
                ppmMaxSetting = snapshot.child("setting/ppmMax").value.toString().toIntOrNull() ?: 1500
                val notifValue = snapshot.child("setting/notifAlert").value

                notifAlert = when (notifValue) {
                    is Boolean -> notifValue
                    is String -> notifValue.toBoolean()
                    else -> false
                }

                tempUnit = snapshot.child("setting/tempUnit").value.toString()

                // ===== DATA =====
                suhuAir = snapshot.child("dataStream/waterTemp").value.toString().toDoubleOrNull() ?: 0.0
                suhuUdara = snapshot.child("dataStream/airTemp").value.toString().toDoubleOrNull() ?: 0.0
                pH = snapshot.child("dataStream/pH").value.toString().toDoubleOrNull() ?: 0.0
                nutrisi = snapshot.child("dataStream/ppm").value.toString().toIntOrNull() ?: 0
                level = snapshot.child("dataStream/waterLevel").value.toString().toDoubleOrNull() ?: 0.0
                intensitasCahaya = snapshot.child("dataStream/light").value.toString().toIntOrNull() ?: 0
                isOn = snapshot.child("control/waterPump").value.toString().toBoolean()

                // ===== NOTIF =====
                if (notifAlert) {
                    checkPH()
                    checkPPM()
                    checkAirTemp()
                    checkWaterTemp()
                    checkWaterLevel()
                }

                saveState()

                // ===== UI =====
                phTextView.text = pH.toString()
                nutrisiTextView.text = nutrisi.toString()
                intensitas.text = intensitasCahaya.toString()
                statusSwitch.text = if (isOn) "ON" else "OFF"

                var displayAirTemp = suhuUdara
                var displayWaterTemp = suhuAir
                var unit = "°C"

                if (tempUnit == "F") {
                    displayAirTemp = (suhuUdara * 9 / 5) + 32
                    displayWaterTemp = (suhuAir * 9 / 5) + 32
                    unit = "°F"
                }

                airTemp.text = String.format("%.1f%s", displayAirTemp, unit)
                waterTemp.text = String.format("%.1f%s", displayWaterTemp, unit)

                // ===== WATER LEVEL =====
                if (level < 15) level = 15.0
                if (level > 100) level = 100.0

                baseWaterLevel.post {
                    val maxHeight = baseWaterLevel.height
                    val newHeight = (level * maxHeight) / 100.0
                    waterLevelPercent.text = "$level%"
                    animateWaterLevel(newHeight.toInt())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ================= GENERIC CHECK =================
    private fun convertTemp(value: Double): Pair<Double, String> {
        return if (tempUnit == "F") {
            Pair((value * 9 / 5) + 32, "°F")
        } else {
            Pair(value, "°C")
        }
    }

    private fun checkPH() {
        checkParameter(
            value = pH,
            min = phMinSetting,
            max = phMaxSetting,
            lastStatus = phStatus,
            lastValue = lastPhValue,
            title = "pH",
            onUpdate = {
                phStatus = it.status
                lastPhValue = it.value
            }
        )
    }

    private fun checkPPM() {
        checkParameter(
            value = nutrisi.toDouble(),
            min = ppmMinSetting.toDouble(),
            max = ppmMaxSetting.toDouble(),
            lastStatus = ppmStatus,
            lastValue = lastPpmValue.toDouble(),
            title = "Nutrisi",
            onUpdate = {
                ppmStatus = it.status
                lastPpmValue = it.value.toInt()
            }
        )
    }

    private fun checkAirTemp() {
        val (value, unit) = convertTemp(suhuUdara)

        val min = if (tempUnit == "F") (20.0 * 9 / 5) + 32 else 20.0
        val max = if (tempUnit == "F") (35.0 * 9 / 5) + 32 else 35.0

        val currentStatus = when {
            value > max -> Status.HIGH
            value < min -> Status.LOW
            else -> Status.NORMAL
        }

        if (currentStatus == lastAirTempStatus) return

        when (currentStatus) {
            Status.HIGH -> sendNotif("Suhu Udara ($unit) Tinggi", "Nilai: $value")
            Status.LOW -> sendNotif("Suhu Udara ($unit) Rendah", "Nilai: $value")
            Status.NORMAL -> {
                if (lastAirTempStatus != Status.NORMAL) {
                    sendNotif("Suhu Udara Normal", "Nilai: $value")
                }
            }
        }

        lastAirTempStatus = currentStatus
    }

    private fun checkWaterTemp() {
        val (value, unit) = convertTemp(suhuAir)

        val min = if (tempUnit == "F") (18.0 * 9 / 5) + 32 else 18.0
        val max = if (tempUnit == "F") (30.0 * 9 / 5) + 32 else 30.0

        val currentStatus = when {
            value > max -> Status.HIGH
            value < min -> Status.LOW
            else -> Status.NORMAL
        }

        if (currentStatus == lastWaterTempStatus) return

        when (currentStatus) {
            Status.HIGH -> sendNotif("Suhu Air ($unit) Tinggi", "Nilai: $value")
            Status.LOW -> sendNotif("Suhu Air ($unit) Rendah", "Nilai: $value")
            Status.NORMAL -> {
                if (lastWaterTempStatus != Status.NORMAL) {
                    sendNotif("Suhu Air Normal", "Nilai: $value")
                }
            }
        }

        lastWaterTempStatus = currentStatus
    }

    private fun checkWaterLevel() {
        checkParameter(
            value = level,
            min = 20.0,
            max = 100.0,
            lastStatus = waterLevelStatus,
            lastValue = lastLevel,
            title = "Air",
            onUpdate = {
                waterLevelStatus = it.status
                lastLevel = it.value
            }
        )
    }

    private fun checkParameter(
        value: Double,
        min: Double,
        max: Double,
        lastStatus: Status,
        lastValue: Double,
        title: String,
        onUpdate: (ResultState) -> Unit
    ) {

        val currentStatus = when {
            value > max -> Status.HIGH
            value < min -> Status.LOW
            else -> Status.NORMAL
        }

        // ===== REVISI =====
        val changed = currentStatus != lastStatus

        if (!changed) return

        when (currentStatus) {
            Status.HIGH -> sendNotif("$title Tinggi", "$title terlalu tinggi : $value")
            Status.LOW -> sendNotif("$title Rendah", "$title terlalu rendah : $value")
            Status.NORMAL -> {
                if (lastStatus != Status.NORMAL) {
                    sendNotif("$title Normal", "$title sudah normal : $value")
                }
            }
        }

        onUpdate(ResultState(currentStatus, value))
    }

    // ===== REVISI =====
    private fun sendNotif(title: String, message: String) {

        if (!isAdded || context == null) return

        NotificationHelper.saveNotification(deviceID ?: "", title, message)

        context?.let {
            NotificationHelper.showNotification(it, title, message)
        }
    }

    data class ResultState(val status: Status, val value: Double)

    // ================= SAVE STATE =================

    private fun saveState() {
        val pref = requireContext().getSharedPreferences("STATE", MODE_PRIVATE)
        pref.edit()
            // pH
            .putString("phStatus", phStatus.name)
            .putFloat("phValue", pH.toFloat())

            // PPM
            .putString("ppmStatus", ppmStatus.name)
            .putInt("ppmValue", nutrisi)

            // Air Temp
            .putString("airTempStatus", airTempStatus.name)
            .putFloat("airTempValue", suhuUdara.toFloat())

            // Water Temp
            .putString("waterTempStatus", waterTempStatus.name)
            .putFloat("waterTempValue", suhuAir.toFloat())

            .putString("airTempStatusLast", lastAirTempStatus.name)
            .putString("waterTempStatusLast", lastWaterTempStatus.name)

            // Water Level
            .putString("waterLevelStatus", waterLevelStatus.name)
            .putFloat("waterLevelValue", level.toFloat())

            .apply()
    }

    private fun loadState() {
        val pref = requireContext().getSharedPreferences("STATE", MODE_PRIVATE)

        // pH
        phStatus = Status.valueOf(pref.getString("phStatus", "NORMAL")!!)
        lastPhValue = pref.getFloat("phValue", 0f).toDouble()

        // PPM
        ppmStatus = Status.valueOf(pref.getString("ppmStatus", "NORMAL")!!)
        lastPpmValue = pref.getInt("ppmValue", 0)

        // Air Temp
        airTempStatus = Status.valueOf(pref.getString("airTempStatus", "NORMAL")!!)
        lastAirTemp = pref.getFloat("airTempValue", 0f).toDouble()

        // Water Temp
        waterTempStatus = Status.valueOf(pref.getString("waterTempStatus", "NORMAL")!!)
        lastWaterTemp = pref.getFloat("waterTempValue", 0f).toDouble()

        lastAirTempStatus = Status.valueOf(pref.getString("airTempStatusLast", "NORMAL")!!)
        lastWaterTempStatus = Status.valueOf(pref.getString("waterTempStatusLast", "NORMAL")!!)

        // Water Level
        waterLevelStatus = Status.valueOf(pref.getString("waterLevelStatus", "NORMAL")!!)
        lastLevel = pref.getFloat("waterLevelValue", 0f).toDouble()
    }

    // ================= ANIMASI =================

    private fun animateWaterLevel(targetHeight: Int) {
        waterAnimator?.cancel()

        val startHeight = waterLevel.height

        waterAnimator = ValueAnimator.ofInt(startHeight, targetHeight).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val params = waterLevel.layoutParams
                params.height = it.animatedValue as Int
                waterLevel.layoutParams = params
            }
            start()
        }
    }
}