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
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val imageUrl: String = "",
    val postedAt: Long = 0L,
    val employerId: String = "",
    val status: String = "active"
) : Serializable