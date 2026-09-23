package com.example.quicktap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object AppSettingsState {
    var isDarkMode by mutableStateOf(false)
    var currentLanguage by mutableStateOf("en")

    // Fungsi untuk memuat tetapan dari Firestore
    fun loadUserSettings(firestore: FirebaseFirestore, uid: String) {
        if (uid.isEmpty() || uid == "STUDENT_12345") return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    isDarkMode = document.getBoolean("isDarkMode") ?: false
                    currentLanguage = document.getString("currentLanguage") ?: "en"
                }
            }
    }

    // Fungsi untuk menyimpan tetapan ke Firestore
    fun updateUserSettings(firestore: FirebaseFirestore, uid: String, darkMode: Boolean, lang: String) {
        isDarkMode = darkMode
        currentLanguage = lang

        if (uid.isNotEmpty() && uid != "STUDENT_12345") {
            val settingsData = mapOf(
                "isDarkMode" to darkMode,
                "currentLanguage" to lang
            )
            firestore.collection("users").document(uid)
                .set(settingsData, SetOptions.merge())
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        }
    }
}