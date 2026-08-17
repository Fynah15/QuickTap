package com.example.quicktap

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
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

    // Sokongan Bahasa Dinamik
    val currentLang = AppSettingsState.currentLanguage
    val titleText = if (currentLang == "ms") "SIJIL PENYERTAAN" else "CERTIFICATE OF PARTICIPATION"
    val subtitleText = if (currentLang == "ms") "Sijil ini dengan bangganya dianugerahkan kepada" else "This certificate is proudly presented to"
    val reasonText = if (currentLang == "ms") "kerana telah berjaya menyertai program / bengkel:" else "for successfully participating in the programme / workshop:"

    val dateLabel = if (currentLang == "ms") "Tarikh: $workshopDate | Masa: $workshopTime" else "Date: $workshopDate | Time: $workshopTime"
    val studentIdLabel = if (currentLang == "ms") "No. ID: $studentId" else "Student ID: $studentId"
    val organizerLabel = organizerName.ifBlank { if (currentLang == "ms") "Penganjur Rasmi" else "Official Organizer" }

    // Resolusi Standard Sijil Melintang (Landscape A4)
    val pageWidth = 3508
    val pageHeight = 2480

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

    val centerX = (pageWidth / 2).toFloat()
    val maxTextWidth = 2600f

    // 1. Tajuk Utama: CERTIFICATE OF PARTICIPATION
    paint.textSize = 95f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.parseColor("#1A1A1A")
    canvas.drawText(titleText, centerX, 700f, paint)

    // 2. Sub-Tajuk: "This certificate is proudly presented to"
    paint.textSize = 50f
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    paint.color = Color.DKGRAY
    canvas.drawText(subtitleText, centerX, 850f, paint)

    // 3. Nama Pelajar (Fokus Utama)
    val safeStudentName = if (studentName.isNotBlank() && studentName != "Student") studentName else "Pelajar"
    var studentTextSize = 120f
    paint.textSize = studentTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.BLACK

    while (paint.measureText(safeStudentName) > maxTextWidth && studentTextSize > 60f) {
        studentTextSize -= 5f
        paint.textSize = studentTextSize
    }
    canvas.drawText(safeStudentName, centerX, 1020f, paint)

    // Garisan Bawah Nama Pelajar
    val nameWidth = paint.measureText(safeStudentName)
    val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 4f
        isAntiAlias = true
    }
    canvas.drawLine(centerX - (nameWidth / 2f) - 100f, 1070f, centerX + (nameWidth / 2f) + 100f, 1070f, linePaint)

    // 4. ID Pelajar
    paint.textSize = 45f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = Color.GRAY
    canvas.drawText(studentIdLabel, centerX, 1150f, paint)

    // 5. Keterangan Program
    paint.textSize = 48f
    paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    paint.color = Color.DKGRAY
    canvas.drawText(reasonText, centerX, 1300f, paint)

    // 6. Nama / Tajuk Bengkel (Warna Merah Tema QuickTap)
    var workshopTextSize = 80f
    paint.textSize = workshopTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.parseColor("#912323")

    while (paint.measureText(workshopName) > maxTextWidth && workshopTextSize > 45f) {
        workshopTextSize -= 5f
        paint.textSize = workshopTextSize
    }
    canvas.drawText(workshopName, centerX, 1430f, paint)

    // 7. Bahagian Bawah: Tarikh (Kiri) & Tandatangan Penganjur (Kanan)
    paint.textSize = 45f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = Color.DKGRAY

    // Tarikh di sebelah kiri bawah
    paint.textAlign = Paint.Align.LEFT
    canvas.drawText(dateLabel, 400f, 2050f, paint)

    // Garisan Tandatangan di sebelah kanan bawah
    paint.textAlign = Paint.Align.CENTER
    val sigLineStartX = (pageWidth - 900).toFloat()
    val sigLineEndX = (pageWidth - 400).toFloat()
    val sigLineY = 2050f

    val sigLinePaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 4f
        isAntiAlias = true
    }
    canvas.drawLine(sigLineStartX, sigLineY, sigLineEndX, sigLineY, sigLinePaint)

    val sigCenterX = (sigLineStartX + sigLineEndX) / 2f

    paint.textSize = 50f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.BLACK
    canvas.drawText(organizerLabel, sigCenterX, sigLineY - 30f, paint)

    paint.textSize = 38f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.color = Color.GRAY
    val signTitle = if (currentLang == "ms") "Tandatangan Penganjur" else "Authorized Signature"
    canvas.drawText(signTitle, sigCenterX, sigLineY + 60f, paint)

    // 8. Simpan ke fail PDF
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