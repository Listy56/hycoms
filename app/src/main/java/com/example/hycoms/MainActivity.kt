@file:Suppress("DEPRECATION")

package com.example.hycoms

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat


class MainActivity : AppCompatActivity() {

    private lateinit var headerMini: View
    private var isHeaderVisible = false

    private lateinit var homeBt: LinearLayout
    private lateinit var chartBt: LinearLayout
    private lateinit var settingBt: LinearLayout

    private lateinit var imgHome: ImageView
    private lateinit var tvHome: TextView

    private lateinit var imgChart: ImageView
    private lateinit var tvChart: TextView

    private lateinit var imgSetting: ImageView
    private lateinit var tvSetting: TextView

    private lateinit var badgeNotif: TextView
    private lateinit var btnNotif: ImageView

    private lateinit var poppinsBold: Typeface
    private lateinit var poppinsRegular: Typeface
    private var indexAcc: Int = -1

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        headerMini = findViewById(R.id.headerMini)

        headerMini.translationY = -200f
        headerMini.visibility = View.GONE

        poppinsBold = ResourcesCompat.getFont(this, R.font.poppinsbold)!!
        poppinsRegular = ResourcesCompat.getFont(this, R.font.poppinsregular)!!
        NotificationHelper.createChannel(this)

        // ================= IZIN NOTIF =================
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        // ================= FCM TOKEN =================
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->

                Log.d("FCM_TOKEN", token)


                val userRef =
                    firebaseDatabase
                        .getReference("user")
                        .child("user_$indexAcc")

                userRef.child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener {

                        Log.d(
                            "FCM_SAVE",
                            "FCM berhasil disimpan"
                        )
                    }
                    .addOnFailureListener {

                        Log.e(
                            "FCM_SAVE",
                            it.message.toString()
                        )
                    }
            }

        val root = findViewById<View>(R.id.main)

        // fade in
        root.alpha = 0f
        root.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        window.statusBarColor = getColor(R.color.hijau_start)
        window.navigationBarColor = getColor(R.color.white)

        // ================= INIT VIEW =================
        homeBt = findViewById(R.id.homeBt)
        chartBt = findViewById(R.id.chartBt)
        settingBt = findViewById(R.id.settingBt)

        imgHome = findViewById(R.id.imgHome)
        tvHome = findViewById(R.id.tvHome)

        imgChart = findViewById(R.id.imgChart)
        tvChart = findViewById(R.id.tvChart)

        imgSetting = findViewById(R.id.imgSetting)
        tvSetting = findViewById(R.id.tvSetting)

        badgeNotif = findViewById(R.id.badge_notif)
        btnNotif = findViewById(R.id.btn_notif)

        // ================= SESSION =================
        val accPref = getSharedPreferences("ACCOUNT", MODE_PRIVATE)

        val isLogin = accPref.getBoolean("isLogin", false)
        indexAcc = accPref.getInt("index", -1)

        if (!isLogin || indexAcc == -1) {

            Toast.makeText(
                this,
                "Session tidak ditemukan, silakan login ulang",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val accFirebase = firebaseDatabase.getReference("user")

        Log.d("MainActivity", "indexAcc: $indexAcc")

        // ================= AMBIL DATA USER =================
        accFirebase.child("user_$indexAcc")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        val id = snapshot.child("id")
                            .value?.toString() ?: ""

                        // simpan session
                        getSharedPreferences("ACCOUNT", MODE_PRIVATE)
                            .edit()
                            .putString("deviceID", id)
                            .putInt("index", indexAcc)
                            .putBoolean("isLogin", true)
                            .apply()

                        // load notif
                        loadNotifications(id)

                    } else {

                        getSharedPreferences("ACCOUNT", MODE_PRIVATE)
                            .edit()
                            .putString("deviceID", "")
                            .putInt("index", -1)
                            .putBoolean("isLogin", false)
                            .apply()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // ================= DEFAULT FRAGMENT =================
        if (savedInstanceState == null) {

            currentFragment = HomeFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.mainFragment, currentFragment!!)
                .commit()
        }

        // ================= BOTTOM NAV =================
        homeBt.setOnClickListener {
            replaceFragment(HomeFragment(), 0)
        }

        chartBt.setOnClickListener {
            replaceFragment(ChartFragment(), 1)
        }

        settingBt.setOnClickListener {
            replaceFragment(SettingFragment(), 2)
        }
    }

    // ================= LOAD NOTIF (FIX NO SPAM) =================
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

                    // ✅ hanya badge (tidak tampilkan notif lagi)
                    if (unreadCount > 0) {
                        badgeNotif.visibility = View.VISIBLE
                        badgeNotif.text = unreadCount.toString()
                    } else {
                        badgeNotif.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // ================= CLICK NOTIF =================
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
                        Intent(this@MainActivity, NotifActivity::class.java)
                    )
                }
        }
    }

    fun showMiniHeader() {

        if (isHeaderVisible) return

        isHeaderVisible = true

        headerMini.visibility = View.VISIBLE

        headerMini.animate()
            .translationY(0f)
            .setDuration(120)
            .start()
    }

    fun hideMiniHeader() {

        if (!isHeaderVisible) return

        isHeaderVisible = false

        headerMini.animate()
            .translationY(-headerMini.height.toFloat())
            .setDuration(120)
            .withEndAction {
                headerMini.visibility = View.GONE
            }
            .start()
    }
    // CHANGE FRAGMENT
    private fun replaceFragment(fragment: Fragment, mode: Int) {

        // RESET BACKGROUND
        homeBt.setBackgroundColor(Color.TRANSPARENT)
        chartBt.setBackgroundColor(Color.TRANSPARENT)
        settingBt.setBackgroundColor(Color.TRANSPARENT)

        // RESET TEXT COLOR
        tvHome.setTextColor(getColor(R.color.abu))
        tvChart.setTextColor(getColor(R.color.abu))
        tvSetting.setTextColor(getColor(R.color.abu))

        // RESET FONT
        tvHome.typeface = poppinsRegular
        tvChart.typeface = poppinsRegular
        tvSetting.typeface = poppinsRegular

        // RESET ICON
        imgHome.setColorFilter(getColor(R.color.abu))
        imgChart.setColorFilter(getColor(R.color.abu))
        imgSetting.setColorFilter(getColor(R.color.abu))

        when (mode) {

            0 -> {

                homeBt.setBackgroundResource(
                    R.drawable.bg_nav_selected
                )

                tvHome.setTextColor(Color.WHITE)
                tvHome.typeface = poppinsBold

                imgHome.setColorFilter(Color.WHITE)
            }

            1 -> {

                chartBt.setBackgroundResource(
                    R.drawable.bg_nav_selected
                )

                tvChart.setTextColor(Color.WHITE)
                tvChart.typeface = poppinsBold

                imgChart.setColorFilter(Color.WHITE)
            }

            2 -> {

                settingBt.setBackgroundResource(
                    R.drawable.bg_nav_selected
                )

                tvSetting.setTextColor(Color.WHITE)
                tvSetting.typeface = poppinsBold

                imgSetting.setColorFilter(Color.WHITE)
            }
        }
        hideMiniHeader()

        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .commit()
    }
}