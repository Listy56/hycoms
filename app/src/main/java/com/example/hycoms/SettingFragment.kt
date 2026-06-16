package com.example.hycoms

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.database.*

class SettingFragment: Fragment() {

    private lateinit var badgeNotif: TextView
    private lateinit var btnNotif: ImageView

    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private lateinit var monitoring: LinearLayout
    private lateinit var control: LinearLayout
    private lateinit var device: LinearLayout
    private lateinit var account: LinearLayout

    private lateinit var settingsScroll: NestedScrollView
    private lateinit var headerContent: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        badgeNotif = view.findViewById(R.id.badge_notif)
        btnNotif = view.findViewById(R.id.btn_notif)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        settingsScroll = view.findViewById(R.id.settingsScroll)
        headerContent = view.findViewById(R.id.headerContent)

        settingsScroll.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->

                val activity = requireActivity() as MainActivity

                if (scrollY > 0) {
                    activity.showMiniHeader()
                } else {
                    activity.hideMiniHeader()
                }

                val maxScroll = 80f
                val progress =
                    (scrollY.coerceAtMost(maxScroll.toInt())) / maxScroll


                val scale = 1f - (progress * 0.5f)

                headerContent.scaleX = scale
                headerContent.scaleY = scale
                headerContent.scaleY = scale


                headerContent.alpha = 1f - (progress * 1.5f)

                headerContent.translationY = -(scrollY * 2f)
            }
        )


        monitoring = view.findViewById(R.id.monitoring)
        control    = view.findViewById(R.id.control)
        device     = view.findViewById(R.id.device)
        account    = view.findViewById(R.id.account)

        val accPref      = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        val indexAcc         = accPref.getInt("index", -1)
        val deviceID         = accPref.getString("deviceID", "")
        if (!deviceID.isNullOrEmpty()) {
            loadNotifications(deviceID)
        }


        monitoring.setOnClickListener {
            startActivity(
                Intent(requireContext(), MonitoringActivity::class.java)
            )
        }

        control.setOnClickListener {
            startActivity(
                Intent(requireContext(), ControlActivity::class.java)
            )
        }
        device.setOnClickListener {
            startActivity(
                Intent(requireContext(), DeviceActivity::class.java)
            )
        }

        account.setOnClickListener {
            startActivity(
                Intent(requireContext(), AccountActivity::class.java)
            )
        }
    }
    private fun loadNotifications(deviceID: String) {

        firebaseDatabase.getReference("hycoms")
            .child(deviceID)
            .child("notifications")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    var unreadCount = 0

                    for (data in snapshot.children) {

                        val isRead = data.child("isRead")
                            .getValue(Boolean::class.java) ?: false

                        if (!isRead) unreadCount++
                    }

                    if (unreadCount > 0) {
                        badgeNotif.visibility = View.VISIBLE
                        badgeNotif.text = unreadCount.toString()
                    } else {
                        badgeNotif.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        btnNotif.setOnClickListener {

            firebaseDatabase.getReference("hycoms")
                .child(deviceID)
                .child("notifications")
                .get()
                .addOnSuccessListener { snapshot ->

                    for (data in snapshot.children) {
                        data.ref.child("isRead").setValue(true)
                    }

                    startActivity(
                        Intent(requireContext(), NotifActivity::class.java)
                    )
                }
        }
    }
}