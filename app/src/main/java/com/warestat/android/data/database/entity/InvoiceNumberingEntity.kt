package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_numbering")
data class InvoiceNumberingEntity(
    @PrimaryKey val year: Int,
    @ColumnInfo(name = "last_number") val lastNumber: Int
)
