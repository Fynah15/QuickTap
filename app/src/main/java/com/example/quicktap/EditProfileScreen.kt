package com.example.quicktap

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val currentUser = auth.currentUser

    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("Bachelor of Information Technology") }
    var profileImageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // State untuk pilih gambar dari galeri
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
        }
    }

    // Ambil data profil sebenar dari Firestore semasa skrin dibuka
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        fullName = document.getString("name") ?: "Nursyafinah Binti Hamdan"
                        studentId = document.getString("studentId") ?: "202209020204"
                        email = document.getString("email") ?: (currentUser.email ?: "")
                        phone = document.getString("phone") ?: ""
                        program = document.getString("program") ?: "Bachelor of Information Technology"
                        profileImageUrl = document.getString("profileImageUrl") ?: ""
                    }
                }
        }
    }

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val editProfileTitle = if (currentLang == "ms") "Sunting Profil" else "Edit Profile"
    val accountText = if (currentLang == "ms") "Akaun" else "Account"
    val universityNameText = "CITY UNIVERSITY MALAYSIA"
    val studentIdLabelText = if (currentLang == "ms") "ID PELAJAR" else "STUDENT ID"

    val emailLabel = if (currentLang == "ms") "Emel" else "Email"
    val phoneLabel = if (currentLang == "ms") "Nombor telefon bimbit" else "Mobile phone"
    val passwordLabel = if (currentLang == "ms") "Kata laluan baharu (Kosongkan jika tidak tukar)" else "New password (Leave blank if unchanged)"
    val saveProfileText = if (currentLang == "ms") "Simpan Profil" else "Save Profile"

    // 2. Sokongan Tema Gelap / Cerah
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            Text(accountText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))

            // --- KAD ID CITY UNIVERSITY MALAYSIA ---
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
                        // Bahagian Gambar Profil (Boleh klik untuk tukar)
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
                                fullName.ifEmpty { "Nursyafinah Binti Hamdan" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColor
                            )
                            Text(
                                studentId.ifEmpty { "202209020204" },
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

            // --- RUANGAN INPUT EDIT ---
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(phoneLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(passwordLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                visualTransformation = PasswordVisualTransformation(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = textColor),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = secondaryTextColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- BUTANG SAVE ---
            Button(
                onClick = {
                    val uid = currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(context, "Sila log masuk semula.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    // Fungsi pembantu untuk kemaskini Firestore
                    val updateFirestoreData: (String?) -> Unit = { imageUrl ->
                        val updatedData = mutableMapOf<String, Any>(
                            "email" to email,
                            "phone" to phone
                        )
                        if (!imageUrl.isNullOrEmpty()) {
                            updatedData["profileImageUrl"] = imageUrl
                        }

                        firestore.collection("users").document(uid)
                            .update(updatedData)
                            .addOnSuccessListener {
                                // Semak jika ada perubahan kata laluan
                                if (password.isNotEmpty() && password.length >= 6) {
                                    currentUser.updatePassword(password)
                                        .addOnCompleteListener { passTask ->
                                            isLoading = false
                                            if (passTask.isSuccessful) {
                                                Toast.makeText(context, "Profil & Kata Laluan Berjaya Dikemaskini!", Toast.LENGTH_SHORT).show()
                                                onBackClick()
                                            } else {
                                                Toast.makeText(context, "Ralat kemaskini kata laluan: ${passTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Profil Berjaya Dikemaskini!", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Gagal simpan ke database: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }

                    // Logik muat naik gambar ke Firebase Storage jika gambar baharu dipilih
                    if (imageUri != null) {
                        val storageRef = storage.reference.child("profile_images/$uid.jpg")
                        storageRef.putFile(imageUri!!)
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    updateFirestoreData(downloadUri.toString())
                                }.addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Gagal dapatkan pautan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Gagal muat naik gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Teruskan kemaskini Firestore tanpa tukar gambar
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
        }
    }
}