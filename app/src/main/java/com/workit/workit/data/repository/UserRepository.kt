package com.workit.workit.data.repository

import com.workit.workit.data.Student
import com.workit.workit.data.Employer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getStudent(userId: String): Result<Student> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = db.collection("students").document(userId).get().await()
            val student = doc.toObject(Student::class.java)?.copy(id = doc.id)
            if (student != null) Result.Success(student) else Result.Error(Exception("Student not found"))
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun getEmployer(userId: String): Result<Employer> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = db.collection("employers").document(userId).get().await()
            val employer = doc.toObject(Employer::class.java)?.copy(id = doc.id)
            if (employer != null) Result.Success(employer) else Result.Error(Exception("Employer not found"))
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun updateStudent(userId: String, updates: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("students").document(userId).update(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }

    suspend fun updateEmployer(userId: String, updates: Map<String, Any>): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            db.collection("employers").document(userId).update(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e as Exception)
        }
    }
}