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

sealed class JobsUiState {
    object Loading : JobsUiState()
    data class Success(val jobs: List<Job>, val hasMore: Boolean = true) : JobsUiState()
    data class Error(val message: String) : JobsUiState()
}

class JobsViewModel : ViewModel() {
    private val repository = JobRepository()

    private val _uiState = MutableStateFlow<JobsUiState>(JobsUiState.Loading)
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Job>>(emptyList())
    val searchResults: StateFlow<List<Job>> = _searchResults.asStateFlow()

    private var currentPage = 0
    private var allJobs = mutableListOf<Job>()

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            _uiState.value = JobsUiState.Loading
            currentPage = 0
            allJobs.clear()
            repository.resetPagination()

            when (val result = repository.getActiveJobs(0)) {
                is Result.Success -> {
                    allJobs.addAll(result.data)
                    _uiState.value = JobsUiState.Success(allJobs.toList(), result.data.size == 10)
                }
                is Result.Error -> {
                    _uiState.value = JobsUiState.Error(result.exception.message ?: "Unknown error")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadMoreJobs() {
        viewModelScope.launch {
            currentPage++
            when (val result = repository.getActiveJobs(currentPage)) {
                is Result.Success -> {
                    allJobs.addAll(result.data)
                    _uiState.value = JobsUiState.Success(allJobs.toList(), result.data.size == 10)
                }
                is Result.Error -> {
                    currentPage--
                    _uiState.value = JobsUiState.Error(result.exception.message ?: "Failed to load more jobs")
                }
                is Result.Loading -> {}
            }
        }
    }

    fun searchJobs(query: String, location: String = "") {
        _searchQuery.value = query

        if (query.isEmpty() && location.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            when (val result = repository.searchJobs(query, location)) {
                is Result.Success -> {
                    _searchResults.value = result.data
                }
                is Result.Error -> {
                    _searchResults.value = emptyList()
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}