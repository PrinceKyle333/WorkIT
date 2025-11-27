package com.workit.workit.ui.theme.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.workit.workit.R
import com.workit.workit.data.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class JobDetailsFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_job_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get job data from arguments
        job = arguments?.getSerializable("job") as? Job

        val tvPosition = view.findViewById<TextView>(R.id.tv_position)
        val tvLocation = view.findViewById<TextView>(R.id.tv_location)
        val tvShift = view.findViewById<TextView>(R.id.tv_shift)
        val tvDescription = view.findViewById<TextView>(R.id.tv_description)
        val tvRequirements = view.findViewById<TextView>(R.id.tv_requirements)
        val ivJobImage = view.findViewById<ImageView>(R.id.iv_job_image)
        val btnApply = view.findViewById<Button>(R.id.btn_apply)

        job?.let { jobData ->
            tvPosition.text = jobData.position
            tvLocation.text = jobData.location
            tvShift.text = jobData.shift
            tvDescription.text = jobData.description
            tvRequirements.text = "Requirements: ${jobData.requirements.joinToString(", ")}"

            if (jobData.imageUrl.isNotEmpty()) {
                Picasso.get().load(jobData.imageUrl).into(ivJobImage)
            }

            btnApply.setOnClickListener {
                applyForJob(jobData)
            }
        }
    }

    private fun applyForJob(job: Job) {
        val studentId = auth.currentUser?.uid ?: return

        // Get student details
        db.collection("students").document(studentId).get()
            .addOnSuccessListener { studentDoc ->
                val studentName = studentDoc.getString("name") ?: ""
                val studentEmail = studentDoc.getString("email") ?: ""

                // Check if already applied
                db.collection("matches")
                    .whereEqualTo("jobId", job.id)
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            Toast.makeText(context, "You already applied for this job", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Create match document
                        val matchData = mapOf(
                            "jobId" to job.id,
                            "studentId" to studentId,
                            "employerId" to job.employerId,
                            "studentName" to studentName,
                            "studentEmail" to studentEmail,
                            "employerName" to job.employer,
                            "position" to job.position,
                            "status" to "pending",
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("matches").add(matchData)
                            .addOnSuccessListener {
                                // Add job to student's appliedJobs
                                db.collection("students").document(studentId)
                                    .update("appliedJobs", com.google.firebase.firestore.FieldValue.arrayUnion(job.id))

                                // Add student to job's applicants
                                db.collection("jobs").document(job.id)
                                    .update("applicants", com.google.firebase.firestore.FieldValue.arrayUnion(studentId))

                                Toast.makeText(context, "Applied successfully!", Toast.LENGTH_SHORT).show()
                                requireActivity().onBackPressed()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
    }
}