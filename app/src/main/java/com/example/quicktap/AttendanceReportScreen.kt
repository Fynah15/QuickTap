package com.example.quicktap

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportScreen(
    workshopId: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onCertificateClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    // ... (existing code remains same until Button)

    var studentList by remember { mutableStateOf(listOf<LiveStudentRow>()) }
    var totalRegisteredCount by remember { mutableIntStateOf(0) }
    var workshopTitleName by remember { mutableStateOf("Loading Workshop...") }
    var bottomNavIndex by remember { mutableIntStateOf(1) }

    val currentLang = AppSettingsState.currentLanguage
    val defaultTitleText = if (currentLang == "ms") "Laporan Kehadiran Firebase" else "Firebase Workshop Attendance"
    val totalRegisteredLabel = if (currentLang == "ms") "Jumlah Pelajar Berdaftar" else "Total Students Registered"
    val presentLabel = if (currentLang == "ms") "Hadir" else "Present"
    val absentLabel = if (currentLang == "ms") "Tidak Hadir" else "Absent"
    val attendanceTodayLabel = if (currentLang == "ms") "Kehadiran Hari Ini" else "Attendance Today"

    val studentNameHeader = if (currentLang == "ms") "Nama Pelajar" else "Student Name"
    val inHeader = if (currentLang == "ms") "Masuk" else "In"
    val outHeader = if (currentLang == "ms") "Keluar" else "Out"
    val downloadReportText = if (currentLang == "ms") "Muat Turun Laporan" else "Download Report"
    val noRecordsText = if (currentLang == "ms") "Tiada rekod kehadiran NFC dijumpai." else "No NFC attendance records found."

    val navHomeLabel = if (currentLang == "ms") "Utama" else "Home"
    val navReportLabel = if (currentLang == "ms") "Laporan" else "Report"
    val navCertLabel = if (currentLang == "ms") "Sijil" else "Certificate"
    val navSettingsLabel = if (currentLang == "ms") "Tetapan" else "Settings"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val statsCardBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEFEFEF)
    val tableHeaderBg = if (isDark) Color(0xFF252525) else Color(0xFFF2F2F2)
    val presentCardColor = if (isDark) Color(0xFF2E6930) else Color(0xFF81C784)
    val absentCardColor = if (isDark) Color(0xFF783131) else Color(0xFFE57373)

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
                    val studentId = doc.getString("studentId") ?: doc.id
                    val studentKey = studentId

                    val hasCheckedIn = AttendanceUtils.hasNfcCheckIn(doc)
                    val hasCheckedOut = AttendanceUtils.hasNfcCheckOut(doc)

                    if (!hasCheckedIn && !hasCheckedOut) {
                        continue
                    }

                    val studentName = AttendanceUtils.resolveStudentName(doc, fallbackId = "Student")

                    val timestampField = doc.get("timestamp") ?: doc.get("checkInTimestamp")
                    val checkOutTimeField = doc.get("checkOutTime") ?: doc.get("checkOutTimestamp") ?: doc.get("timeout")

                    val formattedCheckIn = if (hasCheckedIn) AttendanceUtils.formatTimestamp(timestampField) else "-"
                    val formattedCheckOut = if (hasCheckedOut) AttendanceUtils.formatTimestamp(checkOutTimeField) else "-"

                    if (studentMap.containsKey(studentKey)) {
                        val existing = studentMap[studentKey]!!
                        if (formattedCheckIn != "-") existing.checkIn = formattedCheckIn
                        if (formattedCheckOut != "-") existing.checkOut = formattedCheckOut
                        if (studentName != "Student") {
                            existing.name = studentName
                        }
                    } else {
                        studentMap[studentKey] = LiveStudentRow(
                            studentKey = studentKey,
                            name = studentName,
                            checkIn = formattedCheckIn,
                            checkOut = formattedCheckOut
                        )
                    }
                }

                studentList = studentMap.values.toList()
            }

        onDispose {
            workshopListener.remove()
            registrationListener.remove()
        }
    }

    val presentCount = studentList.size
    val absentCount = if (totalRegisteredCount >= presentCount) totalRegisteredCount - presentCount else 0

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
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF912323)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = navHomeLabel) },
                    label = { Text(navHomeLabel, fontSize = 11.sp, color = Color.White) },
                    selected = bottomNavIndex == 0,
                    onClick = { bottomNavIndex = 0; onHomeClick() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = navReportLabel) },
                    label = { Text(navReportLabel, fontSize = 11.sp, color = Color.White) },
                    selected = bottomNavIndex == 1,
                    onClick = { bottomNavIndex = 1; onReportClick() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = navCertLabel) },
                    label = { Text(navCertLabel, fontSize = 11.sp, color = Color.White) },
                    selected = bottomNavIndex == 2,
                    onClick = { bottomNavIndex = 2; onCertificateClick() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = navSettingsLabel) },
                    label = { Text(navSettingsLabel, fontSize = 11.sp, color = Color.White) },
                    selected = bottomNavIndex == 3,
                    onClick = { bottomNavIndex = 3; onSettingsClick() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(backgroundColor).padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f).height(95.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF912323)), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(totalRegisteredLabel, fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$totalRegisteredCount", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                        }
                    }
                }

                Card(modifier = Modifier.weight(1.3f).height(95.dp), colors = CardDefaults.cardColors(containerColor = statsCardBg), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                onClick = {
                    generateAttendancePdf(context, workshopTitleName, studentList)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(downloadReportText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun generateAttendancePdf(context: Context, workshopName: String, studentList: List<LiveStudentRow>) {
    val pdfDocument = PdfDocument()
    val paint = Paint()
    val titlePaint = Paint()

    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    titlePaint.textSize = 20f
    titlePaint.isFakeBoldText = true
    canvas.drawText("QuickTap: Attendance Report", 40f, 50f, titlePaint)

    paint.textSize = 14f
    canvas.drawText("Workshop: $workshopName", 40f, 80f, paint)
    canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", 40f, 100f, paint)

    paint.isFakeBoldText = true
    canvas.drawText("No", 40f, 140f, paint)
    canvas.drawText("Student Name", 80f, 140f, paint)
    canvas.drawText("Student ID", 300f, 140f, paint)
    canvas.drawText("In", 450f, 140f, paint)
    canvas.drawText("Out", 520f, 140f, paint)

    paint.isFakeBoldText = false
    var yPos = 170f
    var index = 1
    for (student in studentList) {
        if (yPos > 800) break // Simple pagination limit
        canvas.drawText("$index.", 40f, yPos, paint)
        canvas.drawText(student.name, 80f, yPos, paint)
        canvas.drawText(student.studentKey, 300f, yPos, paint)
        canvas.drawText(student.checkIn, 450f, yPos, paint)
        canvas.drawText(student.checkOut, 520f, yPos, paint)
        yPos += 25f
        index++
    }

    pdfDocument.finishPage(page)

    val fileName = "Attendance_${workshopName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

    try {
        pdfDocument.writeTo(FileOutputStream(file))
        Toast.makeText(context, "PDF saved to Documents", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}
