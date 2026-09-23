package com.example.quicktap

import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

data class LiveStudentRow(
    val studentKey: String,
    var name: String,
    var checkIn: String,
    var checkOut: String
)

object AttendanceUtils {

    fun resolveStudentName(doc: DocumentSnapshot, fallbackId: String = "Student"): String {
        val rawName = doc.getString("studentName")
            ?: doc.getString("fullName")
            ?: doc.getString("name")
            ?: doc.getString("userName")
            ?: fallbackId

        val isHexId = rawName.matches(Regex("^[A-Fa-f0-9]{8,}$")) ||
                rawName.matches(Regex("^[A-Fa-f0-9]{14,}$"))

        val isInvalidName = rawName.isBlank() ||
                rawName == fallbackId ||
                isHexId ||
                rawName.length > 40

        return if (!isInvalidName) rawName else fallbackId
    }

    fun isEligibleForCertificate(regDoc: DocumentSnapshot): Boolean {
        val hasCheckedIn = hasNfcCheckIn(regDoc)
        val hasCheckedOut = hasNfcCheckOut(regDoc)
        val isSent = regDoc.getString("certificateStatus") == "Sent"

        return (hasCheckedIn && hasCheckedOut) || isSent
    }

    /**
     * Syarat check-in diselaraskan agar menyokong pelbagai variasi medan Firestore
     * (checkInTimestamp, checkInTime, timestamp umum, atau mode Check-In)
     */
    fun hasNfcCheckIn(doc: DocumentSnapshot): Boolean {
        val hasTimeField = doc.get("checkInTimestamp") != null ||
                doc.get("checkInTime") != null ||
                doc.get("timestamp") != null
        val mode = doc.getString("mode")

        return hasTimeField || mode == "Check-In" || mode == "present"
    }

    /**
     * Syarat check-out diselaraskan untuk mengesan sebarang medan masa keluar yang wujud
     */
    fun hasNfcCheckOut(doc: DocumentSnapshot): Boolean {
        return doc.get("checkOutTime") != null ||
                doc.get("checkOutTimestamp") != null ||
                doc.get("timeout") != null ||
                doc.getString("mode") == "Check-Out"
    }

    fun formatTimestamp(timestampObj: Any?): String {
        if (timestampObj == null) return "-"
        return try {
            when (timestampObj) {
                is com.google.firebase.Timestamp -> {
                    val date = timestampObj.toDate()
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
                }
                is java.util.Date -> {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestampObj)
                }
                is String -> {
                    if (timestampObj.isNotBlank()) timestampObj else "-"
                }
                else -> "-"
            }
        } catch (e: Exception) {
            "-"
        }
    }
}