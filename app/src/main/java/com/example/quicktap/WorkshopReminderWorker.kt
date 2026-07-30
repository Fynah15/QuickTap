package com.example.quicktap.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.quicktap.service.LocalNotificationService

class WorkshopReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Peringatan Bengkel"
        val message = inputData.getString("message") ?: "Bengkel anda akan bermula tidak lama lagi."

        // Panggil LocalNotificationService untuk memaparkan notifikasi pada skrin
        LocalNotificationService.showLocalNotification(applicationContext, title, message)

        return Result.success()
    }
}