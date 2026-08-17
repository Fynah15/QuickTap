package com.example.quicktap.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.quicktap.service.LocalNotificationService

class WorkshopReminderWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Workshop Reminder"
        val message = inputData.getString("message") ?: "Your workshop will start soon."

        // Call LocalNotificationService to display notification on the screen
        LocalNotificationService.showLocalNotification(applicationContext, title, message)

        return Result.success()
    }
}