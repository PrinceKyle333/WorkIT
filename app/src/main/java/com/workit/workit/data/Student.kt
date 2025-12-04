package com.workit.workit.data

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class TimeSlot(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = ""
) : Serializable

data class Student(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val idNumber: String = "",
    val username: String = "",
    val profilePictureUrl: String = "",
    val createdAt: Long = 0L,
    val appliedJobs: List<String> = emptyList(),
    val vacantSchedule: List<TimeSlot> = emptyList() // NEW: Student's available time slots
) : Serializable