package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.ClinicSettings
import com.example.data.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfHelper {

    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        settings: ClinicSettings
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            // Standard A4: 595 x 842 pt, or Thermal 80mm: ~226 pt wide
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(0, 100, 60)
                textSize = 20f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val subTitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.DKGRAY
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }

            val headerPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 14f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }

            val textPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 12f
                textAlign = Paint.Align.RIGHT
            }

            val boldTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 12f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }

            val centerTextPaint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }

            var y = 50f
            val centerX = 595f / 2f
            val rightMargin = 550f
            val leftMargin = 45f

            // Clinic Header
            canvas.drawText(settings.clinicName.ifBlank { "عيادة القعافي البيطرية" }, centerX, y, titlePaint)
            y += 20f
            canvas.drawText(settings.invoiceHeader.ifBlank { "للطب والجراحة البيطرية والأدوية" }, centerX, y, subTitlePaint)
            y += 18f
            canvas.drawText("هاتف: ${settings.clinicPhone} | العنوان: ${settings.clinicAddress}", centerX, y, subTitlePaint)
            y += 18f
            if (settings.taxNumber.isNotBlank()) {
                canvas.drawText("الرقم الضريبي: ${settings.taxNumber}", centerX, y, subTitlePaint)
                y += 20f
            }

            // Divider
            paint.strokeWidth = 2f
            paint.color = Color.rgb(0, 180, 100)
            canvas.drawLine(leftMargin, y, rightMargin, y, paint)
            y += 30f

            // Invoice Title & Info
            canvas.drawText("فاتورة مبيعات / علاج بيطري", rightMargin, y, headerPaint)
            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("التاريخ: $dateStr", leftMargin + 100f, y, textPaint)
            y += 22f

            canvas.drawText("رقم الفاتورة: ${invoice.invoiceNumber}", rightMargin, y, boldTextPaint)
            val statusArabic = when (invoice.status) {
                "paid" -> "مدفوعة ✓"
                "cancelled" -> "ملغاة"
                else -> "مسودة"
            }
            canvas.drawText("الحالة: $statusArabic", leftMargin + 100f, y, boldTextPaint)
            y += 22f

            if (invoice.customerName.isNotBlank()) {
                canvas.drawText("العميل / المربي: ${invoice.customerName}", rightMargin, y, textPaint)
                if (invoice.customerPhone.isNotBlank()) {
                    canvas.drawText("رقم الهاتف: ${invoice.customerPhone}", leftMargin + 100f, y, textPaint)
                }
                y += 25f
            }

            // Table Header Bar
            paint.color = Color.rgb(240, 245, 242)
            canvas.drawRect(leftMargin, y, rightMargin, y + 26f, paint)

            paint.color = Color.BLACK
            y += 18f
            canvas.drawText("الصنف / الدواء", rightMargin - 10f, y, boldTextPaint)
            canvas.drawText("الكمية", rightMargin - 220f, y, boldTextPaint)
            canvas.drawText("السعر (${settings.currencySymbol})", rightMargin - 320f, y, boldTextPaint)
            canvas.drawText("الإجمالي", leftMargin + 50f, y, boldTextPaint)

            y += 15f
            paint.strokeWidth = 1f
            paint.color = Color.LTGRAY
            canvas.drawLine(leftMargin, y, rightMargin, y, paint)
            y += 20f

            // Table Rows
            val items = invoice.parsedItems()
            for (item in items) {
                canvas.drawText(item.productName, rightMargin - 10f, y, textPaint)
                canvas.drawText("${item.quantity}", rightMargin - 220f, y, textPaint)
                canvas.drawText("%.2f".format(item.unitPrice), rightMargin - 320f, y, textPaint)
                canvas.drawText("%.2f".format(item.total), leftMargin + 50f, y, boldTextPaint)

                y += 20f
                canvas.drawLine(leftMargin, y - 5f, rightMargin, y - 5f, paint)
            }

            y += 20f
            // Summary Card
            paint.color = Color.rgb(245, 247, 250)
            canvas.drawRect(rightMargin - 280f, y, rightMargin, y + 100f, paint)

            y += 22f
            canvas.drawText("المجموع الفرعي:", rightMargin - 10f, y, textPaint)
            canvas.drawText("%.2f %s".format(invoice.subtotal, settings.currencySymbol), rightMargin - 150f, y, textPaint)

            y += 22f
            canvas.drawText("ضريبة القيمة المضافة (%.0f%%):".format(invoice.taxRate), rightMargin - 10f, y, textPaint)
            canvas.drawText("%.2f %s".format(invoice.taxAmount, settings.currencySymbol), rightMargin - 150f, y, textPaint)

            y += 25f
            canvas.drawText("المبلغ الإجمالي الكلي:", rightMargin - 10f, y, boldTextPaint)
            canvas.drawText("%.2f %s".format(invoice.total, settings.currencySymbol), rightMargin - 150f, y, boldTextPaint)

            // Footer
            y = 780f
            paint.color = Color.rgb(0, 180, 100)
            canvas.drawLine(leftMargin, y, rightMargin, y, paint)
            y += 22f
            canvas.drawText(settings.invoiceFooter.ifBlank { "شكراً لثقتكم بعيادة القعافي البيطرية" }, centerX, y, subTitlePaint)

            pdfDocument.finishPage(page)

            val dir = File(context.cacheDir, "invoices")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "Invoice_${invoice.invoiceNumber}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            Log.e("InvoicePdf", "فشل إنشاء ملف PDF", e)
            null
        }
    }

    fun shareInvoiceViaWhatsApp(
        context: Context,
        invoice: Invoice,
        settings: ClinicSettings
    ) {
        val pdfFile = generateInvoicePdf(context, invoice, settings)

        val messageText = buildString {
            append("🩺 *${settings.clinicName}*\n")
            append("${settings.invoiceHeader}\n")
            append("-------------------------------\n")
            append("📄 *فاتورة رقم:* ${invoice.invoiceNumber}\n")
            append("📅 *التاريخ:* ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
            if (invoice.customerName.isNotBlank()) {
                append("👤 *العميل:* ${invoice.customerName}\n")
            }
            append("📌 *الحالة:* ${if (invoice.status == "paid") "مدفوعة ✅" else "مسودة 📝"}\n")
            append("-------------------------------\n")
            append("*الأصناف:*\n")
            invoice.parsedItems().forEachIndexed { index, item ->
                append("${index + 1}. ${item.productName} × ${item.quantity} = ${item.total} ${settings.currencySymbol}\n")
            }
            append("-------------------------------\n")
            append("💵 *المجموع الفرعي:* ${invoice.subtotal} ${settings.currencySymbol}\n")
            if (invoice.taxAmount > 0) {
                append("🏛 *الضريبة (${invoice.taxRate}%):* ${invoice.taxAmount} ${settings.currencySymbol}\n")
            }
            append("💰 *الإجمالي الكلي:* *${invoice.total} ${settings.currencySymbol}*\n")
            append("-------------------------------\n")
            append("📞 *للتواصل:* ${settings.clinicPhone}\n")
            append("✨ ${settings.invoiceFooter}")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, messageText)
            if (pdfFile != null && pdfFile.exists()) {
                try {
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    type = "text/plain"
                }
            } else {
                type = "text/plain"
            }
        }

        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفاتورة عبر واتساب"))
    }
}
