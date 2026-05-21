package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "warehouse_movements",
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
data class WarehouseMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "product_id") val productId: Int,
    val date: Long = System.currentTimeMillis(),
    val type: String,       // INWARD / OUTWARD
    val quantity: Int,
    val reason: String,
    @ColumnInfo(name = "document_number") val documentNumber: String = "",
    @ColumnInfo(name = "document_type") val documentType: String = "",
    val notes: String = ""
)
