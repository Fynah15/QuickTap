package com.example.quicktap

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quicktap.AppSettingsState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- VIEW MODEL ---
class AttendanceViewModel : ViewModel() {
    var scanResult by mutableStateOf<ScanResult?>(null)
    var isProcessing by mutableStateOf(false)
}

data class ScanResult(val name: String, val id: String, val time: String, val errorMessage: String? = null)

// --- MAIN SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffNfcScanScreen(
    attendanceMode: String,
    workshopId: String,
    onBackClick: () -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val firestore = remember { FirebaseFirestore.getInstance() }
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var lastScanTime by remember { mutableLongStateOf(0L) }

    val currentLang = AppSettingsState.currentLanguage
    val processingText = if (currentLang == "ms") "Sedang Memproses..." else "Processing..."

    val scanTitleText = if (currentLang == "ms") "Imbas NFC" else "Scan NFC"
    val readyText = if (currentLang == "ms") "TAP KAD UNTUK IMBAS" else "TAP CARD TO SCAN"
    val workshopNameFallback = if (currentLang == "ms") "Bengkel" else "Workshop"

    var workshopTitle by remember { mutableStateOf(workshopNameFallback) }

    LaunchedEffect(workshopId) {
        firestore.collection("workshops").document(workshopId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    workshopTitle = doc.getString("title") ?: doc.getString("name") ?: workshopNameFallback
                }
            }
    }

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val textColor = if (isDark) Color.White else Color.Black
    val processingCircleColor = if (isDark) Color(0xFF424242) else Color.Gray
    val brandColor = Color(0xFF912323)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(scanTitleText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = brandColor)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (viewModel.isProcessing) processingText else readyText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(30.dp))

                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .background(if (isDark) Color(0xFF1E1E1E) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    val strokeColor = if (isDark) Color.White else Color.Black

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 5.dp.toPx()
                        val length = 35.dp.toPx()
                        val w = size.width
                        val h = size.height

                        drawLine(strokeColor, Offset(0f, 0f), Offset(length, 0f), strokeWidth)
                        drawLine(strokeColor, Offset(0f, 0f), Offset(0f, length), strokeWidth)

                        drawLine(strokeColor, Offset(w, 0f), Offset(w - length, 0f), strokeWidth)
                        drawLine(strokeColor, Offset(w, 0f), Offset(w, length), strokeWidth)

                        drawLine(strokeColor, Offset(0f, h), Offset(length, h), strokeWidth)
                        drawLine(strokeColor, Offset(0f, h), Offset(0f, h - length), strokeWidth)

                        drawLine(strokeColor, Offset(w, h), Offset(w - length, h), strokeWidth)
                        drawLine(strokeColor, Offset(w, h), Offset(w, h - length), strokeWidth)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (viewModel.isProcessing) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(processingCircleColor, RoundedCornerShape(60.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⏳", fontSize = 40.sp)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "N",
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Black,
                                    color = strokeColor,
                                    letterSpacing = (-4).sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(width = 6.dp, height = 12.dp).background(strokeColor, RoundedCornerShape(3.dp)))
                                    Box(modifier = Modifier.size(width = 8.dp, height = 20.dp).background(strokeColor, RoundedCornerShape(4.dp)))
                                    Box(modifier = Modifier.size(width = 6.dp, height = 12.dp).background(strokeColor, RoundedCornerShape(3.dp)))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "NFC SCAN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = strokeColor,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                if (viewModel.scanResult?.errorMessage != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = viewModel.scanResult!!.errorMessage!!,
                        color = Color(0xFFE57373),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Display success screen automatically upon successful scan
        if (viewModel.scanResult != null && viewModel.scanResult!!.errorMessage == null) {
            StaffAttendanceSuccessScreen(
                status = attendanceMode,
                studentName = viewModel.scanResult!!.name,
                studentId = viewModel.scanResult!!.id,
                workshopName = workshopTitle,
                timeText = viewModel.scanResult!!.time,
                onNextScanClick = { viewModel.scanResult = null }
            )
        }
    }

    DisposableEffect(Unit) {
        val readerCallback = NfcAdapter.ReaderCallback { tag: Tag ->
            val currentTime = System.currentTimeMillis()

            if (!viewModel.isProcessing && (currentTime - lastScanTime > 3500)) {
                viewModel.isProcessing = true
                lastScanTime = currentTime

                val hexId = tag.id.joinToString("") { String.format("%02X", it) }

                mainHandler.post {
                    fetchStudentAndProcess(firestore, workshopId, hexId, attendanceMode) { studentName, studentIdNum, time, errorMsg ->
                        viewModel.isProcessing = false
                        viewModel.scanResult = ScanResult(name = studentName, id = studentIdNum, time = time, errorMessage = errorMsg)
                    }
                }
            }
        }

        activity?.let { act ->
            nfcAdapter?.enableReaderMode(act, readerCallback, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B, null)
        }
        onDispose { activity?.let { nfcAdapter?.disableReaderMode(it) } }
    }
}

