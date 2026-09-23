package com.example.quicktap

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
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
    currentLang: String = "ms",
    outputPath: File
) {
    val document = PdfDocument()

    // Standard A4 Landscape print resolution
    val pageWidth = 3508
    val pageHeight = 2480
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    // 1. Draw Background Template 100% matching UI
    val drawable = ContextCompat.getDrawable(context, R.drawable.certificate_design)
    if (drawable != null) {
        val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(bitmap)
        drawable.setBounds(0, 0, pageWidth, pageHeight)
        drawable.draw(tempCanvas)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }

    // 2. Setup Paint & Center Alignment (Seiras dengan paparan skrin UI)
    val centerX = (pageWidth / 2f)
    val maxTextWidth = 2600f
    val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // Certificate Title
    paint.textSize = 95f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.parseColor("#1A1A1A")
    canvas.drawText(if (currentLang == "ms") "SIJIL PENYERTAAN" else "CERTIFICATE OF PARTICIPATION", centerX, 900f, paint)

    // Subtitle
    paint.textSize = 50f
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    paint.color = Color.DKGRAY
    canvas.drawText(if (currentLang == "ms") "Sijil ini dengan bangganya dianugerahkan kepada" else "This certificate is proudly presented to", centerX, 1030f, paint)

    // Student Name
    val safeStudentName = if (studentName.isNotBlank() && studentName != "Student") studentName else "Student"
    var studentTextSize = 110f
    paint.textSize = studentTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.BLACK
    while (paint.measureText(safeStudentName) > maxTextWidth && studentTextSize > 50f) {
        studentTextSize -= 4f
        paint.textSize = studentTextSize
    }
    canvas.drawText(safeStudentName, centerX, 1220f, paint)

    // Name Underline
    val nameWidth = paint.measureText(safeStudentName)
    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 4f }
    canvas.drawLine(centerX - (nameWidth / 2f) - 100f, 1280f, centerX + (nameWidth / 2f) + 100f, 1280f, linePaint)

    // Student ID
    paint.textSize = 45f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = Color.GRAY
    canvas.drawText(if (currentLang == "ms") "No. ID: $studentId" else "Student ID: $studentId", centerX, 1370f, paint)

    // Reason Text
    paint.textSize = 48f
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    paint.color = Color.DKGRAY
    canvas.drawText(if (currentLang == "ms") "kerana telah berjaya menyertai program / bengkel:" else "for successfully participating in the programme / workshop:", centerX, 1520f, paint)

    // Workshop Name
    var workshopTextSize = 75f
    paint.textSize = workshopTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.parseColor("#912323")
    while (paint.measureText(workshopName) > maxTextWidth && workshopTextSize > 40f) {
        workshopTextSize -= 4f
        paint.textSize = workshopTextSize
    }
    canvas.drawText(workshopName, centerX, 1650f, paint)

    // Date & Time (Bottom Left)
    paint.textAlign = Paint.Align.LEFT
    paint.textSize = 45f
    paint.color = Color.DKGRAY
    canvas.drawText(if (currentLang == "ms") "Tarikh: $workshopDate | Masa: $workshopTime" else "Date: $workshopDate | Time: $workshopTime", 400f, 2150f, paint)

    // Authorized Signature (Bottom Right)
    paint.textAlign = Paint.Align.CENTER
    val sigX = (pageWidth - 650).toFloat()
    canvas.drawLine(sigX - 220, 2150f, sigX + 220, 2150f, linePaint)
    paint.textSize = 45f
    paint.color = Color.BLACK
    canvas.drawText(organizerName.ifBlank { "QuickTap Organizer" }, sigX, 2110f, paint)
    paint.textSize = 35f
    paint.color = Color.GRAY
    canvas.drawText(if (currentLang == "ms") "Tandatangan Penganjur" else "Authorized Signature", sigX, 2200f, paint)

    document.finishPage(page)

    try {
        FileOutputStream(outputPath).use { fos ->
            document.writeTo(fos)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        document.close()
    }
}