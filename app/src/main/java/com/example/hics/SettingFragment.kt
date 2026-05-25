package com.example.hics

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
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
            // Handle monitoring button click
            val fragment = MonitoringFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.mainFragment, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        control.setOnClickListener {
            // Handle control button click
            val fragment = ControlFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.mainFragment, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        device.setOnClickListener {
            // Handle device button click
            val fragment = DeviceFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.mainFragment, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        account.setOnClickListener {
            // Handle account button click
            val fragment = AccountFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.mainFragment, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}