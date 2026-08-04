package com.example.quicktap

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quicktap.AppSettingsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.quicktap.utils.scheduleWorkshopReminders
import com.example.quicktap.utils.cancelWorkshopReminders
import java.text.SimpleDateFormat
import java.util.*

data class StudentWorkshop(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val currentCount: Int = 0,
    val maxSlots: Int = 0,
    val isRegistered: Boolean = false,
    val isPast: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopListScreen(
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onHomeClick: () -> Unit = {},
    onWorkshopClick: () -> Unit = {},
    onCertificateClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val authUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = authUser?.uid ?: "STUDENT_12345"

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("All") }

    val workshopList = remember { mutableStateListOf<StudentWorkshop>() }
    var isLoading by remember { mutableStateOf(true) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedWorkshop by remember { mutableStateOf<StudentWorkshop?>(null) }
    var fallbackStudentId by remember { mutableStateOf("") }

    val currentLang = AppSettingsState.currentLanguage
    val workshopsTitle = if (currentLang == "ms") "Bengkel" else "Workshops"
    val searchPlaceholder = if (currentLang == "ms") "Cari bengkel..." else "Search workshops..."

    val tabAll = if (currentLang == "ms") "Semua" else "All"
    val tabToday = if (currentLang == "ms") "Hari Ini" else "Today"
    val tabWeek = if (currentLang == "ms") "Minggu Ini" else "This week"
    val tabUpcoming = if (currentLang == "ms") "Akan Datang" else "Upcoming"

    val navHome = if (currentLang == "ms") "Utama" else "Home"
    val navWorkshop = if (currentLang == "ms") "Bengkel" else "Workshop"
    val navCertificate = if (currentLang == "ms") "Sijil" else "Certificate"
    val navSettings = if (currentLang == "ms") "Tetapan" else "Settings"

    val dialogTitle = if (currentLang == "ms") "Pengesahan Pendaftaran" else "Registration Confirmation"
    val registerForText = if (currentLang == "ms") "Adakah anda pasti mahu mendaftar untuk: " else "Are you sure you want to register for: "
    val confirmBtnText = if (currentLang == "ms") "Sahkan" else "Confirm"
    val cancelBtnText = if (currentLang == "ms") "Batal" else "Cancel"

    val successMsg = if (currentLang == "ms") "Berjaya Didaftar!" else "Successfully Registered!"
    val unregisterMsg = if (currentLang == "ms") "Pendaftaran dibatalkan." else "Registration cancelled."
    val fullMsg = if (currentLang == "ms") "Bengkel sudah penuh!" else "Workshop is full!"
    val pastEventMsg = if (currentLang == "ms") "Bengkel ini telah tamat." else "This workshop has already passed."

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    LaunchedEffect(currentUserId) {
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val dbStudentId = doc.getString("studentId") ?: doc.getString("matrixNo") ?: doc.getString("idNumber") ?: ""
                    fallbackStudentId = if (dbStudentId.isNotBlank()) dbStudentId else "STU${(10000..99999).random()}"
                } else {
                    fallbackStudentId = "STU${(10000..99999).random()}"
                }
            }
            .addOnFailureListener {
                fallbackStudentId = "STU${(10000..99999).random()}"
            }

        firestore.collection("workshops").addSnapshotListener { snapshot, e ->
            if (e == null && snapshot != null) {
                val workshops = snapshot.documents

                firestore.collection("registrations")
                    .whereEqualTo("studentId", currentUserId)
                    .addSnapshotListener { regSnapshot, _ ->
                        val registeredWorkshopIds = regSnapshot?.documents?.mapNotNull { it.getString("workshopId") } ?: emptyList()

                        workshopList.clear()
                        val newList = workshops.map { doc ->
                            val regString = doc.getString("registeredCount") ?: "0/0"
                            val parts = regString.split("/")
                            val current = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                            val max = parts.getOrNull(1)?.trim()?.split(" ")?.getOrNull(0)?.toIntOrNull() ?: 0
                            val wId = doc.id
                            val wDate = doc.getString("date") ?: ""

                            StudentWorkshop(
                                id = wId,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                date = wDate,
                                time = doc.getString("time") ?: "",
                                location = doc.getString("location") ?: "",
                                currentCount = current,
                                maxSlots = max,
                                isRegistered = registeredWorkshopIds.contains(wId),
                                isPast = isPastDate(wDate)
                            )
                        }
                        workshopList.addAll(newList)
                        isLoading = false
                    }
            }
        }
    }

    val filteredWorkshops = remember(searchQuery, selectedTab, workshopList.toList()) {
        workshopList.filter { workshop ->
            val matchesSearch = searchQuery.isEmpty() ||
                    workshop.title.contains(searchQuery, ignoreCase = true) ||
                    workshop.description.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                "Today" -> isSameDay(workshop.date, Date())
                "This week" -> isWithinThisWeek(workshop.date)
                "Upcoming" -> isFutureDate(workshop.date)
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workshopsTitle, color = Color.White, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF912323),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = navHome) },
                    label = { Text(navHome, color = Color.White) },
                    selected = false,
                    onClick = onHomeClick,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        indicatorColor = Color(0xFF7A1B1B)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = navWorkshop) },
                    label = { Text(navWorkshop, color = Color.White) },
                    selected = true,
                    onClick = onWorkshopClick,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        indicatorColor = Color(0xFF7A1B1B)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = navCertificate) },
                    label = { Text(navCertificate, color = Color.White) },
                    selected = false,
                    onClick = onCertificateClick,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        indicatorColor = Color(0xFF7A1B1B)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = navSettings) },
                    label = { Text(navSettings, color = Color.White) },
                    selected = false,
                    onClick = onSettingsClick,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        indicatorColor = Color(0xFF7A1B1B)
                    )
                )
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(searchPlaceholder, color = secondaryTextColor) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = secondaryTextColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBgColor,
                    unfocusedContainerColor = cardBgColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = Color(0xFF912323),
                    unfocusedBorderColor = secondaryTextColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val tabs = listOf(
                Pair("All", tabAll),
                Pair("Today", tabToday),
                Pair("This week", tabWeek),
                Pair("Upcoming", tabUpcoming)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { (tabKey, tabLabel) ->
                    FilterTab(
                        text = tabLabel,
                        isSelected = selectedTab == tabKey,
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        onTabClick = { selectedTab = tabKey }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF912323))
                }
            } else if (filteredWorkshops.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (currentLang == "ms") "Tiada bengkel dijumpai." else "No workshops found.",
                        color = secondaryTextColor,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredWorkshops, key = { it.id }) { workshop ->
                        WorkshopCard(
                            title = workshop.title,
                            subtitle = workshop.description,
                            date = workshop.date,
                            time = workshop.time,
                            location = workshop.location,
                            slots = "${workshop.currentCount}/${workshop.maxSlots} " + (if (currentLang == "ms") "didaftar" else "registered"),
                            bannerColor = Color(0xFF1F619E),
                            cardBgColor = cardBgColor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            isRegistered = workshop.isRegistered,
                            isPast = workshop.isPast,
                            registerLabel = if (currentLang == "ms") "Daftar Sekarang" else "Register Now",
                            unregisterLabel = if (currentLang == "ms") "Batalkan Pendaftaran" else "Unregister",
                            pastLabel = if (currentLang == "ms") "Telah Tamat" else "Event Ended",
                            onActionClick = {
                                if (workshop.isPast && !workshop.isRegistered) {
                                    Toast.makeText(context, pastEventMsg, Toast.LENGTH_SHORT).show()
                                    return@WorkshopCard
                                }

                                if (workshop.isRegistered) {
                                    val workshopRef = firestore.collection("workshops").document(workshop.id)
                                    val regDocRef = firestore.collection("registrations").document("${workshop.id}_$currentUserId")

                                    firestore.runTransaction { transaction ->
                                        val snapshot = transaction.get(workshopRef)
                                        val regString = snapshot.getString("registeredCount") ?: "0/0"
                                        val parts = regString.split("/")
                                        val currentCount = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 1
                                        val maxSlots = parts.getOrNull(1)?.trim()?.split(" ")?.getOrNull(0)?.toIntOrNull() ?: 0

                                        val newCount = if (currentCount > 0) currentCount - 1 else 0

                                        transaction.delete(regDocRef)
                                        transaction.update(workshopRef, "registeredCount", "$newCount/$maxSlots registered")
                                    }.addOnSuccessListener {
                                        cancelWorkshopReminders(context, workshop.id, currentUserId)
                                        Toast.makeText(context, unregisterMsg, Toast.LENGTH_SHORT).show()
                                        onRegisterSuccess()
                                    }.addOnFailureListener { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    selectedWorkshop = workshop
                                    showDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showDialog && selectedWorkshop != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = cardBgColor,
                title = { Text(dialogTitle, fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    Text("$registerForText'${selectedWorkshop?.title}'?", fontSize = 14.sp, color = secondaryTextColor)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val workshopId = selectedWorkshop!!.id
                            val workshopRef = firestore.collection("workshops").document(workshopId)
                            val globalRegistrationDocRef = firestore.collection("registrations").document("${workshopId}_$currentUserId")

                            firestore.collection("users").document(currentUserId).get().addOnSuccessListener { userDoc ->
                                val fetchedFullName = userDoc.getString("fullName") ?: userDoc.getString("name") ?: "Student"
                                val fetchedStudentId = userDoc.getString("studentId") ?: userDoc.getString("matrixNo") ?: userDoc.getString("idNumber") ?: fallbackStudentId

                                firestore.runTransaction { transaction ->
                                    val snapshot = transaction.get(workshopRef)
                                    val regString = snapshot.getString("registeredCount") ?: "0/0"
                                    val parts = regString.split("/")
                                    val currentCount = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
                                    val maxSlots = parts.getOrNull(1)?.trim()?.split(" ")?.getOrNull(0)?.toIntOrNull() ?: 0

                                    if (currentCount < maxSlots) {
                                        val newCount = currentCount + 1

                                        // Hanya data pendaftaran biasa (status REGISTERED), TIADA medan timestamp / checkIn
                                        transaction.set(globalRegistrationDocRef, mapOf(
                                            "workshopId" to workshopId,
                                            "studentId" to currentUserId,
                                            "studentNumber" to fetchedStudentId,
                                            "fullName" to fetchedFullName,
                                            "name" to fetchedFullName,
                                            "status" to "REGISTERED"
                                        ))

                                        transaction.update(workshopRef, "registeredCount", "$newCount/$maxSlots registered")
                                        null
                                    } else {
                                        throw Exception("Full")
                                    }
                                }.addOnSuccessListener {
                                    val w = selectedWorkshop!!
                                    scheduleWorkshopReminders(
                                        context, w.id, w.title, w.date, w.time, currentUserId
                                    )
                                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                                    showDialog = false
                                    onRegisterSuccess()
                                }.addOnFailureListener { e ->
                                    val message = if (e.message == "Full") fullMsg else "Error: ${e.message}"
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(context, "Failed to load user profile.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                    ) { Text(confirmBtnText, color = Color.White) }
                },
                dismissButton = {
                    Button(
                        onClick = { showDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF912323))
                    ) {
                        Text(cancelBtnText, color = Color.White)
                    }
                }
            )
        }
    }
}

fun parseDate(dateStr: String): Date? {
    if (dateStr.isBlank()) return null
    val cleaned = dateStr.replace(Regex("(?i)^(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),?\\s*"), "").trim()

    val formats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
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

fun stripTime(date: Date): Date {
    val cal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.time
}

fun isSameDay(dateStr: String, targetDate: Date): Boolean {
    val d = parseDate(dateStr) ?: return false
    return stripTime(d).time == stripTime(targetDate).time
}

fun isWithinThisWeek(dateStr: String): Boolean {
    val d = parseDate(dateStr) ?: return false
    val workshopDate = stripTime(d)
    val today = stripTime(Date())

    val cal = Calendar.getInstance().apply {
        time = today
        firstDayOfWeek = Calendar.MONDAY
    }

    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val startOfWeek = stripTime(cal.time)

    cal.add(Calendar.DAY_OF_YEAR, 6)
    val endOfWeek = stripTime(cal.time)

    return (workshopDate.time >= startOfWeek.time) && (workshopDate.time <= endOfWeek.time)
}

fun isFutureDate(dateStr: String): Boolean {
    val d = parseDate(dateStr) ?: return false
    val workshopDate = stripTime(d)
    val today = stripTime(Date())
    return workshopDate.after(today)
}

fun isPastDate(dateStr: String): Boolean {
    val d = parseDate(dateStr) ?: return false
    val workshopDate = stripTime(d)
    val today = stripTime(Date())
    return workshopDate.before(today)
}

@Composable
fun FilterTab(text: String, isSelected: Boolean, modifier: Modifier = Modifier, isDark: Boolean, onTabClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) Color(0xFF4A90E2) else if (isDark) Color(0xFF1E1E1E) else Color.White)
            .clickable { onTabClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else if (isDark) Color.LightGray else Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WorkshopCard(
    title: String,
    subtitle: String,
    date: String,
    time: String,
    location: String,
    slots: String,
    bannerColor: Color,
    cardBgColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    isRegistered: Boolean,
    isPast: Boolean,
    registerLabel: String,
    unregisterLabel: String,
    pastLabel: String,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(bannerColor)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = subtitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "📅  $date", fontSize = 12.sp, color = secondaryTextColor)
                Text(text = "⏰  $time", fontSize = 12.sp, color = secondaryTextColor)
                Text(text = "📍  $location", fontSize = 12.sp, color = secondaryTextColor)
                Text(text = "👤  $slots", fontSize = 12.sp, color = secondaryTextColor)

                Spacer(modifier = Modifier.height(8.dp))

                val buttonEnabled = !(isPast && !isRegistered)
                val buttonContainerColor = when {
                    isRegistered -> Color(0xFFC62828)
                    isPast -> Color.Gray
                    else -> Color(0xFF4A90E2)
                }

                Button(
                    onClick = onActionClick,
                    enabled = buttonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainerColor,
                        disabledContainerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isRegistered) unregisterLabel else if (isPast) pastLabel else registerLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}