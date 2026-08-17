package com.example.quicktap.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.quicktap.R

@Composable
fun StaffSignUpScreen(onSignUpSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var staffId by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // State untuk mengawal fungsi skrol menegak
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF912323))
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                // Menambah kebolehan skrol menegak serta ruang selamat daripada papan kekunci
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- LOGO QUICKTAP ---
            Image(
                painter = painterResource(id = R.drawable.quicktap_logo),
                contentDescription = "QuickTap Logo",
                modifier = Modifier.size(90.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Kotak Input Staff ID
            OutlinedTextField(
                value = staffId,
                onValueChange = { staffId = it },
                label = { Text("Staff ID", fontSize = 15.sp) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Kotak Input Nickname (Nama Panggilan)
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname", fontSize = 15.sp) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Kotak Input Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontSize = 15.sp) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle Password")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Kotak Input Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", fontSize = 15.sp) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle Confirm Password")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val inputId = staffId.trim()
                    val cleanNickname = nickname.trim()
                    val cleanPassword = password.trim()
                    val cleanConfirmPassword = confirmPassword.trim()

                    if (inputId.isEmpty() || cleanNickname.isEmpty() || cleanPassword.isEmpty() || cleanConfirmPassword.isEmpty()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cleanPassword.length < 6) {
                        Toast.makeText(
                            context,
                            "Password must be at least 6 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    if (cleanPassword != cleanConfirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    val cleanStaffId = if (inputId.contains("@")) inputId.substringBefore("@") else inputId
                    val staffEmail = "${cleanStaffId.trim()}@city.edu.my".lowercase()

                    auth.createUserWithEmailAndPassword(staffEmail, cleanPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid ?: ""
                                val staffData = hashMapOf(
                                    "staffId" to cleanStaffId,
                                    "nickname" to cleanNickname,
                                    "email" to staffEmail,
                                    "role" to "staff"
                                )

                                // Simpan maklumat staff berserta nickname ke dalam Firestore
                                firestore.collection("users").document(userId)
                                    .set(staffData)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Staff Account Created Successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onSignUpSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Failed to save profile: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            } else {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Registration Failed: ${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Register", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Already registered? Login",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onBackToLogin() }
            )
        }
    }
}