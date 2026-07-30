package com.example.quicktap.dashboard.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quicktap.AppSettingsState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateManagementScreen(
    workshopId: String = "WORKSHOP_001",
    onBackClick: () -> Unit,
    onAutoGenerateClick: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    val eligibleStudents = remember { mutableStateMapOf<String, Pair<String, String>>() }
    var dynamicWorkshopName by remember { mutableStateOf("Loading...") }
    var dynamicWorkshopDate by remember { mutableStateOf("2026") }
    var dynamicWorkshopTime by remember { mutableStateOf("10:00 AM") }
    var dynamicOrganizerName by remember { mutableStateOf("QuickTap Organizer") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val titleText = if (currentLang == "ms") "Pengurusan Sijil" else "Certificate Management"
    val noStudentsText = if (currentLang == "ms") "Tiada pelajar yang layak ditemui." else "No eligible students found."
    val sentText = if (currentLang == "ms") "Hantar" else "Sent"
    val actionSendText = if (currentLang == "ms") "Hantar" else "Send"
    val autoGenText = if (currentLang == "ms") "Jana Semua Secara Automatik" else "Auto Generate All"

    val certGenSnackbar = if (currentLang == "ms") "Sijil terperinci dijana untuk " else "Detailed certificate generated for "
    val successGenSnackbar1 = if (currentLang == "ms") "Berjaya menjana " else "Successfully generated "
    val successGenSnackbar2 = if (currentLang == "ms") " sijil terperinci!" else " detailed certificates!"

    // Sokongan Tema Gelap / Cerah
    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val cardContainerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    // Ambil Maklumat Workshop & Tapis Pelajar Layak (Check-In & Check-Out)
    LaunchedEffect(workshopId) {
        firestore.collection("workshops").document(workshopId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    dynamicWorkshopName = doc.getString("title") ?: doc.getString("name") ?: "Workshop"
                    dynamicWorkshopDate = doc.getString("date") ?: doc.getString("workshopDate") ?: "2026"
                    dynamicWorkshopTime = doc.getString("time") ?: doc.getString("workshopTime") ?: "10:00 AM"
                    dynamicOrganizerName = doc.getString("organizer") ?: doc.getString("organizerName") ?: "QuickTap Organizer"
                } else {
                    dynamicWorkshopName = "Workshop"
                }
            }

        firestore.collection("registrations")
            .whereEqualTo("workshopId", workshopId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                eligibleStudents.clear()

                querySnapshot.documents.forEach { doc ->
                    val sId = doc.getString("studentId") ?: doc.getString("userId") ?: doc.id

                    // Ambil nama sebenar yang disimpan dari proses check-in NFC
                    val rawName = doc.getString("studentName") ?: doc.getString("userName") ?: doc.getString("name") ?: ""

                    val name = if (rawName.isNotBlank() && rawName != sId && rawName != doc.getString("nfcUid")) {
                        rawName
                    } else {
                        "Pelajar"
                    }

                    val existingStatus = doc.getString("certificateStatus") ?: "Completed"

                    val hasCheckedIn = doc.get("timestamp") != null || doc.get("checkInTimestamp") != null
                    val hasCheckedOut = doc.get("checkOutTime") != null || doc.get("checkOutTimestamp") != null || doc.get("timeout") != null

                    if (hasCheckedIn && hasCheckedOut && sId.isNotEmpty()) {
                        val displayStatus = if (existingStatus == "Sent") "Sent" else "Completed"
                        eligibleStudents[sId] = Pair(name, displayStatus)
                    }
                }
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (eligibleStudents.isEmpty()) {
                Text(noStudentsText, modifier = Modifier.padding(top = 20.dp), color = secondaryTextColor, fontSize = 16.sp)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    eligibleStudents.forEach { (studentId, data) ->
                        val name = data.first
                        val status = data.second

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = textColor)
                                    Text("ID: $studentId", fontSize = 14.sp, color = secondaryTextColor)
                                }
                                Text(
                                    text = if (status == "Sent") sentText else status,
                                    color = if (status == "Sent") Color(0xFF2E7D32) else secondaryTextColor,
                                    fontSize = 14.sp
                                )

                                Button(
                                    onClick = {
                                        val safeName = if (name.isNotBlank()) name else "Pelajar"
                                        val pdfFile = File(context.getExternalFilesDir(null), "Certificate_${studentId}.pdf")

                                        // Panggilan fungsi kemas kini dengan semua parameter lengkap
                                        generateSimpleCertificate(
                                            context = context,
                                            studentName = safeName,
                                            studentId = studentId,
                                            workshopName = dynamicWorkshopName,
                                            workshopDate = dynamicWorkshopDate,
                                            workshopTime = dynamicWorkshopTime,
                                            organizerName = dynamicOrganizerName,
                                            outputPath = pdfFile
                                        )

                                        // 1. Kemas kini status dalam koleksi registrations
                                        firestore.collection("registrations")
                                            .whereEqualTo("workshopId", workshopId)
                                            .get()
                                            .addOnSuccessListener { querySnapshot ->
                                                querySnapshot.documents.forEach { doc ->
                                                    val sId = doc.getString("studentId") ?: doc.getString("userId") ?: doc.id
                                                    if (sId == studentId) {
                                                        firestore.collection("registrations").document(doc.id)
                                                            .update("certificateStatus", "Sent")
                                                    }
                                                }
                                            }

                                        // 2. Simpan rasmi ke koleksi "certificates"
                                        val certDocId = "${workshopId}_${studentId}"
                                        val certData = hashMapOf(
                                            "studentId" to studentId,
                                            "studentName" to safeName,
                                            "workshopId" to workshopId,
                                            "workshopName" to dynamicWorkshopName,
                                            "issueDate" to SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                                            "status" to "Sent"
                                        )
                                        firestore.collection("certificates").document(certDocId).set(certData)

                                        eligibleStudents[studentId] = Pair(safeName, "Sent")
                                        scope.launch { snackbarHostState.showSnackbar("$certGenSnackbar$safeName") }
                                    },
                                    enabled = status != "Sent",
                                    modifier = Modifier.padding(start = 16.dp)
                                ) {
                                    Text(if (status == "Sent") sentText else actionSendText, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    var count = 0
                    eligibleStudents.forEach { (studentId, data) ->
                        val name = data.first
                        val safeName = if (name.isNotBlank()) name else "Pelajar"
                        val pdfFile = File(context.getExternalFilesDir(null), "Certificate_${studentId}.pdf")

                        // Panggilan fungsi secara auto-generate
                        generateSimpleCertificate(
                            context = context,
                            studentName = safeName,
                            studentId = studentId,
                            workshopName = dynamicWorkshopName,
                            workshopDate = dynamicWorkshopDate,
                            workshopTime = dynamicWorkshopTime,
                            organizerName = dynamicOrganizerName,
                            outputPath = pdfFile
                        )

                        firestore.collection("registrations")
                            .whereEqualTo("workshopId", workshopId)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                querySnapshot.documents.forEach { doc ->
                                    val sId = doc.getString("studentId") ?: doc.getString("userId") ?: doc.id
                                    if (sId == studentId) {
                                        firestore.collection("registrations").document(doc.id)
                                            .update("certificateStatus", "Sent")
                                    }
                                }
                            }

                        val certDocId = "${workshopId}_${studentId}"
                        val certData = hashMapOf(
                            "studentId" to studentId,
                            "studentName" to safeName,
                            "workshopId" to workshopId,
                            "workshopName" to dynamicWorkshopName,
                            "issueDate" to SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                            "status" to "Sent"
                        )
                        firestore.collection("certificates").document(certDocId).set(certData)

                        eligibleStudents[studentId] = Pair(safeName, "Sent")
                        count++
                    }
                    onAutoGenerateClick()
                    scope.launch { snackbarHostState.showSnackbar("$successGenSnackbar1$count$successGenSnackbar2") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
            ) {
                Text(autoGenText, fontSize = 16.sp)
            }
        }
    }
}