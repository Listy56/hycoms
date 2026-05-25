package com.example.hics

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etInput = findViewById<EditText>(R.id.etInput)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val tvBackLogin = findViewById<TextView>(R.id.tvBackLogin)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference

        // =========================
        // 🔙 BACK TO LOGIN
        // =========================
        tvBackLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // =========================
        // 🔥 SEND RESET LINK
        // =========================
        btnNext.setOnClickListener {

            val email = etInput.text.toString().trim()

            // VALIDASI kosong
            if (email.isEmpty()) {
                Toast.makeText(this, "Masukkan email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // VALIDASI format email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // =========================
            // 🔍 CEK EMAIL DI DATABASE
            // =========================
            database.child("User").get()
                .addOnSuccessListener { snapshot ->

                    var emailFound = false

                    for (userSnap in snapshot.children) {
                        val dbEmail = userSnap.child("email").value.toString()

                        if (email == dbEmail) {
                            emailFound = true
                            break
                        }
                    }

                    if (!emailFound) {
                        Toast.makeText(this, "Email tidak terdaftar", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // =========================
                    // 📩 KIRIM RESET EMAIL
                    // =========================
                    auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Link reset dikirim ke email",
                                Toast.LENGTH_LONG
                            ).show()

                            finish() // kembali ke login
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                        }

                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal ambil data", Toast.LENGTH_SHORT).show()
                }
        }
    }
}