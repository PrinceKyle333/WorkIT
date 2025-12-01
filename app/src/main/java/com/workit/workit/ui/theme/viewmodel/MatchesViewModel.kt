package com.workit.workit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workit.workit.data.Match
import com.workit.workit.data.repository.MatchRepository
import com.workit.workit.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MatchesUiState {
    object Loading : MatchesUiState()
    data class Success(val matches: List<Match>) : MatchesUiState()
    data class Error(val message: String) : MatchesUiState()
}

sealed class MatchActionUiState {
    object Idle : MatchActionUiState()
    object Loading : MatchActionUiState()
    data class Success(val message: String) : MatchActionUiState()
    data class Error(val message: String) : MatchActionUiState()
}

class MatchesViewModel : ViewModel() {
    private val repository = MatchRepository()

    private val _studentMatches = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val studentMatches: StateFlow<MatchesUiState> = _studentMatches.asStateFlow()

    private val _employerMatches = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val employerMatches: StateFlow<MatchesUiState> = _employerMatches.asStateFlow()

    private val _actionState = MutableStateFlow<MatchActionUiState>(MatchActionUiState.Idle)
    val actionState: StateFlow<MatchActionUiState> = _actionState.asStateFlow()

    fun loadStudentMatches(studentId: String) {
        viewModelScope.launch {
            _studentMatches.value = MatchesUiState.Loading
            when (val result = repository.getStudentMatches(studentId)) {
                is Result.Success -> {
                    _studentMatches.value = MatchesUiState.Success(result.data)
                }
                is Result.Error -> {
                    _studentMatches.value = MatchesUiState.Error(
                        result.exception.message ?: "Failed to load matches"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadEmployerMatches(employerId: String) {
        viewModelScope.launch {
            _employerMatches.value = MatchesUiState.Loading
            when (val result = repository.getEmployerMatches(employerId)) {
                is Result.Success -> {
                    _employerMatches.value = MatchesUiState.Success(result.data)
                }
                is Result.Error -> {
                    _employerMatches.value = MatchesUiState.Error(
                        result.exception.message ?: "Failed to load applications"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateMatchStatus(matchId: String, status: String) {
        viewModelScope.launch {
            _actionState.value = MatchActionUiState.Loading
            when (val result = repository.updateMatchStatus(matchId, status)) {
                is Result.Success -> {
                    _actionState.value = MatchActionUiState.Success("Application $status")
                }
                is Result.Error -> {
                    _actionState.value = MatchActionUiState.Error(
                        result.exception.message ?: "Failed to update"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }
}