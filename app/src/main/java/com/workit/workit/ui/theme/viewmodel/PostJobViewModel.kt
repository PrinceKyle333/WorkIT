package com.workit.workit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workit.workit.data.Job
import com.workit.workit.data.repository.JobRepository
import com.workit.workit.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PostJobUiState {
    object Idle : PostJobUiState()
    object Loading : PostJobUiState()
    data class Success(val jobId: String) : PostJobUiState()
    data class Error(val message: String) : PostJobUiState()
}

class PostJobViewModel : ViewModel() {
    private val repository = JobRepository()

    private val _postState = MutableStateFlow<PostJobUiState>(PostJobUiState.Idle)
    val postState: StateFlow<PostJobUiState> = _postState.asStateFlow()

    fun postJob(job: Job) {
        viewModelScope.launch {
            _postState.value = PostJobUiState.Loading
            when (val result = repository.postJob(job)) {
                is Result.Success -> {
                    _postState.value = PostJobUiState.Success(result.data)
                }
                is Result.Error -> {
                    _postState.value = PostJobUiState.Error(
                        result.exception.message ?: "Failed to post job"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun resetState() {
        _postState.value = PostJobUiState.Idle
    }
}