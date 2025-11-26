package com.workit.workit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.btn_student_login).setOnClickListener {
            loginAsStudent()
        }

        findViewById<Button>(R.id.btn_employer_login).setOnClickListener {
            loginAsEmployer()
        }
    }

    private fun loginAsStudent() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("userType", "student")
        })
        finish()
    }

    private fun loginAsEmployer() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("userType", "employer")
        })
        finish()
    }
}