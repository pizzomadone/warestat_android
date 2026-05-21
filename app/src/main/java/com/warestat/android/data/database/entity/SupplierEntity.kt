package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "suppliers",
    indices = [Index("vat_number", unique = true)]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "company_name") val companyName: String,
    @ColumnInfo(name = "vat_number") val vatNumber: String,
    @ColumnInfo(name = "tax_code") val taxCode: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    @ColumnInfo(name = "certified_email") val certifiedEmail: String = "",
    val website: String = "",
    val notes: String = ""
)
