package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_details",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoice_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("invoice_id"), Index("product_id")]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "invoice_id") val invoiceId: Int,
    @ColumnInfo(name = "product_id") val productId: Int,
    val quantity: Int,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "vat_rate") val vatRate: Double,
    val total: Double
)
