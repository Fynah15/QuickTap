package com.example.quicktap.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.example.quicktap.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StudentSignUpScreen(onNavigateToHome: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF912323))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // --- QUICKTAP LOGO ---
            Image(
                painter = painterResource(id = R.drawable.quicktap_logo),
                contentDescription = "QuickTap Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Full Name Input
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name", fontSize = 15.sp) },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Nickname Input
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

            Spacer(modifier = Modifier.height(14.dp))

            // Student ID Input
            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID", fontSize = 15.sp) },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Course Input
            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text("Course", fontSize = 15.sp) },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input
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

            Spacer(modifier = Modifier.height(14.dp))

            // Confirm Password Input
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
                    val nameInput = fullName.trim()
                    val nickInput = nickname.trim().ifEmpty { nameInput.substringBefore(" ") }
                    val inputId = studentId.trim()
                    val courseInput = course.trim()
                    val cleanPassword = password.trim()
                    val cleanConfirmPassword = confirmPassword.trim()

                    if (nameInput.isEmpty() || inputId.isEmpty() || courseInput.isEmpty() || cleanPassword.isEmpty() || cleanConfirmPassword.isEmpty()) {
                        Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (cleanPassword.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (cleanPassword != cleanConfirmPassword) {
                        Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    val cleanStudentId = if (inputId.contains("@")) inputId.substringBefore("@") else inputId
                    val studentEmail = "$cleanStudentId@student-city.edu.my".lowercase()

                    auth.createUserWithEmailAndPassword(studentEmail, cleanPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val firebaseUser = auth.currentUser
                                val uid = firebaseUser?.uid ?: cleanStudentId

                                val studentData = hashMapOf(
                                    "studentId" to cleanStudentId,
                                    "studentNumber" to cleanStudentId,
                                    "name" to nameInput,
                                    "fullName" to nameInput,
                                    "nickname" to nickInput,
                                    "email" to studentEmail,
                                    "course" to courseInput,
                                    "program" to courseInput,
                                    "role" to "student",
                                    "nfcUid" to "",
                                    "cardUid" to ""
                                )

                                val batch = firestore.batch()
                                val userDocRefByUid = firestore.collection("users").document(uid)
                                val userDocRefById = firestore.collection("users").document(cleanStudentId)

                                batch.set(userDocRefByUid, studentData)
                                batch.set(userDocRefById, studentData)

                                batch.commit()
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                                        // Immediately navigate to Home Dashboard upon successful signup
                                        onNavigateToHome()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                                    }

                            } else {
                                isLoading = false
                                val exception = task.exception
                                if (exception is FirebaseAuthUserCollisionException) {
                                    Toast.makeText(
                                        context,
                                        "Account already exists! Redirecting to home...",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    coroutineScope.launch {
                                        delay(1000)
                                        onNavigateToHome()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Sign Up Failed: ${exception?.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
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
                    Text("Sign Up", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onNavigateToHome()
                },
                colors = ButtonDefaults.textButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? Login", color = Color.White)
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}