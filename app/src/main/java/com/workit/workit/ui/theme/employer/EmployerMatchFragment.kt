package com.workit.workit.ui.theme.employer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        matchAdapter = MatchAdapter(emptyList())
        matchesRecyclerView.adapter = matchAdapter

        loadMatches()
    }

    private fun loadMatches() {
        val employerId = auth.currentUser?.uid ?: return

        db.collection("matches")
            .whereEqualTo("employerId", employerId)
            .addSnapshotListener { documents, error ->
                if (error != null) return@addSnapshotListener

                val matches = documents?.mapNotNull { doc ->
                    try {
                        Match(
                            id = doc.id,
                            jobId = doc.getString("jobId") ?: "",
                            employerName = doc.getString("employerName") ?: "",
                            position = doc.getString("position") ?: "",
                            status = doc.getString("status") ?: "pending"
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                matchAdapter.updateMatches(matches)
            }
    }
}