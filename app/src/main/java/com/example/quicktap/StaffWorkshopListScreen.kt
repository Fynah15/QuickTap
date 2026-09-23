package com.example.quicktap

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

data class Workshop(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val registeredCount: String = ""
)

enum class FilterType { TODAY, THIS_WEEK, UPCOMING, PAST, ALL }

private fun parseStaffDate(dateStr: String): Date? {
    if (dateStr.isBlank()) return null
    val cleaned = dateStr.replace(Regex("(?i)^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),?\\s*"), "").trim()

    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("yyyy-M-d", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("d/M/yyyy", Locale.ENGLISH),
        SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH),
        SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH),
        SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    )

    for (f in formats) {
        f.isLenient = true
        try {
            val parsed = f.parse(cleaned)
            if (parsed != null) return parsed
        } catch (_: Exception) { }
    }
    return null
}

private fun parseStaffWorkshopDateTime(dateStr: String, timeStr: String): Date? {
    val baseDate = parseStaffDate(dateStr) ?: return null
    if (timeStr.isBlank()) return baseDate

    val timeCleaned = timeStr.trim().uppercase(Locale.ENGLISH)
    val timeFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)
    )

    val dateOnlyStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(baseDate)
    val combinedStr = "$dateOnlyStr $timeCleaned"

    for (f in timeFormats) {
        f.isLenient = true
        try {
            val parsed = f.parse(combinedStr)
            if (parsed != null) return parsed
        } catch (_: Exception) { }
    }
    return baseDate
}

private fun stripStaffTime(date: Date): Date {
    val cal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.time
}

private fun isWorkshopPastDate(dateStr: String, timeStr: String): Boolean {
    val fullDateTime = parseStaffWorkshopDateTime(dateStr, timeStr) ?: parseStaffDate(dateStr) ?: return false
    return fullDateTime.before(Date())
}

