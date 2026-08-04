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

    /**
     * Menyelesaikan dan mendapatkan nama pelajar yang sah daripada dokumen Firestore.
     * Mengelakkan paparan ID Firestore, UID hexadecimal kad NFC, atau string kosong.
     */
    fun resolveStudentName(doc: DocumentSnapshot, fallbackId: String = "Student"): String {
        val rawName = doc.getString("studentName")
            ?: doc.getString("fullName")
            ?: doc.getString("name")
            ?: doc.getString("userName")
            ?: fallbackId

        // A valid name shouldn't be just a hex string of length 8, 14, 16 etc (NFC UIDs)
        val isHexId = rawName.matches(Regex("^[A-Fa-f0-9]{8,}$")) || 
                     rawName.matches(Regex("^[A-Fa-f0-9]{14,}$"))

        val isInvalidName = rawName.isBlank() ||
                rawName == fallbackId ||
                isHexId ||
                rawName.length > 40

        return if (!isInvalidName) rawName else fallbackId
    }

    /**
     * Menentukan sama ada pelajar telah melengkapkan kehadiran (check-in & check-out)
     */
    fun isEligibleForCertificate(regDoc: DocumentSnapshot): Boolean {
        val hasCheckedIn = hasNfcCheckIn(regDoc)
        val hasCheckedOut = hasNfcCheckOut(regDoc)
        val isSent = regDoc.getString("certificateStatus") == "Sent"

        return (hasCheckedIn && hasCheckedOut) || isSent
    }

    /**
     * SYARAT KETAT NFC CHECK-IN:
     * Hanya sah jika wujud field timestamp/checkInTimestamp ATAU status diset kepada PRESENT.
     * Status "REGISTERED" sahaja DIANGGAP BELUM TAP NFC.
     */
    fun hasNfcCheckIn(doc: DocumentSnapshot): Boolean {
        val status = doc.getString("status") ?: ""
        val hasTimestamp = doc.get("timestamp") != null || doc.get("checkInTimestamp") != null

        // Mesti ada timestamp sebenar atau status PRESENT (bukan sekadar REGISTERED kosong)
        return hasTimestamp || status == "PRESENT"
    }

    /**
     * SYARAT KETAT NFC CHECK-OUT:
     * Hanya sah jika wujud rekod masa keluar.
     */
    fun hasNfcCheckOut(doc: DocumentSnapshot): Boolean {
        return doc.get("checkOutTime") != null ||
                doc.get("checkOutTimestamp") != null ||
                doc.get("timeout") != null
    }

    /**
     * Memformat objek masa Firebase Timestamp kepada bentuk jam (contoh: 02:30 PM).
     */
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
                else -> "-"
            }
        } catch (e: Exception) {
            "-"
        }
    }
}