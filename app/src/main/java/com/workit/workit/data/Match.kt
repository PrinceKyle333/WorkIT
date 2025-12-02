package com.workit.workit.data

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class DocumentInfo(
    val fileName: String = "",
    val fileUrl: String = "",
    val uploadedAt: Long = 0L,
    val fileSize: Long = 0L
) : Serializable

data class ApplicationDocuments(
    val resume: DocumentInfo = DocumentInfo(),
    val cor: DocumentInfo = DocumentInfo(),
    val applicationLetter: DocumentInfo = DocumentInfo()
) : Serializable

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
    val studentName: String = "",
    val studentEmail: String = "",
    val documents: ApplicationDocuments = ApplicationDocuments()
) : Serializable