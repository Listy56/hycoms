package com.example.hycoms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // ================= TOKEN BARU =================
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("FCM_TOKEN", token)

        // ================= SIMPAN TOKEN KE FIREBASE =================
        val pref =
            getSharedPreferences(
                "ACCOUNT",
                MODE_PRIVATE
            )

        val index =
            pref.getInt("index", -1)

        if (index != -1) {

            FirebaseDatabase
                .getInstance()
                .getReference("user")
                .child("user_$index")
                .child("fcmToken")
                .setValue(token)
        }
    }

    // ================= TERIMA NOTIF =================
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title =
            message.notification?.title ?: "HYCOMS"

        val body =
            message.notification?.body ?: ""

        showNotification(title, body)
    }

    // ================= TAMPILKAN NOTIF =================
    private fun showNotification(
        title: String,
        message: String
    ) {

        val channelId = "HYCOMS_CHANNEL"

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val builder =
            NotificationCompat.Builder(
                this,
                channelId
            )
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        // ================= CHANNEL =================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    channelId,
                    "HYCOMS Notification",
                    NotificationManager.IMPORTANCE_HIGH
                )

            manager.createNotificationChannel(channel)
        }

        manager.notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }
}