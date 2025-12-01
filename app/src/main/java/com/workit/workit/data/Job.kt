
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
    val workDays: List<String> = emptyList(),
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val imageUrl: String = "",
    val postedAt: Long = 0L,
    val employerId: String = "",
    val status: String = "active",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable