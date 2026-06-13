package com.example.hycoms

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*


class NotifActivity: AppCompatActivity() {
    private lateinit var back: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var title: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: NotificationAdapter
    private lateinit var notifList : MutableList<NotificationModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notif)

        back         = findViewById(R.id.back)
        title        = findViewById(R.id.tvTitle)
        emptyState   = findViewById(R.id.emptyState)
        recyclerView = findViewById(R.id.recyclerView)
        notifList    = mutableListOf()
        adapter      = NotificationAdapter(notifList)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter       = adapter

        val accPref =
            getSharedPreferences("ACCOUNT", MODE_PRIVATE)

        val deviceID =
            accPref.getString("deviceID", "") ?: ""

        FirebaseDatabase.getInstance()
            .getReference("hycoms")
            .child(deviceID)
            .child("notifications")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    notifList.clear()

                    for (data in snapshot.children.reversed()) {

                        val notif =
                            data.getValue(NotificationModel::class.java)

                        if (notif != null) {
                            notifList.add(notif)
                        }
                    }

                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        back.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }

    private fun updateEmptyState() {
        val count = notifList.size
        title.text = if (count > 0) "Notifikasi ($count)" else "Notifikasi"
        emptyState.visibility = if (count == 0) View.VISIBLE else View.GONE
        recyclerView.visibility = if (count == 0) View.GONE else View.VISIBLE
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
