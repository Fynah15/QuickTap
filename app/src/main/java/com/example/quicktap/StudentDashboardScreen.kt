package com.example.quicktap.dashboard.student

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.quicktap.AppSettingsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    myStudentId: String = "STUDENT_12345",
    currentWorkshopId: String = "WORKSHOP_001",
    onWorkshopListClick: () -> Unit,
    onCertificateClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    // State untuk maklumat profil pelajar daripada Firestore
    var fullName by remember { mutableStateOf("Nursyafinah Binti Hamdan") }
    var nickname by remember { mutableStateOf("Nursyafinah") }
    var studentIdNumber by remember { mutableStateOf("202209020204") }
    var courseName by remember { mutableStateOf("Bachelor of Information Technology") }
    var profileImageUrl by remember { mutableStateOf("") }

    // State untuk Acara Akan Datang (Highlight)
    var upcomingEventTitle by remember { mutableStateOf("") }
    var upcomingEventDesc by remember { mutableStateOf("") }
    var upcomingEventDate by remember { mutableStateOf("") }
    var upcomingEventTime by remember { mutableStateOf("") }
    var upcomingEventLocation by remember { mutableStateOf("") }
    var hasUpcomingEvent by remember { mutableStateOf(false) }

    // State untuk Dialog Notifikasi
    var showNotificationDialog by remember { mutableStateOf(false) }

    // Menggunakan rememberSaveable agar state kehadiran kekal semasa navigasi
    var showAttendanceAlert by rememberSaveable { mutableStateOf(false) }
    var attendanceModeDetected by rememberSaveable { mutableStateOf("Check - In") }
    var lastProcessedDocId by rememberSaveable { mutableStateOf("") }

    // Permintaan kebenaran notifikasi untuk Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Ambil data profil & acara akan datang dari Firestore
    LaunchedEffect(currentUser) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val uid = currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        fullName = document.getString("name") ?: "Nursyafinah Binti Hamdan"
                        nickname = document.getString("nickname") ?: fullName.substringBefore(" ")
                        studentIdNumber = document.getString("studentId") ?: "202209020204"
                        courseName = document.getString("course") ?: (document.getString("program") ?: "Bachelor of Information Technology")
                        profileImageUrl = document.getString("profileImageUrl") ?: ""
                    }
                }
                .addOnFailureListener {
                    nickname = currentUser.email?.substringBefore("@") ?: "Student"
                }
        }

        firestore.collection("workshops").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                val currentDate = Date()
                val dateFormatList = listOf(
                    SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH),
                    SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
                    SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                )

                var nearestEvent: com.google.firebase.firestore.DocumentSnapshot? = null
                var nearestDate: Date? = null

                for (doc in snapshot.documents) {
                    val dateStr = doc.getString("date") ?: continue
                    var parsedDate: Date? = null
                    for (fmt in dateFormatList) {
                        try {
                            parsedDate = fmt.parse(dateStr)
                            if (parsedDate != null) break
                        } catch (e: Exception) {
                            continue
                        }
                    }

                    if (parsedDate != null && parsedDate.after(currentDate)) {
                        if (nearestDate == null || parsedDate.before(nearestDate)) {
                            nearestDate = parsedDate
                            nearestEvent = doc
                        }
                    }
                }

                if (nearestEvent != null) {
                    upcomingEventTitle = nearestEvent.getString("title") ?: "Upcoming Event"
                    upcomingEventDesc = nearestEvent.getString("description") ?: ""
                    upcomingEventDate = nearestEvent.getString("date") ?: ""
                    upcomingEventTime = nearestEvent.getString("time") ?: ""
                    upcomingEventLocation = nearestEvent.getString("location") ?: ""
                    hasUpcomingEvent = true
                } else {
                    hasUpcomingEvent = false
                }
            }
        }
    }

    val currentLang = AppSettingsState.currentLanguage
    val welcomeText = if (currentLang == "ms") "Selamat Datang, $nickname!" else "Welcome, $nickname!"
    val homeNav = if (currentLang == "ms") "Utama" else "Home"
    val workshopNav = if (currentLang == "ms") "Bengkel" else "Workshop"
    val certNav = if (currentLang == "ms") "Sijil" else "Certificate"
    val settingsNav = if (currentLang == "ms") "Tetapan" else "Settings"

    val universityNameText = "CITY UNIVERSITY MALAYSIA"
    val studentIdTitle = if (currentLang == "ms") "ID PELAJAR" else "STUDENT ID"

    val quickAccessTitle = if (currentLang == "ms") "Akses Pantas" else "Quick Access"
    val workshopListLabel = if (currentLang == "ms") "Senarai Bengkel" else "Workshop List"
    val certificateLabel = if (currentLang == "ms") "Sijil" else "Certificate"
    val historyLabel = if (currentLang == "ms") "Sejarah" else "History"

    val highlightTitle = if (currentLang == "ms") "Sorotan Acara Akan Datang" else "Upcoming Event Highlight"
    val noUpcomingText = if (currentLang == "ms") "Tiada acara akan datang buat masa ini." else "No upcoming events at the moment."

    val alertTitle = if (currentLang == "ms") "Imbasan Dikesan!" else "Scan Detected!"
    val alertDesc = if (currentLang == "ms") "Akaun anda sedang disahkan untuk $attendanceModeDetected. Sila sahkan butiran kehadiran anda." else "Your account is verifying for $attendanceModeDetected. Please confirm your attendance details."
    val okText = "OK"
    val dismissText = if (currentLang == "ms") "Tutup" else "Dismiss"

    val notifDialogTitle = if (currentLang == "ms") "Notifikasi Terkini" else "Latest Notification"
    val notifDesc1 = if (currentLang == "ms") "• Peringatan: Bengkel akan datang akan dimaklumkan melalui notifikasi peringatan peranti." else "• Reminder: Upcoming workshops will be notified via device schedule reminders."
    val notifDesc2 = if (currentLang == "ms") "• Status: Peranti anda telah bersedia menerima pemberitahuan jadual." else "• Status: Your device is ready to receive schedule notifications."
    val closeText = if (currentLang == "ms") "Tutup" else "Close"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val placeholderColor = if (isDark) Color(0xFF2C2C2C) else Color.LightGray

    DisposableEffect(key1 = currentWorkshopId) {
        val documentId = "${currentWorkshopId}_${myStudentId}"
        val registrationRef = firestore.collection("registrations").document(documentId)

        val listenerRegistration = registrationRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.getString("status")
                val mode = snapshot.getString("mode") ?: "Check - In"

                if (status == "VERIFYING" && lastProcessedDocId != documentId) {
                    attendanceModeDetected = mode
                    showAttendanceAlert = true
                }
            }
        }

        onDispose { listenerRegistration.remove() }
    }

    if (showAttendanceAlert) {
        AlertDialog(
            onDismissRequest = { showAttendanceAlert = false },
            title = { Text(alertTitle, fontWeight = FontWeight.Bold, color = textColor) },
            text = { Text(alertDesc, color = secondaryTextColor) },
            containerColor = cardBgColor,
            confirmButton = {
                Button(
                    onClick = {
                        showAttendanceAlert = false
                        lastProcessedDocId = "${currentWorkshopId}_${myStudentId}"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                ) {
                    Text(okText, color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showAttendanceAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF912323))
                ) {
                    Text(dismissText, color = Color.White)
                }
            }
        )
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text(notifDialogTitle, fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column {
                    Text(notifDesc1, color = secondaryTextColor, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(notifDesc2, color = secondaryTextColor, fontSize = 14.sp)
                }
            },
            containerColor = cardBgColor,
            confirmButton = {
                Button(
                    onClick = { showNotificationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF912323))
                ) {
                    Text(closeText, color = Color.White)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(welcomeText, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showNotificationDialog = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF912323)) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(homeNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color(0xFF7A1D1D)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onWorkshopListClick,
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Workshop") },
                    label = { Text(workshopNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color(0xFF7A1D1D)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onCertificateClick,
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Certificate") },
                    label = { Text(certNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color(0xFF7A1D1D)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(settingsNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color(0xFF7A1D1D)
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Menjadikan keseluruhan halaman boleh diskrol jika paparan panjang
                .padding(16.dp)
        ) {
            // --- KAD ID PELAJAR ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF912323))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(universityNameText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(placeholderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = "Student Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile",
                                    tint = secondaryTextColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                            Text(studentIdNumber, color = secondaryTextColor, fontSize = 14.sp)
                            Text(courseName, color = secondaryTextColor, fontSize = 12.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF912323))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(studentIdTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(quickAccessTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            Spacer(modifier = Modifier.height(12.dp))

            // --- BAHAGIAN AKSES PANTAS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickAccessItem(title = workshopListLabel, icon = Icons.Default.List, color = Color(0xFF912323), textColor = textColor, onClick = onWorkshopListClick)
                QuickAccessItem(title = certificateLabel, icon = Icons.Default.WorkspacePremium, color = Color(0xFF912323), textColor = textColor, onClick = onCertificateClick)
                QuickAccessItem(title = historyLabel, icon = Icons.Default.History, color = Color(0xFF912323), textColor = textColor, onClick = onHistoryClick)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(highlightTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
            Spacer(modifier = Modifier.height(12.dp))

            // --- KAD SOROTAN (UPCOMING EVENT) PENUH & BOLEH DIKLIK UNTUK DAFTAR ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWorkshopListClick() }, // Membawa pengguna terus ke senarai bengkel untuk daftar
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasUpcomingEvent) {
                        Text(
                            text = upcomingEventTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (upcomingEventDesc.isNotEmpty()) {
                            Text(
                                text = upcomingEventDesc,
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "📅  $upcomingEventDate", color = Color.White, fontSize = 12.sp)
                        Text(text = "⏰  $upcomingEventTime", color = Color.White, fontSize = 12.sp)
                        Text(text = "📍  $upcomingEventLocation", color = Color.White, fontSize = 12.sp)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = noUpcomingText,
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessItem(title: String, icon: ImageVector, color: Color, textColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .background(color, RoundedCornerShape(12.dp))
        ) {
            Icon(icon, contentDescription = title, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}