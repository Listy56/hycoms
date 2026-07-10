package com.example.hycoms

import android.animation.ValueAnimator
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.widget.NestedScrollView
import android.widget.ImageView
import kotlin.jvm.java
import com.google.firebase.database.*


class HomeFragment : Fragment() {

    // ================= UI =================

    private lateinit var badgeNotif: TextView
    private lateinit var homeScroll: NestedScrollView
    private lateinit var headerContent: View

    private lateinit var phTextView: TextView
    private lateinit var nutrisiTextView: TextView
    private lateinit var statusSwitch: TextView
    private lateinit var kelembapanUdara: TextView
    private lateinit var airTemp: TextView
    private lateinit var waterTemp: TextView
    private lateinit var waterLevelPercent: TextView
    private lateinit var waterLevel: LinearLayout
    private lateinit var baseWaterLevel: LinearLayout
    private var lastWaterTempStatus = Status.NORMAL
    private var lastAirTempStatus = Status.NORMAL
    private lateinit var tvStatus: TextView
    private lateinit var btnNotif: ImageView
    private var humidityStatus = Status.NORMAL
    private var lastHumidity = 0

    private var deviceID: String? = ""
    private var firebaseDatabase = FirebaseDatabase.getInstance()

    // ================= DATA =================
    var suhuUdara = 0.0
    var suhuAir = 0.0
    var pH = 0.0
    var nutrisi = 0
    var humidity = 0
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

        btnNotif = view.findViewById(R.id.btn_notif)
        badgeNotif = view.findViewById(R.id.badge_notif)

        btnNotif.setOnClickListener {
            startActivity(
                Intent(requireContext(), NotifActivity::class.java)
            )
        }
        homeScroll = view.findViewById(R.id.homeScroll)
        headerContent = view.findViewById(R.id.headerContent)

        homeScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->

                val activity = requireActivity() as MainActivity

                if (scrollY > 0) {
                    activity.showMiniHeader()
                } else {
                    activity.hideMiniHeader()
                }

                val maxScroll = 80f
                val progress =
                    (scrollY.coerceAtMost(maxScroll.toInt())) / maxScroll


                val scale = 1f - (progress * 0.5f)

                headerContent.scaleX = scale
                headerContent.scaleY = scale


                headerContent.alpha = 1f - (progress * 1.5f)

                headerContent.translationY = -(scrollY * 2f)
            }
        )

        // init UI
        phTextView = view.findViewById(R.id.tvPh)
        nutrisiTextView = view.findViewById(R.id.tvNutrisi)
        statusSwitch = view.findViewById(R.id.statusSwitch)
        kelembapanUdara = view.findViewById(R.id.tvHumidity)
        airTemp = view.findViewById(R.id.airTemp)
        waterTemp = view.findViewById(R.id.waterTemp)
        waterLevel = view.findViewById(R.id.waterLevel)
        baseWaterLevel = view.findViewById(R.id.baseWaterLevel)
        waterLevelPercent = view.findViewById(R.id.waterLevelPercent)
        tvStatus = view.findViewById(R.id.tvStatus)

        val accPref = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        deviceID = accPref.getString("deviceID", "")
        if (!deviceID.isNullOrEmpty()) {
            loadNotifications(deviceID!!)
        }

        loadState()

        val baseFirebase = firebaseDatabase.getReference("hycoms")

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
                humidity = snapshot.child("dataStream/humidity").value.toString().toIntOrNull() ?: 0
                isOn = snapshot.child("control/waterPump").value.toString().toBoolean()

                // ===== NOTIF =====
                if (notifAlert) {
                    checkPH()
                    checkPPM()
                    checkAirTemp()
                    checkWaterTemp()
                    checkWaterLevel()
                    checkHumidity()
                }

                saveState()

                // ===== UI =====
                phTextView.text = pH.toString()
                nutrisiTextView.text = nutrisi.toString()
                kelembapanUdara.text = "$humidity%"
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
                val waterPercent = level.coerceIn(0.0, 100.0)
                val text = "${waterPercent.toInt()}%"
                waterLevelPercent.text = text

                baseWaterLevel.post {

                    val maxHeight =
                        baseWaterLevel.height -
                                baseWaterLevel.paddingTop -
                                baseWaterLevel.paddingBottom

                    val newHeight =
                        ((waterPercent / 100.0) * maxHeight).toInt()

                    waterLevelPercent.text = "${waterPercent.toInt()}%"

                    when {
                        waterPercent <= 20 -> {
                            setStatusText(
                                "Critical",
                                resources.getColor(android.R.color.holo_red_dark)
                            )
                        }

                        waterPercent <= 50 -> {
                            setStatusText(
                                "Low",
                                resources.getColor(android.R.color.holo_orange_dark)
                            )
                        }

                        waterPercent <= 80 -> {
                            setStatusText(
                                "Safe",
                                resources.getColor(android.R.color.holo_green_dark)
                            )
                        }

                        else -> {
                            setStatusText(
                                "Full",
                                resources.getColor(android.R.color.holo_green_dark)
                            )
                        }
                    }

                    animateWaterLevel(newHeight)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun setStatusText(status: String, color: Int) {
        val fullText = "Status: $status"

        val spannable = SpannableString(fullText)

        spannable.setSpan(
            ForegroundColorSpan(color),
            8, // posisi setelah "Status: "
            fullText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvStatus.text = spannable
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

            .putString("humidityStatus", humidityStatus.name)
            .putInt("humidityValue", humidity)

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

        humidityStatus =
            Status.valueOf(pref.getString("humidityStatus", "NORMAL")!!)

        lastHumidity =
            pref.getInt("humidityValue", 0)
    }
    private fun checkHumidity() {

        checkParameter(
            value = humidity.toDouble(),
            min = 60.0,
            max = 80.0,
            lastStatus = humidityStatus,
            lastValue = lastHumidity.toDouble(),
            title = "Kelembapan Udara",
            onUpdate = {
                humidityStatus = it.status
                lastHumidity = it.value.toInt()
            }
        )
    }

    // ================= ANIMASI =================

    private fun animateWaterLevel(targetHeight: Int) {

        waterAnimator?.cancel()

        val startHeight = waterLevel.layoutParams.height

        waterAnimator = ValueAnimator.ofInt(startHeight, targetHeight).apply {

            duration = 500
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->

                val params = waterLevel.layoutParams
                params.height = animator.animatedValue as Int
                waterLevel.layoutParams = params
            }

            start()
        }
    }
    private fun loadNotifications(deviceID: String) {

        firebaseDatabase.getReference("hycoms")
            .child(deviceID)
            .child("notifications")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    var unreadCount = 0

                    for (data in snapshot.children) {

                        val isRead = data.child("isRead")
                            .getValue(Boolean::class.java) ?: false

                        if (!isRead) unreadCount++
                    }

                    if (unreadCount > 0) {
                        badgeNotif.visibility = View.VISIBLE
                        badgeNotif.text = unreadCount.toString()
                    } else {
                        badgeNotif.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        btnNotif.setOnClickListener {

            firebaseDatabase.getReference("hycoms")
                .child(deviceID)
                .child("notifications")
                .get()
                .addOnSuccessListener { snapshot ->

                    for (data in snapshot.children) {
                        data.ref.child("isRead").setValue(true)
                    }

                    startActivity(
                        Intent(requireContext(), NotifActivity::class.java)
                    )
                }
        }
    }
}