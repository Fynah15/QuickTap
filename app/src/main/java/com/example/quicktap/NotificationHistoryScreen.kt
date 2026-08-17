package com.example.quicktap

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long
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

    val currentLang = AppSettingsState.currentLanguage
    val titleText = if (currentLang == "ms") "Sejarah Notifikasi" else "Notification History"
    val noNotifText = if (currentLang == "ms") "Tiada notifikasi buat masa ini." else "No notifications at the moment."

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        createNotificationChannel(context)
    }

    // Helper function to fire local push alerts safely
    fun showLocalNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, "QUICKTAP_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(System.currentTimeMillis().toInt(), builder.build())
            } catch (e: SecurityException) {
                // Permission not granted for notifications
            }
        }
    }

    // Load data with robust snapshot tracking for local popups
    LaunchedEffect(currentUser) {
        val uid = currentUser?.uid
        if (uid != null) {
            // Listen to user-specific subcollection
            firestore.collection("users").document(uid).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val newNotifications = snapshot.documents.map { doc ->
                            NotificationItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "Notification",
                                message = doc.getString("message") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        }

                        // Check document modifications to trigger popup for newly added items
                        snapshot.documentChanges.forEach { change ->
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val doc = change.document
                                val title = doc.getString("title") ?: "Notification"
                                val message = doc.getString("message") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: 0L

                                // Only trigger if the notification is recent (e.g., created within the last 10 seconds)
                                // This prevents popping up old history items when the screen first loads.
                                if (System.currentTimeMillis() - timestamp < 10000) {
                                    showLocalNotification(title, message)
                                }
                            }
                        }

                        notifications = newNotifications
                        isLoading = false
                    } else {
                        // Fallback to global notifications if user subcollection is empty
                        firestore.collection("notifications")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .addSnapshotListener { globalSnapshot, globalError ->
                                if (globalError != null) {
                                    isLoading = false
                                    return@addSnapshotListener
                                }

                                if (globalSnapshot != null) {
                                    globalSnapshot.documentChanges.forEach { change ->
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                            val doc = change.document
                                            val title = doc.getString("title") ?: "Notification"
                                            val message = doc.getString("message") ?: ""
                                            val timestamp = doc.getLong("timestamp") ?: 0L

                                            if (System.currentTimeMillis() - timestamp < 10000) {
                                                showLocalNotification(title, message)
                                            }
                                        }
                                    }

                                    notifications = globalSnapshot.documents.map { doc ->
                                        NotificationItem(
                                            id = doc.id,
                                            title = doc.getString("title") ?: "Notification",
                                            message = doc.getString("message") ?: "",
                                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                        )
                                    }
                                }
                                isLoading = false
                            }
                    }
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
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
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF912323)
                )
            } else if (notifications.isEmpty()) {
                Text(
                    text = noNotifText,
                    modifier = Modifier.align(Alignment.Center),
                    color = secondaryTextColor,
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
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
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.message,
                                        fontSize = 13.sp,
                                        color = secondaryTextColor
                                    )
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