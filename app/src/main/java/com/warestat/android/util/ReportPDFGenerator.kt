package com.warestat.android.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportPDFGenerator {

    private val FONT_BOLD = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD)
    private val FONT_NORMAL = Font(Font.FontFamily.HELVETICA, 9f)
    private val FONT_TITLE = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
    private val COLOR_HEADER = BaseColor(52, 73, 94)

    fun generateSalesReport(
        context: Context,
        rows: List<List<String>>,
        columns: List<String>,
        title: String,
        subtitle: String,
        totalSales: Double,
        totalOrders: Int
    ): Result<Uri> {
        return try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "report_vendite_${sdf.format(Date())}.pdf"
            val pdfDir = File(context.getExternalFilesDir(null), "reports")
            pdfDir.mkdirs()
            val pdfFile = File(pdfDir, fileName)

            val document = Document(PageSize.A4, 30f, 30f, 40f, 40f)
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            val titlePara = Paragraph(title, FONT_TITLE)
            titlePara.alignment = Element.ALIGN_CENTER
            document.add(titlePara)

            val subtitlePara = Paragraph(subtitle, FONT_NORMAL)
            subtitlePara.alignment = Element.ALIGN_CENTER
            document.add(subtitlePara)
            document.add(Chunk.NEWLINE)

            // Summary
            val summaryTable = PdfPTable(2)
            summaryTable.widthPercentage = 60f
            summaryTable.horizontalAlignment = Element.ALIGN_LEFT
            addSummaryRow(summaryTable, "Totale Vendite:", String.format("€ %.2f", totalSales))
            addSummaryRow(summaryTable, "Numero Ordini:", totalOrders.toString())
            if (totalOrders > 0) {
                addSummaryRow(summaryTable, "Media per Ordine:", String.format("€ %.2f", totalSales / totalOrders))
            }
            document.add(summaryTable)
            document.add(Chunk.NEWLINE)

            // Data table
            val table = PdfPTable(columns.size)
            table.widthPercentage = 100f

            columns.forEach { col ->
                val cell = PdfPCell(Phrase(col, Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD, BaseColor.WHITE)))
                cell.backgroundColor = COLOR_HEADER
                cell.setPadding(5f)
                table.addCell(cell)
            }

            rows.forEachIndexed { index, row ->
                val bgColor = if (index % 2 == 0) BaseColor.WHITE else BaseColor(245, 245, 245)
                row.forEach { value ->
                    val cell = PdfPCell(Phrase(value, FONT_NORMAL))
                    cell.backgroundColor = bgColor
                    cell.setPadding(4f)
                    table.addCell(cell)
                }
            }
            document.add(table)

            val footer = Paragraph(
                "Generato il ${DateUtils.DATETIME_FORMAT.format(Date())} con WareStat",
                Font(Font.FontFamily.HELVETICA, 7f, Font.NORMAL, BaseColor.GRAY)
            )
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)
            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addSummaryRow(table: PdfPTable, label: String, value: String) {
        table.addCell(PdfPCell(Phrase(label, FONT_BOLD)).apply { border = Rectangle.NO_BORDER })
        table.addCell(PdfPCell(Phrase(value, FONT_NORMAL)).apply { border = Rectangle.NO_BORDER })
    }
}
