package com.example.quicktap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAttendanceScreen(
    workshopId: String,
    onBackClick: () -> Unit,
    onViewSummaryReportClick: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }

    var studentList by remember { mutableStateOf(listOf<LiveStudentRow>()) }
    var totalRegisteredCount by remember { mutableIntStateOf(0) }
    var workshopTitleName by remember { mutableStateOf("Live Attendance") }

    val currentLang = AppSettingsState.currentLanguage
    val defaultTitleText = if (currentLang == "ms") "Kehadiran Langsung" else "Live Attendance"
    val presentLabel = if (currentLang == "ms") "Hadir" else "Present"
    val absentLabel = if (currentLang == "ms") "Tidak Hadir" else "Absent"
    val attendanceTodayLabel = if (currentLang == "ms") "Kehadiran Hari Ini" else "Attendance Today"

    val studentNameHeader = if (currentLang == "ms") "Nama Pelajar" else "Student Name"
    val inHeader = if (currentLang == "ms") "Masuk" else "In"
    val outHeader = if (currentLang == "ms") "Keluar" else "Out"
    val viewSummaryReportText = if (currentLang == "ms") "Lihat Laporan Ringkasan" else "View summary report"
    val noRecordsText = if (currentLang == "ms") "Tiada rekod kehadiran langsung." else "No live attendance records found."

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val statsCardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEFEFEF)
    val tableHeaderBg = if (isDark) Color(0xFF252525) else Color(0xFFF2F2F2)
    val presentCardColor = if (isDark) Color(0xFF2E6930) else Color(0xFF81C784)
    val absentCardColor = if (isDark) Color(0xFF783131) else Color(0xFFE57373)

    val parseTimestampToTime: (Any?) -> String = { rawValue ->
        try {
            when (rawValue) {
                is Timestamp -> {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(rawValue.toDate())
                }
                else -> "-"
            }
        } catch (_: Exception) {
            "-"
        }
    }

    DisposableEffect(workshopId) {
        val workshopRef = firestore.collection("workshops").document(workshopId)
        val workshopListener = workshopRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("name") ?: snapshot.getString("title") ?: defaultTitleText
                workshopTitleName = name
            } else {
                workshopTitleName = defaultTitleText
            }
        }

        val registrationListener = firestore.collection("registrations")
            .whereEqualTo("workshopId", workshopId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                totalRegisteredCount = snapshot.size()

                val studentMap = mutableMapOf<String, LiveStudentRow>()

                for (doc in snapshot.documents) {
                    val timestampField = doc.get("timestamp")
                    val checkOutTimeField = doc.get("checkOutTime")

                    val formattedCheckIn = parseTimestampToTime(timestampField)
                    val formattedCheckOut = parseTimestampToTime(checkOutTimeField)

                    if (formattedCheckIn == "-" && formattedCheckOut == "-") {
                        continue
                    }

                    val rawStudentName = doc.getString("studentName")
                        ?: doc.getString("name")
                        ?: doc.getString("fullName")
                        ?: doc.getString("userName")

                    val studentId = doc.getString("studentId") ?: doc.getString("userId") ?: doc.id.substringAfter("_")

                    val isInvalidName = rawStudentName.isNullOrBlank() ||
                            rawStudentName.length > 20 && rawStudentName.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == '-' } ||
                            rawStudentName == studentId

                    val finalDisplayName = if (!isInvalidName) rawStudentName!! else "Memuatkan nama..."

                    if ((isInvalidName || finalDisplayName == "Memuatkan nama...") && studentId.isNotBlank()) {
                        firestore.collection("users").document(studentId).get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val realName = userDoc.getString("name")
                                        ?: userDoc.getString("fullName")
                                        ?: userDoc.getString("studentName")
                                        ?: userDoc.getString("nickname")

                                    if (!realName.isNullOrBlank()) {
                                        studentMap[studentId]?.let { row ->
                                            row.name = realName
                                            studentList = studentMap.values.toList().sortedBy { it.name }
                                        }
                                    }
                                }
                            }
                    }

                    if (studentMap.containsKey(studentId)) {
                        val existing = studentMap[studentId]!!
                        if (formattedCheckIn != "-") existing.checkIn = formattedCheckIn
                        if (formattedCheckOut != "-") existing.checkOut = formattedCheckOut
                        if (!isInvalidName) {
                            existing.name = finalDisplayName
                        }
                    } else {
                        studentMap[studentId] = LiveStudentRow(
                            studentKey = studentId,
                            name = finalDisplayName,
                            checkIn = formattedCheckIn,
                            checkOut = formattedCheckOut
                        )
                    }
                }

                studentList = studentMap.values.toList().sortedBy { it.name }
            }

        onDispose {
            workshopListener.remove()
            registrationListener.remove()
        }
    }

    val presentCount = studentList.size
    val absentCount = if (totalRegisteredCount >= presentCount) totalRegisteredCount - presentCount else 0
    val attendancePercentage = if (totalRegisteredCount > 0) (presentCount * 100) / totalRegisteredCount else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workshopTitleName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f).height(95.dp), colors = CardDefaults.cardColors(containerColor = statsCardBg), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("$attendancePercentage%", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = textColor)
                    }
                }

                Card(modifier = Modifier.weight(1.3f).height(95.dp), colors = CardDefaults.cardColors(containerColor = statsCardBg), shape = RoundedCornerShape(12.dp)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(attendanceTodayLabel, fontSize = 9.sp, color = secondaryTextColor, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = presentCardColor), modifier = Modifier.size(50.dp, 45.dp), shape = RoundedCornerShape(6.dp)) {
                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("$presentCount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text(presentLabel, fontSize = 7.sp, color = Color.White)
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = absentCardColor), modifier = Modifier.size(50.dp, 45.dp), shape = RoundedCornerShape(6.dp)) {
                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("$absentCount", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text(absentLabel, fontSize = 7.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = cardBgColor), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().background(tableHeaderBg).padding(12.dp)) {
                        Text(studentNameHeader, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                        Text(inHeader, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                        Text(outHeader, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                    }
                    if (studentList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text(noRecordsText, fontSize = 12.sp, color = secondaryTextColor)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            items(studentList) { student ->
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(student.name, modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
                                    Text(student.checkIn, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = secondaryTextColor)
                                    Text(student.checkOut, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = secondaryTextColor)
                                }
                                HorizontalDivider(color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0F0F0), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onViewSummaryReportClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(viewSummaryReportText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}