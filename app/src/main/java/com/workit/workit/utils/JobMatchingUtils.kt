package com.workit.workit.utils

import com.workit.workit.data.Job
import com.workit.workit.data.Student
import com.workit.workit.data.TimeSlot

object JobMatchingUtils {

    /**
     * Checks if a job's shift matches any of the student's vacant schedule
     */
    fun isJobMatchingStudentSchedule(job: Job, student: Student): Boolean {
        // If student has no schedule, show all jobs
        if (student.vacantSchedule.isEmpty()) {
            return true
        }

        // Check each vacant time slot
        for (timeSlot in student.vacantSchedule) {
            if (doesShiftMatchTimeSlot(job, timeSlot)) {
                return true
            }
        }

        return false
    }

    /**
     * Checks if job shift falls within a specific time slot
     */
    private fun doesShiftMatchTimeSlot(job: Job, timeSlot: TimeSlot): Boolean {
        return try {
            // Parse job shift times (24-hour format: HH:mm)
            val jobStart = parseTime(job.shiftStart)
            val jobEnd = parseTime(job.shiftEnd)

            // Parse student vacant time slot (24-hour format: HH:mm)
            val slotStart = parseTime(timeSlot.startTime)
            val slotEnd = parseTime(timeSlot.endTime)

            // Check if job shift falls completely within student's vacant slot
            // Job must start at or after slot start AND end at or before slot end
            jobStart >= slotStart && jobEnd <= slotEnd

        } catch (e: Exception) {
            android.util.Log.e("JobMatching", "Error matching shift: ${e.message}", e)
            // On error, don't match (safer)
            false
        }
    }

    /**
     * Parse time string (HH:mm) to minutes since midnight
     */
    private fun parseTime(timeString: String): Int {
        val parts = timeString.trim().split(":")
        if (parts.size != 2) return 0

        val hour = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0

        return hour * 60 + minute
    }

    /**
     * Format time in 12-hour format with AM/PM
     */
    fun formatTime12Hour(timeString: String): String {
        return try {
            val parts = timeString.trim().split(":")
            if (parts.size != 2) return timeString

            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0

            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }

            String.format("%d:%02d %s", displayHour, minute, amPm)
        } catch (e: Exception) {
            timeString
        }
    }

    /**
     * Format shift display with AM/PM
     */
    fun formatShiftDisplay(job: Job): String {
        return try {
            val startFormatted = formatTime12Hour(job.shiftStart)
            val endFormatted = formatTime12Hour(job.shiftEnd)
            "Shift: $startFormatted - $endFormatted"
        } catch (e: Exception) {
            "Shift: ${job.shift}"
        }
    }
}