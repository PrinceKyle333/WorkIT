package com.workit.workit.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object RoleManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    enum class UserRole {
        STUDENT, EMPLOYER, ADMIN, UNKNOWN
    }

    object StudentPrivileges {
        const val VIEW_JOBS = "view_jobs"
        const val APPLY_JOBS = "apply_jobs"
        const val VIEW_MATCHES = "view_matches"
        const val EDIT_PROFILE = "edit_profile"
        const val VIEW_APPLICATIONS = "view_applications"
    }

    object EmployerPrivileges {
        const val POST_JOBS = "post_jobs"
        const val EDIT_JOBS = "edit_jobs"
        const val DELETE_JOBS = "delete_jobs"
        const val VIEW_APPLICANTS = "view_applicants"
        const val MANAGE_MATCHES = "manage_matches"
        const val EDIT_PROFILE = "edit_profile"
    }

    suspend fun getUserRole(): UserRole {
        return try {
            val userId = auth.currentUser?.uid ?: return UserRole.UNKNOWN

            val studentDoc = db.collection("students").document(userId).get().await()
            if (studentDoc.exists()) {
                val role = studentDoc.getString("role")
                if (role == "student") {
                    return UserRole.STUDENT
                }
            }

            val employerDoc = db.collection("employers").document(userId).get().await()
            if (employerDoc.exists()) {
                val role = employerDoc.getString("role")
                if (role == "employer") {
                    return UserRole.EMPLOYER
                }
            }

            UserRole.UNKNOWN
        } catch (e: Exception) {
            UserRole.UNKNOWN
        }
    }

    suspend fun hasPrivilege(privilege: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val role = getUserRole()

            when (role) {
                UserRole.STUDENT -> {
                    val studentPrivileges = listOf(
                        StudentPrivileges.VIEW_JOBS,
                        StudentPrivileges.APPLY_JOBS,
                        StudentPrivileges.VIEW_MATCHES,
                        StudentPrivileges.EDIT_PROFILE,
                        StudentPrivileges.VIEW_APPLICATIONS
                    )
                    studentPrivileges.contains(privilege)
                }
                UserRole.EMPLOYER -> {
                    val employerPrivileges = listOf(
                        EmployerPrivileges.POST_JOBS,
                        EmployerPrivileges.EDIT_JOBS,
                        EmployerPrivileges.DELETE_JOBS,
                        EmployerPrivileges.VIEW_APPLICANTS,
                        EmployerPrivileges.MANAGE_MATCHES,
                        EmployerPrivileges.EDIT_PROFILE
                    )
                    employerPrivileges.contains(privilege)
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserCollection(): String {
        return when (getUserRole()) {
            UserRole.STUDENT -> "students"
            UserRole.EMPLOYER -> "employers"
            else -> "unknown"
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun logout() {
        auth.signOut()
    }
}