@Composable
fun StaffWorkshopListScreen(
    onBackClick: () -> Unit,
    onCreateNewClick: () -> Unit,
    onEditExistingClick: (String) -> Unit,
    onViewLiveAttendanceClick: (String) -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var allWorkshops by remember { mutableStateOf<List<Workshop>>(emptyList()) }
    var filteredWorkshops by remember { mutableStateOf<List<Workshop>>(emptyList()) }
    var currentFilter by remember { mutableStateOf(FilterType.ALL) }
    var isLoading by remember { mutableStateOf(true) }

    val currentLang = AppSettingsState.currentLanguage
    val createNewBtnText = if (currentLang == "ms") "+ Cipta Bengkel Baru" else "+ Create New Workshop"

    val allFilterText = if (currentLang == "ms") "Semua" else "All"
    val todayFilterText = if (currentLang == "ms") "Hari Ini" else "Today"
    val thisWeekFilterText = if (currentLang == "ms") "Minggu Ini" else "This Week"
    val upcomingFilterText = if (currentLang == "ms") "Akan Datang" else "Upcoming"
    val pastFilterText = if (currentLang == "ms") "Telah Tamat" else "Past"

    val emptyWorkshopText = if (currentLang == "ms") "Tiada bengkel dijumpai untuk kategori ini." else "No workshops found for this category."
    val failedLoadToast = if (currentLang == "ms") "Gagal memuat data: " else "Failed to load data: "
    val viewAttendanceText = if (currentLang == "ms") "Lihat Kehadiran" else "View Attendance"
    val deleteSuccessText = if (currentLang == "ms") "Bengkel berjaya dipadam" else "Workshop deleted successfully"
    val deleteFailedText = if (currentLang == "ms") "Gagal memadam bengkel: " else "Failed to delete workshop: "

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    LaunchedEffect(Unit) {
        firestore.collection("workshops")
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) {
                    Toast.makeText(context, "$failedLoadToast${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.map { document ->
                        Workshop(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            description = document.getString("description") ?: "",
                            date = document.getString("date") ?: "",
                            time = document.getString("time") ?: "",
                            location = document.getString("location") ?: "",
                            registeredCount = document.getString("registeredCount") ?: "0/35 registered"
                        )
                    }

                    val sortedList = list.sortedByDescending { w ->
                        parseStaffDate(w.date)?.time ?: 0L
                    }

                    allWorkshops = sortedList
                }
            }
    }

    LaunchedEffect(allWorkshops, currentFilter) {
        val today = Date()
        val todayStripped = stripStaffTime(today)

        val cal = Calendar.getInstance().apply {
            time = todayStripped
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val startOfWeek = cal.time
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val endOfWeek = cal.time

        filteredWorkshops = when (currentFilter) {
            FilterType.ALL -> allWorkshops
            FilterType.TODAY -> {
                allWorkshops.filter {
                    parseStaffDate(it.date)?.let { d ->
                        stripStaffTime(d).time == todayStripped.time && !isWorkshopPastDate(it.date, it.time)
                    } ?: false
                }
            }
            FilterType.THIS_WEEK -> {
                allWorkshops.filter {
                    val fullDt = parseStaffWorkshopDateTime(it.date, it.time) ?: parseStaffDate(it.date)
                    if (fullDt == null || fullDt.before(today)) false
                    else {
                        val wDateStripped = stripStaffTime(fullDt)
                        wDateStripped.time >= startOfWeek.time && wDateStripped.time <= endOfWeek.time
                    }
                }
            }
            FilterType.UPCOMING -> {
                allWorkshops.filter {
                    val fullDt = parseStaffWorkshopDateTime(it.date, it.time) ?: parseStaffDate(it.date)
                    fullDt != null && fullDt.after(today)
                }
            }
            FilterType.PAST -> {
                allWorkshops.filter { isWorkshopPastDate(it.date, it.time) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed Top Section: Create Button (pushed down slightly) & Filter Buttons
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedButton(
                onClick = onCreateNewClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDark) Color(0xFF1E1E1E) else Color.Transparent,
                    contentColor = textColor
                )
            ) {
                Text(createNewBtnText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterButton(allFilterText, isActive = currentFilter == FilterType.ALL, isDark = isDark, modifier = Modifier.weight(1f)) { currentFilter = FilterType.ALL }
                FilterButton(todayFilterText, isActive = currentFilter == FilterType.TODAY, isDark = isDark, modifier = Modifier.weight(1f)) { currentFilter = FilterType.TODAY }
                FilterButton(thisWeekFilterText, isActive = currentFilter == FilterType.THIS_WEEK, isDark = isDark, modifier = Modifier.weight(1.1f)) { currentFilter = FilterType.THIS_WEEK }
                FilterButton(upcomingFilterText, isActive = currentFilter == FilterType.UPCOMING, isDark = isDark, modifier = Modifier.weight(1.1f)) { currentFilter = FilterType.UPCOMING }
                FilterButton(pastFilterText, isActive = currentFilter == FilterType.PAST, isDark = isDark, modifier = Modifier.weight(1f)) { currentFilter = FilterType.PAST }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content Section: Only the list cards scroll
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF912323))
                }
            } else if (filteredWorkshops.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = emptyWorkshopText,
                        color = secondaryTextColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredWorkshops) { workshop ->
                        val bannerColor = Color(0xFF1F619E)
                        val isPastEvent = isWorkshopPastDate(workshop.date, workshop.time)

                        WorkshopItemCard(
                            title = workshop.title,
                            subtitle = workshop.description,
                            date = workshop.date,
                            time = workshop.time,
                            location = workshop.location,
                            regCount = workshop.registeredCount,
                            bannerColor = bannerColor,
                            cardBgColor = cardBgColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            viewAttendanceText = viewAttendanceText,
                            isPastEvent = isPastEvent,
                            onEditClick = { onEditExistingClick(workshop.id) },
                            onDeleteClick = {
                                firestore.collection("workshops").document(workshop.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, deleteSuccessText, Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "$deleteFailedText${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            onViewAttendanceClick = { onViewLiveAttendanceClick(workshop.id) }
                        )
                    }
                }
            }
        }
    }
}

fun registerStudentForWorkshop(
    workshopId: String,
    studentId: String,
    studentName: String,
    studentNumber: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val registrationId = "${workshopId}_$studentId"

    val registrationData = mapOf(
        "workshopId" to workshopId,
        "studentId" to studentId,
        "fullName" to studentName,
        "name" to studentName,
        "studentNumber" to studentNumber,
        "status" to "REGISTERED"
    )

    firestore.collection("registrations")
        .document(registrationId)
        .set(registrationData, SetOptions.merge())
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onError(e.message ?: "Unknown error") }
}

@Composable
fun FilterButton(
    text: String,
    isActive: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFF4A90E2) else if (isDark) Color(0xFF1E1E1E) else Color.White,
            contentColor = if (isActive) Color.White else if (isDark) Color.LightGray else Color(0xFF555555)
        ),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WorkshopItemCard(
    title: String,
    subtitle: String,
    date: String,
    time: String,
    location: String,
    regCount: String,
    bannerColor: Color,
    cardBgColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    viewAttendanceText: String,
    isPastEvent: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewAttendanceClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(75.dp).background(bannerColor), contentAlignment = Alignment.Center) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(subtitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                    }

                    if (!isPastEvent) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Workshop",
                                tint = Color(0xFF912323)
                            )
                        }
                    } else {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Past Workshop",
                                tint = Color(0xFFB00020)
                            )
                        }
                    }
                }

                Text("📅  $date", color = secondaryTextColor, fontSize = 11.sp)
                Text("🕒  $time", color = secondaryTextColor, fontSize = 11.sp)
                Text("📍  $location", color = secondaryTextColor, fontSize = 11.sp)
                Text("👤  $regCount", color = secondaryTextColor, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onViewAttendanceClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = viewAttendanceText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}