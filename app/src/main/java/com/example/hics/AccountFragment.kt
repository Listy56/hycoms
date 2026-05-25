package com.example.hics

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Intent

class AccountFragment : Fragment() {

    private lateinit var userNameTv: TextView
    private lateinit var emailTv: TextView
    private lateinit var exportCsvLayout: LinearLayout
    private lateinit var disconnectBt: androidx.cardview.widget.CardView

    private var deviceID: String = ""
    private var indexAcc: Int = -1

    private val firebaseDatabase =
        FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.setting_account,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        // =========================================
        // INIT VIEW
        // =========================================

        userNameTv =
            view.findViewById(R.id.userName)

        emailTv =
            view.findViewById(R.id.email)

        exportCsvLayout =
            view.findViewById(R.id.exportCsv)

        disconnectBt =
            view.findViewById(R.id.disconnectBt)

        // =========================================
        // GET SHARED PREF
        // =========================================

        val accPref =
            requireActivity()
                .getSharedPreferences(
                    "ACCOUNT",
                    MODE_PRIVATE
                )

        indexAcc =
            accPref.getInt("index", -1)

        deviceID =
            accPref.getString(
                "deviceID",
                ""
            ) ?: ""

        Log.d(
            "AccountFragment",
            "indexAcc: $indexAcc"
        )

        // =========================================
        // FIREBASE USER
        // =========================================

        val accFirebase =
            firebaseDatabase
                .getReference("User")

        // =========================================
        // GET USER DATA
        // =========================================

        accFirebase
            .child("user_$indexAcc")
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        if (snapshot.exists()) {

                            val email =
                                snapshot.child("email")
                                    .value.toString()

                            val userName =
                                snapshot.child("userName")
                                    .value.toString()

                            emailTv.text = email
                            userNameTv.text = userName
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Toast.makeText(
                            requireContext(),
                            "Error : ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )

        // =========================================
        // OPEN HISTORY FRAGMENT
        // =========================================

        exportCsvLayout.setOnClickListener {

            if (deviceID.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Device ID tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.mainFragment,
                    HistoryFragment()
                )
                .addToBackStack(null)
                .commit()
        }
        disconnectBt.setOnClickListener {

            // logout firebase
            com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .signOut()

            // logout google
            val gso =
                com.google.android.gms.auth.api.signin.GoogleSignInOptions
                    .Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                    )
                    .requestEmail()
                    .build()

            val googleSignInClient =
                com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getClient(
                        requireContext(),
                        gso
                    )

            googleSignInClient
                .revokeAccess()
                .addOnCompleteListener {

                    // hapus session local
                    accPref.edit()
                        .clear()
                        .putBoolean("isLogin", false)
                        .apply()

                    Toast.makeText(
                        requireContext(),
                        "Berhasil keluar akun",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent =
                        Intent(
                            requireContext(),
                            LoginActivity::class.java
                        )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)

                    requireActivity().finish()
                }
        }
    }
}