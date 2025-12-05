package com.workit.workit.ui.theme.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.workit.workit.LoginActivity
import com.workit.workit.R
import com.workit.workit.auth.RoleManager
import com.workit.workit.data.TimeSlot
import com.workit.workit.ui.theme.custom.CustomTimePickerDialog
import com.workit.workit.utils.TimeFormatUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvDisplayName: TextView
    private lateinit var tvDisplayEmail: TextView
    private lateinit var tvDisplayLocation: TextView
    private lateinit var tvPhoneValue: TextView
    private lateinit var tvVacantSchedule: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnLogout: Button

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDisplayName = view.findViewById(R.id.tv_display_name)
        tvDisplayEmail = view.findViewById(R.id.tv_display_email)
        tvDisplayLocation = view.findViewById(R.id.tv_display_location)
        tvPhoneValue = view.findViewById(R.id.tv_phone_value)
        btnEdit = view.findViewById(R.id.btn_edit)
        btnLogout = view.findViewById(R.id.btn_logout)

        loadProfileFromFirebase()

        btnEdit.setOnClickListener {
            showEditDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadProfileFromFirebase() {
        val userId = auth.currentUser?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = RoleManager.getUserRole()

                when (role) {
                    RoleManager.UserRole.STUDENT -> {
                        val doc = db.collection("students").document(userId).get().await()
                        val name = doc.getString("name") ?: "Name not set"
                        val email = doc.getString("email") ?: "Email not set"
                        val phone = doc.getString("phone") ?: ""
                        val address = doc.getString("address") ?: ""

                        tvDisplayName.text = name
                        tvDisplayEmail.text = email
                        tvPhoneValue.text = if (phone.isEmpty()) "Not set" else phone
                        tvDisplayLocation.text = if (address.isEmpty()) "Location not set" else address
                    }
                    RoleManager.UserRole.EMPLOYER -> {
                        val doc = db.collection("employers").document(userId).get().await()
                        val companyName = doc.getString("companyName") ?: "Company not set"
                        val email = doc.getString("email") ?: "Email not set"
                        val phone = doc.getString("phone") ?: ""
                        val address = doc.getString("address") ?: ""

                        tvDisplayName.text = companyName
                        tvDisplayEmail.text = email
                        tvPhoneValue.text = if (phone.isEmpty()) "Not set" else phone
                        tvDisplayLocation.text = if (address.isEmpty()) "Location not set" else address
                    }
                    else -> {
                        showError("Unknown user type")
                    }
                }
            } catch (e: Exception) {
                showError("Failed to load profile: ${e.message}")
            }
        }
    }

    private fun showEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        // Initialize all fields
        val etFirstName = dialogView.findViewById<EditText>(R.id.et_edit_first_name)
        val etMiddleName = dialogView.findViewById<EditText>(R.id.et_edit_middle_name)
        val etLastName = dialogView.findViewById<EditText>(R.id.et_edit_last_name)
        val etExtName = dialogView.findViewById<EditText>(R.id.et_edit_ext_name)
        val etIpNumber = dialogView.findViewById<EditText>(R.id.et_edit_ip_number)
        val etProgram = dialogView.findViewById<EditText>(R.id.et_edit_program)
        val etUsername = dialogView.findViewById<EditText>(R.id.et_edit_username)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_edit_phone)
        val etLocation = dialogView.findViewById<EditText>(R.id.et_edit_location)
        val timeSlotsContainer = dialogView.findViewById<LinearLayout>(R.id.edit_time_slots_container)
        val btnAddTimeSlot = dialogView.findViewById<Button>(R.id.btn_edit_add_time_slot)

        val timeSlots = mutableListOf<TimeSlotView>()

        // Load current profile data
        val userId = auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = RoleManager.getUserRole()

                if (role == RoleManager.UserRole.STUDENT) {
                    val doc = db.collection("students").document(userId).get().await()

                    // Pre-fill fields
                    etFirstName.setText(doc.getString("firstName") ?: "")
                    etMiddleName.setText(doc.getString("middleName") ?: "")
                    etLastName.setText(doc.getString("lastName") ?: "")
                    etExtName.setText(doc.getString("extName") ?: "")
                    etIpNumber.setText(doc.getString("ipNumber") ?: "")
                    etProgram.setText(doc.getString("program") ?: "")
                    etUsername.setText(doc.getString("username") ?: "")
                    etPhone.setText(doc.getString("phone") ?: "")
                    etLocation.setText(doc.getString("address") ?: "")

                    // Load existing time slots
                    val vacantSchedule = doc.get("vacantSchedule") as? List<Map<String, Any>> ?: emptyList()
                    for (slot in vacantSchedule) {
                        val day = slot["day"] as? String ?: "Monday"
                        val startTime = slot["startTime"] as? String ?: "09:00"
                        val endTime = slot["endTime"] as? String ?: "17:00"

                        addTimeSlotView(timeSlotsContainer, timeSlots, day, startTime, endTime)
                    }

                    // Add at least one empty slot if none exist
                    if (timeSlots.isEmpty()) {
                        addTimeSlotView(timeSlotsContainer, timeSlots)
                    }
                }
            } catch (e: Exception) {
                showError("Failed to load profile data: ${e.message}")
            }
        }

        // Add time slot button listener
        btnAddTimeSlot.setOnClickListener {
            addTimeSlotView(timeSlotsContainer, timeSlots)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveProfileToFirebase(
                    etFirstName, etMiddleName, etLastName, etExtName,
                    etIpNumber, etProgram, etUsername,
                    etPhone, etLocation, timeSlots
                )
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addTimeSlotView(
        container: LinearLayout,
        timeSlots: MutableList<TimeSlotView>,
        defaultDay: String = "Monday",
        defaultStartTime: String = "09:00",
        defaultEndTime: String = "17:00"
    ) {
        val inflater = LayoutInflater.from(requireContext())
        val timeSlotView = inflater.inflate(R.layout.item_time_slot, container, false) as LinearLayout

        val daySpinner = timeSlotView.findViewById<Spinner>(R.id.spinner_day)
        val startTimeEdit = timeSlotView.findViewById<EditText>(R.id.et_start_time)
        val endTimeEdit = timeSlotView.findViewById<EditText>(R.id.et_end_time)
        val removeButton = timeSlotView.findViewById<Button>(R.id.btn_remove_slot)

        // Setup day spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        daySpinner.adapter = adapter

        // Set default day
        val dayIndex = daysOfWeek.indexOf(defaultDay)
        if (dayIndex >= 0) {
            daySpinner.setSelection(dayIndex)
        }

        // Parse times using TimeFormatUtils
        val startHour = TimeFormatUtils.parseHour(defaultStartTime)
        val startMinute = TimeFormatUtils.parseMinute(defaultStartTime)
        val endHour = TimeFormatUtils.parseHour(defaultEndTime)
        val endMinute = TimeFormatUtils.parseMinute(defaultEndTime)

        val slotData = TimeSlotView(
            view = timeSlotView,
            daySpinner = daySpinner,
            startTimeEdit = startTimeEdit,
            endTimeEdit = endTimeEdit,
            removeButton = removeButton,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute
        )

        // Set times using uniform format
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
            container.removeView(timeSlotView)
            timeSlots.remove(slotData)
        }

        timeSlots.add(slotData)
        container.addView(timeSlotView)
    }

    private fun showTimePickerForSlot(slotData: TimeSlotView, isStartTime: Boolean) {
        val initialHour = if (isStartTime) slotData.startHour else slotData.endHour
        val initialMinute = if (isStartTime) slotData.startMinute else slotData.endMinute

        val dialog = CustomTimePickerDialog(
            requireContext(),
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
        // UNIFORM FORMAT: Use TimeFormatUtils for storage format
        val startTime = TimeFormatUtils.formatToStorage(slotData.startHour, slotData.startMinute)
        val endTime = TimeFormatUtils.formatToStorage(slotData.endHour, slotData.endMinute)

        slotData.startTimeEdit.setText(startTime)
        slotData.endTimeEdit.setText(endTime)
    }

    private fun saveProfileToFirebase(
        etFirstName: EditText,
        etMiddleName: EditText,
        etLastName: EditText,
        etExtName: EditText,
        etIpNumber: EditText,
        etProgram: EditText,
        etUsername: EditText,
        etPhone: EditText,
        etLocation: EditText,
        timeSlots: List<TimeSlotView>
    ) {
        val userId = auth.currentUser?.uid ?: return

        val firstName = etFirstName.text.toString().trim()
        val middleName = etMiddleName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val extName = etExtName.text.toString().trim()
        val ipNumber = etIpNumber.text.toString().trim()
        val program = etProgram.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val location = etLocation.text.toString().trim()

        // Construct full name
        val fullName = buildString {
            append(firstName)
            if (middleName.isNotEmpty()) append(" $middleName")
            append(" $lastName")
            if (extName.isNotEmpty()) append(" $extName")
        }

        // Convert time slots to data model using UNIFORM FORMAT
        val vacantSchedule = timeSlots.map { slot ->
            mapOf(
                "day" to slot.daySpinner.selectedItem.toString(),
                "startTime" to TimeFormatUtils.formatToStorage(slot.startHour, slot.startMinute),
                "endTime" to TimeFormatUtils.formatToStorage(slot.endHour, slot.endMinute)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = RoleManager.getUserRole()

                when (role) {
                    RoleManager.UserRole.STUDENT -> {
                        val updates = hashMapOf<String, Any>(
                            "name" to fullName,
                            "firstName" to firstName,
                            "middleName" to middleName,
                            "lastName" to lastName,
                            "extName" to extName,
                            "ipNumber" to ipNumber,
                            "program" to program,
                            "username" to username,
                            "phone" to phone,
                            "address" to location,
                            "vacantSchedule" to vacantSchedule
                        )
                        db.collection("students").document(userId).update(updates).await()
                    }
                    RoleManager.UserRole.EMPLOYER -> {
                        val updates = hashMapOf<String, Any>(
                            "companyName" to fullName,
                            "phone" to phone,
                            "address" to location
                        )
                        db.collection("employers").document(userId).update(updates).await()
                    }
                    else -> {
                        showError("Unknown user type")
                        return@launch
                    }
                }

                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                loadProfileFromFirebase()

            } catch (e: Exception) {
                showError("Failed to update profile: ${e.message}")
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        RoleManager.logout()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}