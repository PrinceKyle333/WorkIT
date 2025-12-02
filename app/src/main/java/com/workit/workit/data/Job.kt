package com.workit.workit.data

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Job(
    @DocumentId
    val id: String = "",
    val employer: String = "",
    val position: String = "",
    val location: String = "",
    val shift: String = "",
    val shiftStart: String = "",
    val shiftEnd: String = "",
    val shiftStartHour: Int = 0,
    val shiftStartMinute: Int = 0,
    val shiftEndHour: Int = 0,
    val shiftEndMinute: Int = 0,
    val workDays: List<String> = emptyList(),
    val description: String = "",
    // Requirements are ALWAYS these 3 documents
    val requirements: List<String> = listOf(
        "Resume (PDF)",
        "Certificate of Registration (COR) (PDF)",
        "Application Letter (PDF)"
    ),
    val imageUrl: String = "",
    val postedAt: Long = 0L,
    val employerId: String = "",
    val status: String = "active",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable