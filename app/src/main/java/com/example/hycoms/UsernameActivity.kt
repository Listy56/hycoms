package com.example.hycoms

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.auth.api.signin.*

class UsernameActivity : AppCompatActivity() {

    private lateinit var googleClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.username)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        val email = intent.getStringExtra("email") 
            ?: getSharedPreferences("ACCOUNT", MODE_PRIVATE).getString("currentEmail", "") ?: ""

        val database = FirebaseDatabase.getInstance().reference

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)

        // Completion screen for new Google users, legacy incomplete users,
        // and Splash/Login fallbacks when profileCompleted is false.
        btnSave.setOnClickListener {

            val username = etUsername.text.toString().trim().lowercase()

            if (username.isEmpty()) {
                Toast.makeText(this, "Username wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidUsername(username)) {
                Toast.makeText(
                    this,
                    "Username minimal 3 karakter dan hanya boleh berisi huruf, angka, underscore, atau titik",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            database.child("user").get()
                .addOnSuccessListener { snapshot ->

                    var usernameExists = false

                    for (snap in snapshot.children) {
                        val dbUsername = snap.child("userName").value.toString()
                        val dbEmail = snap.child("email").value.toString()

                        if (dbUsername == username && dbEmail != email) {
                            usernameExists = true
                            break
                        }
                    }

                    if (usernameExists) {
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Username sudah dipakai", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    var userKey: String? = null

                    for (snap in snapshot.children) {
                        if (snap.child("email").value.toString() == email) {
                            userKey = snap.key
                            break
                        }
                    }

                    if (userKey.isNullOrEmpty()) {
                        btnSave.isEnabled = true
                        Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val currentTime = System.currentTimeMillis()

                    val updateMap = HashMap<String, Any>()
                    updateMap["userName"] = username
                    updateMap["profileCompleted"] = true
                    updateMap["updatedAt"] = currentTime

                    database.child("user")
                        .child(userKey)
                        .updateChildren(updateMap)
                        .addOnSuccessListener {

                            getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                .putBoolean("isLogin", true)
                                .apply()

                            Toast.makeText(this, "Username berhasil disimpan", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            btnSave.isEnabled = true
                            Toast.makeText(this, "Gagal simpan username", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Gagal ambil data user", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            logoutAndBack()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                logoutAndBack()
            }
        })
    }

    private fun logoutAndBack() {

        FirebaseAuth.getInstance().signOut()
        googleClient.signOut()

        getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
            .clear()
            .apply()

        Toast.makeText(this, "Login dibatalkan", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun isValidUsername(username: String): Boolean {
        return username.length >= 3 && username.matches(Regex("^[a-z0-9._]+$"))
    }
}