private fun fetchStudentAndProcess(
    firestore: FirebaseFirestore,
    workshopId: String,
    nfcUid: String,
    mode: String,
    onSuccess: (String, String, String, String?) -> Unit
) {
    val now = Timestamp.now()
    val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now.toDate())

    // Cuba semak di koleksi registrations terlebih dahulu (menyokong field nfcUid atau cardUid)
    firestore.collection("registrations")
        .whereEqualTo("workshopId", workshopId)
        .whereEqualTo("nfcUid", nfcUid)
        .get()
        .addOnSuccessListener { regDocs ->
            if (!regDocs.isEmpty) {
                val regDoc = regDocs.documents[0]
                val name = regDoc.getString("fullName") ?: regDoc.getString("studentName") ?: "Student"
                val studentIdNum = regDoc.getString("studentNumber") ?: regDoc.getString("studentId") ?: "N/A"
                val firebaseUid = regDoc.getString("studentId") ?: regDoc.id

                processAttendanceAction(firestore, workshopId, nfcUid, mode, firebaseUid, name, studentIdNum, now, timeFormatted, onSuccess)
            } else {
                firestore.collection("users")
                    .whereEqualTo("nfcUid", nfcUid)
                    .get()
                    .addOnSuccessListener { userDocs ->
                        if (!userDocs.isEmpty) {
                            handleUserFound(firestore, workshopId, nfcUid, userDocs.documents[0], mode, now, timeFormatted, onSuccess)
                        } else {
                            firestore.collection("users")
                                .whereEqualTo("cardUid", nfcUid)
                                .get()
                                .addOnSuccessListener { cardDocs ->
                                    if (!cardDocs.isEmpty) {
                                        handleUserFound(firestore, workshopId, nfcUid, cardDocs.documents[0], mode, now, timeFormatted, onSuccess)
                                    } else {
                                        onSuccess("Unknown", "N/A", timeFormatted, "Kad NFC tidak dipautkan dengan mana-mana akaun pelajar!")
                                    }
                                }
                        }
                    }
            }
        }
}

private fun handleUserFound(
    firestore: FirebaseFirestore,
    workshopId: String,
    nfcUid: String,
    userDoc: DocumentSnapshot,
    mode: String,
    now: Timestamp,
    timeFormatted: String,
    onSuccess: (String, String, String, String?) -> Unit
) {
    val name = userDoc.getString("fullName") ?: userDoc.getString("name") ?: "Student"
    val studentIdNum = userDoc.getString("studentId") ?: userDoc.getString("studentNumber") ?: userDoc.getString("matrixNo") ?: "N/A"
    val firebaseUid = userDoc.id

    // Semak sama ada pelajar ini telah mendaftar workshop ini sebelumnya
    val regDocId = "${workshopId}_$firebaseUid"
    firestore.collection("registrations").document(regDocId).get()
        .addOnSuccessListener { regDoc ->
            if (!regDoc.exists()) {
                // Pelajar belum daftar workshop secara online, tapi ada kad NFC. Kita auto-daftarkan ke workshop ini.
                val autoRegData = mapOf(
                    "workshopId" to workshopId,
                    "studentId" to firebaseUid,
                    "studentNumber" to studentIdNum,
                    "fullName" to name,
                    "name" to name,
                    "nfcUid" to nfcUid,
                    "cardUid" to nfcUid,
                    "status" to "REGISTERED"
                )
                firestore.collection("registrations").document(regDocId).set(autoRegData, SetOptions.merge())
                    .addOnSuccessListener {
                        processAttendanceAction(firestore, workshopId, nfcUid, mode, firebaseUid, name, studentIdNum, now, timeFormatted, onSuccess)
                    }
                    .addOnFailureListener {
                        onSuccess(name, studentIdNum, timeFormatted, "Gagal mendaftarkan kehadiran automatik.")
                    }
            } else {
                processAttendanceAction(firestore, workshopId, nfcUid, mode, firebaseUid, name, studentIdNum, now, timeFormatted, onSuccess)
            }
        }
}

