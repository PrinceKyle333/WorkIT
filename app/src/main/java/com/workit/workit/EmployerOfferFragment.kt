package com.workit.workit.ui.theme.employer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.workit.workit.R
import com.workit.workit.ui.theme.custom.CustomTimePickerDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class EmployerOfferFragment : Fragment() {
    private lateinit var positionInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var shiftStartInput: EditText
    private lateinit var shiftEndInput: EditText
    private lateinit var postButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedStartHour = 9
    private var selectedStartMinute = 0
    private var selectedEndHour = 17
    private var selectedEndMinute = 0

    // Fixed requirements
    private val requiredDocuments = listOf(
        "Resume (PDF)",
        "Certificate of Registration (COR) (PDF)",
        "Application Letter (PDF)"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employer_offer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        positionInput = view.findViewById(R.id.et_position)
        locationInput = view.findViewById(R.id.et_location)
        descriptionInput = view.findViewById(R.id.et_description)
        shiftStartInput = view.findViewById(R.id.et_shift_start)
        shiftEndInput = view.findViewById(R.id.et_shift_end)
        postButton = view.findViewById(R.id.btn_post)

        // Make time inputs non-editable (only time picker)
        shiftStartInput.isFocusable = false
        shiftStartInput.isClickable = true
        shiftEndInput.isFocusable = false
        shiftEndInput.isClickable = true

        // Set default times
        updateTimeDisplay()

        // Time picker listeners
        shiftStartInput.setOnClickListener {
            showTimePickerDialog(true)
        }

        shiftEndInput.setOnClickListener {
            showTimePickerDialog(false)
        }

        postButton.setOnClickListener {
            postJobOffer()
        }
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val initialHour = if (isStartTime) selectedStartHour else selectedEndHour
        val initialMinute = if (isStartTime) selectedStartMinute else selectedEndMinute

        val dialog = CustomTimePickerDialog(
            requireContext(),
            initialHour,
            initialMinute
        ) { hour, minute ->
            if (isStartTime) {
                selectedStartHour = hour
                selectedStartMinute = minute
            } else {
                selectedEndHour = hour
                selectedEndMinute = minute
            }
            updateTimeDisplay()
        }

        dialog.show()
    }

    private fun updateTimeDisplay() {
        shiftStartInput.setText(String.format("%02d:%02d", selectedStartHour, selectedStartMinute))
        shiftEndInput.setText(String.format("%02d:%02d", selectedEndHour, selectedEndMinute))
    }

    private fun postJobOffer() {
        val position = positionInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val shiftStart = shiftStartInput.text.toString().trim()
        val shiftEnd = shiftEndInput.text.toString().trim()

        if (position.isEmpty() || location.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.length < 20) {
            Toast.makeText(context, "Description must be at least 20 characters", Toast.LENGTH_SHORT).show()
            return
        }

        postButton.isEnabled = false
        postButton.text = "Posting..."

        val employerId = auth.currentUser?.uid ?: return
        val employerEmail = auth.currentUser?.email ?: ""

        val jobData = hashMapOf(
            "employer" to employerEmail,
            "employerId" to employerId,
            "position" to position,
            "location" to location,
            "description" to description,
            "shift" to "$shiftStart - $shiftEnd",
            "shiftStart" to shiftStart,
            "shiftEnd" to shiftEnd,
            "shiftStartHour" to selectedStartHour,
            "shiftStartMinute" to selectedStartMinute,
            "shiftEndHour" to selectedEndHour,
            "shiftEndMinute" to selectedEndMinute,
            "requirements" to requiredDocuments,  // FIXED: Always these 3 documents
            "workDays" to emptyList<String>(),
            "imageUrl" to "",
            "status" to "active",
            "postedAt" to FieldValue.serverTimestamp(),
            "latitude" to 0.0,
            "longitude" to 0.0
        )

        db.collection("jobs")
            .add(jobData)
            .addOnSuccessListener {
                Toast.makeText(context, "Job posted successfully!", Toast.LENGTH_SHORT).show()
                clearForm()
                postButton.isEnabled = true
                postButton.text = "Post Job"
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error posting job: ${e.message}", Toast.LENGTH_SHORT).show()
                postButton.isEnabled = true
                postButton.text = "Post Job"
            }
    }

    private fun clearForm() {
        positionInput.text.clear()
        locationInput.text.clear()
        descriptionInput.text.clear()
        selectedStartHour = 9
        selectedStartMinute = 0
        selectedEndHour = 17
        selectedEndMinute = 0
        updateTimeDisplay()
    }
}