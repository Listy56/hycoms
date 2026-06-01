package com.example.hycoms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoSplash)
        val subtitle = findViewById<TextView>(R.id.subtitleSplash)
        val bg = findViewById<ImageView>(R.id.bgSplash)

        val smooth = AccelerateDecelerateInterpolator()

        logo.post {

            logo.alpha = 0f
            subtitle.alpha = 0f

            bg.scaleX = 1.1f
            bg.scaleY = 1.1f

            bg.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .setInterpolator(smooth)
                .start()

            logo.animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(smooth)
                .start()

            subtitle.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(400)
                .setInterpolator(smooth)
                .start()

            logo.animate()
                .setStartDelay(1800) 
                .setDuration(300)
                .withEndAction {
                    validateAndNavigate()
                }
                .start()
        }
    }

    private fun validateAndNavigate() {

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
            return
        }

        val email = user.email ?: ""
        val database = FirebaseDatabase.getInstance().reference

        database.child("user").get()
            .addOnSuccessListener { snapshot ->

                var userFound = false
                var profileCompleted = true
                var userName = ""

                for (snap in snapshot.children) {
                    if (snap.child("email").value.toString() == email) {
                        userFound = true

                        val completed = snap.child("profileCompleted").value
                        profileCompleted = completed != null && completed.toString().toBoolean()

                        userName = snap.child("userName").value?.toString() ?: ""

                        break
                    }
                }

                if (!userFound) {
                    FirebaseAuth.getInstance().signOut()
                    getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit().clear().apply()
                    Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                } else if (!profileCompleted || userName.isEmpty()) {
                    val intent = Intent(this, UsernameActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal validasi akun", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            }
    }
}
