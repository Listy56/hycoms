package com.example.hycoms

import android.os.Bundle
import android.util.Log
import android.content.Context.MODE_PRIVATE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DeviceActivity : AppCompatActivity() {

    private lateinit var connectButton: CardView
    private lateinit var idInput: EditText
    private lateinit var btnBack: ImageView


    private var firebaseDatabase = FirebaseDatabase.getInstance()
    private var id: String        = ""
    private var deviceID: String? = ""
    private var indexAcc: Int?    = 0

    private lateinit var deviceIDtv : TextView
    private lateinit var ssidtv     : TextView
    private lateinit var tvUpdate   : TextView
    private lateinit var connectBt      : CardView
    private lateinit var disconnectBt   : CardView
    private lateinit var tvInputDevice  : TextView
    private lateinit var layoutInputDevice   : CardView
    private lateinit var layoutInfo          : CardView

    private lateinit var progressBar: ProgressBar



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_device)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }


        connectButton       = findViewById(R.id.connectBt)
        idInput             = findViewById(R.id.idInput)
        deviceIDtv          = findViewById(R.id.deviceID)
        connectBt           = findViewById(R.id.connectBt)
        tvInputDevice       = findViewById(R.id.tvInputDevice)
        layoutInputDevice   = findViewById(R.id.layoutInputDevice)
        layoutInfo          = findViewById(R.id.layoutInfo)
        disconnectBt        = findViewById(R.id.disconnectBt)
        ssidtv              = findViewById(R.id.ssid)
        progressBar         = findViewById(R.id.progres)
        tvUpdate            = findViewById(R.id.lastUpdate)

        showWhenStart()

        val accPref = getSharedPreferences("ACCOUNT", MODE_PRIVATE)

        indexAcc         = accPref.getInt("index", -1)
        deviceID         = accPref.getString("deviceID", "")

        Log.d("DeviceFragment", "indexAcc: $indexAcc")

        var baseFirebase = firebaseDatabase.getReference("hycoms")
        var accFirebase = firebaseDatabase.getReference("user")

        progressBar.visibility = View.VISIBLE

        accFirebase.child("user_$indexAcc").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && snapshot.child("id").exists()) {
                    val id   = snapshot.child("id").value.toString()

                    showWhenIdExist()
                    deviceIDtv.text = id

                    baseFirebase.child(id).child("device").addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val ssid = snapshot.child("ssid").value.toString()
                                val last = snapshot.child("lastUpdate").value.toString()
                                ssidtv.text = ssid
                                tvUpdate.text   = last
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@DeviceActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })

                } else {
                    showWhenIdNotExist()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DeviceActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        disconnectBt.setOnClickListener {
            accFirebase.child("user_$indexAcc").child("id").removeValue()
            getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                .remove("deviceID")
                .apply()

            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
        }

        connectButton.setOnClickListener {
            id = idInput.text.toString()

            if(id.isEmpty()) {
                Toast.makeText(this, "ID Kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            baseFirebase.child(id).get()
                .addOnSuccessListener { snapshot ->
                    if(snapshot.exists()) {

                        accFirebase.child("user_$indexAcc").child("id").setValue(id)

                        getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                            .putString("deviceID", id)
                            .commit()

                        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()

                    } else {
                        Toast.makeText(this, "ID Not Available", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    fun showWhenIdExist() {
        progressBar.visibility       = View.GONE
        tvInputDevice.visibility     = View.GONE
        layoutInputDevice.visibility = View.GONE
        connectBt.visibility         = View.GONE

        layoutInfo.visibility   = View.VISIBLE
        disconnectBt.visibility = View.VISIBLE
    }

    fun showWhenIdNotExist() {
        progressBar.visibility       = View.GONE
        tvInputDevice.visibility     = View.VISIBLE
        layoutInputDevice.visibility = View.VISIBLE
        connectBt.visibility         = View.VISIBLE

        layoutInfo.visibility   = View.GONE
        disconnectBt.visibility = View.GONE
    }

    fun showWhenStart() {
        progressBar.visibility       = View.GONE
        tvInputDevice.visibility     = View.GONE
        layoutInputDevice.visibility = View.GONE
        connectBt.visibility         = View.GONE

        layoutInfo.visibility   = View.GONE
        disconnectBt.visibility = View.GONE
    }
}