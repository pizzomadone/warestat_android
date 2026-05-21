package com.warestat.android.data.database.dao

import androidx.room.*
import com.warestat.android.data.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

data class ProductWithSupplier(
    val id: Int,
    val code: String,
    val name: String,
    val description: String,
    val price: Double,
    val quantity: Int,
    @ColumnInfo(name = "reserved_quantity") val reservedQuantity: Int,
    val category: String,
    @ColumnInfo(name = "alternative_sku") val alternativeSku: String,
    val weight: Double,
    @ColumnInfo(name = "unit_of_measure") val unitOfMeasure: String,
    @ColumnInfo(name = "minimum_quantity") val minimumQuantity: Int,
    @ColumnInfo(name = "acquisition_cost") val acquisitionCost: Double,
    val active: Boolean,
    @ColumnInfo(name = "supplier_id") val supplierId: Int?,
    val supplierName: String?,
    @ColumnInfo(name = "warehouse_position") val warehousePosition: String,
    @ColumnInfo(name = "vat_rate") val vatRate: Double
)

@Dao
interface ProductDao {
    @Query("""
        SELECT p.*, s.company_name as supplierName
        FROM products p
        LEFT JOIN suppliers s ON p.supplier_id = s.id
        WHERE p.active = 1
        ORDER BY p.name
    """)
    fun getAllActiveProducts(): Flow<List<ProductWithSupplier>>

    @Query("""
        SELECT p.*, s.company_name as supplierName
        FROM products p
        LEFT JOIN suppliers s ON p.supplier_id = s.id
        ORDER BY p.name
    """)
    fun getAllProducts(): Flow<List<ProductWithSupplier>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE code = :code")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Query("""
        SELECT p.*, s.company_name as supplierName
        FROM products p
        LEFT JOIN suppliers s ON p.supplier_id = s.id
        WHERE (p.name LIKE :query OR p.code LIKE :query OR p.alternative_sku LIKE :query)
        AND p.active = 1
        ORDER BY p.name
    """)
    fun searchProducts(query: String): Flow<List<ProductWithSupplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET quantity = quantity + :delta WHERE id = :productId")
    suspend fun updateStock(productId: Int, delta: Int)

    @Query("UPDATE products SET reserved_quantity = :reservedQty WHERE id = :productId")
    suspend fun updateReservedQuantity(productId: Int, reservedQty: Int)

    @Query("SELECT COUNT(*) FROM products WHERE active = 1 AND quantity < minimum_quantity AND minimum_quantity > 0")
    suspend fun getLowStockCount(): Int

    @Query("SELECT COUNT(*) FROM products WHERE active = 1 AND quantity = 0")
    suspend fun getZeroStockCount(): Int

    @Query("SELECT SUM(quantity * acquisition_cost) FROM products WHERE active = 1")
    suspend fun getTotalWarehouseValue(): Double?

    @Query("""
        SELECT p.*, s.company_name as supplierName
        FROM products p
        LEFT JOIN suppliers s ON p.supplier_id = s.id
        WHERE p.active = 1 AND p.quantity < p.minimum_quantity AND p.minimum_quantity > 0
        ORDER BY (p.minimum_quantity - p.quantity) DESC
        LIMIT :limit
    """)
    suspend fun getLowStockProducts(limit: Int = 10): List<ProductWithSupplier>
}
