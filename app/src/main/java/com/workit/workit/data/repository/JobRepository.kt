package com.workit.workit.data.repository

import com.workit.workit.data.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class JobRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var lastJobDocument: DocumentSnapshot? = null
    private val pageSize = 10

    suspend fun getActiveJobs(pageNumber: Int = 0): Result<List<Job>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val query = if (pageNumber == 0) {
                db.collection("jobs")
                    .whereEqualTo("status", "active")
                    .orderBy("postedAt", Query.Direction.DESCENDING)
                    .limit(pageSize.toLong())
            } else {
                if (lastJobDocument == null) return@withContext Result.Error(Exception("No more jobs available"))
                db.collection("jobs")
                    .whereEqualTo("status", "active")
                    .orderBy("postedAt", Query.Direction.DESCENDING)
                    .startAfter(lastJobDocument)
                    .limit(pageSize.toLong())
            }

            val snapshot = query.get().await()
            lastJobDocument = snapshot.documents.lastOrNull()
            val jobs = snapshot.mapNotNull { doc -> doc.toObject(Job::class.java)?.copy(id = doc.id) }
            Result.Success(jobs)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun getJobById(jobId: String): Result<Job> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = db.collection("jobs").document(jobId).get().await()
            val job = doc.toObject(Job::class.java)?.copy(id = doc.id)
            if (job != null) Result.Success(job) else Result.Error(Exception("Job not found"))
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun getEmployerJobs(employerId: String): Result<List<Job>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = db.collection("jobs")
                .whereEqualTo("employerId", employerId)
                .orderBy("postedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val jobs = snapshot.mapNotNull { doc -> doc.toObject(Job::class.java)?.copy(id = doc.id) }
            Result.Success(jobs)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun postJob(job: Job): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val validation = validateJob(job)
            if (!validation.first) {
                return@withContext Result.Error(Exception(validation.second))
            }

            val employerId = auth.currentUser?.uid ?: return@withContext Result.Error(Exception("Not authenticated"))
            val jobData = job.copy(
                employerId = employerId,
                employer = auth.currentUser?.email ?: "",
                postedAt = System.currentTimeMillis(),
                status = "active"
            )

            val docRef = db.collection("jobs").add(jobData).await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun updateJob(jobId: String, updates: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("jobs").document(jobId).update(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun deleteJob(jobId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val employerId = auth.currentUser?.uid ?: return@withContext Result.Error(Exception("Not authenticated"))
            val job = db.collection("jobs").document(jobId).get().await()

            if (job.getString("employerId") != employerId) {
                return@withContext Result.Error(Exception("Unauthorized"))
            }

            db.collection("jobs").document(jobId).update("status", "deleted").await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun searchJobs(query: String, location: String = ""): Result<List<Job>> = withContext(Dispatchers.IO) {
        return@withContext try {
            var dbQuery = db.collection("jobs").whereEqualTo("status", "active")

            val snapshot = dbQuery.get().await()
            val jobs = snapshot.mapNotNull { doc -> doc.toObject(Job::class.java)?.copy(id = doc.id) }
                .filter { job ->
                    val matchesQuery = job.position.contains(query, ignoreCase = true) ||
                            job.description.contains(query, ignoreCase = true)
                    val matchesLocation = location.isEmpty() || job.location.contains(location, ignoreCase = true)
                    matchesQuery && matchesLocation
                }

            Result.Success(jobs)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    private fun validateJob(job: Job): Pair<Boolean, String> {
        return when {
            job.position.trim().isEmpty() -> Pair(false, "Position cannot be empty")
            job.position.length > 100 -> Pair(false, "Position must be under 100 characters")
            job.location.trim().isEmpty() -> Pair(false, "Location cannot be empty")
            job.description.trim().isEmpty() -> Pair(false, "Description cannot be empty")
            job.description.length < 20 -> Pair(false, "Description must be at least 20 characters")
            job.shift.trim().isEmpty() -> Pair(false, "Shift cannot be empty")
            else -> Pair(true, "")
        }
    }

    fun resetPagination() {
        lastJobDocument = null
    }
}