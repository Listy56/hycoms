package com.example.hics

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.FirebaseDatabase

object NotificationHelper {

    private const val CHANNEL_ID = "HICS_CHANNEL"

    // ================= CHANNEL =================
    fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "HICS Notification",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.description =
                "Notification for HICS"

            val manager =
                context.getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    // ================= STATUS BAR =================
    @SuppressLint("MissingPermission")
    fun showNotification(
        context: Context,
        title: String,
        message: String
    ) {

        // buka aplikasi ketika notif diklik
        val intent =
            Intent(context, MainActivity::class.java).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val builder =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)

                // supaya muncul popup/status bar seperti WA
                .setPriority(NotificationCompat.PRIORITY_HIGH)

                // notif hilang setelah diklik
                .setAutoCancel(true)

                // ketika notif diklik buka app
                .setContentIntent(pendingIntent)

                // tampil lebih jelas
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )

        NotificationManagerCompat.from(context)
            .notify(
                System.currentTimeMillis().toInt(),
                builder.build()
            )
    }

    // ================= SAVE FIREBASE =================
    fun saveNotification(
        deviceID: String,
        title: String,
        message: String
    ) {

        val notifRoot =
            FirebaseDatabase.getInstance()
                .getReference("Hics")
                .child(deviceID)
                .child("notifications")

        notifRoot.get().addOnSuccessListener { snapshot ->

            val nextIndex =
                snapshot.childrenCount.toInt()

            val notifRef =
                notifRoot.child(nextIndex.toString())

            val notif = hashMapOf(
                "title" to title,
                "message" to message,
                "time" to System.currentTimeMillis(),
                "isRead" to false
            )

            notifRef.setValue(notif)

        }
    }
}