package com.workit.workit.ui.theme.student

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.adapter.JobAdapter
import com.workit.workit.ui.viewmodel.JobsUiState
import com.workit.workit.ui.viewmodel.JobsViewModel
import kotlinx.coroutines.launch

class StudentHomeFragment : Fragment() {
    private val viewModel: JobsViewModel by viewModels()
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var jobAdapter: JobAdapter

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
            val bundle = Bundle().apply {
                putSerializable("job", job)
            }
            findNavController().navigate(R.id.action_nav_home_to_job_details, bundle)
        }
        jobsRecyclerView.adapter = jobAdapter

        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is JobsUiState.Loading -> showLoading()
                        is JobsUiState.Success -> {
                            hideLoading()
                            jobAdapter.updateJobs(state.jobs)
                        }

                        is JobsUiState.Error -> {
                            hideLoading()
                            showError(state.message)
                        }
                    }
                }
            }
        }

        // Load initial jobs
        viewModel.loadJobs()
    }

    private fun showLoading() {
        // Show loading indicator
    }

    private fun hideLoading() {
        // Hide loading indicator
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}