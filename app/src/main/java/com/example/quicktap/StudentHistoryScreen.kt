package com.example.quicktap

import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class WorkshopHistoryItem(
    val workshopId: String,
    val workshopTitle: String,
    val date: String,
    val checkInStatus: String,
    val checkOutStatus: String,
    val isEligibleForCert: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHistoryScreen(
    studentId: String,
    onBackClick: () -> Unit,
    onViewCertificateClick: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var historyList = remember { mutableStateListOf<WorkshopHistoryItem>() }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val historyTitle = if (currentLang == "ms") "Sejarah & Sijil Bengkel" else "Workshop History & Certificates"
    val noHistoryText = if (currentLang == "ms") "Tiada sejarah bengkel dijumpai." else "No workshop history found."
    val dateLabel = if (currentLang == "ms") "Tarikh: " else "Date: "
    val checkedInText = if (currentLang == "ms") "Sudah Daftar Masuk" else "Checked-In"
    val checkedOutText = if (currentLang == "ms") "Sudah Daftar Keluar" else "Checked-Out"
    val pendingText = if (currentLang == "ms") "Dalam Proses" else "Pending"
    val viewCertText = if (currentLang == "ms") "Lihat Sijil 🏆" else "View Certificate 🏆"
    val certReadyText = if (currentLang == "ms") "Sijil Layak Dimuat Turun" else "Certificate Available"

    // 2. Sokongan Tema Gelap / Cerah (Dark / Light Mode)
    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF9F9F9)
    val borderColor = if (isDark) Color(0xFF333333) else Color.LightGray
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    // Real-time snapshot listener dengan DisposableEffect yang sah pada skop Composable
    DisposableEffect(studentId) {
        val TAG = "HistoryDebug"
        if (studentId.isEmpty()) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        Log.d(TAG, "Memulakan real-time listener untuk studentId: $studentId")

        val listenerRegistration = firestore.collection("registrations")
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { regSnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Ralat mendengar perubahan pendaftaran", error)
                    isLoading = false
                    return@addSnapshotListener
                }

                if (regSnapshot == null || regSnapshot.isEmpty) {
                    historyList.clear()
                    isLoading = false
                    return@addSnapshotListener
                }

                // Lancarkan coroutine yang selamat menggunakan remembered scope
                coroutineScope.launch {
                    val tempList = mutableListOf<WorkshopHistoryItem>()

                    for (regDoc in regSnapshot.documents) {
                        val workshopId = regDoc.getString("workshopId") ?: ""
                        val hasCheckedIn = AttendanceUtils.hasNfcCheckIn(regDoc)
                        val hasCheckedOut = AttendanceUtils.hasNfcCheckOut(regDoc)
                        val isEligible = AttendanceUtils.isEligibleForCertificate(regDoc)

                        var workshopTitle = "Workshop"
                        var workshopDate = "Recent"

                        if (workshopId.isNotEmpty()) {
                            try {
                                val workshopDoc = firestore.collection("workshops").document(workshopId).get().await()
                                if (workshopDoc.exists()) {
                                    workshopTitle = workshopDoc.getString("title") ?: workshopDoc.getString("name") ?: "Workshop"
                                    workshopDate = workshopDoc.getString("date") ?: "Recent"
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Gagal ambil info workshop: $workshopId", e)
                            }
                        }

                        val checkInStr = if (hasCheckedIn) checkedInText else pendingText
                        val checkOutStr = if (hasCheckedOut) checkedOutText else pendingText

                        tempList.add(
                            WorkshopHistoryItem(
                                workshopId = workshopId,
                                workshopTitle = workshopTitle,
                                date = workshopDate,
                                checkInStatus = checkInStr,
                                checkOutStatus = checkOutStr,
                                isEligibleForCert = isEligible
                            )
                        )
                    }

                    historyList.clear()
                    historyList.addAll(tempList)
                    isLoading = false
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(historyTitle, color = Color.White, fontSize = 16.sp) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = Color(0xFF912323))
                }
                historyList.isEmpty() -> {
                    Text(
                        text = noHistoryText,
                        color = secondaryTextColor,
                        fontSize = 16.sp
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList, key = { it.workshopId }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = item.workshopTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$dateLabel${item.date}",
                                        fontSize = 13.sp,
                                        color = secondaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Check-In: ${item.checkInStatus}",
                                                fontSize = 12.sp,
                                                color = secondaryTextColor
                                            )
                                            Text(
                                                text = "Check-Out: ${item.checkOutStatus}",
                                                fontSize = 12.sp,
                                                color = secondaryTextColor
                                            )

                                            if (item.isEligibleForCert) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = certReadyText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        if (item.isEligibleForCert) {
                                            Button(
                                                onClick = { onViewCertificateClick(item.workshopId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(viewCertText, color = Color.White, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}