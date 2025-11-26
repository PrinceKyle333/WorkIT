package com.workit.workit.data

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Match(
    @DocumentId
    val id: String = "",
    val jobId: String = "",
    val studentId: String = "",
    val employerId: String = "",
    val employerName: String = "",
    val position: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val createdAt: Long = 0L,
    val studentName: String = ""
) : Serializable
