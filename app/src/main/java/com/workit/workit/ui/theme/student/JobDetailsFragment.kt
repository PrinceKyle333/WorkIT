package com.workit.workit.ui.theme.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.workit.workit.R
import com.workit.workit.data.Job
import com.workit.workit.ui.viewmodel.ApplicationUiState
import com.workit.workit.ui.viewmodel.JobDetailsUiState
import com.workit.workit.ui.viewmodel.JobDetailsViewModel
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class JobDetailsFragment : Fragment() {
    private val viewModel: JobDetailsViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_job_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvPosition = view.findViewById<TextView>(R.id.tv_position)
        val tvLocation = view.findViewById<TextView>(R.id.tv_location)
        val tvShift = view.findViewById<TextView>(R.id.tv_shift)
        val tvDescription = view.findViewById<TextView>(R.id.tv_description)
        val tvRequirements = view.findViewById<TextView>(R.id.tv_requirements)
        val ivJobImage = view.findViewById<ImageView>(R.id.iv_job_image)
        val btnApply = view.findViewById<Button>(R.id.btn_apply)

        // Get job from arguments
        job = arguments?.getSerializable("job") as? Job

        job?.let { jobData ->
            viewModel.loadJob(jobData.id)
        }

        // Observe job state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.jobState.collect { state ->
                    when (state) {
                        is JobDetailsUiState.Loading -> showJobLoading()
                        is JobDetailsUiState.Success -> {
                            job = state.job
                            tvPosition.text = state.job.position
                            tvLocation.text = state.job.location
                            tvShift.text = state.job.shift
                            tvDescription.text = state.job.description
                            tvRequirements.text = "Requirements: ${state.job.requirements.joinToString(", ")}"

                            if (state.job.imageUrl.isNotEmpty()) {
                                Picasso.get()
                                    .load(state.job.imageUrl)
                                    .placeholder(R.drawable.ic_upload)
                                    .error(R.drawable.ic_upload)
                                    .into(ivJobImage)
                            }
                        }
                        is JobDetailsUiState.Error -> showError(state.message)
                    }
                }
            }
        }

        // Observe application state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.applicationState.collect { state ->
                    when (state) {
                        is ApplicationUiState.Loading -> {
                            btnApply.isEnabled = false
                            btnApply.text = "Applying..."
                        }
                        is ApplicationUiState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            btnApply.isEnabled = false
                            btnApply.text = "Applied"
                            requireActivity().onBackPressed()
                        }
                        is ApplicationUiState.Error -> {
                            showError(state.message)
                            btnApply.isEnabled = true
                        }
                        else -> {
                            btnApply.isEnabled = true
                            btnApply.text = "Apply Now"
                        }
                    }
                }
            }
        }

        btnApply.setOnClickListener {
            job?.let { jobData ->
                val studentId = auth.currentUser?.uid ?: return@setOnClickListener
                viewModel.applyForJob(jobData, studentId)
            }
        }
    }

    private fun showJobLoading() {
        // Show loading state
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
