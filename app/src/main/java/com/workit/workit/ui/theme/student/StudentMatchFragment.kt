package com.workit.workit.ui.theme.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.adapter.ExpandableJobAdapter
import com.workit.workit.data.Application
import com.workit.workit.data.Job
import com.workit.workit.data.Student
import com.workit.workit.data.TimeSlot
import com.workit.workit.utils.JobMatchingUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StudentMatchFragment : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var matchesRecyclerView: RecyclerView
    private lateinit var tvEmptyMessage: TextView
    private lateinit var jobAdapter: ExpandableJobAdapter

    // Section titles
    private lateinit var tvMatchingJobsTitle: TextView
    private lateinit var tvApplicationsTitle: TextView
    private lateinit var applicationsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_match, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchesRecyclerView = view.findViewById(R.id.matches_recycler_view)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        // Find new views for section titles
        tvMatchingJobsTitle = view.findViewById(R.id.tv_matching_jobs_title)
        tvApplicationsTitle = view.findViewById(R.id.tv_applications_title)
        applicationsContainer = view.findViewById(R.id.applications_container)

        matchesRecyclerView.layoutManager = LinearLayoutManager(context)

        jobAdapter = ExpandableJobAdapter(
            emptyList(),
            onJobClick = { job ->
                // Job click handled by expand/collapse
            },
            onApplyClick = { job ->
                // Navigate to job details for application
                val bundle = Bundle().apply {
                    putSerializable("job", job)
                }
                findNavController().navigate(R.id.action_nav_home_to_job_details, bundle)
            }
        )
        matchesRecyclerView.adapter = jobAdapter

        loadMatchingJobsAndApplications()
    }

    private fun loadMatchingJobsAndApplications() {
        val studentId = auth.currentUser?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load matching jobs
                loadMatchingJobs(studentId)

                // Load applications
                loadStudentApplications(studentId)

            } catch (e: Exception) {
                Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadMatchingJobs(studentId: String) {
        try {
            // Get student data with schedule
            val studentDoc = db.collection("students")
                .document(studentId)
                .get()
                .await()

            if (!studentDoc.exists()) {
                showNoScheduleMessage()
                return
            }

            // Parse vacant schedule safely
            val vacantScheduleData = studentDoc.get("vacantSchedule") as? List<*>
            val vacantSchedule = vacantScheduleData?.mapNotNull { item ->
                try {
                    val map = item as? Map<*, *>
                    TimeSlot(
                        day = map?.get("day") as? String ?: "",
                        startTime = map?.get("startTime") as? String ?: "",
                        endTime = map?.get("endTime") as? String ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            if (vacantSchedule.isEmpty()) {
                showNoScheduleMessage()
                return
            }

            // Create student object
            val student = Student(
                id = studentId,
                name = studentDoc.getString("name") ?: "",
                email = studentDoc.getString("email") ?: "",
                vacantSchedule = vacantSchedule
            )

            // Get all active jobs
            val jobsSnapshot = db.collection("jobs")
                .whereEqualTo("status", "active")
                .get()
                .await()

            val allJobs = jobsSnapshot.documents.mapNotNull { doc ->
                try {
                    Job(
                        id = doc.id,
                        employer = doc.getString("employer") ?: "",
                        employerId = doc.getString("employerId") ?: "",
                        position = doc.getString("position") ?: "",
                        location = doc.getString("location") ?: "",
                        shift = doc.getString("shift") ?: "",
                        shiftStart = doc.getString("shiftStart") ?: "",
                        shiftEnd = doc.getString("shiftEnd") ?: "",
                        workDays = doc.get("workDays") as? List<String> ?: emptyList(),
                        description = doc.getString("description") ?: "",
                        requirements = doc.get("requirements") as? List<String> ?: emptyList(),
                        imageUrl = doc.getString("imageUrl") ?: "",
                        postedAt = doc.getLong("postedAt") ?: System.currentTimeMillis(),
                        status = doc.getString("status") ?: "active",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0
                    )
                } catch (e: Exception) {
                    android.util.Log.e("StudentMatch", "Error parsing job", e)
                    null
                }
            }

            // Filter for matching jobs
            val matchingJobs = allJobs.filter { job ->
                JobMatchingUtils.isJobMatchingStudentSchedule(job, student)
            }

            if (matchingJobs.isEmpty()) {
                showNoMatchingJobsMessage()
            } else {
                showMatchingJobs(matchingJobs)
            }

        } catch (e: Exception) {
            android.util.Log.e("StudentMatch", "Error loading matching jobs", e)
            showNoScheduleMessage()
        }
    }

    private fun loadStudentApplications(studentId: String) {
        db.collection("matches")
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading applications: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                applicationsContainer.removeAllViews()

                if (documents == null || documents.isEmpty) {
                    tvApplicationsTitle.visibility = View.GONE
                    return@addSnapshotListener
                }

                tvApplicationsTitle.visibility = View.VISIBLE

                documents.forEach { doc ->
                    val match = com.workit.workit.data.Match(
                        id = doc.id,
                        jobId = doc.getString("jobId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        employerId = doc.getString("employerId") ?: "",
                        employerName = doc.getString("employerName") ?: "",
                        position = doc.getString("position") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )

                    val applicationView = createApplicationView(match)
                    applicationsContainer.addView(applicationView)
                }
            }
    }

    private fun showMatchingJobs(jobs: List<Job>) {
        tvMatchingJobsTitle.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.GONE
        matchesRecyclerView.visibility = View.VISIBLE
        jobAdapter.updateJobs(jobs)
    }

    private fun showNoScheduleMessage() {
        tvMatchingJobsTitle.visibility = View.GONE
        tvEmptyMessage.visibility = View.VISIBLE
        tvEmptyMessage.text = "Set your vacant schedule in Profile to see matching jobs"
        matchesRecyclerView.visibility = View.GONE
    }

    private fun showNoMatchingJobsMessage() {
        tvMatchingJobsTitle.visibility = View.VISIBLE
        tvEmptyMessage.visibility = View.VISIBLE
        tvEmptyMessage.text = "No jobs match your schedule right now.\nCheck back later or update your vacant times in Profile."
        matchesRecyclerView.visibility = View.GONE
    }

    private fun createApplicationView(match: com.workit.workit.data.Match): View {
        val view = layoutInflater.inflate(R.layout.item_match, applicationsContainer, false)

        val tvEmployerName = view.findViewById<TextView>(R.id.tv_employer_name)
        val tvJobPosition = view.findViewById<TextView>(R.id.tv_job_position)
        val tvStatus = view.findViewById<TextView>(R.id.tv_status)
        val btnAccept = view.findViewById<Button>(R.id.btn_accept)
        val btnReject = view.findViewById<Button>(R.id.btn_reject)

        tvEmployerName.text = match.employerName
        tvJobPosition.text = match.position

        // Update status display
        when (match.status) {
            "pending" -> {
                tvStatus.text = "Status: Pending Review"
                tvStatus.setTextColor(resources.getColor(R.color.orange, null))
            }
            "accepted" -> {
                tvStatus.text = "Status: Accepted ✓"
                tvStatus.setTextColor(resources.getColor(R.color.green, null))
            }
            "rejected" -> {
                tvStatus.text = "Status: Rejected ✗"
                tvStatus.setTextColor(resources.getColor(R.color.red, null))
            }
        }

        // Hide accept/reject buttons for students
        btnAccept.visibility = View.GONE
        btnReject.visibility = View.GONE

        return view
    }

    private fun openDocument(url: String) {
        if (url.isEmpty()) {
            Toast.makeText(context, "Document not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload when fragment becomes visible
        loadMatchingJobsAndApplications()
    }
}