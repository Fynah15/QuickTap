package com.example.quicktap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quicktap.AppSettingsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    onAttendanceModeClick: () -> Unit,
    onSelectWorkshopClick: () -> Unit,
    onViewAnalyticsClick: () -> Unit,
    onReportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCertificateManagementClick: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser

    // State untuk data profil staf
    var staffName by remember { mutableStateOf("Staff") }

    // State untuk data bengkel aktif secara real-time
    var activeWorkshopId by remember { mutableStateOf<String?>(null) }
    var activeWorkshopName by remember { mutableStateOf("No Active Workshop") }

    // State untuk data statistik jumlah keseluruhan bengkel & kehadiran
    var totalWorkshopsCount by remember { mutableStateOf(0) }
    var totalPresent by remember { mutableStateOf(0) }
    var totalAbsent by remember { mutableStateOf(0) }
    var attendancePercentage by remember { mutableStateOf(0f) }

    // State tambahan untuk metrik masa nyata Check-In & Check-Out
    var totalCheckedIn by remember { mutableStateOf(0) }
    var totalCheckedOut by remember { mutableStateOf(0) }

    // State dinamik untuk mengesan item navigasi bawah yang sedang dipilih
    var selectedNavigationIndex by remember { mutableStateOf(0) }

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val welcomeText = if (currentLang == "ms") "Selamat datang, $staffName!" else "Welcome, $staffName!"
    val homeNav = if (currentLang == "ms") "Utama" else "Home"
    val reportNav = if (currentLang == "ms") "Laporan" else "Report"
    val certNav = if (currentLang == "ms") "Sijil" else "Certificate"
    val settingsNav = if (currentLang == "ms") "Tetapan" else "Settings"

    val statisticsTitle = if (currentLang == "ms") "Statistik" else "Statistics"
    val totalWorkshopLabel = if (currentLang == "ms") "Jumlah Bengkel" else "Total Workshop"
    val attendanceTodayLabel = if (currentLang == "ms") "Kehadiran Hari Ini" else "Attendance Today"
    val presentLabel = if (currentLang == "ms") "Hadir" else "Present"
    val absentLabel = if (currentLang == "ms") "Tidak Hadir" else "Absent"

    val quickAccessTitle = if (currentLang == "ms") "Akses Pantas" else "Quick Access"
    val workshopBtn = if (currentLang == "ms") "Bengkel" else "Workshop"
    val modeBtn = if (currentLang == "ms") "Mod" else "Mode"
    val certsBtn = if (currentLang == "ms") "Sijil" else "Certificates"
    val reportsBtn = if (currentLang == "ms") "Laporan" else "Reports"

    val liveAttendanceTitle = if (currentLang == "ms") "Gambaran Keseluruhan Kehadiran Langsung" else "Live Attendance Overview"
    val noActiveWorkshopText = if (currentLang == "ms") "Tiada Bengkel Aktif" else "No Active Workshop"

    val liveCheckInLabel = if (currentLang == "ms") "Daftar Masuk: " else "Check-In: "
    val liveCheckOutLabel = if (currentLang == "ms") "Daftar Keluar: " else "Check-Out: "
    val liveSyncText = if (currentLang == "ms") "Segerak kehadiran langsung aktif" else "Live attendance sync actively"
    val viewAnalyticsBtnText = if (currentLang == "ms") "Lihat Analitis Terperinci" else "View Detailed Analytics"

    // 2. Sokongan Tema Gelap / Cerah (Dark / Light Mode)
    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardContainerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val statsCardBg2 = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEFEFEF)
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val presentBoxBg = if (isDark) Color(0xFF1E3A24) else Color(0xFFE2F5E1)
    val absentBoxBg = if (isDark) Color(0xFF4A2222) else Color(0xFFFDEAEA)

    // Dapatkan jumlah keseluruhan bengkel secara Real-Time dari koleksi "workshops"
    DisposableEffect(Unit) {
        val allWorkshopsListener = firestore.collection("workshops")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    totalWorkshopsCount = snapshot.size()
                }
            }
        onDispose {
            allWorkshopsListener.remove()
        }
    }

    // Muat turun data bengkel TERKINI (Latest workshop) secara Real-Time dari Firestore
    DisposableEffect(currentUser) {
        var registrationsListener: ListenerRegistration? = null

        // Ambil nama staf
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    staffName = doc.getString("nickname") ?: doc.getString("name") ?: "Staff"
                }
            }
        }

        val processWorkshopDocument: (com.google.firebase.firestore.DocumentSnapshot) -> Unit = { workshopDoc ->
            val workshopId = workshopDoc.id
            activeWorkshopId = workshopId

            // Ambil tajuk sebenar bengkel secara dinamik
            val fetchedName = workshopDoc.getString("title") ?: workshopDoc.getString("name")
            activeWorkshopName = if (!fetchedName.isNullOrBlank()) fetchedName else noActiveWorkshopText

            registrationsListener?.remove()

            // Dapatkan pendaftaran & kehadiran untuk bengkel aktif ini
            registrationsListener = firestore.collection("registrations")
                .whereEqualTo("workshopId", workshopId)
                .addSnapshotListener { regSnapshots, regError ->
                    if (regError != null || regSnapshots == null) return@addSnapshotListener

                    val total = regSnapshots.size()
                    var presentCount = 0
                    var absentCount = 0
                    var checkInCount = 0
                    var checkOutCount = 0

                    for (doc in regSnapshots.documents) {
                        val status = doc.getString("status") ?: "ABSENT"
                        if (status == "PRESENT" || status == "VERIFIED") {
                            presentCount++
                        } else {
                            absentCount++
                        }

                        val checkInTimestamp = doc.get("checkInTimestamp")
                        val checkOutTimestamp = doc.get("checkOutTimestamp")

                        if (checkInTimestamp != null) checkInCount++
                        if (checkOutTimestamp != null) checkOutCount++
                    }

                    totalPresent = presentCount
                    totalAbsent = absentCount
                    totalCheckedIn = checkInCount
                    totalCheckedOut = checkOutCount
                    attendancePercentage = if (total > 0) (presentCount.toFloat() / total.toFloat()) else 0f
                }
        }

        // Mendengar perubahan koleksi "workshops" dengan menyusun mengikut tarikh terkini di atas
        val workshopQuery = firestore.collection("workshops")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)

        val workshopListener = workshopQuery.addSnapshotListener { workshopSnapshots, workshopError ->
            if (workshopError == null && workshopSnapshots != null && !workshopSnapshots.isEmpty) {
                processWorkshopDocument(workshopSnapshots.documents.first())
            } else {
                // Fallback jika susunan tarikh gagal
                firestore.collection("workshops").limit(1).get()
                    .addOnSuccessListener { fallbackSnapshots ->
                        if (!fallbackSnapshots.isEmpty) {
                            processWorkshopDocument(fallbackSnapshots.documents.first())
                        } else {
                            activeWorkshopId = null
                            activeWorkshopName = noActiveWorkshopText
                            totalPresent = 0
                            totalAbsent = 0
                            totalCheckedIn = 0
                            totalCheckedOut = 0
                            attendancePercentage = 0f
                        }
                    }
                    .addOnFailureListener {
                        activeWorkshopId = null
                        activeWorkshopName = noActiveWorkshopText
                    }
            }
        }

        onDispose {
            workshopListener.remove()
            registrationsListener?.remove()
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF912323))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Text(
                    text = welcomeText,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF912323)) {
                // Home
                NavigationBarItem(
                    selected = selectedNavigationIndex == 0,
                    onClick = { selectedNavigationIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(homeNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color(0xFF7A1D1D)
                    )
                )
                // Report
                NavigationBarItem(
                    selected = selectedNavigationIndex == 1,
                    onClick = {
                        selectedNavigationIndex = 1
                        onReportClick()
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "Report") },
                    label = { Text(reportNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color(0xFF7A1D1D)
                    )
                )
                // Certificate
                NavigationBarItem(
                    selected = selectedNavigationIndex == 2,
                    onClick = {
                        selectedNavigationIndex = 2
                        onCertificateManagementClick()
                    },
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Certificate") },
                    label = { Text(certNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color(0xFF7A1D1D)
                    )
                )
                // Settings
                NavigationBarItem(
                    selected = selectedNavigationIndex == 3,
                    onClick = {
                        selectedNavigationIndex = 3
                        onSettingsClick()
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(settingsNav) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(statisticsTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Workshop Card (Ditukar daripada Total Registered kepada Total Workshop)
                Card(
                    modifier = Modifier.weight(1f).height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF801A1A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = totalWorkshopLabel,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$totalWorkshopsCount", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Attendance Today Card
                Card(
                    modifier = Modifier.weight(1f).height(100.dp),
                    colors = CardDefaults.cardColors(containerColor = statsCardBg2),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = attendanceTodayLabel,
                            color = textColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Present Box
                            Box(modifier = Modifier.weight(1f).background(presentBoxBg, RoundedCornerShape(8.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$totalPresent", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(text = presentLabel, color = Color(0xFF4CAF50), fontSize = 9.sp)
                                }
                            }
                            // Absent Box
                            Box(modifier = Modifier.weight(1f).background(absentBoxBg, RoundedCornerShape(8.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$totalAbsent", color = Color(0xFFF44336), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(text = absentLabel, color = Color(0xFFF44336), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION: QUICK ACCESS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(quickAccessTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickAccessIconButton(workshopBtn, Icons.Default.EventNote, onSelectWorkshopClick, secondaryTextColor)
                        QuickAccessIconButton(modeBtn, Icons.Default.PhoneAndroid, onAttendanceModeClick, secondaryTextColor)
                        QuickAccessIconButton(certsBtn, Icons.Default.WorkspacePremium, onCertificateManagementClick, secondaryTextColor)
                        QuickAccessIconButton(reportsBtn, Icons.Default.List, onReportClick, secondaryTextColor)
                    }
                }
            }

            // --- SECTION: LIVE ATTENDANCE OVERVIEW ---
            Text(liveAttendanceTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { attendancePercentage },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF4CAF50),
                            strokeWidth = 6.dp,
                            trackColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
                        )
                        val displayPercentage = (attendancePercentage * 100).toInt()
                        Text(text = "$displayPercentage%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeWorkshopName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "$liveCheckInLabel$totalCheckedIn | $liveCheckOutLabel$totalCheckedOut", color = Color(0xFF4A90E2), fontSize = 11.sp, fontWeight = FontWeight.Medium)

                        Text(text = liveSyncText, color = secondaryTextColor, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = onViewAnalyticsClick,
                            enabled = activeWorkshopId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(viewAnalyticsBtnText, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessIconButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
        Text(title, fontSize = 9.sp, color = textColor, maxLines = 1)
        Spacer(modifier = Modifier.height(4.dp))
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(50.dp).background(Color(0xFF912323), RoundedCornerShape(8.dp))
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}