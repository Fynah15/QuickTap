package com.example.quicktap

import android.app.Activity
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = auth.currentUser

    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("Bachelor of Information Technology") }
    var cardUid by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // State for NFC
    var isListeningNfc by remember { mutableStateOf(false) }
    var hasScannedNewCard by remember { mutableStateOf(false) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
        }
    }

    // Fetch latest user data from Firestore collection 'users' based on current user UID
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        fullName = document.getString("fullName") ?: document.getString("name") ?: ""
                        studentId = document.getString("studentId") ?: document.getString("studentNumber") ?: ""
                        email = document.getString("email") ?: (currentUser.email ?: "")
                        program = document.getString("course") ?: document.getString("program") ?: "Bachelor of Information Technology"
                        // Support both cardUid and nfcUid fields from database
                        cardUid = document.getString("cardUid") ?: document.getString("nfcUid") ?: ""
                        profileImageUrl = document.getString("profileImageUrl") ?: ""
                    }
                }
        }
    }

    // Function to read physical NFC card using Android NfcAdapter
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    DisposableEffect(isListeningNfc) {
        if (isListeningNfc && nfcAdapter != null && activity != null) {
            val readerCallback = NfcAdapter.ReaderCallback { tag: Tag ->
                val tagId = tag.id
                val hexUid = tagId.joinToString("") { "%02X".format(it) }

                activity.runOnUiThread {
                    cardUid = hexUid
                    isListeningNfc = false
                    hasScannedNewCard = true
                    Toast.makeText(context, "Card Scanned Successfully!", Toast.LENGTH_SHORT).show()
                }
            }

            val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V

            nfcAdapter.enableReaderMode(activity, readerCallback, flags, null)
        }

        onDispose {
            if (nfcAdapter != null && activity != null) {
                nfcAdapter.disableReaderMode(activity)
            }
        }
    }

    val currentLang = AppSettingsState.currentLanguage
    val editProfileTitle = if (currentLang == "ms") "Sunting Profil" else "Edit Profile"
    val accountText = if (currentLang == "ms") "Akaun" else "Account"
    val universityNameText = "CITY UNIVERSITY MALAYSIA"
    val studentIdLabelText = if (currentLang == "ms") "ID PELAJAR" else "STUDENT ID"

    val emailLabel = if (currentLang == "ms") "Email" else "Email"
    val cardUidLabel = if (currentLang == "ms") "Status Kad NFC Fizikal" else "Physical NFC Card Status"
    val saveProfileText = if (currentLang == "ms") "Simpan Profil" else "Save Profile"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val cardBackground = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.LightGray else Color.Gray
    val placeholderColor = if (isDark) Color(0xFF2C2C2C) else Color.LightGray

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(editProfileTitle, color = Color.White, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323))
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(accountText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Student Digital ID Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF912323))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            universityNameText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(55.dp)
                                .clip(CircleShape)
                                .background(placeholderColor)
                                .clickable { imageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                imageUri != null -> {
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = "Selected Profile Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                profileImageUrl.isNotEmpty() -> {
                                    AsyncImage(
                                        model = profileImageUrl,
                                        contentDescription = "Cloud Profile Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Upload Photo",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                fullName.ifEmpty { "User Name" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColor
                            )
                            Text(
                                studentId.ifEmpty { "Student ID" },
                                color = secondaryTextColor,
                                fontSize = 11.sp
                            )
                            Text(program, color = secondaryTextColor, fontSize = 10.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF912323))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            studentIdLabelText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Email Field
            Text(emailLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Physical NFC Card Status
            Text(cardUidLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isListeningNfc -> Color(0xFFD32F2F)
                        hasScannedNewCard -> Color(0xFF2E7D32)
                        else -> Color(0xFF757575)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC Icon",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when {
                                    isListeningNfc -> if (currentLang == "ms") "Sila tap kad di belakang telefon..." else "Tap card on back of phone..."
                                    hasScannedNewCard -> if (currentLang == "ms") "Kad Fizikal Berjaya Di-scan" else "Physical Card Scanned"
                                    else -> if (currentLang == "ms") "Tiada Kad Diimbas (Belum Paut)" else "No Card Scanned"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = when {
                                    isListeningNfc -> if (currentLang == "ms") "Tunggu sebentar..." else "Waiting..."
                                    hasScannedNewCard -> if (currentLang == "ms") "Tekan simpan untuk kemaskini" else "Press save to update"
                                    else -> if (currentLang == "ms") "Tekan butang imbas untuk pautan" else "Press scan to link card"
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isListeningNfc) {
                                isListeningNfc = false
                                Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
                            } else {
                                if (nfcAdapter == null) {
                                    Toast.makeText(context, "This device does not support NFC!", Toast.LENGTH_LONG).show()
                                } else if (!nfcAdapter.isEnabled) {
                                    Toast.makeText(context, "Please enable NFC in your phone settings.", Toast.LENGTH_LONG).show()
                                } else {
                                    isListeningNfc = true
                                    Toast.makeText(context, "Please tap physical card on the back of phone...", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isListeningNfc) (if (currentLang == "ms") "Batal" else "Cancel") else (if (currentLang == "ms") "Imbas" else "Scan"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Save Profile Button
            Button(
                onClick = {
                    val uid = currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    val updateFirestoreData: (String?) -> Unit = { imageUrl ->
                        val updatedData = mutableMapOf<String, Any>(
                            "email" to email,
                            "course" to program,
                            "program" to program
                        )

                        // Save to BOTH cardUid and nfcUid so staff scanner catches it instantly regardless of key used
                        if (cardUid.isNotEmpty()) {
                            updatedData["cardUid"] = cardUid
                            updatedData["nfcUid"] = cardUid
                        }

                        if (!imageUrl.isNullOrEmpty()) {
                            updatedData["profileImageUrl"] = imageUrl
                        }

                        // Update both the UID document and Student ID document to ensure complete synchronization
                        val batch = firestore.batch()
                        val userDocByUid = firestore.collection("users").document(uid)
                        batch.update(userDocByUid, updatedData)

                        if (studentId.isNotEmpty()) {
                            val userDocById = firestore.collection("users").document(studentId)
                            batch.set(userDocById, updatedData, com.google.firebase.firestore.SetOptions.merge())
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Profile & NFC Status Saved Successfully!", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }

                    if (imageUri != null) {
                        val storageRef = storage.reference.child("profile_images/$uid.jpg")
                        storageRef.putFile(imageUri!!)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    updateFirestoreData(downloadUri.toString())
                                }.addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Failed to get image link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        updateFirestoreData(null)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(saveProfileText, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}