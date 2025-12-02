package com.workit.workit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workit.workit.data.Job
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class JobsUiState {
    object Loading : JobsUiState()
    data class Success(val jobs: List<Job>, val hasMore: Boolean = true) : JobsUiState()
    data class Error(val message: String) : JobsUiState()
}

class JobsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<JobsUiState>(JobsUiState.Loading)
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Job>>(emptyList())
    val searchResults: StateFlow<List<Job>> = _searchResults.asStateFlow()

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            try {
                _uiState.value = JobsUiState.Loading

                val snapshot = db.collection("jobs")
                    .whereEqualTo("status", "active")
                    .orderBy("postedAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val jobs = snapshot.documents.mapNotNull { doc ->
                    try {
                        Job(
                            id = doc.id,
                            employer = doc.getString("employer") ?: "",
                            employerId = doc.getString("employerId") ?: "",
                            position = doc.getString("position") ?: "",
                            location = doc.getString("location") ?: "",
                            shift = doc.getString("shift") ?: "",
                            shiftStart = doc.getString("shiftStart") ?: "",
                            shiftEnd = doc.getString("shiftEnd") ?: "",
                            workDays = doc.get("workDays") as? List<String> ?: emptyList(),
                            description = doc.getString("description") ?: "",
                            requirements = doc.get("requirements") as? List<String> ?: emptyList(),
                            imageUrl = doc.getString("imageUrl") ?: "",
                            postedAt = doc.getLong("postedAt") ?: System.currentTimeMillis(),
                            status = doc.getString("status") ?: "active",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("JobsViewModel", "Error parsing job", e)
                        null
                    }
                }

                android.util.Log.d("JobsViewModel", "Loaded ${jobs.size} jobs")
                _uiState.value = JobsUiState.Success(jobs)

            } catch (e: Exception) {
                android.util.Log.e("JobsViewModel", "Error loading jobs", e)
                _uiState.value = JobsUiState.Error(e.message ?: "Unknown error")
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
            try {
                val snapshot = db.collection("jobs")
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                val jobs = snapshot.documents.mapNotNull { doc ->
                    try {
                        Job(
                            id = doc.id,
                            employer = doc.getString("employer") ?: "",
                            employerId = doc.getString("employerId") ?: "",
                            position = doc.getString("position") ?: "",
                            location = doc.getString("location") ?: "",
                            shift = doc.getString("shift") ?: "",
                            shiftStart = doc.getString("shiftStart") ?: "",
                            shiftEnd = doc.getString("shiftEnd") ?: "",
                            workDays = doc.get("workDays") as? List<String> ?: emptyList(),
                            description = doc.getString("description") ?: "",
                            requirements = doc.get("requirements") as? List<String> ?: emptyList(),
                            imageUrl = doc.getString("imageUrl") ?: "",
                            postedAt = doc.getLong("postedAt") ?: System.currentTimeMillis(),
                            status = doc.getString("status") ?: "active",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filter { job ->
                    val matchesQuery = job.position.contains(query, ignoreCase = true) ||
                            job.description.contains(query, ignoreCase = true) ||
                            job.employer.contains(query, ignoreCase = true)
                    val matchesLocation = location.isEmpty() || job.location.contains(location, ignoreCase = true)
                    matchesQuery && matchesLocation
                }

                _searchResults.value = jobs
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}