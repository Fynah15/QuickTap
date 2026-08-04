package com.example.quicktap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quicktap.AppSettingsState
import kotlinx.coroutines.delay

@Composable
fun StaffAttendanceSuccessScreen(
    status: String,
    studentName: String,
    studentId: String,
    workshopName: String,
    timeText: String,
    onNextScanClick: () -> Unit
) {
    // Auto-dismiss selepas 1.5 saat supaya boleh imbas pelajar seterusnya secara automatik
    LaunchedEffect(Unit) {
        delay(1500L)
        onNextScanClick()
    }

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val statusTranslated = if (status.equals("Check-In", ignoreCase = true)) {
        if (currentLang == "ms") "Daftar Masuk" else "Check-In"
    } else {
        if (currentLang == "ms") "Daftar Keluar" else "Check-Out"
    }

    val successLabel = if (currentLang == "ms") " Berjaya" else " Successful"
    val capturedAtLabel = if (currentLang == "ms") "Direkodkan pada: $timeText" else "Captured at: $timeText"
    val scanNextText = if (currentLang == "ms") "Imbas Pelajar Seterusnya" else "Scan Next Student"

    // 2. Sokongan Tema Gelap / Cerah (Dark / Light Mode)
    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val timeTextColor = if (isDark) Color(0xFFCCCCCC) else Color.DarkGray

    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$statusTranslated$successLabel",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF62D662), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = studentName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )

            Text(
                text = "ID: $studentId",
                fontSize = 14.sp,
                color = secondaryTextColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Masa dipaparkan di sini
            Text(
                text = capturedAtLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = timeTextColor
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onNextScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(scanNextText, color = Color.White)
            }
        }
    }
}