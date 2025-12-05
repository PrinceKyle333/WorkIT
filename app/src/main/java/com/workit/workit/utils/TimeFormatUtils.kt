package com.workit.workit.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for uniform time formatting across the application
 * Ensures consistent time display for student schedules and employer shifts
 */
object TimeFormatUtils {

    // Standard 24-hour format for storage (HH:mm)
    private const val STORAGE_FORMAT = "HH:mm"

    // 12-hour format for display (hh:mm a)
    private const val DISPLAY_FORMAT_12H = "hh:mm a"

    /**
     * Formats time from 24-hour format to 12-hour display format
     * Input: "14:30" -> Output: "02:30 PM"
     */
    fun formatTo12Hour(time24: String): String {
        return try {
            val parser = SimpleDateFormat(STORAGE_FORMAT, Locale.getDefault())
            val formatter = SimpleDateFormat(DISPLAY_FORMAT_12H, Locale.getDefault())
            val date = parser.parse(time24)
            formatter.format(date ?: return time24).uppercase()
        } catch (e: Exception) {
            time24
        }
    }

    /**
     * Formats time from hour and minute to storage format
     * Input: hour=14, minute=30 -> Output: "14:30"
     */
    fun formatToStorage(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Formats time range for display
     * Input: "09:00", "17:00" -> Output: "09:00 AM - 05:00 PM"
     */
    fun formatTimeRange(startTime: String, endTime: String): String {
        val start = formatTo12Hour(startTime)
        val end = formatTo12Hour(endTime)
        return "$start - $end"
    }

    /**
     * Formats day and time range for display
     * Input: "Monday", "09:00", "17:00" -> Output: "Monday 09:00 AM - 05:00 PM"
     */
    fun formatDayTimeRange(day: String, startTime: String, endTime: String): String {
        return "$day ${formatTimeRange(startTime, endTime)}"
    }

    /**
     * Formats shift schedule for job cards
     * Input: "Monday", "09:00", "17:00" -> Output: "Shift: Monday 9:00 AM - 5:00 PM"
     */
    fun formatShiftDisplay(day: String, startTime: String, endTime: String): String {
        val start = formatTo12Hour(startTime).replace(":00", "")
        val end = formatTo12Hour(endTime).replace(":00", "")
        return "Shift: $day $start - $end"
    }

    /**
     * Formats multiple time slots for display
     */
    fun formatMultipleTimeSlots(timeSlots: List<Pair<String, Pair<String, String>>>): String {
        return timeSlots.joinToString("\n") { (day, times) ->
            "• ${formatDayTimeRange(day, times.first, times.second)}"
        }
    }

    /**
     * Parse hour from time string
     * Input: "14:30" -> Output: 14
     */
    fun parseHour(time: String): Int {
        return try {
            time.split(":")[0].toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Parse minute from time string
     * Input: "14:30" -> Output: 30
     */
    fun parseMinute(time: String): Int {
        return try {
            time.split(":")[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Validates time format (HH:mm)
     */
    fun isValidTimeFormat(time: String): Boolean {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return false
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            hour in 0..23 && minute in 0..59
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if two time ranges overlap
     */
    fun timeRangesOverlap(
        start1: String, end1: String,
        start2: String, end2: String
    ): Boolean {
        val s1 = parseHour(start1) * 60 + parseMinute(start1)
        val e1 = parseHour(end1) * 60 + parseMinute(end1)
        val s2 = parseHour(start2) * 60 + parseMinute(start2)
        val e2 = parseHour(end2) * 60 + parseMinute(end2)

        return s1 < e2 && s2 < e1
    }

    /**
     * Formats time for job card display (compact format)
     * Input: "09:00" -> Output: "9:00 AM"
     */
    fun formatCompact(time: String): String {
        return formatTo12Hour(time).replace(":00 ", " ")
    }
}