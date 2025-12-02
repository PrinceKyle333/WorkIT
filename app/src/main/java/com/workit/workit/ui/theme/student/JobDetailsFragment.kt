package com.workit.workit.ui.theme.student

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class JobDetailsFragment : Fragment() {
    private val viewModel: JobDetailsViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var job: Job? = null

    // Document URIs
    private var resumeUri: Uri? = null
    private var corUri: Uri? = null
    private var applicationLetterUri: Uri? = null

    // UI Elements
    private lateinit var btnSelectResume: Button
    private lateinit var btnSelectCOR: Button
    private lateinit var btnSelectApplicationLetter: Button
    private lateinit var tvResumeSelected: TextView
    private lateinit var tvCORSelected: TextView
    private lateinit var tvApplicationLetterSelected: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnApply: Button

    private val RESUME_REQUEST_CODE = 1001
    private val COR_REQUEST_CODE = 1002
    private val APPLICATION_LETTER_REQUEST_CODE = 1003

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

        btnSelectResume = view.findViewById(R.id.btn_select_resume)
        btnSelectCOR = view.findViewById(R.id.btn_select_cor)
        btnSelectApplicationLetter = view.findViewById(R.id.btn_select_application_letter)
        tvResumeSelected = view.findViewById(R.id.tv_resume_selected)
        tvCORSelected = view.findViewById(R.id.tv_cor_selected)
        tvApplicationLetterSelected = view.findViewById(R.id.tv_application_letter_selected)
        progressBar = view.findViewById(R.id.progress_bar)
        btnApply = view.findViewById(R.id.btn_apply)

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
                            tvRequirements.text = "Requirements:\n• Resume\n• Certificate of Registration (COR)\n• Application Letter"

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
                            btnApply.text = "Uploading..."
                            progressBar.visibility = View.VISIBLE
                        }
                        is ApplicationUiState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                            btnApply.isEnabled = false
                            btnApply.text = "Applied"
                            requireActivity().onBackPressed()
                        }
                        is ApplicationUiState.Error -> {
                            showError(state.message)
                            progressBar.visibility = View.GONE
                            btnApply.isEnabled = true
                        }
                        else -> {
                            btnApply.isEnabled = true
                            btnApply.text = "Apply Now"
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

        // Document selection buttons
        btnSelectResume.setOnClickListener {
            selectPDF(RESUME_REQUEST_CODE)
        }

        btnSelectCOR.setOnClickListener {
            selectPDF(COR_REQUEST_CODE)
        }

        btnSelectApplicationLetter.setOnClickListener {
            selectPDF(APPLICATION_LETTER_REQUEST_CODE)
        }

        btnApply.setOnClickListener {
            if (resumeUri == null || corUri == null || applicationLetterUri == null) {
                Toast.makeText(context, "Please upload all required documents", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            job?.let { jobData ->
                val studentId = auth.currentUser?.uid ?: return@setOnClickListener
                viewModel.applyForJobWithDocuments(
                    jobData,
                    studentId,
                    resumeUri!!,
                    corUri!!,
                    applicationLetterUri!!
                )
            }
        }
    }

    private fun selectPDF(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(context, "No file selected", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = data?.data ?: return
        val fileName = getFileName(uri) ?: "document.pdf"

        when (requestCode) {
            RESUME_REQUEST_CODE -> {
                resumeUri = uri
                tvResumeSelected.text = "✓ $fileName"
                tvResumeSelected.setTextColor(android.graphics.Color.GREEN)
            }
            COR_REQUEST_CODE -> {
                corUri = uri
                tvCORSelected.text = "✓ $fileName"
                tvCORSelected.setTextColor(android.graphics.Color.GREEN)
            }
            APPLICATION_LETTER_REQUEST_CODE -> {
                applicationLetterUri = uri
                tvApplicationLetterSelected.text = "✓ $fileName"
                tvApplicationLetterSelected.setTextColor(android.graphics.Color.GREEN)
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                val index = it.getColumnIndex("_display_name")
                if (index >= 0) it.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showJobLoading() {
        // Show loading state
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}