private fun processAttendanceAction(
    firestore: FirebaseFirestore,
    workshopId: String,
    nfcUid: String,
    mode: String,
    firebaseUid: String,
    name: String,
    studentNumber: String,
    now: Timestamp,
    timeFormatted: String,
    onSuccess: (String, String, String, String?) -> Unit
) {
    val lowerMode = mode.lowercase(Locale.getDefault())
    val isCheckOutMode = lowerMode.contains("out") || lowerMode.contains("checkout")

    firestore.collection("workshops").document(workshopId).get()
        .addOnSuccessListener { workshopDoc ->
            if (workshopDoc.exists()) {
                val isTimeValid = validateWorkshopWindowRobust(workshopDoc, isCheckOutMode)
                if (!isTimeValid) {
                    val windowMsg = if (isCheckOutMode)
                        "Check-Out only allowed 30 mins before/after workshop end time!"
                    else
                        "Check-In only allowed 30 mins before/after workshop start time!"
                    onSuccess(name, studentNumber, timeFormatted, windowMsg)
                    return@addOnSuccessListener
                }
            }

            // Menggunakan format ID dokumen yang diselaraskan: `${workshopId}_$firebaseUid`
            val docId = "${workshopId}_$firebaseUid"
            val docRef = firestore.collection("registrations").document(docId)

            docRef.get().addOnSuccessListener { existingDoc ->
                val hasCheckedIn = existingDoc.exists() && existingDoc.get("timestamp") != null
                val hasCheckedOut = existingDoc.exists() && existingDoc.get("checkOutTime") != null

                if (hasCheckedIn && hasCheckedOut) {
                    onSuccess(name, studentNumber, timeFormatted, "$name has already completed attendance (Check-In & Check-Out)!")
                    return@addOnSuccessListener
                }

                if (isCheckOutMode) {
                    if (hasCheckedOut) {
                        onSuccess(name, studentNumber, timeFormatted, "$name has already completed Check-Out!")
                        return@addOnSuccessListener
                    }
                    if (!hasCheckedIn) {
                        onSuccess(name, studentNumber, timeFormatted, "$name has not checked in yet!")
                        return@addOnSuccessListener
                    }
                } else {
                    if (hasCheckedIn) {
                        onSuccess(name, studentNumber, timeFormatted, "$name has already checked in!")
                        return@addOnSuccessListener
                    }
                }

                val updates = mutableMapOf<String, Any>(
                    "status" to "PRESENT", // Status dikemas kini kepada PRESENT supaya sistem mengira pelajar hadir
                    "workshopId" to workshopId,
                    "nfcUid" to nfcUid,
                    "cardUid" to nfcUid,
                    "studentName" to name,
                    "fullName" to name,
                    "studentId" to firebaseUid,
                    "studentNumber" to studentNumber,
                    "certificateStatus" to "Pending"
                )

                if (isCheckOutMode) {
                    updates["checkOutTime"] = now
                    updates["mode"] = "Check-Out"
                } else {
                    updates["timestamp"] = now
                    updates["mode"] = "Check-In"
                }

                docRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        if (isCheckOutMode) {
                            autoIssueCertificate(firestore, workshopId, firebaseUid, name, studentNumber)
                        }
                        onSuccess(name, studentNumber, timeFormatted, null)
                    }
                    .addOnFailureListener {
                        onSuccess(name, studentNumber, timeFormatted, "Failed to save attendance record to Firestore")
                    }
            }
        }
        .addOnFailureListener {
            onSuccess(name, studentNumber, timeFormatted, "Failed to fetch workshop timing details.")
        }
}

