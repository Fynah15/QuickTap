package com.example.quicktap.auth.staff

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
import com.example.quicktap.R

@Composable
fun StaffLoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var staffId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
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
                // Menambah kebolehan skrol menegak serta memastikan ruang selamat dari papan kekunci
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- LOGO QUICKTAP ---
            Image(
                painter = painterResource(id = R.drawable.quicktap_logo),
                contentDescription = "QuickTap Logo",
                modifier = Modifier.size(100.dp)
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

            // Kotak Input Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontSize = 15.sp) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }, enabled = !isLoading) {
                        Icon(imageVector = image, contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password")
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
                    val cleanPassword = password.trim()

                    if (inputId.isEmpty() || cleanPassword.isEmpty()) {
                        Toast.makeText(context, "Please enter your Staff ID and Password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    val cleanStaffId = if (inputId.contains("@")) inputId.substringBefore("@") else inputId
                    val staffEmail = "${cleanStaffId.trim()}@city.edu.my".lowercase()

                    auth.signInWithEmailAndPassword(staffEmail, cleanPassword)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(
                                    context, "Login Failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
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
                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "New Staff? Register Here",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
    }
}