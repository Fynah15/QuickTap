package com.example.quicktap.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object WorkshopReminderManager {

    private const val TAG = "WorkshopReminderManager"

    fun checkAndTriggerReminders(context: Context, studentId: String) {
        val firestore = FirebaseFirestore.getInstance()
        Log.d(TAG, "Memulakan semakan notifikasi untuk Student ID: $studentId")

        firestore.collection("registrations")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { regDocuments ->
                if (regDocuments.isEmpty) {
                    Log.d(TAG, "Tiada pendaftaran dijumpai untuk studentId: $studentId")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "Jumpa ${regDocuments.size()} pendaftaran bengkel.")

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
                                    val currentTimeMillis = System.currentTimeMillis()
                                    val diffMillis = workshopDateMillis - currentTimeMillis
                                    val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(workshopDateMillis))

                                    Log.d(TAG, "Bengkel: $workshopName | Jangka masa (jam): $diffHours")

                                    when {
                                        // 3 Hari (48 - 72 jam)
                                        diffHours in 48..72 -> {
                                            showLocalNotification(context, workshopName, "in 3 Days", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "in 3 Days", dateStr)
                                        }
                                        // 2 Hari (24 - 47 jam)
                                        diffHours in 24..47 -> {
                                            showLocalNotification(context, workshopName, "in 2 Days", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "in 2 Days", dateStr)
                                        }
                                        // 1 Hari / Esok (1 - 23 jam)
                                        diffHours in 1..23 -> {
                                            showLocalNotification(context, workshopName, "Tomorrow!", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "Tomorrow!", dateStr)
                                        }
                                        // Hari ini / Sedang berlangsung (Kurang dari 24 jam atau diffMillis positif kecil)
                                        diffMillis in 0..86400000 -> {
                                            showLocalNotification(context, workshopName, "Today!", dateStr)
                                            saveNotificationToFirestore(firestore, studentId, workshopName, "Today!", dateStr)
                                        }
                                        else -> {
                                            Log.d(TAG, "Tarikh bengkel telah lepas atau di luar julat syarat notifikasi.")
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "Gagal parse tarikh untuk bengkel: $workshopName")
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Ralat mendapatkan pendaftaran: ${e.message}")
                e.printStackTrace()
            }
    }

    private fun saveNotificationToFirestore(
        firestore: FirebaseFirestore,
        studentId: String,
        workshopName: String,
        timeRemaining: String,
        dateStr: String
    ) {
        val notificationData = hashMapOf(
            "title" to "Workshop Reminder: $workshopName",
            "message" to "Your workshop is starting $timeRemaining ($dateStr). Please get ready!",
            "timestamp" to System.currentTimeMillis(),
            "type" to "REMINDER"
        )

        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val docId = "rem_${studentId}_${workshopName.hashCode()}_$todayStr"

        firestore.collection("users").document(studentId)
            .collection("notifications").document(docId)
            .set(notificationData)
            .addOnSuccessListener {
                Log.d(TAG, "Notifikasi berjaya disimpan ke Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal simpan notifikasi ke Firestore: ${e.message}")
            }
    }

    private fun showLocalNotification(context: Context, workshopName: String, timeRemaining: String, dateStr: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Permission POST_NOTIFICATIONS tidak diberi.")
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
                .setContentText("Your workshop is starting $timeRemaining ($dateStr). Don't forget to attend!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationId = (System.currentTimeMillis() % 10000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            Log.d(TAG, "Local Notification berjaya dipaparkan untuk: $workshopName")
        } catch (e: Exception) {
            Log.e(TAG, "Ralat memaparkan notifikasi: ${e.message}")
            e.printStackTrace()
        }
    }
}