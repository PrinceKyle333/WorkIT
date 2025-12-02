package com.workit.workit.data

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Application(
    @DocumentId
    val id: String = "",
    val jobId: String = "",
    val studentId: String = "",
    val employerId: String = "",
    val studentName: String = "",
    val employerName: String = "",
    val position: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val resumeUrl: String = "",
    val corUrl: String = "", // Certificate of Registration
    val applicationLetterUrl: String = "",
    val createdAt: Long = 0L
) : Serializable