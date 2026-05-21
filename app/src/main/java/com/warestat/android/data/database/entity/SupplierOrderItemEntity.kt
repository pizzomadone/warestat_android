package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "supplier_order_details",
    foreignKeys = [
        ForeignKey(
            entity = SupplierOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("order_id"), Index("product_id")]
)
data class SupplierOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "order_id") val orderId: Int,
    @ColumnInfo(name = "product_id") val productId: Int,
    val quantity: Int,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    val total: Double,
    val notes: String = ""
)
