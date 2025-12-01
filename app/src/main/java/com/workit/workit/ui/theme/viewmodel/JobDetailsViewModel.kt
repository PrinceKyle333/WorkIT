package com.workit.workit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workit.workit.data.Job
import com.workit.workit.data.Student
import com.workit.workit.data.repository.JobRepository
import com.workit.workit.data.repository.MatchRepository
import com.workit.workit.data.repository.UserRepository
import com.workit.workit.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class JobDetailsUiState {
    object Loading : JobDetailsUiState()
    data class Success(val job: Job) : JobDetailsUiState()
    data class Error(val message: String) : JobDetailsUiState()
}

sealed class ApplicationUiState {
    object Idle : ApplicationUiState()
    object Loading : ApplicationUiState()
    data class Success(val message: String) : ApplicationUiState()
    data class Error(val message: String) : ApplicationUiState()
}

class JobDetailsViewModel : ViewModel() {
    private val jobRepository = JobRepository()
    private val matchRepository = MatchRepository()
    private val userRepository = UserRepository()

    private val _jobState = MutableStateFlow<JobDetailsUiState>(JobDetailsUiState.Loading)
    val jobState: StateFlow<JobDetailsUiState> = _jobState.asStateFlow()

    private val _applicationState = MutableStateFlow<ApplicationUiState>(ApplicationUiState.Idle)
    val applicationState: StateFlow<ApplicationUiState> = _applicationState.asStateFlow()

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            _jobState.value = JobDetailsUiState.Loading
            when (val result = jobRepository.getJobById(jobId)) {
                is Result.Success -> {
                    _jobState.value = JobDetailsUiState.Success(result.data)
                }
                is Result.Error -> {
                    _jobState.value = JobDetailsUiState.Error(result.exception.message ?: "Job not found")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun applyForJob(job: Job, studentId: String) {
        viewModelScope.launch {
            _applicationState.value = ApplicationUiState.Loading

            when (val userResult = userRepository.getStudent(studentId)) {
                is Result.Success -> {
                    val student = userResult.data
                    when (val applyResult = matchRepository.applyForJob(job, student)) {
                        is Result.Success -> {
                            _applicationState.value = ApplicationUiState.Success("Applied successfully!")
                        }
                        is Result.Error -> {
                            _applicationState.value = ApplicationUiState.Error(
                                applyResult.exception.message ?: "Failed to apply"
                            )
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    _applicationState.value = ApplicationUiState.Error("Failed to load student profile")
                }
                is Result.Loading -> {}
            }
        }
    }
}
