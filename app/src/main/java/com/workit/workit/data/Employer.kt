package com.workit.workit.data

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Employer(
    @DocumentId
    val id: String = "",
    val companyName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val logoUrl: String = "",
    val description: String = "",
    val createdAt: Long = 0L,
    val postedJobs: List<String> = emptyList()
) : Serializable
