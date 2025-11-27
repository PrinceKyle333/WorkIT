package com.workit.workit.ui.theme.employer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.adapter.JobAdapter
import com.workit.workit.data.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmployerHomeFragment : Fragment() {
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var jobAdapter: JobAdapter
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
        jobsRecyclerView.layoutManager = LinearLayoutManager(context)
        jobAdapter = JobAdapter(emptyList()) { job ->
            // Handle job click - maybe edit or view details
        }
        jobsRecyclerView.adapter = jobAdapter

        loadEmployerJobs()
    }

    private fun loadEmployerJobs() {
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
}