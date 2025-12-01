package com.workit.workit.ui.theme.employer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.workit.workit.R
import com.workit.workit.data.Job
import com.workit.workit.ui.viewmodel.PostJobUiState
import com.workit.workit.ui.viewmodel.PostJobViewModel
import kotlinx.coroutines.launch

class EmployerOfferFragment : Fragment() {
    private val viewModel: PostJobViewModel by viewModels()
    private lateinit var positionInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var shiftInput: EditText
    private lateinit var requirementsInput: EditText
    private lateinit var postButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employer_offer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        positionInput = view.findViewById(R.id.et_position)
        locationInput = view.findViewById(R.id.et_location)
        descriptionInput = view.findViewById(R.id.et_description)
        shiftInput = view.findViewById(R.id.et_shift)
        requirementsInput = view.findViewById(R.id.et_requirements)
        postButton = view.findViewById(R.id.btn_post)

        // Observe post state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.postState.collect { state ->
                    when (state) {
                        is PostJobUiState.Loading -> {
                            postButton.isEnabled = false
                            postButton.text = "Posting..."
                        }
                        is PostJobUiState.Success -> {
                            Toast.makeText(context, "Job posted successfully!", Toast.LENGTH_SHORT).show()
                            clearForm()
                            viewModel.resetState()
                            postButton.isEnabled = true
                            postButton.text = "Post Job"
                        }
                        is PostJobUiState.Error -> {
                            showError(state.message)
                            postButton.isEnabled = true
                            postButton.text = "Post Job"
                        }
                        is PostJobUiState.Idle -> {
                            postButton.isEnabled = true
                            postButton.text = "Post Job"
                        }
                    }
                }
            }
        }

        postButton.setOnClickListener {
            postJobOffer()
        }
    }

    private fun postJobOffer() {
        val position = positionInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val shift = shiftInput.text.toString().trim()
        val requirements = requirementsInput.text.toString().split(",").map { it.trim() }

        val job = Job(
            position = position,
            location = location,
            description = description,
            shift = shift,
            requirements = requirements
        )

        viewModel.postJob(job)
    }

    private fun clearForm() {
        positionInput.text.clear()
        locationInput.text.clear()
        descriptionInput.text.clear()
        shiftInput.text.clear()
        requirementsInput.text.clear()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}