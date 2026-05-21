package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("customer_id"), Index("number", unique = true)]
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val date: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "customer_id") val customerId: Int?,
    @ColumnInfo(name = "taxable_amount") val taxableAmount: Double,
    val vat: Double,
    val total: Double,
    val status: String
)
