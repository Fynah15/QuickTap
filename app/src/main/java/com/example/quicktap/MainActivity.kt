package com.example.quicktap

import android.Manifest
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
import com.example.quicktap.auth.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
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
    val firestore = remember { FirebaseFirestore.getInstance() }

    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    DisposableEffect(Unit) {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            currentUser?.uid?.let { uid ->
                AppSettingsState.loadUserSettings(firestore, uid)
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        }
    }

    // Auto-load tetapan apabila aplikasi bermula jika pengguna sudah log masuk
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { uid ->
            AppSettingsState.loadUserSettings(firestore, uid)
        }
    }

    val currentLoggedInStudentId = currentUser?.uid ?: "STUDENT_12345"
    val defaultWorkshopId = "WORKSHOP_001"

    var activeWorkshopId by remember { mutableStateOf(defaultWorkshopId) }

    // Auto fetch today's active workshop dynamically to eliminate hardcoded/test IDs
    LaunchedEffect(Unit) {
        try {
            val dateFormatStandard = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateFormatAlternate = SimpleDateFormat("yyyy-M-d", Locale.getDefault())
            val currentDate = Date()
            val todayStandard = dateFormatStandard.format(currentDate)
            val todayAlternate = dateFormatAlternate.format(currentDate)

            firestore.collection("workshops")
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val matchingDoc = result.documents.firstOrNull { doc ->
                            val dateField = doc.getString("date")
                                ?: doc.getString("workshopDate")
                                ?: doc.getString("tarikh")
                                ?: ""
                            dateField == todayStandard || dateField == todayAlternate
                        }

                        if (matchingDoc != null) {
                            activeWorkshopId = matchingDoc.id
                        } else {
                            activeWorkshopId = result.documents[0].id
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val startDestinationRoute = "role_select"

    NavHost(navController = navController, startDestination = startDestinationRoute) {

        // --- AUTHENTICATION ---
        composable("role_select") {
            RoleSelectScreen(
                onStudentChoose = { navController.navigate("student_login") },
                onStaffChoose = { navController.navigate("staff_login") }
            )
        }
        composable("student_login") {
            StudentLoginScreen(
                onNavigateHome = {
                    navController.navigate("student_dashboard") {
                        popUpTo("role_select") { inclusive = true }
                    }
                },
                onSignUp = { navController.navigate("student_signup") },
                onForgotPasswordClick = { navController.navigate("forgot_password") }
            )
        }

        composable("student_signup") {
            StudentSignUpScreen(
                onNavigateToHome = {
                    navController.navigate("student_dashboard") {
                        popUpTo("role_select") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("staff_login") {
            StaffLoginScreen(
                onLoginSuccess = {
                    navController.navigate("staff_dashboard") {
                        popUpTo("role_select") { inclusive = true }
                    }
                },
                onSignUpClick = { navController.navigate("staff_signup") },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("staff_signup") {
            StaffSignUpScreen(
                onSignUpSuccess = {
                    navController.navigate("staff_dashboard") {
                        popUpTo("role_select") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // --- STUDENT DASHBOARD ---
        composable("student_dashboard") {
            StudentDashboardScreen(
                myStudentId = currentLoggedInStudentId,
                currentWorkshopId = activeWorkshopId,
                onWorkshopListClick = { navController.navigate("workshop_list") },
                onCertificateClick = { navController.navigate("student_certificate/$activeWorkshopId/$currentLoggedInStudentId") },
                onHistoryClick = { navController.navigate("student_history/$currentLoggedInStudentId") },
                onSettingsClick = { navController.navigate("student_settings") },
                onNotificationClick = { navController.navigate("notification_history") }
            )
        }

        composable("notification_history") {
            NotificationHistoryScreen(onBackClick = { navController.popBackStack() })
        }

        composable("workshop_list") {
            WorkshopListScreen(
                onBackClick = { navController.popBackStack() },
                onRegisterSuccess = {},
                onHomeClick = { navController.navigate("student_dashboard") { popUpTo("student_dashboard") { inclusive = true } } },
                onWorkshopClick = { },
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
                onCertificateClick = {},
                onSettingsClick = { navController.navigate("student_settings") }
            )
        }

        composable("student_settings") {
            SettingsScreen(
                onBackClick = {
                    navController.navigate("student_dashboard") {
                        popUpTo("student_dashboard") { inclusive = true }
                    }
                },
                onAccountClick = { navController.navigate("student_edit_profile") },
                onLogoutClick = {
                    try {
                        FirebaseAuth.getInstance().signOut()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                onViewAnalyticsClick = { workshopId ->
                    activeWorkshopId = workshopId
                    navController.navigate("staff_analytics/$workshopId")
                },
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
                workshopId = activeWorkshopId,
                onModeSelected = { mode ->
                    navController.navigate("staff_nfc_scan/${Uri.encode(mode)}/$activeWorkshopId")
                },
                onBackClick = { navController.popBackStack() } // <--- Parameter onBackClick yang ditambah
            )
        }

        composable(
            route = "staff_nfc_scan/{attendanceMode}/{workshopId}",
            arguments = listOf(
                navArgument("attendanceMode") { type = NavType.StringType },
                navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("attendanceMode") ?: "Check-In"
            val targetWorkshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            StaffNfcScanScreen(
                attendanceMode = mode,
                workshopId = targetWorkshopId,
                onBackClick = { navController.popBackStack() }
            )
        }

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

        composable(
            route = "staff_attendance_report/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            activeWorkshopId = workshopId
            AttendanceReportScreen(
                workshopId = workshopId,
                onBackClick = { navController.popBackStack() },
                onHomeClick = { navController.navigate("staff_dashboard") { popUpTo("staff_dashboard") { inclusive = true } } },
                onReportClick = {  },
                onCertificateClick = { navController.navigate("staff_certificate_management/$workshopId") },
                onSettingsClick = { navController.navigate("staff_settings") }
            )
        }

        composable(
            route = "staff_certificate_management/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workshopId = backStackEntry.arguments?.getString("workshopId") ?: activeWorkshopId
            activeWorkshopId = workshopId
            CertificateManagementScreen(
                workshopId = workshopId,
                onBackClick = { navController.popBackStack() },
                onAutoGenerateClick = {},
                onHomeClick = { navController.navigate("staff_dashboard") { popUpTo("staff_dashboard") { inclusive = true } } },
                onReportClick = { navController.navigate("staff_attendance_report/$workshopId") },
                onCertificateClick = {  },
                onSettingsClick = { navController.navigate("staff_settings") }
            )
        }

        composable("staff_settings") {
            StaffSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    try {
                        FirebaseAuth.getInstance().signOut()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    navController.navigate("role_select") { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}