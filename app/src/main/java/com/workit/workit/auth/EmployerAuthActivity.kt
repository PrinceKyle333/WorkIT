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

class EmployerAuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var isSignupMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_auth)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etCompanyName = findViewById<EditText>(R.id.et_company_name)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvToggle = findViewById<TextView>(R.id.tv_toggle_mode)

        tvToggle.setOnClickListener {
            isSignupMode = !isSignupMode
            if (isSignupMode) {
                etCompanyName.visibility = android.view.View.VISIBLE
                btnLogin.text = "Sign Up"
                tvToggle.text = "Already have account? Login"
            } else {
                etCompanyName.visibility = android.view.View.GONE
                btnLogin.text = "Login"
                tvToggle.text = "Don't have account? Sign Up"
            }
        }

        btnLogin.setOnClickListener {
            if (isSignupMode) {
                signupEmployer(etEmail, etPassword, etCompanyName)
            } else {
                loginEmployer(etEmail, etPassword)
            }
        }
    }

    private fun signupEmployer(etEmail: EditText, etPassword: EditText, etCompanyName: EditText) {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val companyName = etCompanyName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || companyName.isEmpty()) {
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
                    val employerData = mapOf(
                        "id" to userId,
                        "email" to email,
                        "role" to "employer",
                        "companyName" to companyName,
                        "phone" to "",
                        "address" to "",
                        "logoUrl" to "",
                        "description" to "",
                        "createdAt" to System.currentTimeMillis(),
                        "postedJobs" to emptyList<String>()
                    )

                    db.collection("employers").document(userId).set(employerData)
                        .addOnSuccessListener {
                            Toast.makeText(this@EmployerAuthActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@EmployerAuthActivity, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@EmployerAuthActivity, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@EmployerAuthActivity, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loginEmployer(etEmail: EditText, etPassword: EditText) {
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
                    if (role == RoleManager.UserRole.EMPLOYER) {
                        Toast.makeText(this@EmployerAuthActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@EmployerAuthActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@EmployerAuthActivity, "This account is registered as a student, not an employer", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@EmployerAuthActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}