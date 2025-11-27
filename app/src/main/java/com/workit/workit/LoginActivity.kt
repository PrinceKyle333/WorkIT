package com.workit.workit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.workit.workit.auth.StudentAuthActivity
import com.workit.workit.auth.EmployerAuthActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        findViewById<Button>(R.id.btn_student_login).setOnClickListener {
            startActivity(Intent(this, StudentAuthActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btn_employer_login).setOnClickListener {
            startActivity(Intent(this, EmployerAuthActivity::class.java))
            finish()
        }
    }
}