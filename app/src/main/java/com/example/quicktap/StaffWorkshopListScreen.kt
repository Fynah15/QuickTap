package com.example.quicktap

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.quicktap.AppSettingsState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

// Enum untuk jenis penapisan tarikh
enum class FilterType { TODAY, THIS_WEEK, UPCOMING, ALL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffWorkshopListScreen(
    onBackClick: () -> Unit,
    onCreateNewClick: () -> Unit,
    onEditExistingClick: (String) -> Unit,
    onViewLiveAttendanceClick: (String) -> Unit // Navigasi ke analitis live attendance mengikut workshop ID
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var allWorkshops by remember { mutableStateOf<List<Workshop>>(emptyList()) }
    var filteredWorkshops by remember { mutableStateOf<List<Workshop>>(emptyList()) }
    var currentFilter by remember { mutableStateOf(FilterType.ALL) }
    var isLoading by remember { mutableStateOf(true) }

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val workshopListTitle = if (currentLang == "ms") "Senarai Bengkel" else "Workshop List"
    val createNewBtnText = if (currentLang == "ms") "+ Cipta Bengkel Baru" else "+ Create New Workshop"

    val allFilterText = if (currentLang == "ms") "Semua" else "All"
    val todayFilterText = if (currentLang == "ms") "Hari Ini" else "Today"
    val thisWeekFilterText = if (currentLang == "ms") "Minggu Ini" else "This Week"
    val upcomingFilterText = if (currentLang == "ms") "Akan Datang" else "Upcoming"

    val emptyWorkshopText = if (currentLang == "ms") "Tiada bengkel dijumpai untuk kategori ini." else "No workshops found for this category."
    val failedLoadToast = if (currentLang == "ms") "Gagal memuat data: " else "Failed to load data: "
    val viewAttendanceText = if (currentLang == "ms") "Lihat Kehadiran" else "View Attendance"

    // 2. Sokongan Tema Gelap / Cerah (Dark / Light Mode)
    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    // 3. Ambil data secara Real-time dari Firestore - Susun yang TERKINI (Latest) di atas
    LaunchedEffect(Unit) {
        firestore.collection("workshops")
            .orderBy("date", Query.Direction.DESCENDING)
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
                    allWorkshops = list
                }
            }
    }

    // 4. Logik Pemprosesan Penapisan Tarikh
    LaunchedEffect(allWorkshops, currentFilter) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val flexibleSdf = SimpleDateFormat("yyyy-M-d", Locale.getDefault())

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayDate = todayCal.time

        filteredWorkshops = when (currentFilter) {
            FilterType.ALL -> allWorkshops
            FilterType.TODAY -> {
                allWorkshops.filter {
                    val wDate = try { sdf.parse(it.date) } catch (e: Exception) { flexibleSdf.parse(it.date) }
                    wDate != null && sdf.format(wDate) == sdf.format(todayDate)
                }
            }
            FilterType.THIS_WEEK -> {
                val endOfWeekCal = Calendar.getInstance().apply {
                    time = todayDate
                    add(Calendar.DAY_OF_YEAR, 7)
                }
                allWorkshops.filter {
                    val wDate = try { sdf.parse(it.date) } catch (e: Exception) { flexibleSdf.parse(it.date) }
                    wDate != null && (wDate == todayDate || wDate.after(todayDate)) && wDate.before(endOfWeekCal.time)
                }
            }
            FilterType.UPCOMING -> {
                allWorkshops.filter {
                    val wDate = try { sdf.parse(it.date) } catch (e: Exception) { flexibleSdf.parse(it.date) }
                    wDate != null && wDate.after(todayDate)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workshopListTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323), titleContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF912323),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

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

                    // Bar Butang Penapis Julat Tarikh
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterButton(allFilterText, isActive = currentFilter == FilterType.ALL, isDark = isDark, modifier = Modifier.weight(1f)) { currentFilter = FilterType.ALL }
                        FilterButton(todayFilterText, isActive = currentFilter == FilterType.TODAY, isDark = isDark, modifier = Modifier.weight(1f)) { currentFilter = FilterType.TODAY }
                        FilterButton(thisWeekFilterText, isActive = currentFilter == FilterType.THIS_WEEK, isDark = isDark, modifier = Modifier.weight(1.2f)) { currentFilter = FilterType.THIS_WEEK }
                        FilterButton(upcomingFilterText, isActive = currentFilter == FilterType.UPCOMING, isDark = isDark, modifier = Modifier.weight(1.2f)) { currentFilter = FilterType.UPCOMING }
                    }

                    if (filteredWorkshops.isEmpty()) {
                        Text(
                            text = emptyWorkshopText,
                            color = secondaryTextColor,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally)
                        )
                    } else {
                        filteredWorkshops.forEach { workshop ->
                            val bannerColor = when (workshop.title.lowercase()) {
                                "python", "python programming" -> Color(0xFF0D1B2A)
                                "firebase", "firebase workshop" -> Color(0xFF3E5075)
                                else -> Color(0xFF1F619E)
                            }

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
                                onEditClick = { onEditExistingClick(workshop.id) },
                                onViewAttendanceClick = { onViewLiveAttendanceClick(workshop.id) } // MEMBAWA WORKSHOP ID YANG BETUL
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom UI Button Component untuk Penapis
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
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    onEditClick: () -> Unit,
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

                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Workshop",
                            tint = Color(0xFF912323)
                        )
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