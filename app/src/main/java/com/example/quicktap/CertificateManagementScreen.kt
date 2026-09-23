package com.example.quicktap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EligibleStudentInfo(
    val studentId: String,
    val name: String,
    val matrix: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateManagementScreen(
    workshopId: String = "WORKSHOP_001",
    onBackClick: () -> Unit,
    onAutoGenerateClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onCertificateClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // Map: registrationDocId -> EligibleStudentInfo
    val eligibleStudents = remember { mutableStateMapOf<String, EligibleStudentInfo>() }
    var dynamicWorkshopName by remember { mutableStateOf("Loading...") }
    var dynamicWorkshopDate by remember { mutableStateOf("2026") }
    var dynamicWorkshopTime by remember { mutableStateOf("10:00 AM") }
    var dynamicOrganizerName by remember { mutableStateOf("QuickTap Organizer") }
    var bottomNavIndex by remember { mutableIntStateOf(2) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentLang = AppSettingsState.currentLanguage
    val titleText = if (currentLang == "ms") "Pengurusan Sijil" else "Certificate Management"
    val noStudentsText = if (currentLang == "ms") "Tiada pelajar yang layak ditemui." else "No eligible students found."
    val actionSendText = if (currentLang == "ms") "Hantar Sijil" else "Send Certificate"
    val autoGenText = if (currentLang == "ms") "Hantar Semua Secara Automatik" else "Auto Send All"

    val navHomeLabel = if (currentLang == "ms") "Utama" else "Home"
    val navReportLabel = if (currentLang == "ms") "Laporan" else "Report"
    val navCertLabel = if (currentLang == "ms") "Sijil" else "Certificate"
    val navSettingsLabel = if (currentLang == "ms") "Tetapan" else "Settings"

    val certGenSnackbar = if (currentLang == "ms") "Sijil berjaya dihantar kepada " else "Certificate successfully sent to "
    val successGenSnackbar1 = if (currentLang == "ms") "Berjaya menghantar " else "Successfully sent "
    val successGenSnackbar2 = if (currentLang == "ms") " sijil kepada pelajar!" else " certificates to students!"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val cardContainerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

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
                    val sId = doc.getString("studentId") ?: doc.getString("userId") ?: ""
                    val matrix = doc.getString("studentNumber") ?: doc.getString("studentIdNum") ?: "-"
                    val rawName = doc.getString("studentName") ?: doc.getString("userName") ?: doc.getString("name") ?: ""

                    val name = if (rawName.isNotBlank() && rawName != sId && rawName != doc.getString("nfcUid")) {
                        rawName
                    } else {
                        "Pelajar"
                    }

                    val existingStatus = doc.getString("certificateStatus") ?: "Completed"
                    val hasCheckedIn = doc.get("timestamp") != null || doc.get("checkInTimestamp") != null || doc.get("checkInTime") != null
                    val hasCheckedOut = doc.get("checkOutTime") != null || doc.get("checkOutTimestamp") != null || doc.get("timeout") != null

                    if (hasCheckedIn && hasCheckedOut && sId.isNotEmpty()) {
                        val displayStatus = if (existingStatus == "Sent") "Sent" else "Completed"
                        eligibleStudents[doc.id] = EligibleStudentInfo(sId, name, matrix, displayStatus)
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
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White, indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = navReportLabel) },
                    label = { Text(navReportLabel, fontSize = 11.sp, color = Color.White) },
                    selected = bottomNavIndex == 1,
                    onClick = { bottomNavIndex = 1; onReportClick() },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White, indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = navCertLabel) },
                    label = { Text(navCertLabel, fontSize = 11.sp, color = Color.White) },
                    selected = bottomNavIndex == 2,
                    onClick = { bottomNavIndex = 2; onCertificateClick() },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White, indicatorColor = Color.Transparent)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = navSettingsLabel) },
                    label = { Text(navSettingsLabel, fontSize = 11.sp, color = Color.White) },
                    selected = bottomNavIndex == 3,
                    onClick = { bottomNavIndex = 3; onSettingsClick() },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, unselectedIconColor = Color.White, indicatorColor = Color.Transparent)
                )
            }
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
                    eligibleStudents.forEach { (docId, info) ->
                        val studentId = info.studentId
                        val name = info.name
                        val matrix = info.matrix
                        val status = info.status

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
                                    Text("ID: $matrix", fontSize = 14.sp, color = secondaryTextColor)
                                }

                                val isSent = status == "Sent"

                                Button(
                                    onClick = {
                                        if (!isSent) {
                                            val safeName = if (name.isNotBlank()) name else "Pelajar"

                                            firestore.collection("registrations").document(docId)
                                                .update("certificateStatus", "Sent")

                                            val certDocId = "${workshopId}_$studentId"
                                            val certData = hashMapOf(
                                                "studentId" to studentId,
                                                "studentNumber" to matrix,
                                                "studentName" to safeName,
                                                "workshopId" to workshopId,
                                                "workshopName" to dynamicWorkshopName,
                                                "workshopDate" to dynamicWorkshopDate,
                                                "workshopTime" to dynamicWorkshopTime,
                                                "issueDate" to SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                                                "status" to "Sent"
                                            )
                                            firestore.collection("certificates").document(certDocId).set(certData)

                                            val notificationData = hashMapOf(
                                                "title" to "Certificate Issued! 🏆",
                                                "message" to "Congratulations! Your certificate for '$dynamicWorkshopName' has been issued.",
                                                "timestamp" to System.currentTimeMillis(),
                                                "type" to "CERTIFICATE",
                                                "workshopId" to workshopId
                                            )
                                            firestore.collection("users").document(studentId)
                                                .collection("notifications").add(notificationData)

                                            eligibleStudents[docId] = EligibleStudentInfo(studentId, name, matrix, "Sent")
                                            scope.launch { snackbarHostState.showSnackbar("$certGenSnackbar$safeName") }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSent) Color.Gray else Color(0xFF4A90E2)
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(if (isSent) "Sent" else actionSendText, fontSize = 14.sp)
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
                    eligibleStudents.forEach { (docId, info) ->
                        val studentId = info.studentId
                        val name = info.name
                        val matrix = info.matrix
                        val status = info.status

                        if (status != "Sent") {
                            val safeName = if (name.isNotBlank()) name else "Pelajar"

                            firestore.collection("registrations").document(docId)
                                .update("certificateStatus", "Sent")

                            val certDocId = "${workshopId}_$studentId"
                            val certData = hashMapOf(
                                "studentId" to studentId,
                                "studentNumber" to matrix,
                                "studentName" to safeName,
                                "workshopId" to workshopId,
                                "workshopName" to dynamicWorkshopName,
                                "workshopDate" to dynamicWorkshopDate,
                                "workshopTime" to dynamicWorkshopTime,
                                "issueDate" to SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                                "status" to "Sent"
                            )
                            firestore.collection("certificates").document(certDocId).set(certData)

                            val notificationData = hashMapOf(
                                "title" to "Certificate Issued! 🏆",
                                "message" to "Congratulations! Your certificate for '$dynamicWorkshopName' has been issued.",
                                "timestamp" to System.currentTimeMillis(),
                                "type" to "CERTIFICATE",
                                "workshopId" to workshopId
                            )
                            firestore.collection("users").document(studentId)
                                .collection("notifications").add(notificationData)

                            eligibleStudents[docId] = EligibleStudentInfo(studentId, name, matrix, "Sent")
                            count++
                        }
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