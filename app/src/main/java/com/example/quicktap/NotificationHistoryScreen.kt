package com.example.quicktap

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val workshopId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(onBackClick: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current

    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    val currentLang = AppSettingsState.currentLanguage
    val titleText = if (currentLang == "ms") "Sejarah Notifikasi" else "Notification History"
    val noNotifText = if (currentLang == "ms") "Tiada notifikasi untuk bengkel yang didaftarkan." else "No notifications for registered workshops."
    val errorPrefix = if (currentLang == "ms") "Gagal memuatkan data: " else "Failed to load data: "

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        createNotificationChannel(context)
    }

    // Real-time listener untuk menapis notifikasi berdasarkan bengkel yang student telah register
    LaunchedEffect(currentUser) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        // Langkah 1: Dapatkan senarai ID bengkel yang telah didaftar oleh student ini dari koleksi "registrations"
        firestore.collection("registrations")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { regSnapshot, regError ->
                if (regError != null) {
                    Log.e("FirestoreError", "Failed to listen registrations.", regError)
                    errorMessage = "$errorPrefix${regError.message}"
                    isLoading = false
                    return@addSnapshotListener
                }

                val registeredWorkshopIds = regSnapshot?.documents?.mapNotNull {
                    it.getString("workshopId")
                }?.toSet() ?: emptySet()

                // Langkah 2: Dengar koleksi notifikasi secara real-time
                firestore.collection("users").document(uid).collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { notifSnapshot, notifError ->
                        if (notifError != null) {
                            Log.e("FirestoreError", "Listen failed for notifications.", notifError)
                            errorMessage = "$errorPrefix${notifError.message}"
                            isLoading = false
                            return@addSnapshotListener
                        }

                        if (notifSnapshot != null) {
                            val list = notifSnapshot.documents.mapNotNull { doc ->
                                try {
                                    val wsId = doc.getString("workshopId")
                                    // Tapis: Hanya ambil notifikasi umum (tiada workshopId) ATAU notifikasi untuk bengkel yang student dah register
                                    if (wsId != null && wsId.isNotEmpty() && !registeredWorkshopIds.contains(wsId)) {
                                        return@mapNotNull null
                                    }

                                    NotificationItem(
                                        id = doc.id,
                                        title = doc.getString("title") ?: if (currentLang == "ms") "Tiada Tajuk" else "No Title",
                                        message = doc.getString("message") ?: if (currentLang == "ms") "Tiada mesej" else "No message",
                                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                        workshopId = wsId
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            notifications = list
                            isLoading = false
                            Log.d("FirestoreData", "Filtered notifications loaded: ${list.size}")
                        }
                    }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(paddingValues)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF912323))
                errorMessage != null -> Text(errorMessage!!, modifier = Modifier.align(Alignment.Center), color = Color.Red)
                notifications.isEmpty() -> Text(noNotifText, modifier = Modifier.align(Alignment.Center), color = secondaryTextColor)
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(notifications, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF912323),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.message, fontSize = 13.sp, color = secondaryTextColor)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp)),
                                            fontSize = 11.sp,
                                            color = secondaryTextColor.copy(alpha = 0.7f)
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

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "QuickTap Notifications"
        val descriptionText = "Workshop and System Updates"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("QUICKTAP_CHANNEL_ID", name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}