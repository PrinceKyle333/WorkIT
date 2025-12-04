package com.workit.workit.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.workit.workit.R
import com.workit.workit.MainActivity
import com.workit.workit.data.TimeSlot
import com.workit.workit.ui.theme.custom.CustomTimePickerDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class StudentAuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private var isSignupMode = false
    private val timeSlots = mutableListOf<TimeSlotView>()

    private lateinit var scheduleSection: LinearLayout
    private lateinit var timeSlotsContainer: LinearLayout
    private lateinit var btnAddTimeSlot: Button

    private val daysOfWeek = arrayOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    data class TimeSlotView(
        val view: LinearLayout,
        val daySpinner: Spinner,
        val startTimeEdit: EditText,
        val endTimeEdit: EditText,
        val removeButton: Button,
        var startHour: Int = 9,
        var startMinute: Int = 0,
        var endHour: Int = 17,
        var endMinute: Int = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_auth)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etName = findViewById<EditText>(R.id.et_name)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvToggle = findViewById<TextView>(R.id.tv_toggle_mode)

        scheduleSection = findViewById(R.id.schedule_section)
        timeSlotsContainer = findViewById(R.id.time_slots_container)
        btnAddTimeSlot = findViewById(R.id.btn_add_time_slot)

        btnAddTimeSlot.setOnClickListener {
            addTimeSlotView()
        }

        tvToggle.setOnClickListener {
            isSignupMode = !isSignupMode
            if (isSignupMode) {
                etName.visibility = android.view.View.VISIBLE
                scheduleSection.visibility = android.view.View.VISIBLE
                btnLogin.text = "Sign Up"
                tvToggle.text = "Already have account? Login"

                // Add one default time slot
                if (timeSlots.isEmpty()) {
                    addTimeSlotView()
                }
            } else {
                etName.visibility = android.view.View.GONE
                scheduleSection.visibility = android.view.View.GONE
                btnLogin.text = "Login"
                tvToggle.text = "Don't have account? Sign Up"

                // Clear time slots
                timeSlotsContainer.removeAllViews()
                timeSlots.clear()
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

    private fun addTimeSlotView() {
        val inflater = LayoutInflater.from(this)
        val timeSlotView = inflater.inflate(R.layout.item_time_slot, timeSlotsContainer, false) as LinearLayout

        val daySpinner = timeSlotView.findViewById<Spinner>(R.id.spinner_day)
        val startTimeEdit = timeSlotView.findViewById<EditText>(R.id.et_start_time)
        val endTimeEdit = timeSlotView.findViewById<EditText>(R.id.et_end_time)
        val removeButton = timeSlotView.findViewById<Button>(R.id.btn_remove_slot)

        // Setup day spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        daySpinner.adapter = adapter

        val slotData = TimeSlotView(
            view = timeSlotView,
            daySpinner = daySpinner,
            startTimeEdit = startTimeEdit,
            endTimeEdit = endTimeEdit,
            removeButton = removeButton
        )

        // Set default times
        updateTimeDisplay(slotData)

        // Setup time pickers
        startTimeEdit.setOnClickListener {
            showTimePickerForSlot(slotData, true)
        }

        endTimeEdit.setOnClickListener {
            showTimePickerForSlot(slotData, false)
        }

        // Remove button
        removeButton.setOnClickListener {
            timeSlotsContainer.removeView(timeSlotView)
            timeSlots.remove(slotData)
        }

        timeSlots.add(slotData)
        timeSlotsContainer.addView(timeSlotView)
    }

    private fun showTimePickerForSlot(slotData: TimeSlotView, isStartTime: Boolean) {
        val initialHour = if (isStartTime) slotData.startHour else slotData.endHour
        val initialMinute = if (isStartTime) slotData.startMinute else slotData.endMinute

        val dialog = CustomTimePickerDialog(
            this,
            initialHour,
            initialMinute
        ) { hour, minute ->
            if (isStartTime) {
                slotData.startHour = hour
                slotData.startMinute = minute
            } else {
                slotData.endHour = hour
                slotData.endMinute = minute
            }
            updateTimeDisplay(slotData)
        }

        dialog.show()
    }

    private fun updateTimeDisplay(slotData: TimeSlotView) {
        slotData.startTimeEdit.setText(String.format("%02d:%02d", slotData.startHour, slotData.startMinute))
        slotData.endTimeEdit.setText(String.format("%02d:%02d", slotData.endHour, slotData.endMinute))
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

        // Validate time slots
        if (timeSlots.isEmpty()) {
            Toast.makeText(this, "Please add at least one vacant time slot", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert time slots to data model
        val vacantSchedule = timeSlots.map { slot ->
            TimeSlot(
                day = slot.daySpinner.selectedItem.toString(),
                startTime = String.format("%02d:%02d", slot.startHour, slot.startMinute),
                endTime = String.format("%02d:%02d", slot.endHour, slot.endMinute)
            )
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
                        "appliedJobs" to emptyList<String>(),
                        "vacantSchedule" to vacantSchedule.map { slot ->
                            mapOf(
                                "day" to slot.day,
                                "startTime" to slot.startTime,
                                "endTime" to slot.endTime
                            )
                        }
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