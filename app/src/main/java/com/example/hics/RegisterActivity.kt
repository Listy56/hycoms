package com.example.hics

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException

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
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().reference

        // 🔥 GOOGLE CONFIG
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnBack.setOnClickListener { finish() }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // =========================
        // 🔥 REGISTER MANUAL
        // =========================
        btnRegister.setOnClickListener {

            val username = etUsername.text.toString().trim().lowercase()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            when {
                username.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
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

                    // 🔥 CEK USERNAME TANPA INDEX
                    database.child("User").get()
                        .addOnSuccessListener { snapshot ->

                            for (snap in snapshot.children) {
                                val usernameDB = snap.child("userName").value.toString()

                                if (usernameDB == username) {
                                    btnRegister.isEnabled = true
                                    Toast.makeText(this, "Username sudah dipakai", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }
                            }

                            // 🔥 CEK EMAIL DI AUTH
                            auth.fetchSignInMethodsForEmail(email)
                                .addOnSuccessListener { result ->

                                    if (result.signInMethods?.isNotEmpty() == true) {
                                        btnRegister.isEnabled = true
                                        Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }

                                    // 🔥 BUAT USER AUTH
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener {

                                            database.child("User").get()
                                                .addOnSuccessListener { snapshot ->

                                                    var index = 1
                                                    var key: String

                                                    do {
                                                        key = "user_$index"
                                                        index++
                                                    } while (snapshot.hasChild(key))

                                                    val userMap = HashMap<String, Any>()
                                                    userMap["userName"] = username
                                                    userMap["email"] = email
                                                    userMap["id"] = ""

                                                    // 🔥 SIMPAN USER
                                                    database.child("User")
                                                        .child(key)
                                                        .setValue(userMap)
                                                        .addOnSuccessListener {

                                                            val indexFix = key.substringAfter("_").toIntOrNull()
                                                            saveSession(indexFix)

                                                            Toast.makeText(this, "Register berhasil", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .addOnFailureListener {
                                                            btnRegister.isEnabled = true
                                                            Toast.makeText(this, "Gagal simpan user", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                        }
                                        .addOnFailureListener {
                                            btnRegister.isEnabled = true
                                            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                                        }
                                }
                        }
                        .addOnFailureListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(this, "Gagal cek username", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        // =========================
        // 🔥 GOOGLE REGISTER
        // =========================
        btnGoogle.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }
    }

    // =========================
    // 🔥 RESULT GOOGLE
    // =========================
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

            } catch (e: Exception) {
                Toast.makeText(this, "Google gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =========================
    // 🔥 GOOGLE AUTH
    // =========================
    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener {

                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email
                val username = user?.displayName ?: "user"

                if (email.isNullOrEmpty()) {
                    Toast.makeText(this, "Email tidak ditemukan", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val database = FirebaseDatabase.getInstance().reference

                database.child("User").get()
                    .addOnSuccessListener { snapshot ->

                        var foundIndex: Int? = null

                        for (snap in snapshot.children) {
                            if (snap.child("email").value.toString() == email) {
                                val key = snap.key
                                foundIndex = key?.substringAfter("_")?.toIntOrNull()
                                break
                            }
                        }

                        if (foundIndex != null) {
                            saveSession(foundIndex)
                        } else {
                            val intent = Intent(this, UsernameActivity::class.java)
                            intent.putExtra("email", email)
                            intent.putExtra("defaultUsername", username)
                            startActivity(intent)
                        }
                    }
            }
    }

    // =========================
    // 🔥 SAVE SESSION
    // =========================
    private fun saveSession(index: Int?) {
        getSharedPreferences("ACCOUNT", MODE_PRIVATE).edit()
            .putInt("index", index ?: -1)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}