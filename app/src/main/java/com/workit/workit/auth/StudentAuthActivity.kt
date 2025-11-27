package com.workit.workit.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.workit.workit.R
import com.workit.workit.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class StudentAuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var isSignupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_auth)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etName = findViewById<EditText>(R.id.et_name)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvToggle = findViewById<TextView>(R.id.tv_toggle_mode)

        tvToggle.setOnClickListener {
            isSignupMode = !isSignupMode
            if (isSignupMode) {
                etName.visibility = android.view.View.VISIBLE
                btnLogin.text = "Sign Up"
                tvToggle.text = "Already have account? Login"
            } else {
                etName.visibility = android.view.View.GONE
                btnLogin.text = "Login"
                tvToggle.text = "Don't have account? Sign Up"
            }
        }

        btnLogin.setOnClickListener {
            if (isSignupMode) {
                signupStudent(etEmail, etPassword, etName)
            } else {
                loginStudent(etEmail, etPassword)
            }
        }
    }

    private fun signupStudent(etEmail: EditText, etPassword: EditText, etName: EditText) {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val name = etName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    val studentData = mapOf(
                        "id" to userId,
                        "email" to email,
                        "name" to name,
                        "role" to "student",
                        "phone" to "",
                        "address" to "",
                        "idNumber" to "",
                        "username" to name.replace(" ", "").lowercase(),
                        "profilePictureUrl" to "",
                        "createdAt" to System.currentTimeMillis(),
                        "appliedJobs" to emptyList<String>()
                    )

                    db.collection("students").document(userId).set(studentData)
                        .addOnSuccessListener {
                            Toast.makeText(this@StudentAuthActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@StudentAuthActivity, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@StudentAuthActivity, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@StudentAuthActivity, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loginStudent(etEmail: EditText, etPassword: EditText) {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                lifecycleScope.launch {
                    val role = RoleManager.getUserRole()
                    if (role == RoleManager.UserRole.STUDENT) {
                        Toast.makeText(this@StudentAuthActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@StudentAuthActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@StudentAuthActivity, "This account is registered as an employer, not a student", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@StudentAuthActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}