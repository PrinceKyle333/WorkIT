package com.workit.workit.data

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Notification(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val type: String = "", // job_offer, match_accepted, match_rejected, new_application
    val title: String = "",
    val message: String = "",
    val relatedJobId: String = "",
    val relatedMatchId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L
) : Serializable
