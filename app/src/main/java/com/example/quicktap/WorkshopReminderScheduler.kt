package com.example.quicktap.utils

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.quicktap.worker.WorkshopReminderWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

fun scheduleWorkshopReminders(
    context: Context,
    workshopId: String,
    workshopTitle: String,
    workshopDate: String,
    workshopTime: String,
    studentId: String
) {
    val workshopStartMillis = parseWorkshopStartMillis(workshopDate, workshopTime)
    if (workshopStartMillis <= 0L) return

    val now = System.currentTimeMillis()
    val tagPrefix = "${workshopId}_$studentId"

    scheduleIfFuture(context, workshopTitle,
        "Peringatan: Bengkel '$workshopTitle' akan bermula dalam 3 hari!",
        "Reminder: Workshop '$workshopTitle' starts in 3 days!",
        workshopStartMillis - (3 * ONE_DAY_MS) - now,
        "reminder_3d_$tagPrefix")

    scheduleIfFuture(context, workshopTitle,
        "Peringatan: Bengkel '$workshopTitle' akan bermula dalam 2 hari!",
        "Reminder: Workshop '$workshopTitle' starts in 2 days!",
        workshopStartMillis - (2 * ONE_DAY_MS) - now,
        "reminder_2d_$tagPrefix")

    scheduleIfFuture(context, workshopTitle,
        "Peringatan: Bengkel '$workshopTitle' bermula ESOK!",
        "Reminder: Workshop '$workshopTitle' is TOMORROW!",
        workshopStartMillis - ONE_DAY_MS - now,
        "reminder_1d_$tagPrefix")
}

fun cancelWorkshopReminders(context: Context, workshopId: String, studentId: String) {
    val wm = WorkManager.getInstance(context)
    val tagPrefix = "${workshopId}_$studentId"
    wm.cancelUniqueWork("reminder_3d_$tagPrefix")
    wm.cancelUniqueWork("reminder_2d_$tagPrefix")
    wm.cancelUniqueWork("reminder_1d_$tagPrefix")
}

private fun scheduleIfFuture(
    context: Context,
    title: String,
    messageMs: String,
    messageEn: String,
    delayMs: Long,
    uniqueTag: String
) {
    if (delayMs <= 0L) return
    val message = messageMs // or pick by AppSettingsState if needed
    enqueueWork(context, title, message, delayMs, uniqueTag)
}

private fun enqueueWork(context: Context, title: String, message: String, delay: Long, uniqueTag: String) {
    val inputData = Data.Builder()
        .putString("title", title)
        .putString("message", message)
        .build()

    val reminderRequest = OneTimeWorkRequestBuilder<WorkshopReminderWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(inputData)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        uniqueTag,
        ExistingWorkPolicy.REPLACE,
        reminderRequest
    )
}

private fun parseWorkshopStartMillis(dateStr: String, timeStr: String): Long {
    if (dateStr.isBlank()) return 0L
    val cleanedDate = dateStr.replace(
        Regex("(?i)^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),?\\s*"), ""
    ).trim()

    val dateFormats = listOf(
        SimpleDateFormat("yyyy-M-d", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
    )

    var parsedDate: java.util.Date? = null
    for (f in dateFormats) {
        f.isLenient = true
        try { parsedDate = f.parse(cleanedDate); if (parsedDate != null) break } catch (_: Exception) {}
    }
    if (parsedDate == null) return 0L

    val cal = Calendar.getInstance().apply { time = parsedDate }
    val startPart = timeStr.split("-").firstOrNull()?.trim() ?: timeStr
    parseTimeTo24h(startPart)?.let { (hour, minute) ->
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun parseTimeTo24h(timeStr: String): Pair<Int, Int>? {
    return try {
        val trimmed = timeStr.trim()
        val isPm = trimmed.contains("PM", ignoreCase = true)
        val isAm = trimmed.contains("AM", ignoreCase = true)
        val numbers = trimmed.replace(Regex("[^0-9:]"), "")
        val parts = numbers.split(":")
        var hour = parts[0].toInt()
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        hour to minute
    } catch (_: Exception) { null }
}