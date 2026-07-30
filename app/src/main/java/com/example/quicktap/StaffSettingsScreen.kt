package com.example.quicktap.dashboard.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quicktap.AppSettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffSettingsScreen(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // State management tempatan untuk menu dropdown bahasa & dialog pengesahan logout
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val settingsTitle = if (currentLang == "ms") "Tetapan" else "Settings"
    val logoutLabel = if (currentLang == "ms") "Log Keluar" else "Logout"
    val darkModeLabel = if (currentLang == "ms") "Mod Gelap" else "Dark Mode"
    val languageLabel = if (currentLang == "ms") "Bahasa" else "Language"
    val languageName = if (currentLang == "ms") "Malay" else "English"

    // Teks Dialog Pengesahan Logout
    val logoutDialogTitle = if (currentLang == "ms") "Pengesahan Log Keluar" else "Logout Confirmation"
    val logoutDialogMessage = if (currentLang == "ms") "Adakah anda pasti mahu log keluar dari akaun staf ini?" else "Are you sure you want to log out from this staff account?"
    val cancelLabel = if (currentLang == "ms") "Tidak" else "No"
    val yesLabel = if (currentLang == "ms") "Ya" else "Yes"

    // 2. Sokongan Tema Gelap / Cerah (Dark / Light Mode)
    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settingsTitle, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row: Dark Mode
                FigmaSettingRow(
                    title = darkModeLabel,
                    icon = Icons.Default.DarkMode,
                    textColor = textColor,
                    cardColor = cardColor
                ) {
                    Switch(
                        checked = AppSettingsState.isDarkMode,
                        onCheckedChange = { AppSettingsState.isDarkMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF7ED321)
                        )
                    )
                }

                // Row: Language
                Box {
                    FigmaSettingRow(
                        title = languageLabel,
                        icon = Icons.Default.Language,
                        textColor = textColor,
                        cardColor = cardColor
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isLanguageMenuExpanded = true }
                        ) {
                            Text(languageName, fontSize = 12.sp, color = secondaryTextColor)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = secondaryTextColor)
                        }
                    }
                    DropdownMenu(
                        expanded = isLanguageMenuExpanded,
                        onDismissRequest = { isLanguageMenuExpanded = false },
                        modifier = Modifier.background(cardColor)
                    ) {
                        DropdownMenuItem(
                            text = { Text("English", color = textColor) },
                            onClick = {
                                AppSettingsState.currentLanguage = "en"
                                isLanguageMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Malay", color = textColor) },
                            onClick = {
                                AppSettingsState.currentLanguage = "ms"
                                isLanguageMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(logoutLabel, color = Color.White, fontSize = 15.sp)
            }
        }

        // Dialog Pengesahan Log Keluar
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(logoutDialogTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text(logoutDialogMessage, fontSize = 14.sp) },
                confirmButton = {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    // Warna bertukar jadi Biru apabila ditekan, jika tidak warna kelabu neutral
                    val buttonColor = if (isPressed) Color(0xFF4A90E2) else Color(0xFFE0E0E0)
                    val textButtonColor = if (isPressed) Color.White else Color.Black

                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogoutClick()
                        },
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) {
                        Text(yesLabel, color = textButtonColor)
                    }
                },
                dismissButton = {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    // Warna bertukar jadi Biru apabila ditekan, jika tidak warna kelabu neutral
                    val buttonColor = if (isPressed) Color(0xFF4A90E2) else Color(0xFFE0E0E0)
                    val textButtonColor = if (isPressed) Color.White else Color.Black

                    Button(
                        onClick = { showLogoutDialog = false },
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) {
                        Text(cancelLabel, color = textButtonColor)
                    }
                },
                containerColor = cardColor,
                titleContentColor = textColor,
                textContentColor = secondaryTextColor
            )
        }
    }
}

@Composable
fun FigmaSettingRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color,
    cardColor: Color,
    trailingContent: @Composable () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF4A90E2),
                    modifier = Modifier.size(22.dp)
                )
                Text(text = title, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Medium)
            }
            trailingContent()
        }
    }
}