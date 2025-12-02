package com.workit.workit.ui.theme.employer

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.workit.workit.R
import com.workit.workit.adapter.JobAdapter
import com.workit.workit.data.Job
import com.workit.workit.data.Match
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.squareup.picasso.Picasso
import java.util.Calendar

class EmployerHomeFragment : Fragment() {
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var applicantsContainer: LinearLayout
    private lateinit var fabAddJob: FloatingActionButton
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employer_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        jobsRecyclerView = view.findViewById(R.id.jobs_recycler_view)
        applicantsContainer = view.findViewById(R.id.applicants_container)
        fabAddJob = view.findViewById(R.id.fab_add_job)

        jobsRecyclerView.layoutManager = LinearLayoutManager(context)

        val jobAdapter = JobAdapter(emptyList()) { job ->
            showJobApplicants(job)
        }
        jobsRecyclerView.adapter = jobAdapter

        fabAddJob.setOnClickListener {
            showAddJobDialog()
        }

        loadEmployerJobs(jobAdapter)
    }

    private fun showAddJobDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_post_job, null)
        val etPosition = dialogView.findViewById<EditText>(R.id.et_position)
        val etLocation = dialogView.findViewById<EditText>(R.id.et_location)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_description)
        val etShiftStart = dialogView.findViewById<EditText>(R.id.et_shift_start)
        val etShiftEnd = dialogView.findViewById<EditText>(R.id.et_shift_end)
        val etRequirements = dialogView.findViewById<EditText>(R.id.et_requirements)
        val btnPost = dialogView.findViewById<Button>(R.id.btn_post_dialog)

        // Time pickers for shift times
        etShiftStart.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                etShiftStart.setText(String.format("%02d:%02d", hour, minute))
            }
        }

        etShiftEnd.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                etShiftEnd.setText(String.format("%02d:%02d", hour, minute))
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Post a New Job")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnPost.setOnClickListener {
            val position = etPosition.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val shiftStart = etShiftStart.text.toString().trim()
            val shiftEnd = etShiftEnd.text.toString().trim()
            val requirements = etRequirements.text.toString().split(",").map { it.trim() }

            if (position.isEmpty() || location.isEmpty() || description.isEmpty()) {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (description.length < 20) {
                Toast.makeText(context, "Description must be at least 20 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postJobOffer(position, location, description, shiftStart, shiftEnd, requirements)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTimePickerDialog(onTimeSelected: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                onTimeSelected(selectedHour, selectedMinute)
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun postJobOffer(
        position: String,
        location: String,
        description: String,
        shiftStart: String,
        shiftEnd: String,
        requirements: List<String>
    ) {
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
            .addOnSuccessListener {
                Toast.makeText(context, "Job posted successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error posting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadEmployerJobs(jobAdapter: JobAdapter) {
        val employerId = auth.currentUser?.uid ?: return

        db.collection("jobs")
            .whereEqualTo("employerId", employerId)
            .orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading jobs: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val jobs = documents?.mapNotNull { doc ->
                    try {
                        Job(
                            id = doc.id,
                            employer = doc.getString("employer") ?: "",
                            position = doc.getString("position") ?: "",
                            location = doc.getString("location") ?: "",
                            shift = doc.getString("shift") ?: "",
                            shiftStart = doc.getString("shiftStart") ?: "",
                            shiftEnd = doc.getString("shiftEnd") ?: "",
                            description = doc.getString("description") ?: "",
                            requirements = doc.get("requirements") as? List<String> ?: emptyList(),
                            imageUrl = doc.getString("imageUrl") ?: "",
                            postedAt = doc.getLong("postedAt") ?: 0L,
                            employerId = doc.getString("employerId") ?: "",
                            status = doc.getString("status") ?: "active"
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                jobAdapter.updateJobs(jobs)
            }
    }

    private fun showJobApplicants(job: Job) {
        applicantsContainer.removeAllViews()

        val jobHeaderView = layoutInflater.inflate(R.layout.item_job_detail_header, applicantsContainer, false)
        val tvJobTitle = jobHeaderView.findViewById<TextView>(R.id.tv_job_title)
        val tvJobLocation = jobHeaderView.findViewById<TextView>(R.id.tv_job_location)
        val btnToggleStatus = jobHeaderView.findViewById<Button>(R.id.btn_toggle_status)
        val btnEditJob = jobHeaderView.findViewById<Button>(R.id.btn_edit_job)

        tvJobTitle.text = job.position
        tvJobLocation.text = job.location
        btnToggleStatus.text = if (job.status == "active") "Mark Unavailable" else "Mark Available"

        btnToggleStatus.setOnClickListener {
            toggleJobStatus(job)
        }

        btnEditJob.setOnClickListener {
            val bundle = Bundle().apply {
                putSerializable("job", job)
            }
            requireActivity().supportFragmentManager.setFragmentResult("editJob", bundle)
        }

        applicantsContainer.addView(jobHeaderView)

        val applicantsTitleView = layoutInflater.inflate(R.layout.item_applicants_title, applicantsContainer, false)
        applicantsContainer.addView(applicantsTitleView)

        loadApplicants(job.id)
    }

    private fun loadApplicants(jobId: String) {
        db.collection("matches")
            .whereEqualTo("jobId", jobId)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading applicants: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                documents?.forEach { doc ->
                    val match = Match(
                        id = doc.id,
                        jobId = doc.getString("jobId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        employerId = doc.getString("employerId") ?: "",
                        studentName = doc.getString("studentName") ?: "",
                        position = doc.getString("position") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )

                    val applicantView = layoutInflater.inflate(R.layout.item_applicant_card, applicantsContainer, false)
                    val tvApplicantName = applicantView.findViewById<TextView>(R.id.tv_applicant_name)
                    val tvStatus = applicantView.findViewById<TextView>(R.id.tv_status)
                    val btnAccept = applicantView.findViewById<Button>(R.id.btn_accept)
                    val btnReject = applicantView.findViewById<Button>(R.id.btn_reject)

                    tvApplicantName.text = match.studentName
                    setStatusDisplay(tvStatus, match.status)

                    if (match.status == "pending") {
                        btnAccept.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE

                        btnAccept.setOnClickListener {
                            updateMatchStatus(match, "accepted", tvStatus, btnAccept, btnReject)
                        }
                        btnReject.setOnClickListener {
                            updateMatchStatus(match, "rejected", tvStatus, btnAccept, btnReject)
                        }
                    } else {
                        btnAccept.visibility = View.GONE
                        btnReject.visibility = View.GONE
                    }

                    applicantsContainer.addView(applicantView)
                }
            }
    }

    private fun updateMatchStatus(match: Match, newStatus: String, tvStatus: TextView, btnAccept: Button, btnReject: Button) {
        db.collection("matches").document(match.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                setStatusDisplay(tvStatus, newStatus)
                btnAccept.visibility = View.GONE
                btnReject.visibility = View.GONE
                Toast.makeText(context, "Application ${newStatus}!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleJobStatus(job: Job) {
        val newStatus = if (job.status == "active") "inactive" else "active"

        db.collection("jobs").document(job.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(context, "Job marked ${newStatus}!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setStatusDisplay(tvStatus: TextView, status: String) {
        when (status) {
            "pending" -> {
                tvStatus.text = "Pending"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            }
            "accepted" -> {
                tvStatus.text = "Accepted ✓"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            }
            "rejected" -> {
                tvStatus.text = "Rejected ✗"
                tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
            }
        }
    }
}