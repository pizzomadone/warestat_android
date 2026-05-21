package com.warestat.android.data.database.dao

import androidx.room.*
import com.warestat.android.data.database.entity.*
import kotlinx.coroutines.flow.Flow

data class StockStatus(
    val productId: Int,
    val code: String,
    val name: String,
    val physicalStock: Int,
    val reservedStock: Int,
    val availableStock: Int,
    val minimumQuantity: Int?,
    val reorderQuantity: Int?,
    val preferredSupplierName: String?
)

data class MovementWithProduct(
    val id: Int,
    val productId: Int,
    val productName: String,
    val date: Long,
    val type: String,
    val quantity: Int,
    val reason: String,
    val documentNumber: String,
    val documentType: String,
    val notes: String
)

data class NotificationWithProduct(
    val id: Int,
    val productId: Int,
    val productName: String,
    val date: Long,
    val type: String,
    val message: String,
    val status: String
)

@Dao
interface WarehouseDao {
    @Query("""
        SELECT p.id as productId, p.code, p.name,
               p.quantity as physicalStock, p.reserved_quantity as reservedStock,
               (p.quantity - p.reserved_quantity) as availableStock,
               ms.minimum_quantity as minimumQuantity, ms.reorder_quantity as reorderQuantity,
               s.company_name as preferredSupplierName
        FROM products p
        LEFT JOIN minimum_stock ms ON p.id = ms.product_id
        LEFT JOIN suppliers s ON ms.preferred_supplier_id = s.id
        WHERE p.active = 1
        ORDER BY p.name
    """)
    fun getStockStatus(): Flow<List<StockStatus>>

    // Movements
    @Query("""
        SELECT m.id, m.product_id as productId, p.name as productName,
               m.date, m.type, m.quantity, m.reason,
               m.document_number as documentNumber, m.document_type as documentType, m.notes
        FROM warehouse_movements m
        JOIN products p ON m.product_id = p.id
        ORDER BY m.date DESC
        LIMIT 200
    """)
    fun getAllMovements(): Flow<List<MovementWithProduct>>

    @Query("""
        SELECT m.id, m.product_id as productId, p.name as productName,
               m.date, m.type, m.quantity, m.reason,
               m.document_number as documentNumber, m.document_type as documentType, m.notes
        FROM warehouse_movements m
        JOIN products p ON m.product_id = p.id
        WHERE p.name LIKE :query OR m.reason LIKE :query OR m.document_number LIKE :query
        ORDER BY m.date DESC
    """)
    fun searchMovements(query: String): Flow<List<MovementWithProduct>>

    @Insert
    suspend fun insertMovement(movement: WarehouseMovementEntity): Long

    @Update
    suspend fun updateMovement(movement: WarehouseMovementEntity)

    @Query("DELETE FROM warehouse_movements WHERE id = :id")
    suspend fun deleteMovement(id: Int)

    @Query("SELECT * FROM warehouse_movements WHERE id = :id")
    suspend fun getMovementById(id: Int): WarehouseMovementEntity?

    // Minimum Stock
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMinimumStock(minStock: MinimumStockEntity)

    @Query("SELECT * FROM minimum_stock WHERE product_id = :productId")
    suspend fun getMinimumStock(productId: Int): MinimumStockEntity?

    @Delete
    suspend fun deleteMinimumStock(minStock: MinimumStockEntity)

    // Notifications
    @Query("""
        SELECT n.id, n.product_id as productId, p.name as productName,
               n.date, n.type, n.message, n.status
        FROM warehouse_notifications n
        JOIN products p ON n.product_id = p.id
        WHERE n.status != 'HANDLED'
        ORDER BY n.date DESC
    """)
    fun getActiveNotifications(): Flow<List<NotificationWithProduct>>

    @Insert
    suspend fun insertNotification(notification: WarehouseNotificationEntity): Long

    @Query("UPDATE warehouse_notifications SET status = :status WHERE id = :id")
    suspend fun updateNotificationStatus(id: Int, status: String)

    @Query("UPDATE warehouse_notifications SET status = :status WHERE id IN (:ids)")
    suspend fun updateNotificationStatuses(ids: List<Int>, status: String)

    // Stock Reservations
    @Query("""
        SELECT id, reserved_quantity, status
        FROM stock_reservations
        WHERE product_id = :productId AND document_type = :docType AND document_id = :docId
    """)
    suspend fun getReservation(productId: Int, docType: String, docId: Int): ReservationResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: StockReservationEntity): Long

    @Query("""
        UPDATE stock_reservations
        SET reserved_quantity = :qty, status = 'ACTIVE', notes = :notes
        WHERE id = :reservationId
    """)
    suspend fun updateReservation(reservationId: Int, qty: Int, notes: String)

    @Query("""
        UPDATE stock_reservations
        SET status = 'CANCELLED'
        WHERE document_type = :docType AND document_id = :docId AND status = 'ACTIVE'
    """)
    suspend fun cancelReservations(docType: String, docId: Int)

    @Query("""
        SELECT product_id, reserved_quantity
        FROM stock_reservations
        WHERE document_type = :docType AND document_id = :docId AND status = 'ACTIVE'
    """)
    suspend fun getActiveReservations(docType: String, docId: Int): List<ReservationQuantity>

    @Query("""
        UPDATE stock_reservations
        SET status = 'COMPLETED'
        WHERE document_type = :docType AND document_id = :docId AND status = 'ACTIVE'
    """)
    suspend fun completeReservations(docType: String, docId: Int)

    // Check if today's low stock notification already exists
    @Query("""
        SELECT COUNT(*) > 0 FROM warehouse_notifications
        WHERE product_id = :productId AND type = 'MIN_STOCK' AND status != 'HANDLED'
        AND date >= :startOfDay
    """)
    suspend fun hasTodayNotification(productId: Int, startOfDay: Long): Boolean

    // Products below minimum stock
    @Query("""
        SELECT p.id as productId, p.quantity, ms.minimum_quantity, ms.reorder_quantity
        FROM products p
        JOIN minimum_stock ms ON p.id = ms.product_id
        WHERE p.quantity <= ms.minimum_quantity
    """)
    suspend fun getProductsBelowMinStock(): List<LowStockProduct>
}

data class ReservationResult(
    val id: Int,
    val reservedQuantity: Int,
    val status: String
)

data class ReservationQuantity(
    val productId: Int,
    val reservedQuantity: Int
)

data class LowStockProduct(
    val productId: Int,
    val quantity: Int,
    val minimumQuantity: Int,
    val reorderQuantity: Int
)
