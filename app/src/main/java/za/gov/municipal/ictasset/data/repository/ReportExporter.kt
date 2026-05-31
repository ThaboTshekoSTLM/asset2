package za.gov.municipal.ictasset.data.repository

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import za.gov.municipal.ictasset.domain.model.ExportedReport
import za.gov.municipal.ictasset.domain.model.ExportFormat
import za.gov.municipal.ictasset.domain.model.TabularReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportExporter(
    private val context: Context
) {
    fun export(report: TabularReport, format: ExportFormat): ExportedReport {
        val safeTitle = report.title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${safeTitle}_$stamp.${format.extension}"
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "reports"
        ).also { it.mkdirs() }
        val file = File(directory, fileName)

        when (format) {
            ExportFormat.PDF -> writePdf(report, file)
            ExportFormat.EXCEL -> writeExcelXml(report, file)
        }

        return ExportedReport(
            fileName = fileName,
            absolutePath = file.absolutePath
        )
    }

    private fun writePdf(report: TabularReport, file: File) {
        val document = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            textSize = 11f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 10f
        }

        val pageWidth = 595
        val pageHeight = 842
        val left = 36f
        val lineHeight = 18f
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 42f

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = 42f
        }

        canvas.drawText(report.title, left, y, titlePaint)
        y += 30f
        canvas.drawText(report.headers.joinToString("  |  "), left, y, headerPaint)
        y += lineHeight

        report.rows.forEach { row ->
            if (y > pageHeight - 50) {
                newPage()
                canvas.drawText(report.title, left, y, titlePaint)
                y += 30f
                canvas.drawText(report.headers.joinToString("  |  "), left, y, headerPaint)
                y += lineHeight
            }
            canvas.drawText(row.joinToString("  |  ").take(145), left, y, bodyPaint)
            y += lineHeight
        }

        document.finishPage(page)
        FileOutputStream(file).use { output -> document.writeTo(output) }
        document.close()
    }

    private fun writeExcelXml(report: TabularReport, file: File) {
        // SpreadsheetML keeps the export dependency-free while still opening cleanly in Excel.
        val rows = buildString {
            appendLine("""<?xml version="1.0"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            appendLine("""<Worksheet ss:Name="${report.title.xmlEscape().take(28)}"><Table>""")
            append("<Row>")
            report.headers.forEach { header ->
                append("""<Cell><Data ss:Type="String">${header.xmlEscape()}</Data></Cell>""")
            }
            appendLine("</Row>")
            report.rows.forEach { row ->
                append("<Row>")
                row.forEach { cell ->
                    append("""<Cell><Data ss:Type="String">${cell.xmlEscape()}</Data></Cell>""")
                }
                appendLine("</Row>")
            }
            appendLine("</Table></Worksheet></Workbook>")
        }
        file.writeText(rows, Charsets.UTF_8)
    }

    private fun String.xmlEscape(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
