package com.example.quicktap.utils

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.quicktap.worker.WorkshopReminderWorker
import java.util.concurrent.TimeUnit

fun scheduleWorkshopReminders(
    context: Context,
    workshopTitle: String,
    workshopDateTimeMillis: Long
) {
    val currentTime = System.currentTimeMillis()

    // ==========================================
    // UBAH BAHAGIAN INI UNTUK PRESENTATION / TEST
    // ==========================================
    val delay3 = 30 * 1000L  // 30 saat untuk notifikasi pertama
    val delay2 = 60 * 1000L  // 60 saat (1 minit) untuk notifikasi kedua
    val delay1 = 90 * 1000L  // 90 saat (1.5 minit) untuk notifikasi ketiga

    // Terus hantar ke WorkManager tanpa perlu tunggu hari sebenar
    enqueueWork(context, workshopTitle, "Peringatan: Bengkel '$workshopTitle' akan bermula!", delay3, "reminder_3d_$workshopTitle")
    enqueueWork(context, workshopTitle, "Peringatan: Bengkel '$workshopTitle' tinggal sebentar sahaja lagi!", delay2, "reminder_2d_$workshopTitle")
    enqueueWork(context, workshopTitle, "Peringatan: Bengkel '$workshopTitle' bermula SEKARANG! Sila bersedia.", delay1, "reminder_1d_$workshopTitle")
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