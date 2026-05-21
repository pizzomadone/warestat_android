package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "minimum_stock",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["preferred_supplier_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("preferred_supplier_id")]
)
data class MinimumStockEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "minimum_quantity") val minimumQuantity: Int,
    @ColumnInfo(name = "reorder_quantity") val reorderQuantity: Int,
    @ColumnInfo(name = "lead_time_days") val leadTimeDays: Int = 0,
    @ColumnInfo(name = "preferred_supplier_id") val preferredSupplierId: Int? = null,
    val notes: String = ""
)
