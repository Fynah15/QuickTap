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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    onAttendanceModeClick: () -> Unit,
    onSelectWorkshopClick: () -> Unit,
    onViewAnalyticsClick: (String) -> Unit,
    onReportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCertificateManagementClick: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser

    var staffName by remember { mutableStateOf("Staff") }
    var activeWorkshopId by remember { mutableStateOf<String?>(null) }
    var activeWorkshopName by remember { mutableStateOf("No Active Workshop Today") }

    var totalWorkshopsCount by remember { mutableStateOf(0) }
    var todayTotalPresent by remember { mutableStateOf(0) }
    var todayTotalAbsent by remember { mutableStateOf(0) }

    var activeWorkshopAttendancePercentage by remember { mutableStateOf(0f) }

    var selectedNavigationIndex by remember { mutableStateOf(0) }

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
    val noActiveWorkshopText = if (currentLang == "ms") "Tiada Bengkel Aktif Hari Ini" else "No Active Workshop Today"

    val liveSyncText = if (currentLang == "ms") "Segerak kehadiran langsung aktif" else "Live attendance sync actively"
    val viewAnalyticsBtnText = if (currentLang == "ms") "Lihat Analitis Terperinci" else "View Detailed Analytics"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardContainerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val statsCardBg2 = if (isDark) Color(0xFF2C2C2C) else Color(0xFFEFEFEF)
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val presentBoxBg = if (isDark) Color(0xFF1E3A24) else Color(0xFFE2F5E1)
    val absentBoxBg = if (isDark) Color(0xFF4A2222) else Color(0xFFFDEAEA)

    DisposableEffect(Unit) {
        val allWorkshopsListener = firestore.collection("workshops")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    totalWorkshopsCount = snapshot.size()
                }
            }
        onDispose { allWorkshopsListener.remove() }
    }

    DisposableEffect(currentUser) {
        var activeWorkshopRegListener: ListenerRegistration? = null
        var allTodayRegListener: ListenerRegistration? = null

        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    staffName = doc.getString("nickname") ?: doc.getString("name") ?: "Staff"
                }
            }
        }

        val dateFormatStandard = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatAlternate = SimpleDateFormat("yyyy-M-d", Locale.getDefault())
        val dateFormatSlash = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = Date()
        val todayStandard = dateFormatStandard.format(currentDate)
        val todayAlternate = dateFormatAlternate.format(currentDate)
        val todaySlash = dateFormatSlash.format(currentDate)

        val workshopListener = firestore.collection("workshops")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.documents.isNotEmpty()) {

                    val todayWorkshops = snapshot.documents.filter { doc ->
                        val dateField = doc.getString("date")
                            ?: doc.getString("workshopDate")
                            ?: doc.getString("tarikh")
                            ?: ""

                        dateField.trim() == todayStandard ||
                                dateField.trim() == todayAlternate ||
                                dateField.trim() == todaySlash
                    }

                    val todayWorkshopIds = todayWorkshops.map { it.id }

                    allTodayRegListener?.remove()
                    if (todayWorkshopIds.isNotEmpty()) {
                        allTodayRegListener = firestore.collection("registrations")
                            .whereIn("workshopId", todayWorkshopIds)
                            .addSnapshotListener { regSnapshots, _ ->
                                if (regSnapshots != null) {
                                    val uniqueRegistrations = mutableMapOf<String, Map<String, Any>>()

                                    for (doc in regSnapshots.documents) {
                                        val data = doc.data ?: continue
                                        val workshopId = data["workshopId"]?.toString() ?: continue
                                        val studentId = data["studentId"]?.toString()
                                            ?: data["userId"]?.toString()
                                            ?: data["email"]?.toString()
                                            ?: continue

                                        val compositeKey = "${workshopId}_$studentId"
                                        uniqueRegistrations[compositeKey] = data
                                    }

                                    var presentCount = 0
                                    var absentCount = 0

                                    // --- KEMASKINI LOGIK: Mesti ada timestamp / check-in yang sah baru dikira Present ---
                                    for ((_, data) in uniqueRegistrations) {
                                        val checkIn = data["timestamp"]
                                            ?: data["checkInTimestamp"]
                                            ?: data["checkInTime"]
                                            ?: data["status"]

                                        val hasCheckedIn = checkIn != null &&
                                                checkIn.toString().trim() != "-" &&
                                                checkIn.toString().trim().isNotBlank() &&
                                                !checkIn.toString().equals("Absent", ignoreCase = true) &&
                                                !checkIn.toString().equals("Not Checked In", ignoreCase = true) &&
                                                !checkIn.toString().equals("Belum Hadir", ignoreCase = true)

                                        val isExplicitlyPresent = checkIn.toString().equals("Present", ignoreCase = true)
                                                || checkIn.toString().equals("Hadir", ignoreCase = true)

                                        if (hasCheckedIn || isExplicitlyPresent) {
                                            presentCount++
                                        } else {
                                            absentCount++
                                        }
                                    }

                                    todayTotalPresent = presentCount
                                    todayTotalAbsent = absentCount
                                }
                            }
                    } else {
                        todayTotalPresent = 0
                        todayTotalAbsent = 0
                    }

                    if (todayWorkshops.isNotEmpty()) {
                        val currentTimeCal = Calendar.getInstance()
                        val currentMinutes = currentTimeCal.get(Calendar.HOUR_OF_DAY) * 60 + currentTimeCal.get(Calendar.MINUTE)

                        fun parseTimeToMinutes(timeStr: String): Int {
                            val cleanStr = timeStr.trim().uppercase()
                            val formats = listOf(
                                SimpleDateFormat("HH:mm", Locale.getDefault()),
                                SimpleDateFormat("H:mm", Locale.getDefault()),
                                SimpleDateFormat("hh:mm a", Locale.getDefault()),
                                SimpleDateFormat("h:mm a", Locale.getDefault())
                            )
                            for (sdf in formats) {
                                try {
                                    sdf.isLenient = false
                                    val date = sdf.parse(cleanStr)
                                    if (date != null) {
                                        val cal = Calendar.getInstance().apply { time = date }
                                        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                                    }
                                } catch (_: Exception) {}
                            }
                            return 0
                        }

                        val sortedTodayWorkshops = todayWorkshops.sortedBy { doc ->
                            val timeStr = doc.getString("time") ?: "00:00"
                            parseTimeToMinutes(timeStr)
                        }

                        val activeOrNextDoc = sortedTodayWorkshops.firstOrNull { doc ->
                            val timeStr = doc.getString("time") ?: "00:00"
                            val startMinutes = parseTimeToMinutes(timeStr)
                            currentMinutes <= (startMinutes + 120)
                        } ?: sortedTodayWorkshops.lastOrNull()

                        if (activeOrNextDoc != null) {
                            val workshopId = activeOrNextDoc.id
                            activeWorkshopId = workshopId

                            val fetchedName = activeOrNextDoc.getString("title")
                                ?: activeOrNextDoc.getString("name")
                                ?: activeOrNextDoc.getString("workshopName")
                                ?: noActiveWorkshopText

                            activeWorkshopName = if (!fetchedName.isNullOrBlank()) fetchedName else noActiveWorkshopText

                            activeWorkshopRegListener?.remove()
                            activeWorkshopRegListener = firestore.collection("registrations")
                                .whereEqualTo("workshopId", workshopId)
                                .addSnapshotListener { regSnapshots, _ ->
                                    if (regSnapshots == null) return@addSnapshotListener

                                    val studentMap = mutableMapOf<String, MutableMap<String, Any>>()
                                    for (doc in regSnapshots.documents) {
                                        val studentKey = doc.getString("studentId")
                                            ?: doc.getString("userId")
                                            ?: doc.getString("email")
                                            ?: doc.id
                                        val existingData = studentMap.getOrPut(studentKey) { mutableMapOf() }
                                        doc.data?.let { existingData.putAll(it) }
                                    }

                                    val totalRegistered = studentMap.size
                                    var activePresentCount = 0

                                    // --- KEMASKINI LOGIK: Mengira peratusan berdasarkan yang sudah check-in sahaja ---
                                    for ((_, data) in studentMap) {
                                        val checkInTimestamp = data["timestamp"]
                                            ?: data["checkInTimestamp"]
                                            ?: data["checkInTime"]
                                            ?: data["status"]

                                        val hasCheckIn = checkInTimestamp != null &&
                                                checkInTimestamp.toString().trim() != "-" &&
                                                checkInTimestamp.toString().trim().isNotBlank() &&
                                                !checkInTimestamp.toString().equals("Absent", ignoreCase = true) &&
                                                !checkInTimestamp.toString().equals("Not Checked In", ignoreCase = true) &&
                                                !checkInTimestamp.toString().equals("Belum Hadir", ignoreCase = true)

                                        val isExplicitlyPresent = checkInTimestamp.toString().equals("Present", ignoreCase = true)
                                                || checkInTimestamp.toString().equals("Hadir", ignoreCase = true)

                                        if (hasCheckIn || isExplicitlyPresent) {
                                            activePresentCount++
                                        }
                                    }

                                    activeWorkshopAttendancePercentage = if (totalRegistered > 0) (activePresentCount.toFloat() / totalRegistered.toFloat()) else 0f
                                }
                        } else {
                            activeWorkshopId = null
                            activeWorkshopName = noActiveWorkshopText
                            activeWorkshopAttendancePercentage = 0f
                            activeWorkshopRegListener?.remove()
                        }
                    } else {
                        activeWorkshopId = null
                        activeWorkshopName = noActiveWorkshopText
                        activeWorkshopAttendancePercentage = 0f
                        activeWorkshopRegListener?.remove()
                    }
                }
            }

        onDispose {
            workshopListener.remove()
            activeWorkshopRegListener?.remove()
            allTodayRegListener?.remove()
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
                            Box(modifier = Modifier.weight(1f).background(presentBoxBg, RoundedCornerShape(8.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$todayTotalPresent", color = Color(0xFF4CAF50), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(text = presentLabel, color = Color(0xFF4CAF50), fontSize = 9.sp)
                                }
                            }
                            Box(modifier = Modifier.weight(1f).background(absentBoxBg, RoundedCornerShape(8.dp)).padding(4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$todayTotalAbsent", color = Color(0xFFF44336), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(text = absentLabel, color = Color(0xFFF44336), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

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
                            progress = { activeWorkshopAttendancePercentage },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF4CAF50),
                            strokeWidth = 6.dp,
                            trackColor = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
                        )
                        val displayPercentage = (activeWorkshopAttendancePercentage * 100).toInt()
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

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = liveSyncText, color = secondaryTextColor, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                activeWorkshopId?.let { id ->
                                    onViewAnalyticsClick(id)
                                }
                            },
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