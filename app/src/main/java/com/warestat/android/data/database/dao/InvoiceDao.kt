package com.warestat.android.data.database.dao

import androidx.room.*
import com.warestat.android.data.database.entity.InvoiceEntity
import com.warestat.android.data.database.entity.InvoiceItemEntity
import com.warestat.android.data.database.entity.InvoiceNumberingEntity
import kotlinx.coroutines.flow.Flow

data class InvoiceWithCustomer(
    val id: Int,
    val number: String,
    val date: Long,
    val customerId: Int?,
    val customerName: String?,
    val taxableAmount: Double,
    val vat: Double,
    val total: Double,
    val status: String
)

data class InvoiceItemWithProduct(
    val id: Int,
    val invoiceId: Int,
    val productId: Int,
    val productName: String?,
    val productCode: String?,
    val quantity: Int,
    val unitPrice: Double,
    val vatRate: Double,
    val total: Double
)

@Dao
interface InvoiceDao {
    @Query("""
        SELECT i.id, i.number, i.date, i.customer_id as customerId,
               (c.first_name || ' ' || c.last_name) as customerName,
               i.taxable_amount as taxableAmount, i.vat, i.total, i.status
        FROM invoices i
        LEFT JOIN customers c ON i.customer_id = c.id
        ORDER BY i.date DESC
    """)
    fun getAllInvoices(): Flow<List<InvoiceWithCustomer>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Int): InvoiceEntity?

    @Query("""
        SELECT id.id, id.invoice_id as invoiceId, id.product_id as productId,
               p.name as productName, p.code as productCode,
               id.quantity, id.unit_price as unitPrice, id.vat_rate as vatRate, id.total
        FROM invoice_details id
        LEFT JOIN products p ON id.product_id = p.id
        WHERE id.invoice_id = :invoiceId
        ORDER BY id.id
    """)
    suspend fun getInvoiceItems(invoiceId: Int): List<InvoiceItemWithProduct>

    @Insert
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Insert
    suspend fun insertInvoiceItem(item: InvoiceItemEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoice_details WHERE invoice_id = :invoiceId")
    suspend fun deleteInvoiceItems(invoiceId: Int)

    @Query("SELECT * FROM invoice_numbering WHERE year = :year")
    suspend fun getInvoiceNumbering(year: Int): InvoiceNumberingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoiceNumbering(numbering: InvoiceNumberingEntity)
}
