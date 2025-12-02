package com.workit.workit.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workit.workit.data.Job
import com.workit.workit.data.Student
import com.workit.workit.data.repository.JobRepository
import com.workit.workit.data.repository.MatchRepository
import com.workit.workit.data.repository.UserRepository
import com.workit.workit.data.repository.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

    fun applyForJobWithDocuments(
        job: Job,
        studentId: String,
        resumeUri: Uri,
        corUri: Uri,
        applicationLetterUri: Uri
    ) {
        viewModelScope.launch {
            _applicationState.value = ApplicationUiState.Loading

            try {
                // Get student data
                val studentResult = userRepository.getStudent(studentId)
                if (studentResult !is Result.Success) {
                    _applicationState.value = ApplicationUiState.Error("Failed to load student profile")
                    return@launch
                }

                val student = studentResult.data

                // Check if already applied
                val existing = db.collection("matches")
                    .whereEqualTo("jobId", job.id)
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    _applicationState.value = ApplicationUiState.Error("You already applied for this job")
                    return@launch
                }

                // Create match document first
                val matchData = hashMapOf(
                    "jobId" to job.id,
                    "studentId" to studentId,
                    "employerId" to job.employerId,
                    "studentName" to student.name,
                    "studentEmail" to student.email,
                    "employerName" to job.employer,
                    "position" to job.position,
                    "status" to "pending",
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "documents" to hashMapOf(
                        "resume" to hashMapOf("uploading" to true),
                        "cor" to hashMapOf("uploading" to true),
                        "applicationLetter" to hashMapOf("uploading" to true)
                    )
                )

                val matchRef = db.collection("matches").add(matchData).await()
                val matchId = matchRef.id

                // Upload documents to Firebase Storage
                val resumeUrl = uploadDocument(resumeUri, matchId, "resume")
                val corUrl = uploadDocument(corUri, matchId, "cor")
                val applicationLetterUrl = uploadDocument(applicationLetterUri, matchId, "applicationLetter")

                // Update match document with document URLs
                val documentsData = hashMapOf(
                    "documents" to hashMapOf(
                        "resume" to hashMapOf(
                            "fileName" to getFileName(resumeUri),
                            "fileUrl" to resumeUrl,
                            "uploadedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "fileSize" to getFileSize(resumeUri)
                        ),
                        "cor" to hashMapOf(
                            "fileName" to getFileName(corUri),
                            "fileUrl" to corUrl,
                            "uploadedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "fileSize" to getFileSize(corUri)
                        ),
                        "applicationLetter" to hashMapOf(
                            "fileName" to getFileName(applicationLetterUri),
                            "fileUrl" to applicationLetterUrl,
                            "uploadedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "fileSize" to getFileSize(applicationLetterUri)
                        )
                    )
                )

                db.collection("matches").document(matchId).update(documentsData as Map<String, Any>).await()
                // Add job to student's appliedJobs
                db.collection("students").document(studentId)
                    .update("appliedJobs", com.google.firebase.firestore.FieldValue.arrayUnion(job.id))
                    .await()

                _applicationState.value = ApplicationUiState.Success("Applied successfully with documents!")

            } catch (e: Exception) {
                _applicationState.value = ApplicationUiState.Error(e.message ?: "Failed to apply")
            }
        }
    }

    private suspend fun uploadDocument(uri: Uri, matchId: String, docType: String): String {
        val fileName = getFileName(uri) ?: "$docType.pdf"
        val storageRef = storage.reference
            .child("applications")
            .child(matchId)
            .child(docType)
            .child(fileName)

        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            val cursor = android.app.Application().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                val index = it.getColumnIndex("_display_name")
                if (index >= 0) it.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            val cursor = android.app.Application().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                val index = it.getColumnIndex("_size")
                if (index >= 0) it.getLong(index) else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}