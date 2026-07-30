package com.example.quicktap.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quicktap.R

@Composable
fun RoleSelectScreen(onStudentChoose: () -> Unit, onStaffChoose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF912323)).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // --- LOGO KAD QUICKTAP ---
            Image(
                painter = painterResource(id = R.drawable.quicktap_logo),
                contentDescription = "QuickTap Logo",
                modifier = Modifier
                    .size(160.dp) // Dilaraskan sedikit besar kerana teks sudah dibuang
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onStudentChoose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Student", color = Color.Black, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStaffChoose,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Staff", color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}