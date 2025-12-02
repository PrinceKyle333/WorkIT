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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class EmployerOfferFragment : Fragment() {
    private lateinit var positionInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var shiftInput: EditText
    private lateinit var requirementsInput: EditText
    private lateinit var postButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        shiftInput = view.findViewById(R.id.et_shift)
        requirementsInput = view.findViewById(R.id.et_requirements)
        postButton = view.findViewById(R.id.btn_post)

        postButton.setOnClickListener {
            postJobOffer()
        }
    }

    private fun postJobOffer() {
        val position = positionInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val shift = shiftInput.text.toString().trim()
        val requirements = requirementsInput.text.toString().split(",").map { it.trim() }

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
            "shift" to shift,
            "shiftStart" to "",
            "shiftEnd" to "",
            "workDays" to emptyList<String>(),
            "requirements" to requirements,
            "imageUrl" to "",
            "status" to "active",
            "postedAt" to FieldValue.serverTimestamp(),
            "latitude" to 0.0,
            "longitude" to 0.0
        )

        db.collection("jobs")
            .add(jobData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(context, "Job posted successfully!", Toast.LENGTH_SHORT).show()
                clearForm()
                postButton.isEnabled = true
                postButton.text = "Post Job"

                // Log for debugging
                android.util.Log.d("JobPost", "Job posted with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error posting job: ${e.message}", Toast.LENGTH_SHORT).show()
                postButton.isEnabled = true
                postButton.text = "Post Job"

                // Log error for debugging
                android.util.Log.e("JobPost", "Error posting job", e)
            }
    }

    private fun clearForm() {
        positionInput.text.clear()
        locationInput.text.clear()
        descriptionInput.text.clear()
        shiftInput.text.clear()
        requirementsInput.text.clear()
    }
}