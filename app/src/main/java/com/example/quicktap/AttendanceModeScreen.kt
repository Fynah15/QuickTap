package com.example.quicktap

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceModeScreen(
    workshopId: String = "default_workshop",
    onModeSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf("") }

    val checkInInteractionSource = remember { MutableInteractionSource() }
    val checkOutInteractionSource = remember { MutableInteractionSource() }
    val isCheckInPressed by checkInInteractionSource.collectIsPressedAsState()
    val isCheckOutPressed by checkOutInteractionSource.collectIsPressedAsState()

    val currentLang = AppSettingsState.currentLanguage
    val titleText = if (currentLang == "ms") "Attendance Mode" else "Attendance Mode"
    val chooseModeText = if (currentLang == "ms") "Choose Mode Attendance" else "Choose Attendance Mode"
    val checkInText = if (currentLang == "ms") "Check-in" else "Check-In"
    val checkOutText = if (currentLang == "ms") "Check-out" else "Check-Out"

    val backgroundColor = if (AppSettingsState.isDarkMode) Color(0xFF121212) else Color.White
    val textColor = if (AppSettingsState.isDarkMode) Color.White else Color.Black

    LaunchedEffect(workshopId) {
        if (workshopId != "default_workshop") {
            FirebaseFirestore.getInstance().collection("workshops").document(workshopId)
                .get()
                .addOnSuccessListener { doc ->
                    val currentMode = doc.getString("attendanceMode") ?: ""
                    selectedMode = currentMode
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(titleText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
            )
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
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = chooseModeText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            val targetCheckInColor: Color = when {
                isCheckInPressed -> Color(0xFF1D62B4)
                selectedMode == "Check-In" -> Color(0xFF4A90E2)
                else -> if (AppSettingsState.isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
            }

            val checkInButtonColor by animateColorAsState(
                targetValue = targetCheckInColor,
                label = "CheckInColor"
            )

            Button(
                onClick = {
                    selectedMode = "Check-In"
                    setLiveAttendanceStatusInFirebase(context = context, workshopId = workshopId, mode = "Check-In")
                    onModeSelected("Check-In")
                },
                interactionSource = checkInInteractionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = checkInButtonColor,
                    disabledContainerColor = checkInButtonColor
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp, pressedElevation = 8.dp)
            ) {
                val checkInTextColor = when {
                    selectedMode == "Check-In" || isCheckInPressed -> Color.White
                    AppSettingsState.isDarkMode -> Color.LightGray
                    else -> Color.Black
                }

                Text(
                    text = checkInText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = checkInTextColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val targetCheckOutColor: Color = when {
                isCheckOutPressed -> Color(0xFF1D62B4)
                selectedMode == "Check-Out" -> Color(0xFF4A90E2)
                else -> if (AppSettingsState.isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
            }

            val checkOutButtonColor by animateColorAsState(
                targetValue = targetCheckOutColor,
                label = "CheckOutColor"
            )

            Button(
                onClick = {
                    selectedMode = "Check-Out"
                    setLiveAttendanceStatusInFirebase(context = context, workshopId = workshopId, mode = "Check-Out")
                    onModeSelected("Check-Out")
                },
                interactionSource = checkOutInteractionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = checkOutButtonColor,
                    disabledContainerColor = checkOutButtonColor
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp, pressedElevation = 8.dp)
            ) {
                val checkOutTextColor = when {
                    selectedMode == "Check-Out" || isCheckOutPressed -> Color.White
                    AppSettingsState.isDarkMode -> Color.LightGray
                    else -> Color.Black
                }

                Text(
                    text = checkOutText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = checkOutTextColor
                )
            }
        }
    }
}

fun setLiveAttendanceStatusInFirebase(context: android.content.Context, workshopId: String, mode: String) {
    if (workshopId == "default_workshop") {
        Toast.makeText(context, "Attendance mode changed locally (Simulation)", Toast.LENGTH_SHORT).show()
        return
    }

    val firestore = FirebaseFirestore.getInstance()
    val updateData = hashMapOf<String, Any>(
        "attendanceMode" to mode
    )

    firestore.collection("workshops").document(workshopId)
        .update(updateData)
        .addOnSuccessListener {
            Toast.makeText(context, "Attendance Mode: $mode saved to Firebase!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to update Firebase: ${e.message}", Toast.LENGTH_LONG).show()
        }
}