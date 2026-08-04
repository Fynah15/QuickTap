package com.example.quicktap

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quicktap.AppSettingsState
import com.example.quicktap.R
import com.example.quicktap.generateSimpleCertificate
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateScreen(
    workshopId: String,
    studentId: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onWorkshopClick: () -> Unit = {},
    onCertificateClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var studentName by remember { mutableStateOf("Loading...") }
    var matrixNumber by remember { mutableStateOf("-") }
    var workshopName by remember { mutableStateOf("Workshop") }
    var workshopDate by remember { mutableStateOf("2026") }
    var workshopTime by remember { mutableStateOf("10:00 AM") }
    var organizerName by remember { mutableStateOf("QuickTap Organizer") }

    var isEligible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val currentLang = AppSettingsState.currentLanguage
    val titleText = if (currentLang == "ms") "Sijil" else "Certificate"

    val certTitleText = if (currentLang == "ms") "SIJIL PENYERTAAN" else "CERTIFICATE OF PARTICIPATION"
    val certSubtitleText = if (currentLang == "ms") "Sijil ini dengan bangganya dianugerahkan kepada" else "This certificate is proudly presented to"
    val certReasonText = if (currentLang == "ms") "kerana telah berjaya menyertai program / bengkel:" else "for successfully participating in the programme / workshop:"

    val checkingStatusText = if (currentLang == "ms") "Memeriksa Status..." else "Checking Status..."
    val downloadCertText = if (currentLang == "ms") "Muat Turun Sijil" else "Download Certificate"
    val notEligibleText = if (currentLang == "ms") "Tidak Layak (Belum Selesai)" else "Not Eligible (Pending Completion)"

    val pdfSavedToast = if (currentLang == "ms") "PDF berjaya disimpan ke Download!" else "PDF successfully saved to Downloads!"
    val pdfErrorToast = if (currentLang == "ms") "Gagal menyimpan PDF." else "Failed to save PDF."
    val notEligibleToast = if (currentLang == "ms") "Sijil anda belum dikeluarkan oleh staf atau anda belum melengkapkan kehadiran." else "Your certificate has not been issued by staff or you haven't completed attendance."

    val navHome = if (currentLang == "ms") "Utama" else "Home"
    val navWorkshop = if (currentLang == "ms") "Bengkel" else "Workshop"
    val navCertificate = if (currentLang == "ms") "Sijil" else "Certificate"
    val navSettings = if (currentLang == "ms") "Tetapan" else "Settings"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val cardBackground = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderColor = if (isDark) Color(0xFF2C2C2C) else Color.LightGray

    LaunchedEffect(workshopId) {
        firestore.collection("workshops").document(workshopId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    workshopName = doc.getString("title") ?: doc.getString("name") ?: "Workshop"
                    workshopDate = doc.getString("date") ?: doc.getString("workshopDate") ?: "2026"
                    workshopTime = doc.getString("time") ?: doc.getString("workshopTime") ?: "10:00 AM"
                    organizerName = doc.getString("organizer") ?: doc.getString("organizerName") ?: "QuickTap Organizer"
                }
            }
    }

    DisposableEffect(workshopId, studentId) {
        val certDocId = "${workshopId}_$studentId"

        val certListener = firestore.collection("certificates").document(certDocId)
            .addSnapshotListener { certDoc, _ ->
                if (certDoc != null && certDoc.exists()) {
                    val rawName = certDoc.getString("studentName") ?: "Student"
                    studentName = if (rawName.isNotBlank() && rawName != studentId) rawName else "Student"
                    matrixNumber = certDoc.getString("studentNumber") ?: "-"

                    certDoc.getString("workshopName")?.let { if (it.isNotBlank()) workshopName = it }
                    certDoc.getString("workshopDate")?.let { if (it.isNotBlank()) workshopDate = it }
                    certDoc.getString("workshopTime")?.let { if (it.isNotBlank()) workshopTime = it }

                    isEligible = true
                    isLoading = false
                } else {
                    firestore.collection("users").document(studentId).get().addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            matrixNumber = userDoc.getString("studentId") ?: userDoc.getString("studentNumber") ?: "-"
                        }
                    }

                    firestore.collection("registrations").document(certDocId).get()
                        .addOnSuccessListener { regDoc ->
                            if (regDoc.exists()) {
                                val rawName = regDoc.getString("studentName") ?: regDoc.getString("name") ?: "Student"
                                studentName = if (rawName.isNotBlank() && rawName != studentId) rawName else "Student"

                                if (matrixNumber == "-") {
                                    matrixNumber = regDoc.getString("studentNumber") ?: "-"
                                }

                                val hasCheckedIn = regDoc.get("timestamp") != null || regDoc.get("checkInTimestamp") != null
                                val hasCheckedOut = regDoc.get("checkOutTime") != null || regDoc.get("checkOutTimestamp") != null || regDoc.get("timeout") != null
                                val isSent = regDoc.getString("certificateStatus") == "Sent"

                                isEligible = (hasCheckedIn && hasCheckedOut) || isSent
                            } else {
                                studentName = "Student"
                                isEligible = false
                            }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            studentName = "Student"
                            isEligible = false
                            isLoading = false
                        }
                }
            }

        onDispose { certListener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = Color.White, fontSize = 16.sp) },
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
                    selected = false,
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
                    selected = true,
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- KAD PREBIU SIJIL DENGAN TEKS DINAMIK LENGKAP ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.414f)
                    .background(cardBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Imej Latar Belakang Sijil (Kosong)
                Image(
                    painter = painterResource(id = R.drawable.certificate_design),
                    contentDescription = "Certificate",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Kandungan Teks Prebiu di atas Imej Kosong
                if (!isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Tajuk & Sub-Tajuk Sijil
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = certTitleText,
                                color = Color(0xFF1A1A1A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = certSubtitleText,
                                color = Color.DarkGray,
                                fontSize = 7.sp,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Nama & ID Pelajar
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = studentName,
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (currentLang == "ms") "No. ID: $matrixNumber" else "Student ID: $matrixNumber",
                                color = Color.Gray,
                                fontSize = 7.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Keterangan & Nama Bengkel
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = certReasonText,
                                color = Color.DarkGray,
                                fontSize = 7.sp,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = workshopName,
                                color = Color(0xFF912323),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- BUTANG MUAT TURUN ---
            Button(
                onClick = {
                    if (isEligible) {
                        try {
                            val fileName = "Certificate_${studentName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
                            val tempFile = File(context.cacheDir, fileName)

                            generateSimpleCertificate(
                                context = context,
                                studentName = studentName,
                                studentId = matrixNumber,
                                workshopName = workshopName,
                                workshopDate = workshopDate,
                                workshopTime = workshopTime,
                                organizerName = organizerName,
                                outputPath = tempFile
                            )

                            if (tempFile.exists()) {
                                val saved = saveFileToPublicDownloads(context, tempFile, fileName)
                                if (saved) {
                                    Toast.makeText(context, pdfSavedToast, Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, pdfErrorToast, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, pdfErrorToast, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, notEligibleToast, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !isLoading && isEligible,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2),
                    disabledContainerColor = if (isDark) Color(0xFF2C2C2C) else Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isLoading) checkingStatusText else if (isEligible) downloadCertText else notEligibleText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

fun saveFileToPublicDownloads(context: android.content.Context, sourceFile: File, fileName: String): Boolean {
    return try {
        var outputStream: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                outputStream = resolver.openOutputStream(uri)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destinationFile = File(downloadsDir, fileName)
            outputStream = FileOutputStream(destinationFile)
        }

        outputStream?.use { output ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}