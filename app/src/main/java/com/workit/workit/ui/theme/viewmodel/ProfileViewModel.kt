package com.workit.workit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workit.workit.data.Student
import com.workit.workit.data.Employer
import com.workit.workit.auth.RoleManager
import com.workit.workit.data.repository.UserRepository
import com.workit.workit.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class StudentLoaded(val student: Student) : ProfileUiState()
    data class EmployerLoaded(val employer: Employer) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class ProfileUpdateUiState {
    object Idle : ProfileUpdateUiState()
    object Loading : ProfileUpdateUiState()
    data class Success(val message: String) : ProfileUpdateUiState()
    data class Error(val message: String) : ProfileUpdateUiState()
}

class ProfileViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<ProfileUpdateUiState>(ProfileUpdateUiState.Idle)
    val updateState: StateFlow<ProfileUpdateUiState> = _updateState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            val role = RoleManager.getUserRole()

            when (role) {
                RoleManager.UserRole.STUDENT -> {
                    when (val result = repository.getStudent(userId)) {
                        is Result.Success -> {
                            _profileState.value = ProfileUiState.StudentLoaded(result.data)
                        }
                        is Result.Error -> {
                            _profileState.value = ProfileUiState.Error(
                                result.exception.message ?: "Failed to load profile"
                            )
                        }
                        is Result.Loading -> {}
                    }
                }
                RoleManager.UserRole.EMPLOYER -> {
                    when (val result = repository.getEmployer(userId)) {
                        is Result.Success -> {
                            _profileState.value = ProfileUiState.EmployerLoaded(result.data)
                        }
                        is Result.Error -> {
                            _profileState.value = ProfileUiState.Error(
                                result.exception.message ?: "Failed to load profile"
                            )
                        }
                        is Result.Loading -> {}
                    }
                }
                else -> {
                    _profileState.value = ProfileUiState.Error("Unknown user role")
                }
            }
        }
    }

    fun updateStudentProfile(userId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateUiState.Loading
            when (val result = repository.updateStudent(userId, updates)) {
                is Result.Success -> {
                    _updateState.value = ProfileUpdateUiState.Success("Profile updated successfully")
                }
                is Result.Error -> {
                    _updateState.value = ProfileUpdateUiState.Error(
                        result.exception.message ?: "Failed to update profile"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateEmployerProfile(userId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateUiState.Loading
            when (val result = repository.updateEmployer(userId, updates)) {
                is Result.Success -> {
                    _updateState.value = ProfileUpdateUiState.Success("Profile updated successfully")
                }
                is Result.Error -> {
                    _updateState.value = ProfileUpdateUiState.Error(
                        result.exception.message ?: "Failed to update profile"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }
}