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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class CertificateHistoryItem(
    val workshopId: String,
    val workshopTitle: String,
    val date: String,
    val checkInTimeText: String,
    val checkOutTimeText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHistoryScreen(
    studentId: String,
    onBackClick: () -> Unit,
    onViewCertificateClick: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var certificateList = remember { mutableStateListOf<CertificateHistoryItem>() }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val historyTitle = "Your Workshop Certificates"
    val noHistoryText = "No workshop certificates available yet."
    val dateLabel = "Date: "
    val viewCertText = "View Certificate"
    val certReadyText = "Official Certificate Available"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE5E7EB)
    val textColor = if (isDark) Color.White else Color(0xFF1F2937)
    val secondaryTextColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    DisposableEffect(studentId, currentUserId) {
        if (studentId.isEmpty() && currentUserId == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val possibleIds = listOf(studentId, "STU_${studentId.takeLast(6)}", studentId.uppercase(), studentId.lowercase()).filter { it.isNotEmpty() }
        val allDocsMap = mutableMapOf<String, DocumentSnapshot>()
        val listeners = mutableListOf<ListenerRegistration>()

        fun updateUiIfReady() {
            coroutineScope.launch {
                processCertificateDocs(
                    docs = allDocsMap.values.toList(),
                    firestore = firestore,
                    certificateList = certificateList,
                    onComplete = { isLoading = false }
                )
            }
        }

        if (currentUserId != null) {
            val l1 = firestore.collection("registrations")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            allDocsMap[doc.id] = doc
                        }
                        updateUiIfReady()
                    }
                }
            listeners.add(l1)
        }

        if (possibleIds.isNotEmpty()) {
            val l2 = firestore.collection("registrations")
                .whereIn("studentId", possibleIds)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            allDocsMap[doc.id] = doc
                        }
                        updateUiIfReady()
                    }
                }
            listeners.add(l2)

            val l3 = firestore.collection("registrations")
                .whereEqualTo("studentNumber", studentId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            allDocsMap[doc.id] = doc
                        }
                        updateUiIfReady()
                    }
                }
            listeners.add(l3)
        }

        onDispose {
            for (listener in listeners) {
                listener.remove()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(historyTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = Color(0xFF912323))
                }
                certificateList.isEmpty() -> {
                    Text(
                        text = noHistoryText,
                        color = secondaryTextColor,
                        fontSize = 15.sp
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(certificateList, key = { it.workshopId }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, borderColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                        fontSize = 17.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "$dateLabel${item.date}",
                                        fontSize = 13.sp,
                                        color = secondaryTextColor
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = borderColor, thickness = 0.8.dp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = certReadyText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A)
                                            )
                                            if (item.checkInTimeText.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Completed: ${item.checkInTimeText}",
                                                    fontSize = 11.sp,
                                                    color = secondaryTextColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { onViewCertificateClick(item.workshopId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                    ) {
                                        Text(
                                            text = viewCertText,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
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

private suspend fun processCertificateDocs(
    docs: List<DocumentSnapshot>,
    firestore: FirebaseFirestore,
    certificateList: SnapshotStateList<CertificateHistoryItem>,
    onComplete: () -> Unit
) {
    val tempList = mutableListOf<CertificateHistoryItem>()

    for (regDoc in docs) {
        val isEligible = AttendanceUtils.isEligibleForCertificate(regDoc)
        if (!isEligible) continue

        val workshopId = regDoc.getString("workshopId") ?: ""
        Log.d("CertHistoryDebug", "Processing registration with workshopId: $workshopId")

        val checkInTimeStr = AttendanceUtils.formatTimestamp(regDoc.get("timestamp") ?: regDoc.get("checkInTimestamp"))
        val checkOutTimeStr = AttendanceUtils.formatTimestamp(regDoc.get("checkOutTime") ?: regDoc.get("checkOutTimestamp"))

        var workshopTitle = "Workshop"
        var workshopDate = "Recent"

        if (workshopId.isNotEmpty()) {
            try {
                val workshopDoc = firestore.collection("workshops").document(workshopId).get().await()
                if (workshopDoc.exists()) {
                    // Semak pelbagai variasi field tajuk
                    workshopTitle = workshopDoc.getString("title")
                        ?: workshopDoc.getString("name")
                                ?: workshopDoc.getString("workshopName")
                                ?: workshopDoc.getString("eventName")
                                ?: workshopDoc.getString("eventTitle")
                                ?: "Workshop"

                    // Semak pelbagai variasi field tarikh (Termasuk juga jenis Timestamp jika ada)
                    val rawDate = workshopDoc.get("date")
                        ?: workshopDoc.get("workshopDate")
                        ?: workshopDoc.get("eventDate")
                        ?: workshopDoc.get("timestamp")

                    workshopDate = when (rawDate) {
                        is com.google.firebase.Timestamp -> {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(rawDate.toDate())
                        }
                        is Date -> {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(rawDate)
                        }
                        is String -> {
                            if (rawDate.isNotBlank()) rawDate else "Recent"
                        }
                        else -> "Recent"
                    }
                } else {
                    Log.w("CertHistoryDebug", "Workshop document not found for ID: $workshopId")
                }
            } catch (e: Exception) {
                Log.e("CertHistoryDebug", "Failed to retrieve workshop data for ID: $workshopId", e)
            }
        } else {
            Log.w("CertHistoryDebug", "workshopId is empty in registration doc: ${regDoc.id}")
        }

        if (tempList.none { it.workshopId == workshopId }) {
            tempList.add(
                CertificateHistoryItem(
                    workshopId = workshopId,
                    workshopTitle = workshopTitle,
                    date = workshopDate,
                    checkInTimeText = checkInTimeStr,
                    checkOutTimeText = checkOutTimeStr
                )
            )
        }
    }

    certificateList.clear()
    certificateList.addAll(tempList)
    onComplete()
}