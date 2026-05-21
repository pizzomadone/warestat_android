package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "supplier_orders",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplier_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("supplier_id")]
)
data class SupplierOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "supplier_id") val supplierId: Int,
    val number: String,
    @ColumnInfo(name = "order_date") val orderDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "expected_delivery_date") val expectedDeliveryDate: Long? = null,
    val status: String,
    val total: Double,
    val notes: String = ""
)
