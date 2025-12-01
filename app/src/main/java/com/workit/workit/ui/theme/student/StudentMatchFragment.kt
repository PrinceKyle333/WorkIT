package com.workit.workit.ui.theme.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.adapter.MatchAdapter
import com.workit.workit.ui.viewmodel.MatchesUiState
import com.workit.workit.ui.viewmodel.MatchesViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class StudentMatchFragment : Fragment() {
    private val viewModel: MatchesViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var matchesRecyclerView: RecyclerView
    private lateinit var tvEmptyMessage: TextView
    private lateinit var matchAdapter: MatchAdapter

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

        matchesRecyclerView.layoutManager = LinearLayoutManager(context)
        matchAdapter = MatchAdapter(emptyList())
        matchesRecyclerView.adapter = matchAdapter

        val studentId = auth.currentUser?.uid ?: return
        viewModel.loadStudentMatches(studentId)

        // Observe matches state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.studentMatches.collect { state ->
                    when (state) {
                        is MatchesUiState.Loading -> showLoading()
                        is MatchesUiState.Success -> {
                            hideLoading()
                            if (state.matches.isEmpty()) {
                                tvEmptyMessage.visibility = View.VISIBLE
                                matchesRecyclerView.visibility = View.GONE
                            } else {
                                tvEmptyMessage.visibility = View.GONE
                                matchesRecyclerView.visibility = View.VISIBLE
                                matchAdapter.updateMatches(state.matches)
                            }
                        }
                        is MatchesUiState.Error -> {
                            hideLoading()
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showLoading() {}

    private fun hideLoading() {}

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}