// Robust validation supporting 12-hour, 24-hour, and Firestore Timestamps safely
private fun validateWorkshopWindowRobust(workshopDoc: DocumentSnapshot, isCheckOut: Boolean): Boolean {
    try {
        val calendarTarget = Calendar.getInstance()

        val dateObj = workshopDoc.get("date") ?: workshopDoc.get("workshopDate") ?: workshopDoc.get("tarikh")
        if (dateObj is Timestamp) {
            calendarTarget.time = dateObj.toDate()
        } else if (dateObj is String && dateObj.isNotBlank()) {
            val dateFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
                SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH),
                SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
            )
            var parsedDate: Date? = null
            for (fmt in dateFormats) {
                try {
                    parsedDate = fmt.parse(dateObj)
                    if (parsedDate != null) break
                } catch (_: Exception) {}
            }
            if (parsedDate != null) {
                val tempCal = Calendar.getInstance().apply { time = parsedDate }
                calendarTarget.set(Calendar.YEAR, tempCal.get(Calendar.YEAR))
                calendarTarget.set(Calendar.MONTH, tempCal.get(Calendar.MONTH))
                calendarTarget.set(Calendar.DAY_OF_MONTH, tempCal.get(Calendar.DAY_OF_MONTH))
            }
        }

        val timeKey = if (isCheckOut) "endTime" else {
            when {
                workshopDoc.get("startTime") != null -> "startTime"
                workshopDoc.get("time") != null -> "time"
                workshopDoc.get("start") != null -> "start"
                else -> "startTime"
            }
        }
        val timeObj = workshopDoc.get(timeKey) ?: workshopDoc.get("time") ?: workshopDoc.get("startTime") ?: if (isCheckOut) "05:00 PM" else "12:00 PM"

        val timeCalendar = Calendar.getInstance()
        var timeParsed = false

        if (timeObj is Timestamp) {
            timeCalendar.time = timeObj.toDate()
            timeParsed = true
        } else if (timeObj is String && timeObj.isNotBlank()) {
            val timeFormats = listOf(
                SimpleDateFormat("hh:mm a", Locale.ENGLISH),
                SimpleDateFormat("h:mm a", Locale.ENGLISH),
                SimpleDateFormat("HH:mm", Locale.ENGLISH),
                SimpleDateFormat("H:mm", Locale.ENGLISH),
                SimpleDateFormat("hh:mm", Locale.ENGLISH)
            )
            for (fmt in timeFormats) {
                try {
                    val parsedTime = fmt.parse(timeObj.trim())
                    if (parsedTime != null) {
                        timeCalendar.time = parsedTime
                        timeParsed = true
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        if (timeParsed) {
            calendarTarget.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendarTarget.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            calendarTarget.set(Calendar.SECOND, 0)
            calendarTarget.set(Calendar.MILLISECOND, 0)
        } else {
            return true
        }

        val targetMillis = calendarTarget.timeInMillis
        val currentMillis = System.currentTimeMillis()

        val toleranceMillis = 30 * 60 * 1000L
        val lowerBound = targetMillis - toleranceMillis
        val upperBound = targetMillis + toleranceMillis

        return currentMillis in lowerBound..upperBound
    } catch (e: Exception) {
        e.printStackTrace()
        return true
    }
}

private fun autoIssueCertificate(
    firestore: FirebaseFirestore,
    workshopId: String,
    firebaseUid: String,
    studentName: String,
    studentNumber: String
) {
    firestore.collection("workshops").document(workshopId).get()
        .addOnSuccessListener { workshopDoc ->
            val workshopName = workshopDoc.getString("title") ?: workshopDoc.getString("name") ?: "Workshop"
            val workshopDate = workshopDoc.getString("date") ?: ""
            val workshopTime = workshopDoc.getString("time") ?: ""
            val certDocId = "${workshopId}_$firebaseUid"

            val certData = hashMapOf(
                "studentId" to firebaseUid,
                "studentNumber" to studentNumber,
                "studentName" to studentName,
                "workshopId" to workshopId,
                "workshopName" to workshopName,
                "workshopDate" to workshopDate,
                "workshopTime" to workshopTime,
                "issueDate" to SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                "status" to "Sent"
            )

            firestore.collection("certificates").document(certDocId).set(certData)

            firestore.collection("registrations").document(certDocId)
                .update("certificateStatus", "Sent")

            val notificationData = hashMapOf(
                "title" to "Certificate Issued! 🏆",
                "message" to "Congratulations! Your certificate for '$workshopName' has been automatically issued. Check the Certificate menu.",
                "timestamp" to System.currentTimeMillis(),
                "type" to "CERTIFICATE",
                "workshopId" to workshopId
            )
            firestore.collection("users").document(firebaseUid)
                .collection("notifications").add(notificationData)
        }
}