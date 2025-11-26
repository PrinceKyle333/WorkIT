package com.workit.workit.ui.theme.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.data.Job
import com.workit.workit.adapter.JobAdapter
import com.google.firebase.firestore.FirebaseFirestore

class StudentHomeFragment : Fragment() {
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var jobAdapter: JobAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        jobsRecyclerView = view.findViewById(R.id.jobs_recycler_view)
        jobsRecyclerView.layoutManager = LinearLayoutManager(context)
        jobAdapter = JobAdapter(emptyList()) { job ->
            // Navigate to job details
        }
        jobsRecyclerView.adapter = jobAdapter

        loadJobs()
    }

    private fun loadJobs() {
        db.collection("jobs")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                val jobs = documents.map { doc ->
                    Job(
                        id = doc.id,
                        employer = doc.getString("employer") ?: "",
                        position = doc.getString("position") ?: "",
                        location = doc.getString("location") ?: "",
                        shift = doc.getString("shift") ?: "",
                        description = doc.getString("description") ?: "",
                        requirements = doc.get("requirements") as? List<String> ?: emptyList(),
                        imageUrl = doc.getString("imageUrl") ?: "",
                        postedAt = doc.getLong("postedAt") ?: 0L,
                        employerId = doc.getString("employerId") ?: ""
                    )
                }
                jobAdapter.updateJobs(jobs)
            }
    }
}
