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
import com.google.firebase.firestore.FirebaseFirestore

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
    var presentCount by remember { mutableIntStateOf(0) }
    var absentCount by remember { mutableIntStateOf(0) }
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

    val updateState: (List<LiveStudentRow>) -> Unit = { allRows ->
        val uniqueStudents = allRows.distinctBy { it.studentKey }

        totalRegisteredCount = uniqueStudents.size
        presentCount = uniqueStudents.count { it.checkIn != "-" }
        absentCount = uniqueStudents.count { it.checkIn == "-" }

        // Paparkan hanya pelajar yang sudah check-in di dalam senarai live
        studentList = uniqueStudents.filter { it.checkIn != "-" }.sortedBy { it.name }
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

                val studentMap = mutableMapOf<String, LiveStudentRow>()

                for (doc in snapshot.documents) {
                    val studentId = doc.getString("studentId")
                        ?: doc.getString("userId")
                        ?: doc.id.substringAfter("_", doc.id)

                    // Menyokong semua variasi medan masa check-in dan check-out
                    val rawCheckIn = doc.get("timestamp")
                        ?: doc.get("checkInTimestamp")
                        ?: doc.get("checkInTime")
                        ?: doc.get("timeIn")
                        ?: doc.get("checkIn")
                        ?: doc.get("time")

                    val rawCheckOut = doc.get("checkOutTime")
                        ?: doc.get("checkOutTimestamp")
                        ?: doc.get("timeout")
                        ?: doc.get("timeOut")
                        ?: doc.get("checkOut")

                    val formattedCheckIn = AttendanceUtils.formatTimestamp(rawCheckIn)
                    val formattedCheckOut = AttendanceUtils.formatTimestamp(rawCheckOut)
                    val resolvedName = AttendanceUtils.resolveStudentName(doc, "Memuatkan nama...")

                    val mapKey = if (studentId.isNotBlank()) studentId else resolvedName.trim().lowercase()

                    val existingRow = studentMap[mapKey]
                    val finalCheckIn = if (formattedCheckIn != "-") formattedCheckIn else (existingRow?.checkIn ?: "-")
                    val finalCheckOut = if (formattedCheckOut != "-") formattedCheckOut else (existingRow?.checkOut ?: "-")

                    val finalName = when {
                        resolvedName.isNotBlank() && resolvedName != "Memuatkan nama..." -> resolvedName
                        existingRow != null && existingRow.name.isNotBlank() && existingRow.name != "Memuatkan nama..." -> existingRow.name
                        else -> "Memuatkan nama..."
                    }

                    studentMap[mapKey] = LiveStudentRow(
                        studentKey = mapKey,
                        name = finalName,
                        checkIn = finalCheckIn,
                        checkOut = finalCheckOut
                    )

                    if (finalName == "Memuatkan nama..." && studentId.isNotBlank()) {
                        firestore.collection("users").document(studentId).get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val realName = AttendanceUtils.resolveStudentName(userDoc, "")
                                    if (realName.isNotBlank()) {
                                        studentMap[mapKey]?.let { row ->
                                            row.name = realName
                                            updateState(studentMap.values.toList())
                                        }
                                    }
                                }
                            }
                    }
                }
                updateState(studentMap.values.toList())
            }

        onDispose {
            workshopListener.remove()
            registrationListener.remove()
        }
    }

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
                            items(studentList, key = { it.studentKey }) { student ->
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