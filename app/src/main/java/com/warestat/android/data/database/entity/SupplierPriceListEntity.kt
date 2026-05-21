package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "supplier_price_lists",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplier_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("supplier_id"), Index("product_id")]
)
data class SupplierPriceListEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "supplier_id") val supplierId: Int,
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "supplier_product_code") val supplierProductCode: String = "",
    val price: Double,
    @ColumnInfo(name = "minimum_quantity") val minimumQuantity: Int = 1,
    @ColumnInfo(name = "validity_start_date") val validityStartDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "validity_end_date") val validityEndDate: Long? = null,
    val notes: String = ""
)
