package com.example.hycoms

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.fragment.app.Fragment

class SettingFragment: Fragment() {

    private lateinit var monitoring: LinearLayout
    private lateinit var control: LinearLayout
    private lateinit var device: LinearLayout
    private lateinit var account: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().finish()
        }

        monitoring = view.findViewById(R.id.monitoring)
        control    = view.findViewById(R.id.control)
        device     = view.findViewById(R.id.device)
        account    = view.findViewById(R.id.account)

        val accPref      = requireActivity().getSharedPreferences("ACCOUNT", MODE_PRIVATE)
        val indexAcc         = accPref.getInt("index", -1)
        val deviceID         = accPref.getString("deviceID", "")


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
}