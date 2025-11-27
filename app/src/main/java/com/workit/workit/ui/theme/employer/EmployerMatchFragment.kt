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
import com.workit.workit.adapter.MatchAdapter
import com.workit.workit.data.Match
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmployerMatchFragment : Fragment() {
    private lateinit var matchesRecyclerView: RecyclerView
    private lateinit var matchAdapter: MatchAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employer_match, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchesRecyclerView = view.findViewById(R.id.matches_recycler_view)
        matchesRecyclerView.layoutManager = LinearLayoutManager(context)
        matchAdapter = MatchAdapter(emptyList()) { match, newStatus ->
            updateMatchStatus(match, newStatus)
        }
        matchesRecyclerView.adapter = matchAdapter

        loadMatches()
    }

    private fun loadMatches() {
        val employerId = auth.currentUser?.uid ?: return

        // Get all matches for this employer
        db.collection("matches")
            .whereEqualTo("employerId", employerId)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading applications: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val matches = documents?.mapNotNull { doc ->
                    try {
                        val jobId = doc.getString("jobId") ?: ""

                        // Get job details to verify employer posted it
                        db.collection("jobs").document(jobId).get()
                            .addOnSuccessListener { jobDoc ->
                                // Only show if current employer posted this job
                                val jobEmployerId = jobDoc.getString("employerId") ?: ""
                                if (jobEmployerId == employerId) {
                                    // Job belongs to this employer
                                }
                            }

                        Match(
                            id = doc.id,
                            jobId = jobId,
                            studentId = doc.getString("studentId") ?: "",
                            employerId = doc.getString("employerId") ?: "",
                            studentName = doc.getString("studentName") ?: "",
                            position = doc.getString("position") ?: "",
                            status = doc.getString("status") ?: "pending",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                // Filter to only show applications for jobs posted by this employer
                filterMatchesByJobOwner(matches, employerId)
            }
    }

    private fun filterMatchesByJobOwner(matches: List<Match>, employerId: String) {
        val filteredMatches = mutableListOf<Match>()
        var processedCount = 0

        matches.forEach { match ->
            db.collection("jobs").document(match.jobId).get()
                .addOnSuccessListener { jobDoc ->
                    processedCount++

                    // Only include if employer posted this job
                    if (jobDoc.exists()) {
                        val jobEmployerId = jobDoc.getString("employerId") ?: ""
                        if (jobEmployerId == employerId) {
                            filteredMatches.add(match)
                        }
                    }

                    // Update adapter when all jobs have been checked
                    if (processedCount == matches.size) {
                        matchAdapter.updateMatches(filteredMatches)
                    }
                }
                .addOnFailureListener {
                    processedCount++
                    if (processedCount == matches.size) {
                        matchAdapter.updateMatches(filteredMatches)
                    }
                }
        }

        // Handle empty list
        if (matches.isEmpty()) {
            matchAdapter.updateMatches(emptyList())
        }
    }

    private fun updateMatchStatus(match: Match, newStatus: String) {
        val employerId = auth.currentUser?.uid ?: return

        // Verify this employer posted the job
        db.collection("jobs").document(match.jobId).get()
            .addOnSuccessListener { jobDoc ->
                if (jobDoc.exists()) {
                    val jobEmployerId = jobDoc.getString("employerId") ?: ""

                    if (jobEmployerId == employerId) {
                        // Update the match status
                        db.collection("matches").document(match.id)
                            .update("status", newStatus)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Application ${newStatus}!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "You don't have permission to accept this application", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error verifying job", Toast.LENGTH_SHORT).show()
            }
    }
}