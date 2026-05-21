package com.warestat.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_data")
data class CompanyDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "company_name") val companyName: String,
    @ColumnInfo(name = "vat_number") val vatNumber: String = "",
    @ColumnInfo(name = "tax_code") val taxCode: String = "",
    val address: String = "",
    val city: String = "",
    @ColumnInfo(name = "postal_code") val postalCode: String = "",
    val country: String = "Italy",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    @ColumnInfo(name = "logo_path") val logoPath: String = ""
)
