package com.example.quicktap.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object WorkshopReminderManager {

    fun checkAndTriggerReminders(context: Context, studentId: String) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("registrations")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { regDocuments ->

                if (regDocuments.isEmpty) {
                    return@addOnSuccessListener
                }

                for (regDoc in regDocuments) {
                    val workshopId = regDoc.getString("workshopId") ?: continue

                    firestore.collection("workshops").document(workshopId)
                        .get()
                        .addOnSuccessListener { workshopDoc ->
                            if (workshopDoc.exists()) {
                                val workshopName = workshopDoc.getString("name")
                                    ?: workshopDoc.getString("title")
                                    ?: "Workshop"

                                // Safely handle date as either Timestamp or String to prevent crash
                                val workshopDateMillis: Long? = when (val dateObj = workshopDoc.get("date") ?: workshopDoc.get("workshopDate")) {
                                    is com.google.firebase.Timestamp -> dateObj.toDate().time
                                    is String -> {
                                        var parsed: Long? = null
                                        val formats = listOf(
                                            SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH),
                                            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
                                            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                                        )
                                        for (fmt in formats) {
                                            try {
                                                parsed = fmt.parse(dateObj)?.time
                                                if (parsed != null) break
                                            } catch (e: Exception) {
                                                continue
                                            }
                                        }
                                        parsed
                                    }
                                    else -> null
                                }

                                if (workshopDateMillis != null) {
                                    val isDemoMode = false

                                    val currentTimeMillis = if (isDemoMode) {
                                        workshopDateMillis - TimeUnit.DAYS.toMillis(3)
                                    } else {
                                        System.currentTimeMillis()
                                    }

                                    // Calculate days remaining precisely
                                    val diffMillis = workshopDateMillis - currentTimeMillis
                                    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

                                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(workshopDateMillis))

                                    when {
                                        diffDays == 3L -> {
                                            showLocalNotification(context, workshopName, "in 3 Days", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "in 3 Days", dateStr)
                                        }
                                        diffDays == 2L -> {
                                            showLocalNotification(context, workshopName, "in 2 Days", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "in 2 Days", dateStr)
                                        }
                                        diffDays == 1L -> {
                                            showLocalNotification(context, workshopName, "Tomorrow!", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "Tomorrow!", dateStr)
                                        }
                                        diffDays == 0L && diffMillis > 0 -> {
                                            showLocalNotification(context, workshopName, "Today!", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "Today!", dateStr)
                                        }
                                    }
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun saveNotificationToFirestore(firestore: FirebaseFirestore, studentId: String, workshopName: String, timeRemaining: String, dateStr: String) {
        val notificationData = hashMapOf(
            "title" to "Workshop Reminder: $workshopName",
            "message" to "Your workshop is starting $timeRemaining ($dateStr). Please get ready!",
            "timestamp" to System.currentTimeMillis(),
            "type" to "REMINDER"
        )

        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val docId = "rem_${studentId}_${workshopName.hashCode()}_$todayStr"

        firestore.collection("users").document(studentId)
            .collection("notifications").document(docId).set(notificationData)
    }

    private fun showLocalNotification(context: Context, workshopName: String, timeRemaining: String, dateStr: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            val channelId = "workshop_reminder_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "QuickTap Workshop Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Automatic workshop attendance reminder notifications"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Workshop Reminder: $workshopName")
                .setContentText("$timeRemaining ($dateStr). Don't forget to attend!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationId = (System.currentTimeMillis() % 10000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}