package com.example.hycoms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class NotificationAdapter(private val notifList: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView     = view.findViewById(R.id.judulNotif)
        val message: TextView   = view.findViewById(R.id.detailNotif)
        val time: TextView      = view.findViewById(R.id.timeNotif)
        val unreadAccent: View  = view.findViewById(R.id.unreadAccent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notifikasi, parent, false) // Pakai custom XML
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif           = notifList[position]
        holder.title.text   = notif.title
        holder.message.text = notif.message
        holder.time.text    = formatTime(notif.time)
        holder.unreadAccent.visibility = if (notif.isRead) View.INVISIBLE else View.VISIBLE

        holder.itemView.alpha = 0f
        holder.itemView.translationY = 18f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(240)
            .start()

        // Event jika notifikasi diklik
        holder.itemView.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Notif: ${notif.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = notifList.size

    private fun formatTime(time: Long): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            .format(Date(time))
    }
}
