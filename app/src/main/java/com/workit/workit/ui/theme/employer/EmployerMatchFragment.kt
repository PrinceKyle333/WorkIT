package com.workit.workit.ui.theme.employer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.workit.workit.ui.viewmodel.MatchActionUiState
import com.workit.workit.ui.viewmodel.MatchesUiState
import com.workit.workit.ui.viewmodel.MatchesViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EmployerMatchFragment : Fragment() {
    private val viewModel: MatchesViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var matchesRecyclerView: RecyclerView
    private lateinit var matchAdapter: MatchAdapter

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
            viewModel.updateMatchStatus(match.id, newStatus)
        }
        matchesRecyclerView.adapter = matchAdapter

        val employerId = auth.currentUser?.uid ?: return
        viewModel.loadEmployerMatches(employerId)

        // Observe matches state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.employerMatches.collect { state ->
                    when (state) {
                        is MatchesUiState.Loading -> showLoading()
                        is MatchesUiState.Success -> {
                            hideLoading()
                            matchAdapter.updateMatches(state.matches)
                        }
                        is MatchesUiState.Error -> {
                            hideLoading()
                            showError(state.message)
                        }
                    }
                }
            }
        }

        // Observe action state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionState.collect { state ->
                    when (state) {
                        is MatchActionUiState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            val employerId = auth.currentUser?.uid ?: return@collect
                            viewModel.loadEmployerMatches(employerId)
                        }
                        is MatchActionUiState.Error -> {
                            showError(state.message)
                        }
                        else -> {}
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
