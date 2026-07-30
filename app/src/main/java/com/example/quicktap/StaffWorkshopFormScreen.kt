package com.example.quicktap

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffWorkshopFormScreen(
    workshopId: String? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var maxCapacity by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isVenueExpanded by remember { mutableStateOf(false) }
    val venueOptions = listOf("Auditorium 1", "Auditorium 2", "Level 1, Lab 201", "Level 1, Lab 203", "Classroom 201", "Classroom 208", "Classroom 212", "Classroom 213")

    val isEditMode = !workshopId.isNullOrEmpty()
    val calendar = Calendar.getInstance()

    // 1. Sekatan Tarikh Lepas: Tetapkan minDate kepada masa semasa (hari ini)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d -> date = "$y-${m + 1}-$d" },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = calendar.timeInMillis
    }

    val startTimePickerDialog = TimePickerDialog(context, { _, h, m ->
        val hourIn12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        val amPm = if (h < 12) "AM" else "PM"
        startTime = String.format("%d:%02d %s", hourIn12, m, amPm)
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

    val endTimePickerDialog = TimePickerDialog(context, { _, h, m ->
        val hourIn12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        val amPm = if (h < 12) "AM" else "PM"
        endTime = String.format("%d:%02d %s", hourIn12, m, amPm)
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

    val currentLang = AppSettingsState.currentLanguage
    val editTitleText = if (currentLang == "ms") "Sunting Bengkel" else "Edit Workshop"
    val createTitleText = if (currentLang == "ms") "Cipta Bengkel" else "Create Workshop"
    val titleLabel = if (currentLang == "ms") "Tajuk Bengkel" else "Workshop Title"
    val descLabel = if (currentLang == "ms") "Penerangan" else "Description"
    val venueLabel = if (currentLang == "ms") "Tempat" else "Venue"
    val dateLabel = if (currentLang == "ms") "Tarikh" else "Date"
    val startTimeLabel = if (currentLang == "ms") "Masa Mula" else "Start Time"
    val endTimeLabel = if (currentLang == "ms") "Masa Tamat" else "End Time"
    val maxCapacityLabel = if (currentLang == "ms") "Kapasiti Maksimum: " else "Max Capacity: "
    val updateBtnText = if (currentLang == "ms") "Kemas Kini Perubahan" else "Update Changes"
    val saveBtnText = if (currentLang == "ms") "Simpan Bengkel" else "Save Workshop"
    val deleteBtnText = if (currentLang == "ms") "Padam Bengkel" else "Delete Workshop"

    val fillFieldsToast = if (currentLang == "ms") "Sila isi semua medan" else "Please fill in all fields"
    val successUpdateToast = if (currentLang == "ms") "Berjaya dikemas kini!" else "Successfully updated!"
    val successSaveToast = if (currentLang == "ms") "Berjaya disimpan!" else "Successfully saved!"
    val successDeleteToast = if (currentLang == "ms") "Bengkel dipadamkan!" else "Workshop deleted!"

    val dialogTitle = if (currentLang == "ms") "Padam Bengkel" else "Delete Workshop"
    val dialogDesc = if (currentLang == "ms") "Adakah anda pasti mahu memadamkan bengkel ini? Tindakan ini tidak boleh diundurkan." else "Are you sure you want to delete this workshop? This action cannot be undone."
    val cancelText = if (currentLang == "ms") "Batal" else "Cancel"
    val deleteConfirmText = if (currentLang == "ms") "Padam" else "Delete"

    val isDark = AppSettingsState.isDarkMode
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF9F9F9)
    val textColor = if (isDark) Color.White else Color.Black

    LaunchedEffect(workshopId) {
        if (isEditMode && workshopId != null) {
            isLoading = true
            firestore.collection("workshops").document(workshopId).get().addOnSuccessListener { doc ->
                isLoading = false
                if (doc != null && doc.exists()) {
                    title = doc.getString("title") ?: ""
                    description = doc.getString("description") ?: ""
                    date = doc.getString("date") ?: ""
                    location = doc.getString("location") ?: ""
                    val fullTime = doc.getString("time") ?: ""
                    if (fullTime.contains(" - ")) {
                        val parts = fullTime.split(" - ")
                        startTime = parts.getOrNull(0) ?: ""
                        endTime = parts.getOrNull(1) ?: ""
                    }
                    maxCapacity = doc.getString("maxCapacity")?.toIntOrNull() ?: 0
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) editTitleText else createTitleText, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF912323), titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(titleLabel) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = if (isDark) Color(0xFF912323) else Color.Gray,
                    unfocusedLabelColor = if (isDark) Color.Gray else Color.DarkGray
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(descLabel) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = if (isDark) Color(0xFF912323) else Color.Gray,
                    unfocusedLabelColor = if (isDark) Color.Gray else Color.DarkGray
                )
            )

            ExposedDropdownMenuBox(
                expanded = isVenueExpanded,
                onExpandedChange = { isVenueExpanded = !isVenueExpanded }
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(venueLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isVenueExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color(0xFF912323) else Color.Gray,
                        unfocusedLabelColor = if (isDark) Color.Gray else Color.DarkGray
                    )
                )
                ExposedDropdownMenu(
                    expanded = isVenueExpanded,
                    onDismissRequest = { isVenueExpanded = false }
                ) {
                    venueOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                location = selectionOption
                                isVenueExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                label = { Text(dateLabel) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.DateRange, null, tint = if (isDark) Color.LightGray else Color.Gray) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedLabelColor = if (isDark) Color(0xFF912323) else Color.Gray,
                    unfocusedLabelColor = if (isDark) Color.Gray else Color.DarkGray
                )
            )

            // 2. Masa Mula & Tamat: Disusun sebaris dengan saiz teks label disesuaikan supaya muat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(startTimeLabel, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = { IconButton(onClick = { startTimePickerDialog.show() }) { Icon(Icons.Default.DateRange, null, tint = if (isDark) Color.LightGray else Color.Gray) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color(0xFF912323) else Color.Gray,
                        unfocusedLabelColor = if (isDark) Color.Gray else Color.DarkGray
                    )
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(endTimeLabel, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    trailingIcon = { IconButton(onClick = { endTimePickerDialog.show() }) { Icon(Icons.Default.DateRange, null, tint = if (isDark) Color.LightGray else Color.Gray) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = if (isDark) Color(0xFF912323) else Color.Gray,
                        unfocusedLabelColor = if (isDark) Color.Gray else Color.DarkGray
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(maxCapacityLabel, color = textColor)
                IconButton(onClick = { if (maxCapacity > 0) maxCapacity-- }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = textColor) }
                Text(maxCapacity.toString(), fontWeight = FontWeight.Bold, color = textColor)
                IconButton(onClick = { maxCapacity++ }) { Icon(Icons.Default.KeyboardArrowUp, null, tint = textColor) }
            }

            Button(
                onClick = {
                    if (title.isBlank() || date.isBlank() || location.isBlank() || maxCapacity <= 0) {
                        Toast.makeText(context, fillFieldsToast, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    val combinedTime = "$startTime - $endTime"

                    if (isEditMode && workshopId != null) {
                        val ref = firestore.collection("workshops").document(workshopId)
                        firestore.runTransaction { trans ->
                            val snapshot = trans.get(ref)
                            val oldReg = snapshot.getString("registeredCount") ?: "0/0"
                            val currentCount = oldReg.split("/").firstOrNull()?.trim()?.toIntOrNull() ?: 0
                            val newRegString = "$currentCount/$maxCapacity registered"

                            trans.update(ref, mapOf(
                                "title" to title, "description" to description, "date" to date,
                                "time" to combinedTime, "location" to location,
                                "maxCapacity" to maxCapacity.toString(), "registeredCount" to newRegString
                            ))
                        }.addOnSuccessListener {
                            isLoading = false
                            Toast.makeText(context, successUpdateToast, Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    } else {
                        val data = mapOf(
                            "title" to title, "description" to description, "date" to date,
                            "time" to combinedTime, "location" to location,
                            "maxCapacity" to maxCapacity.toString(), "registeredCount" to "0/$maxCapacity registered"
                        )
                        firestore.collection("workshops").add(data).addOnSuccessListener {
                            isLoading = false
                            Toast.makeText(context, successSaveToast, Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
            ) {
                Text(if (isEditMode) updateBtnText else saveBtnText, color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (isEditMode) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text(deleteBtnText, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDeleteDialog && workshopId != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(dialogTitle) },
                text = { Text(dialogDesc) },
                confirmButton = {
                    TextButton(onClick = {
                        firestore.collection("workshops").document(workshopId).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, successDeleteToast, Toast.LENGTH_SHORT).show()
                                onBackClick()
                            }
                    }) { Text(deleteConfirmText, color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(cancelText) }
                }
            )
        }
    }
}