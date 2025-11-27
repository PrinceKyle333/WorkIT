package com.workit.workit.ui.theme.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.data.Match
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentMatchFragment : Fragment() {
    private lateinit var matchesRecyclerView: RecyclerView
    private lateinit var tvEmptyMessage: TextView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        matchesRecyclerView.layoutManager = LinearLayoutManager(context)

        // Create adapter with null for callback since students can't accept/decline
        val matchAdapter = StudentMatchAdapter(emptyList())
        matchesRecyclerView.adapter = matchAdapter

        // Empty state message
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        loadMatches(matchAdapter)
    }

    private fun loadMatches(adapter: StudentMatchAdapter) {
        val studentId = auth.currentUser?.uid ?: return

        db.collection("matches")
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading matches: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val matches = documents?.mapNotNull { doc ->
                    try {
                        Match(
                            id = doc.id,
                            jobId = doc.getString("jobId") ?: "",
                            studentId = doc.getString("studentId") ?: "",
                            employerId = doc.getString("employerId") ?: "",
                            studentName = doc.getString("studentName") ?: "",
                            employerName = doc.getString("employerName") ?: "",
                            position = doc.getString("position") ?: "",
                            status = doc.getString("status") ?: "pending",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                if (matches.isEmpty()) {
                    tvEmptyMessage.visibility = View.VISIBLE
                    matchesRecyclerView.visibility = View.GONE
                } else {
                    tvEmptyMessage.visibility = View.GONE
                    matchesRecyclerView.visibility = View.VISIBLE
                }

                adapter.updateMatches(matches)
            }
    }
}

// Adapter for student view (no buttons)
class StudentMatchAdapter(
    private var matches: List<Match>
) : RecyclerView.Adapter<StudentMatchAdapter.StudentMatchViewHolder>() {

    inner class StudentMatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmployerName: TextView = itemView.findViewById(R.id.tv_employer_name)
        val tvJobPosition: TextView = itemView.findViewById(R.id.tv_job_position)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val btnAccept: Button? = itemView.findViewById(R.id.btn_accept)
        val btnReject: Button? = itemView.findViewById(R.id.btn_reject)

        fun bind(match: Match) {
            tvEmployerName.text = match.employerName
            tvJobPosition.text = match.position

            // Hide accept/reject buttons for students
            btnAccept?.visibility = View.GONE
            btnReject?.visibility = View.GONE

            // Show status only
            setStatusDisplay(match.status)
        }

        private fun setStatusDisplay(status: String) {
            when (status) {
                "pending" -> {
                    tvStatus.text = "Status: Pending - Waiting for employer response"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Orange
                }
                "accepted" -> {
                    tvStatus.text = "Status: Accepted ✓"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green
                }
                "rejected" -> {
                    tvStatus.text = "Status: Rejected ✗"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336")) // Red
                }
                else -> {
                    tvStatus.text = "Status: $status"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#999999")) // Gray
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentMatchViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)
        return StudentMatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentMatchViewHolder, position: Int) {
        holder.bind(matches[position])
    }

    override fun getItemCount() = matches.size

    fun updateMatches(newMatches: List<Match>) {
        matches = newMatches
        notifyDataSetChanged()
    }
}