package com.example.quicktap.dashboard.student

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
import com.example.quicktap.AppSettingsState
import com.google.firebase.firestore.FirebaseFirestore

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

    // Ambil data sejarah pendaftaran & kehadiran pelajar dari Firestore
    LaunchedEffect(studentId) {
        val TAG = "HistoryDebug"
        Log.d(TAG, "Mula mencari history untuk studentId: $studentId")

        firestore.collection("registrations")
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { regSnapshot ->
                val tempList = mutableListOf<WorkshopHistoryItem>()
                val workshopsProcessed = regSnapshot.documents.size

                if (workshopsProcessed == 0) {
                    isLoading = false
                    return@addOnSuccessListener
                }

                regSnapshot.documents.forEach { regDoc ->
                    val workshopId = regDoc.getString("workshopId") ?: ""

                    val checkIn = regDoc.get("checkInTimestamp")
                        ?: regDoc.get("checkInTime")
                        ?: regDoc.get("timestamp")

                    val checkOut = regDoc.get("checkOutTimestamp")
                        ?: regDoc.get("checkOutTime")

                    val hasCheckedIn = checkIn != null
                    val hasCheckedOut = checkOut != null

                    // Syarat kelayakan sijil: Pelajar wajib lengkap check-in DAN check-out
                    val isEligible = hasCheckedIn && hasCheckedOut

                    if (workshopId.isNotEmpty()) {
                        firestore.collection("workshops").document(workshopId).get()
                            .addOnSuccessListener { workshopDoc ->
                                val title = workshopDoc.getString("title") ?: workshopDoc.getString("name") ?: "Workshop"
                                val date = workshopDoc.getString("date") ?: "Recent"

                                val checkInStr = if (hasCheckedIn) checkedInText else pendingText
                                val checkOutStr = if (hasCheckedOut) checkedOutText else pendingText

                                tempList.add(
                                    WorkshopHistoryItem(
                                        workshopId = workshopId,
                                        workshopTitle = title,
                                        date = date,
                                        checkInStatus = checkInStr,
                                        checkOutStatus = checkOutStr,
                                        isEligibleForCert = isEligible
                                    )
                                )

                                if (tempList.size == workshopsProcessed) {
                                    historyList.clear()
                                    historyList.addAll(tempList)
                                    isLoading = false
                                }
                            }
                            .addOnFailureListener {
                                if (tempList.size == workshopsProcessed) {
                                    isLoading = false
                                }
                            }
                    } else {
                        if (tempList.size == workshopsProcessed) {
                            isLoading = false
                        }
                    }
                }
            }
            .addOnFailureListener {
                isLoading = false
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
                        items(historyList) { item ->
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