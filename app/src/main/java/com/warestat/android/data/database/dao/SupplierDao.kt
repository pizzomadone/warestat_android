package com.warestat.android.data.database.dao

import androidx.room.*
import com.warestat.android.data.database.entity.*
import kotlinx.coroutines.flow.Flow

data class SupplierOrderWithSupplier(
    val id: Int,
    val supplierId: Int,
    val supplierName: String,
    val number: String,
    val orderDate: Long,
    val expectedDeliveryDate: Long?,
    val status: String,
    val total: Double,
    val notes: String
)

data class SupplierOrderItemWithProduct(
    val id: Int,
    val orderId: Int,
    val productId: Int,
    val productName: String?,
    val productCode: String?,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double,
    val notes: String
)

data class PriceListWithDetails(
    val id: Int,
    val supplierId: Int,
    val supplierName: String,
    val productId: Int,
    val productName: String?,
    val productCode: String?,
    val supplierProductCode: String,
    val price: Double,
    val minimumQuantity: Int,
    val validityStartDate: Long,
    val validityEndDate: Long?,
    val notes: String
)

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY company_name")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Int): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE company_name LIKE :query OR vat_number LIKE :query ORDER BY company_name")
    fun searchSuppliers(query: String): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    // Supplier Orders
    @Query("""
        SELECT so.id, so.supplier_id as supplierId, s.company_name as supplierName,
               so.number, so.order_date as orderDate, so.expected_delivery_date as expectedDeliveryDate,
               so.status, so.total, so.notes
        FROM supplier_orders so
        JOIN suppliers s ON so.supplier_id = s.id
        ORDER BY so.order_date DESC
    """)
    fun getAllSupplierOrders(): Flow<List<SupplierOrderWithSupplier>>

    @Query("SELECT * FROM supplier_orders WHERE id = :id")
    suspend fun getSupplierOrderById(id: Int): SupplierOrderEntity?

    @Query("""
        SELECT sod.id, sod.order_id as orderId, sod.product_id as productId,
               p.name as productName, p.code as productCode,
               sod.quantity, sod.unit_price as unitPrice, sod.total, sod.notes
        FROM supplier_order_details sod
        LEFT JOIN products p ON sod.product_id = p.id
        WHERE sod.order_id = :orderId
    """)
    suspend fun getSupplierOrderItems(orderId: Int): List<SupplierOrderItemWithProduct>

    @Insert
    suspend fun insertSupplierOrder(order: SupplierOrderEntity): Long

    @Insert
    suspend fun insertSupplierOrderItem(item: SupplierOrderItemEntity): Long

    @Update
    suspend fun updateSupplierOrder(order: SupplierOrderEntity)

    @Delete
    suspend fun deleteSupplierOrder(order: SupplierOrderEntity)

    @Query("DELETE FROM supplier_order_details WHERE order_id = :orderId")
    suspend fun deleteSupplierOrderItems(orderId: Int)

    // Price Lists
    @Query("""
        SELECT spl.id, spl.supplier_id as supplierId, s.company_name as supplierName,
               spl.product_id as productId, p.name as productName, p.code as productCode,
               spl.supplier_product_code as supplierProductCode, spl.price,
               spl.minimum_quantity as minimumQuantity,
               spl.validity_start_date as validityStartDate,
               spl.validity_end_date as validityEndDate, spl.notes
        FROM supplier_price_lists spl
        JOIN suppliers s ON spl.supplier_id = s.id
        LEFT JOIN products p ON spl.product_id = p.id
        ORDER BY s.company_name, p.name
    """)
    fun getAllPriceLists(): Flow<List<PriceListWithDetails>>

    @Query("""
        SELECT spl.id, spl.supplier_id as supplierId, s.company_name as supplierName,
               spl.product_id as productId, p.name as productName, p.code as productCode,
               spl.supplier_product_code as supplierProductCode, spl.price,
               spl.minimum_quantity as minimumQuantity,
               spl.validity_start_date as validityStartDate,
               spl.validity_end_date as validityEndDate, spl.notes
        FROM supplier_price_lists spl
        JOIN suppliers s ON spl.supplier_id = s.id
        LEFT JOIN products p ON spl.product_id = p.id
        WHERE spl.supplier_id = :supplierId
        ORDER BY p.name
    """)
    fun getPriceListBySupplier(supplierId: Int): Flow<List<PriceListWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceList(priceList: SupplierPriceListEntity): Long

    @Update
    suspend fun updatePriceList(priceList: SupplierPriceListEntity)

    @Delete
    suspend fun deletePriceList(priceList: SupplierPriceListEntity)
}
