package com.example.quicktap.dashboard.staff

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import com.example.quicktap.AppSettingsState
import com.example.quicktap.R
import java.io.File
import java.io.FileOutputStream

fun generateSimpleCertificate(
    context: Context,
    studentName: String,
    studentId: String = "-",
    workshopName: String,
    workshopDate: String = "2026",
    workshopTime: String = "10:00 AM",
    organizerName: String = "QuickTap Organizer",
    outputPath: File
) {
    val document = PdfDocument()

    // 1. Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val workshopLabel = if (currentLang == "ms") "Bengkel: $workshopName" else "Workshop: $workshopName"
    val dateLabel = if (currentLang == "ms") "Tarikh: $workshopDate | Masa: $workshopTime" else "Date: $workshopDate | Time: $workshopTime"
    val studentIdLabel = if (currentLang == "ms") "No. ID Pelajar: $studentId" else "Student ID: $studentId"
    val organizerLabel = organizerName.ifBlank { if (currentLang == "ms") "Penganjur Rasmi" else "Official Organizer" }

    val pageWidth = 2480
    val pageHeight = 3508

    val drawable = ContextCompat.getDrawable(context, R.drawable.certificate_design)
    val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    drawable?.setBounds(0, 0, pageWidth, pageHeight)
    drawable?.draw(canvas)

    val paint = Paint().apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // 2. Kawalan Saiz Automatik untuk Nama Pelajar
    val safeStudentName = if (studentName.isNotBlank() && studentName != "Student") studentName else "Pelajar"
    var studentTextSize = 140f
    paint.textSize = studentTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    val maxTextWidth = 2000f
    while (paint.measureText(safeStudentName) > maxTextWidth && studentTextSize > 70f) {
        studentTextSize -= 5f
        paint.textSize = studentTextSize
    }

    canvas.drawText(safeStudentName, (pageWidth / 2).toFloat(), 1350f, paint)

    // 3. Papar ID Pelajar
    paint.textSize = 60f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = Color.DKGRAY
    canvas.drawText(studentIdLabel, (pageWidth / 2).toFloat(), 1480f, paint)

    paint.color = Color.BLACK

    // 4. Tajuk Bengkel
    var workshopTextSize = 90f
    paint.textSize = workshopTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    while (paint.measureText(workshopLabel) > maxTextWidth && workshopTextSize > 50f) {
        workshopTextSize -= 5f
        paint.textSize = workshopTextSize
    }

    canvas.drawText(workshopLabel, (pageWidth / 2).toFloat(), 1750f, paint)

    // 5. Tarikh & Masa Bengkel
    paint.textSize = 55f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText(dateLabel, (pageWidth / 2).toFloat(), 1900f, paint)

    // 6. Bahagian Tandatangan Penganjur
    paint.textAlign = Paint.Align.RIGHT
    paint.textSize = 50f

    val sigLineStartX = (pageWidth - 600).toFloat()
    val sigLineEndX = (pageWidth - 200).toFloat()
    val sigLineY = 2850f
    canvas.drawLine(sigLineStartX, sigLineY, sigLineEndX, sigLineY, paint)

    val sigCenterX = (sigLineStartX + sigLineEndX) / 2f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(organizerLabel, sigCenterX, sigLineY + 70f, paint)

    paint.textSize = 40f
    paint.color = Color.GRAY
    val signTitle = if (currentLang == "ms") "Tandatangan Penganjur" else "Authorized Signature"
    canvas.drawText(signTitle, sigCenterX, sigLineY + 130f, paint)

    // 7. Simpan ke fail PDF
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = document.startPage(pageInfo)
    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
    document.finishPage(page)

    try {
        document.writeTo(FileOutputStream(outputPath))
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        document.close()
    }
}