package com.example.hics

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoSplash)
        val title = findViewById<TextView>(R.id.titleSplash)
        val subtitle = findViewById<TextView>(R.id.subtitleSplash)
        val bg = findViewById<ImageView>(R.id.bgSplash)

        val smooth = AccelerateDecelerateInterpolator()

        logo.post {

            // initial state
            logo.alpha = 0f
            title.alpha = 0f
            subtitle.alpha = 0f

            bg.scaleX = 1.1f
            bg.scaleY = 1.1f

            // 🔥 ANIMASI MASUK (cinematic zoom + fade)
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

            title.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(200)
                .setInterpolator(smooth)
                .start()

            subtitle.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(400)
                .setInterpolator(smooth)
                .start()

            // 🔥 LANGSUNG PINDAH TANPA JEDA NGACO
            logo.animate()
                .setStartDelay(1800) // tunggu animasi selesai
                .setDuration(300)
                .withEndAction {

                    val user = FirebaseAuth.getInstance().currentUser

                    if (user != null) {
                        // ✅ sudah login
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // ❌ belum login
                        startActivity(Intent(this, LoginActivity::class.java))
                    }

                    overridePendingTransition(0, 0)
                    finish()
                }
                .start()
        }
    }
}