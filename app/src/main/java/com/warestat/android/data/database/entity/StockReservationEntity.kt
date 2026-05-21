package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_reservations",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("product_id")]
)
data class StockReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "document_type") val documentType: String,
    @ColumnInfo(name = "document_id") val documentId: Int,
    @ColumnInfo(name = "reserved_quantity") val reservedQuantity: Int,
    @ColumnInfo(name = "reservation_date") val reservationDate: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE",
    val notes: String = ""
)
