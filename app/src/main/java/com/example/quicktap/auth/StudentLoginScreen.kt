package com.example.quicktap.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.example.quicktap.AppSettingsState

@Composable
fun StudentLoginScreen(
    onNavigateHome: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current

    var emailInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val currentLang = AppSettingsState.currentLanguage
    val forgotPasswordText = if (currentLang == "ms") "Lupa Kata Laluan?" else "Forgot Password?"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF912323)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.quicktap_logo),
                contentDescription = "QuickTap Logo",
                modifier = Modifier.size(90.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Email Address", fontSize = 15.sp) },
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = forgotPasswordText,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val email = emailInput.trim()
                    val cleanPassword = password.trim()

                    if (email.isEmpty() || cleanPassword.isEmpty()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    // Terus lakukan log masuk menggunakan Firebase Auth dengan e-mel
                    performLoginAndCheckFirestore(auth, firestore, email, cleanPassword, context, onNavigateHome) {
                        isLoading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSignUp() },
                colors = ButtonDefaults.textButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? Sign up", color = Color.White)
            }
        }
    }
}

private fun performLoginAndCheckFirestore(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    pass: String,
    context: android.content.Context,
    onSuccess: () -> Unit,
    onFinished: () -> Unit
) {
    auth.signInWithEmailAndPassword(email, pass)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            onFinished()
                            if (document.exists()) {
                                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                auth.signOut()
                                Toast.makeText(context, "Account not registered in database. Please Sign Up.", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener {
                            onFinished()
                            auth.signOut()
                            Toast.makeText(context, "Error checking profile: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    onFinished()
                    Toast.makeText(context, "Login Failed: Invalid user session.", Toast.LENGTH_SHORT).show()
                }
            } else {
                onFinished()
                Toast.makeText(
                    context,
                    "Login Failed: ${task.exception?.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
}