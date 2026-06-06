package com.example.hycoms

import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Intent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AccountActivity : AppCompatActivity() {

    private lateinit var userNameTv: TextView
    private lateinit var emailTv: TextView
    private lateinit var exportCsvLayout: LinearLayout
    private lateinit var disconnectBt: androidx.cardview.widget.CardView
    private lateinit var btnBack: ImageView

    private var deviceID: String = ""
    private var indexAcc: Int = -1

    private val firebaseDatabase =
        FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_account)


        // =========================================
        // INIT VIEW
        // =========================================

        userNameTv =
            findViewById(R.id.userName)

        emailTv =
            findViewById(R.id.email)

        exportCsvLayout =
            findViewById(R.id.exportCsv)

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        disconnectBt =
            findViewById(R.id.disconnectBt)

        // =========================================
        // GET SHARED PREF
        // =========================================

        val accPref =
            getSharedPreferences(
                    "ACCOUNT",
                    MODE_PRIVATE
                )

        indexAcc =
            accPref.getInt("index", -1)

        deviceID =
            accPref.getString(
                "deviceID",
                ""
            ) ?: ""

        Log.d(
            "AccountFragment",
            "indexAcc: $indexAcc"
        )

        // =========================================
        // FIREBASE USER
        // =========================================

        val accFirebase =
            firebaseDatabase
                .getReference("user")

        // =========================================
        // GET USER DATA
        // =========================================

        accFirebase
            .child("user_$indexAcc")
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        if (snapshot.exists()) {

                            val email =
                                snapshot.child("email")
                                    .value.toString()

                            val userName =
                                snapshot.child("userName")
                                    .value.toString()

                            emailTv.text = email
                            userNameTv.text = userName
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Toast.makeText(
                            this@AccountActivity,
                            "Error : ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

        // =========================================
        // OPEN HISTORY FRAGMENT
        // =========================================

        exportCsvLayout.setOnClickListener {

            if (deviceID.isEmpty()) {

                Toast.makeText(
                    this@AccountActivity,
                    "Device ID tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val intent =
                Intent(
                    this@AccountActivity,
                    HistoryActivity::class.java
                )

            startActivity(intent)
        }

        disconnectBt.setOnClickListener {

            // logout firebase
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .signOut()

            // logout google
            val gso =
                com.google.android.gms.auth.api.signin.GoogleSignInOptions
                    .Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                    )
                    .requestEmail()
                    .build()

            val googleSignInClient =
                com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getClient(
                        this,
                        gso
                    )

            googleSignInClient
                .revokeAccess()
                .addOnCompleteListener {

                    // hapus session local
                    accPref.edit()
                        .clear()
                        .putBoolean("isLogin", false)
                        .apply()

                    Toast.makeText(
                        this@AccountActivity,
                        "Berhasil keluar akun",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent =
                        Intent(
                            this@AccountActivity,
                            LoginActivity::class.java
                        )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)

                    finish()
                }
        }
    }
}