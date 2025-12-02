package com.workit.workit.data.repository

import android.net.Uri
import com.workit.workit.data.Match
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MatchRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getStudentMatches(studentId: String): Result<List<Match>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = db.collection("matches")
                .whereEqualTo("studentId", studentId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val matches = snapshot.mapNotNull { doc -> doc.toObject(Match::class.java)?.copy(id = doc.id) }
            Result.Success(matches)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun getEmployerMatches(employerId: String): Result<List<Match>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val snapshot = db.collection("matches")
                .whereEqualTo("employerId", employerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val matches = snapshot.mapNotNull { doc -> doc.toObject(Match::class.java)?.copy(id = doc.id) }
            Result.Success(matches)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun applyForJob(job: com.workit.workit.data.Job, student: com.workit.workit.data.Student): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val studentId = auth.currentUser?.uid ?: return@withContext Result.Error(Exception("Not authenticated"))

            // Check if already applied
            val existing = db.collection("matches")
                .whereEqualTo("jobId", job.id)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            if (!existing.isEmpty) {
                return@withContext Result.Error(Exception("You already applied for this job"))
            }

            val matchData = Match(
                jobId = job.id,
                studentId = studentId,
                employerId = job.employerId,
                studentName = student.name,
                employerName = job.employer,
                position = job.position,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )

            val docRef = db.collection("matches").add(matchData).await()
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun updateMatchStatus(matchId: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (status !in listOf("pending", "accepted", "rejected")) {
                return@withContext Result.Error(Exception("Invalid status"))
            }
            db.collection("matches").document(matchId).update("status", status).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun downloadDocument(matchId: String, documentType: String): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val storageRef = storage.reference
                .child("applications")
                .child(matchId)
                .child(documentType)

            val downloadUrl = storageRef.downloadUrl.await()
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun getMatchDocuments(matchId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = db.collection("matches").document(matchId).get().await()
            val documents = doc.get("documents") as? Map<String, Any> ?: emptyMap()
            Result.Success(documents)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }
}