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
    val reservedQuantity: Int,
    val category: String,
    val alternativeSku: String,
    val weight: Double,
    val unitOfMeasure: String,
    val minimumQuantity: Int,
    val acquisitionCost: Double,
    val active: Boolean,
    val supplierId: Int?,
    val supplierName: String?,
    val warehousePosition: String,
    val vatRate: Double
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
