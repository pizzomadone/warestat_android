package com.warestat.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.warestat.android.data.database.dao.InvoiceItemWithProduct
import com.warestat.android.data.database.entity.CompanyDataEntity
import com.warestat.android.data.database.entity.CustomerEntity
import com.warestat.android.data.database.entity.InvoiceEntity
import java.io.File
import java.io.FileOutputStream

object InvoicePDFGenerator {

    private val FONT_BOLD = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD)
    private val FONT_NORMAL = Font(Font.FontFamily.HELVETICA, 9f)
    private val FONT_SMALL = Font(Font.FontFamily.HELVETICA, 8f)
    private val FONT_TITLE = Font(Font.FontFamily.HELVETICA, 22f, Font.BOLD)
    private val COLOR_HEADER = BaseColor(52, 73, 94)
    private val COLOR_LIGHT = BaseColor(240, 240, 240)

    fun generate(
        context: Context,
        invoice: InvoiceEntity,
        customer: CustomerEntity,
        items: List<InvoiceItemWithProduct>,
        company: CompanyDataEntity?
    ): Result<Uri> {
        return try {
            val fileName = "Fattura_${invoice.number.replace("/", "-")}_${
                DateUtils.formatDate(invoice.date).replace("/", "-")
            }.pdf"
            val pdfDir = File(context.getExternalFilesDir(null), "invoices")
            pdfDir.mkdirs()
            val pdfFile = File(pdfDir, fileName)

            val document = Document(PageSize.A4, 40f, 40f, 40f, 40f)
            PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            // INVOICE title
            val titlePara = Paragraph("FATTURA", FONT_TITLE)
            titlePara.alignment = Element.ALIGN_CENTER
            document.add(titlePara)
            document.add(Chunk.NEWLINE)

            // Company and Customer info side by side
            val infoTable = PdfPTable(2)
            infoTable.widthPercentage = 100f
            infoTable.setWidths(floatArrayOf(1f, 1f))

            // FROM box
            val fromCell = PdfPCell()
            fromCell.addElement(Paragraph("DA", Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD)))
            if (company != null) {
                fromCell.addElement(Paragraph(company.companyName, FONT_BOLD))
                if (company.address.isNotEmpty()) fromCell.addElement(Paragraph(company.address, FONT_NORMAL))
                val cityLine = listOf(company.city, company.postalCode).filter { it.isNotEmpty() }.joinToString(" ")
                if (cityLine.isNotEmpty()) fromCell.addElement(Paragraph(cityLine, FONT_NORMAL))
                if (company.phone.isNotEmpty()) fromCell.addElement(Paragraph("Tel: ${company.phone}", FONT_NORMAL))
                if (company.email.isNotEmpty()) fromCell.addElement(Paragraph("Email: ${company.email}", FONT_NORMAL))
                if (company.vatNumber.isNotEmpty()) fromCell.addElement(Paragraph("P.IVA: ${company.vatNumber}", FONT_NORMAL))
            }
            infoTable.addCell(fromCell)

            // BILL TO box
            val toCell = PdfPCell()
            toCell.addElement(Paragraph("FATTURA A", Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD)))
            toCell.addElement(Paragraph("${customer.firstName} ${customer.lastName}", FONT_BOLD))
            if (customer.address.isNotEmpty()) toCell.addElement(Paragraph(customer.address, FONT_NORMAL))
            if (customer.email.isNotEmpty()) toCell.addElement(Paragraph(customer.email, FONT_NORMAL))
            if (customer.phone.isNotEmpty()) toCell.addElement(Paragraph(customer.phone, FONT_NORMAL))
            infoTable.addCell(toCell)
            document.add(infoTable)
            document.add(Chunk.NEWLINE)

            // Invoice details
            val detailsTable = PdfPTable(2)
            detailsTable.widthPercentage = 50f
            detailsTable.horizontalAlignment = Element.ALIGN_RIGHT
            addDetailRow(detailsTable, "Numero:", invoice.number)
            addDetailRow(detailsTable, "Data:", DateUtils.formatDate(invoice.date))
            addDetailRow(detailsTable, "Stato:", invoice.status)
            document.add(detailsTable)
            document.add(Chunk.NEWLINE)

            // Items table
            val itemsTable = PdfPTable(6)
            itemsTable.widthPercentage = 100f
            itemsTable.setWidths(floatArrayOf(1.5f, 3.5f, 1f, 1.5f, 1f, 1.5f))

            // Header
            listOf("Codice", "Descrizione", "Qtà", "Prezzo €", "IVA%", "Totale €").forEach { header ->
                val cell = PdfPCell(Phrase(header, Font(Font.FontFamily.HELVETICA, 9f, Font.BOLD, BaseColor.WHITE)))
                cell.backgroundColor = COLOR_HEADER
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.setPadding(5f)
                itemsTable.addCell(cell)
            }

            items.forEachIndexed { index, item ->
                val bgColor = if (index % 2 == 0) BaseColor.WHITE else COLOR_LIGHT
                addItemRow(itemsTable, item, bgColor)
            }
            document.add(itemsTable)
            document.add(Chunk.NEWLINE)

            // Totals
            val totalsTable = PdfPTable(2)
            totalsTable.widthPercentage = 40f
            totalsTable.horizontalAlignment = Element.ALIGN_RIGHT
            addTotalRow(totalsTable, "Imponibile:", String.format("€ %.2f", invoice.taxableAmount))
            addTotalRow(totalsTable, "IVA:", String.format("€ %.2f", invoice.vat))
            addTotalRow(totalsTable, "TOTALE:", String.format("€ %.2f", invoice.total), bold = true)
            document.add(totalsTable)

            // Footer
            document.add(Chunk.NEWLINE)
            val footer = Paragraph("Documento generato con WareStat - Business Management System",
                Font(Font.FontFamily.HELVETICA, 7f, Font.NORMAL, BaseColor.GRAY))
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)

            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openPdf(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        context.startActivity(intent)
    }

    private fun addDetailRow(table: PdfPTable, label: String, value: String) {
        table.addCell(PdfPCell(Phrase(label, FONT_NORMAL)).apply { border = Rectangle.NO_BORDER })
        table.addCell(PdfPCell(Phrase(value, FONT_BOLD)).apply { border = Rectangle.NO_BORDER })
    }

    private fun addItemRow(table: PdfPTable, item: InvoiceItemWithProduct, bgColor: BaseColor) {
        listOf(
            item.productCode ?: "N/A",
            item.productName ?: "N/A",
            item.quantity.toString(),
            String.format("%.2f", item.unitPrice),
            String.format("%.1f", item.vatRate),
            String.format("%.2f", item.total)
        ).forEachIndexed { i, text ->
            val cell = PdfPCell(Phrase(text, FONT_SMALL))
            cell.backgroundColor = bgColor
            cell.horizontalAlignment = if (i >= 2) Element.ALIGN_RIGHT else Element.ALIGN_LEFT
            cell.setPadding(4f)
            table.addCell(cell)
        }
    }

    private fun addTotalRow(table: PdfPTable, label: String, value: String, bold: Boolean = false) {
        val f = if (bold) FONT_BOLD else FONT_NORMAL
        table.addCell(PdfPCell(Phrase(label, f)).apply { border = Rectangle.NO_BORDER })
        table.addCell(PdfPCell(Phrase(value, f)).apply {
            border = Rectangle.NO_BORDER
            horizontalAlignment = Element.ALIGN_RIGHT
        })
    }
}
