package com.example.quicktap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quicktap.auth.RoleSelectScreen
import com.example.quicktap.auth.staff.*
import com.example.quicktap.auth.student.*
import com.example.quicktap.dashboard.staff.*
import com.example.quicktap.dashboard.student.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Object global untuk urus tetapan Tema dan Bahasa secara global
object AppSettingsState {
    var isDarkMode by mutableStateOf(false)
    var currentLanguage by mutableStateOf("en") // "en" untuk English, "ms" untuk Bahasa Melayu
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Minta kebenaran notifikasi untuk peranti Android 13 (API 33) dan ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
                    // Tindakan selepas pengguna memberi atau menolak kebenaran
                }.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            // Menggunakan MaterialTheme berasaskan tetapan global AppSettingsState secara reaktif
            androidx.compose.material3.MaterialTheme(
                colorScheme = if (AppSettingsState.isDarkMode) {
                    androidx.compose.material3.darkColorScheme()
                } else {
                    androidx.compose.material3.lightColorScheme()
                }
            ) {
                QuickTapAppNavigation()
            }
        }
    }
}

@Composable
fun QuickTapAppNavigation() {
    val navController = rememberNavController()

    // Dapatkan ID pengguna sebenar daripada Firebase Auth jika sudah log masuk, jika tidak guna default
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentLoggedInStudentId = currentUser?.uid ?: "STUDENT_12345"
    val defaultWorkshopId = "WORKSHOP_001"

    // State untuk mengesan ID workshop yang aktif secara dinamik (real-time)
    var activeWorkshopId by remember { mutableStateOf(defaultWorkshopId) }

    // Ambil ID workshop terkini/aktif dari Firestore jika ada
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("workshops")
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents[0]
                    activeWorkshopId = doc.id
                }
            }
    }

    NavHost(navController = navController, startDestination = "role_select") {

        // --- AUTHENTICATION ---
        composable("role_select") {
            RoleSelectScreen(
                onStudentChoose = { navController.navigate("student_login") },
                onStaffChoose = { navController.navigate("staff_login") }
            )
        }
        composable("student_login") {
            StudentLoginScreen(
                onNavigateHome = { navController.navigate("student_dashboard") { popUpTo("student_login") { inclusive = true } } },
                onSignUp = { navController.navigate("student_signup") }
            )
        }
        composable("student_signup") { StudentSignUpScreen(onNavigateToLogin = { navController.navigate("student_login") }) }

        composable("staff_login") {
            StaffLoginScreen(
                onLoginSuccess = { navController.navigate("staff_dashboard") { popUpTo("staff_login") { inclusive = true } } },
                onSignUpClick = { navController.navigate("staff_signup") },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("staff_signup") { StaffSignUpScreen(onSignUpSuccess = { navController.navigate("staff_login") }, onBackToLogin = { navController.popBackStack() }) }

        // --- STUDENT DASHBOARD ---
        composable("student_dashboard") {
            StudentDashboardScreen(
                myStudentId = currentLoggedInStudentId,
                currentWorkshopId = activeWorkshopId,
                onWorkshopListClick = { navController.navigate("workshop_list") },
                onCertificateClick = { navController.navigate("student_certificate/$activeWorkshopId/$currentLoggedInStudentId") },
                onHistoryClick = { navController.navigate("student_history/$currentLoggedInStudentId") },
                onSettingsClick = { navController.navigate("student_settings") }
            )
        }

        composable("workshop_list") {
            WorkshopListScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterSuccess = {},
                onHomeClick = { navController.navigate("student_dashboard") { popUpTo("student_dashboard") { inclusive = true } } },
                onWorkshopClick = { /* Kekal di skrin senarai bengkel semasa */ },
                onCertificateClick = { navController.navigate("student_certificate/$activeWorkshopId/$currentLoggedInStudentId") },
                onSettingsClick = { navController.navigate("student_settings") }
            )
        }

        composable(
            route = "student_history/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: currentLoggedInStudentId
            StudentHistoryScreen(
                studentId = studentId,
                onBackClick = { navController.popBackStack() },
                onViewCertificateClick = { workshopId ->
                    navController.navigate("student_certificate/$workshopId/$studentId")
                }
            )
        }

        composable(
            route = "student_certificate/{workshopId}/{studentId}",
            arguments = listOf(
                navArgument("workshopId") { type = NavType.StringType },
                navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            val studentId = backStackEntry.arguments?.getString("studentId") ?: currentLoggedInStudentId
            CertificateScreen(
                workshopId = workshopId,
                studentId = studentId,
                onBackClick = { navController.popBackStack() },
                onHomeClick = { navController.navigate("student_dashboard") { popUpTo("student_dashboard") { inclusive = true } } },
                onWorkshopClick = { navController.navigate("workshop_list") },
                onCertificateClick = { /* Kekal di skrin sijil semasa */ },
                onSettingsClick = { navController.navigate("student_settings") }
            )
        }

        composable("student_settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onAccountClick = { navController.navigate("student_edit_profile") },
                onLogoutClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("role_select") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable("student_edit_profile") {
            EditProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- STAFF DASHBOARD ---
        composable("staff_dashboard") {
            StaffDashboardScreen(
                onAttendanceModeClick = { navController.navigate("staff_attendance_mode") },
                onSelectWorkshopClick = { navController.navigate("staff_workshop_list") },
                onViewAnalyticsClick = { navController.navigate("staff_analytics/$activeWorkshopId") },
                onReportClick = { navController.navigate("staff_attendance_report/$activeWorkshopId") },
                onSettingsClick = { navController.navigate("staff_settings") },
                onCertificateManagementClick = { navController.navigate("staff_certificate_management/$activeWorkshopId") }
            )
        }

        composable("staff_workshop_list") {
            StaffWorkshopListScreen(
                onBackClick = { navController.popBackStack() },
                onCreateNewClick = { navController.navigate("create_workshop") },
                onEditExistingClick = { id -> navController.navigate("edit_workshop/$id") },
                onViewLiveAttendanceClick = { selectedWorkshopId ->
                    activeWorkshopId = selectedWorkshopId
                    navController.navigate("staff_analytics/$selectedWorkshopId")
                }
            )
        }

        composable("create_workshop") {
            StaffWorkshopFormScreen(
                workshopId = null,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit_workshop/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            StaffWorkshopFormScreen(
                workshopId = backStackEntry.arguments?.getString("workshopId"),
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("staff_attendance_mode") {
            AttendanceModeScreen(
                onModeSelected = { mode ->
                    navController.navigate("staff_nfc_scan/${Uri.encode(mode)}/$activeWorkshopId")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "staff_nfc_scan/{attendanceMode}/{workshopId}",
            arguments = listOf(
                navArgument("attendanceMode") { type = NavType.StringType },
                navArgument("workshopId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("attendanceMode") ?: "Check-In"
            val targetWorkshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            StaffNfcScanScreen(
                attendanceMode = mode,
                workshopId = targetWorkshopId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- LIVE ATTENDANCE ---
        composable(
            route = "staff_analytics/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            activeWorkshopId = workshopId
            LiveAttendanceScreen(
                workshopId = workshopId,
                onBackClick = { navController.popBackStack() },
                onViewSummaryReportClick = { navController.navigate("staff_attendance_report/$workshopId") }
            )
        }

        composable(
            route = "staff_live_attendance/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            activeWorkshopId = workshopId
            LiveAttendanceScreen(
                workshopId = workshopId,
                onBackClick = { navController.popBackStack() },
                onViewSummaryReportClick = { navController.navigate("staff_attendance_report/$workshopId") }
            )
        }

        // --- ATTENDANCE REPORT ---
        composable(
            route = "staff_attendance_report/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            activeWorkshopId = workshopId
            AttendanceReportScreen(
                workshopId = workshopId,
                onBackClick = { navController.popBackStack() },
                onDownloadReportClick = {},
                onHomeClick = { navController.navigate("staff_dashboard") { popUpTo("staff_dashboard") { inclusive = true } } },
                onReportClick = { /* Kekal di skrin laporan semasa */ },
                onCertificateClick = { navController.navigate("staff_certificate_management/$workshopId") },
                onSettingsClick = { navController.navigate("staff_settings") }
            )
        }

        // --- CERTIFICATE MANAGEMENT ---
        composable(
            route = "staff_certificate_management/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            activeWorkshopId = workshopId
            CertificateManagementScreen(
                workshopId = workshopId,
                onBackClick = { navController.popBackStack() },
                onAutoGenerateClick = {}
            )
        }

        composable("staff_settings") {
            StaffSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("role_select") { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}