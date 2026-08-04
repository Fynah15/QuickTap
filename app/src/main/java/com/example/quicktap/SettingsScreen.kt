package com.example.quicktap

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // State untuk Dropdown Bahasa
    var langExpanded by remember { mutableStateOf(false) }

    // State untuk Dialog Pengesahan Log Keluar
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val settingsTitleText = if (currentLang == "ms") "Tetapan" else "Settings"
    val accountText = if (currentLang == "ms") "Akaun" else "Account"
    val darkModeText = if (currentLang == "ms") "Mod Gelap" else "Dark Mode"
    val languageText = if (currentLang == "ms") "Bahasa Aplikasi" else "App Language"
    val logoutText = if (currentLang == "ms") "Log Keluar" else "Logout"

    // Dialog Teks
    val logoutTitle = if (currentLang == "ms") "Pengesahan Log Keluar" else "Logout Confirmation"
    val logoutMessage = if (currentLang == "ms") "Adakah anda pasti mahu log keluar daripada akaun anda?" else "Are you sure you want to log out from your account?"
    val yesText = if (currentLang == "ms") "Ya" else "Yes"
    val noText = if (currentLang == "ms") "Tidak" else "No"

    // 2. Sokongan Tema Gelap / Cerah (Dark / Light Mode)
    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val cardBackground = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settingsTitleText, color = Color.White, fontSize = 16.sp) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kad Account
            Card(
                onClick = onAccountClick,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Account Icon",
                            tint = Color(0xFF4A90E2),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(accountText, fontSize = 14.sp, color = textColor)
                    }
                }
            }

            // Kad Dark Mode
            Card(
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode Icon",
                            tint = Color(0xFF4A90E2),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(darkModeText, fontSize = 14.sp, color = textColor)
                    }
                    Switch(
                        checked = AppSettingsState.isDarkMode,
                        onCheckedChange = { AppSettingsState.isDarkMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF7ED321)
                        )
                    )
                }
            }

            // Kad Language
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    onClick = { langExpanded = !langExpanded },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language Icon",
                                tint = Color(0xFF4A90E2),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(languageText, fontSize = 14.sp, color = textColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (AppSettingsState.currentLanguage == "ms") "Malay" else "English",
                                fontSize = 12.sp,
                                color = secondaryTextColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (langExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown Arrow",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = langExpanded,
                    onDismissRequest = { langExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(cardBackground) // Menukar warna latar belakang kotak dropdown menjadi putih (atau gelap mengikut tema)
                ) {
                    DropdownMenuItem(
                        text = { Text("English", color = textColor) },
                        onClick = {
                            AppSettingsState.currentLanguage = "en"
                            langExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Malay", color = textColor) },
                        onClick = {
                            AppSettingsState.currentLanguage = "ms"
                            langExpanded = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Butang Logout
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(logoutText, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }

    // --- DIALOG PENGESAHAN LOG KELUAR ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = cardBackground,
            title = { Text(logoutTitle, fontWeight = FontWeight.Bold, color = textColor) },
            text = { Text(logoutMessage, color = secondaryTextColor) },
            confirmButton = {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val buttonColor = if (isPressed) Color(0xFF4A90E2) else Color(0xFFE0E0E0)
                val textColorDynamic = if (isPressed) Color.White else Color.Black

                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    interactionSource = interactionSource,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text(yesText, color = textColorDynamic)
                }
            },
            dismissButton = {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val buttonColor = if (isPressed) Color(0xFF4A90E2) else Color(0xFFE0E0E0)
                val textColorDynamic = if (isPressed) Color.White else Color.Black

                Button(
                    onClick = { showLogoutDialog = false },
                    interactionSource = interactionSource,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text(noText, color = textColorDynamic)
                }
            }
        )
    }
}