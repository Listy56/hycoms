package com.example.hycoms

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim().lowercase()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            when {
                username.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                }

                !isValidUsername(username) -> {
                    Toast.makeText(
                        this,
                        "Username minimal 3 karakter dan hanya boleh berisi huruf, angka, underscore, atau titik",
                        Toast.LENGTH_LONG
                    ).show()
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                }

                password.length < 6 -> {
                    Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                }

                password != confirm -> {
                    Toast.makeText(this, "Password tidak sama", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    btnRegister.isEnabled = false

                    database.child("user").get()
                        .addOnSuccessListener { snapshot ->
                            val usernameExists = snapshot.children.any { snap ->
                                snap.child("userName").value?.toString() == username
                            }

                            if (usernameExists) {
                                btnRegister.isEnabled = true
                                Toast.makeText(this, "Username sudah dipakai", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            auth.fetchSignInMethodsForEmail(email)
                                .addOnSuccessListener { result ->
                                    if (result.signInMethods?.isNotEmpty() == true) {
                                        btnRegister.isEnabled = true
                                        Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }

                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener { authResult ->
                                            var index = 1
                                            var key: String

                                            do {
                                                key = "user_$index"
                                                index++
                                            } while (snapshot.hasChild(key))

                                            val currentTime = System.currentTimeMillis()
                                            val uid = authResult.user?.uid
                                                ?: FirebaseAuth.getInstance().currentUser?.uid
                                                ?: ""

                                            val userMap = hashMapOf<String, Any>(
                                                "userName" to username,
                                                "email" to email,
                                                "id" to uid,
                                                "profileCompleted" to true,
                                                "createdAt" to currentTime,
                                                "updatedAt" to currentTime
                                            )

                                            database.child("user")
                                                .child(key)
                                                .setValue(userMap)
                                                .addOnSuccessListener {
                                                    val indexFix = key.substringAfter("_").toIntOrNull()

                                                    getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                                        .putInt("index", indexFix ?: -1)
                                                        .putBoolean("isLogin", true)
                                                        .apply()

                                                    Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()
                                                    startActivity(Intent(this, MainActivity::class.java))
                                                    finish()
                                                }
                                                .addOnFailureListener {
                                                    btnRegister.isEnabled = true
                                                    Toast.makeText(this, "Gagal simpan user", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener {
                                            btnRegister.isEnabled = true
                                            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener {
                                    btnRegister.isEnabled = true
                                    Toast.makeText(this, "Gagal cek email", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(this, "Gagal ambil data user", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        btnGoogle.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Token Google null", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                android.util.Log.e("GoogleSignIn_Register", "API Exception StatusCode: ${e.statusCode}", e)
                android.util.Log.e("GoogleSignIn_Register", "Message: ${e.message}")
                android.util.Log.e("GoogleSignIn_Register", "LocalizedMessage: ${e.localizedMessage}")

                val errorMsg = when (e.statusCode) {
                    10 -> "DEVELOPER_ERROR - Certificate hash/package name mismatch. Check Firebase Console."
                    12501 -> "Sign-in cancelled by user"
                    12502 -> "Sign-in failed - network error"
                    12500 -> "Internal error"
                    else -> "Google Sign-In Error ${e.statusCode}: ${e.message}"
                }

                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("GoogleSignIn_Register", "Unexpected Exception: ${e.message}", e)
                Toast.makeText(this, "Google gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener {
                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email
                val fullName = user?.displayName ?: "user"
                val uid = user?.uid ?: ""

                if (email.isNullOrEmpty()) {
                    Toast.makeText(this, "Email tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val database = FirebaseDatabase.getInstance().reference

                database.child("user").get()
                    .addOnSuccessListener { snapshot ->
                        var foundIndex: Int? = null
                        var profileCompleted = true
                        var userName = ""

                        for (snap in snapshot.children) {
                            if (snap.child("email").value.toString() == email) {
                                foundIndex = snap.key?.substringAfter("_")?.toIntOrNull()
                                val completed = snap.child("profileCompleted").value
                                profileCompleted = completed != null && completed.toString().toBoolean()
                                userName = snap.child("userName").value?.toString() ?: ""
                                break
                            }
                        }

                        if (foundIndex != null && profileCompleted && userName.isNotEmpty()) {
                            getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                .putInt("index", foundIndex)
                                .putBoolean("isLogin", true)
                                .apply()

                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else if (foundIndex != null) {
                            getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                .putInt("index", foundIndex)
                                .apply()

                            val intent = Intent(this, UsernameActivity::class.java)
                            intent.putExtra("email", email)
                            intent.putExtra("fullName", fullName)
                            startActivity(intent)
                            finish()
                        } else {
                            var index = 1
                            var key: String

                            do {
                                key = "user_$index"
                                index++
                            } while (snapshot.hasChild(key))

                            val currentTime = System.currentTimeMillis()
                            val userMap = hashMapOf<String, Any>(
                                "fullName" to fullName,
                                "userName" to "",
                                "email" to email,
                                "id" to uid,
                                "profileCompleted" to false,
                                "createdAt" to currentTime,
                                "updatedAt" to currentTime
                            )

                            database.child("user")
                                .child(key)
                                .setValue(userMap)
                                .addOnSuccessListener {
                                    val indexFix = key.substringAfter("_").toIntOrNull()

                                    getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
                                        .putInt("index", indexFix ?: -1)
                                        .apply()

                                    val intent = Intent(this, UsernameActivity::class.java)
                                    intent.putExtra("email", email)
                                    intent.putExtra("fullName", fullName)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Gagal simpan user", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("GoogleAuth_Register", "Firebase signInWithCredential failed: ${exception.message}", exception)
                Toast.makeText(this, "Auth Google gagal: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isValidUsername(username: String): Boolean {
        return username.length >= 3 && username.matches(Regex("^[a-z0-9._]+$"))
    }
}
