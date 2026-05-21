package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("customer_id")]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "customer_id") val customerId: Int?,
    @ColumnInfo(name = "order_date") val orderDate: Long = System.currentTimeMillis(),
    val status: String,
    val total: Double,
    @ColumnInfo(name = "payment_status") val paymentStatus: String = "NOT_PAID",
    @ColumnInfo(name = "paid_amount") val paidAmount: Double = 0.0
)
