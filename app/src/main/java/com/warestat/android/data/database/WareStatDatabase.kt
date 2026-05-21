package com.warestat.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.warestat.android.data.database.dao.*
import com.warestat.android.data.database.entity.*

@Database(
    entities = [
        CustomerEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        InvoiceNumberingEntity::class,
        SupplierEntity::class,
        SupplierOrderEntity::class,
        SupplierOrderItemEntity::class,
        SupplierPriceListEntity::class,
        WarehouseMovementEntity::class,
        MinimumStockEntity::class,
        WarehouseNotificationEntity::class,
        StockReservationEntity::class,
        CompanyDataEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WareStatDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun supplierDao(): SupplierDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun companyDataDao(): CompanyDataDao
    abstract fun reportDao(): ReportDao
}
