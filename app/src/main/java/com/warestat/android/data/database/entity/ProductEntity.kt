package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplier_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("supplier_id"), Index("code", unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val name: String,
    val description: String = "",
    val price: Double,
    val quantity: Int = 0,
    @ColumnInfo(name = "reserved_quantity") val reservedQuantity: Int = 0,
    val category: String = "",
    @ColumnInfo(name = "alternative_sku") val alternativeSku: String = "",
    val weight: Double = 0.0,
    @ColumnInfo(name = "unit_of_measure") val unitOfMeasure: String = "pcs",
    @ColumnInfo(name = "minimum_quantity") val minimumQuantity: Int = 0,
    @ColumnInfo(name = "acquisition_cost") val acquisitionCost: Double = 0.0,
    val active: Boolean = true,
    @ColumnInfo(name = "supplier_id") val supplierId: Int? = null,
    @ColumnInfo(name = "warehouse_position") val warehousePosition: String = "",
    @ColumnInfo(name = "vat_rate") val vatRate: Double = 0.0
)
