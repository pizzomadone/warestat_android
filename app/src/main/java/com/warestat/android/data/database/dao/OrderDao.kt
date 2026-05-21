package com.warestat.android.data.database.dao

import androidx.room.*
import com.warestat.android.data.database.entity.OrderEntity
import com.warestat.android.data.database.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

data class OrderWithCustomer(
    val id: Int,
    val customerId: Int?,
    val customerName: String?,
    val orderDate: Long,
    val status: String,
    val total: Double,
    val paymentStatus: String,
    val paidAmount: Double,
    val notes: String = ""
)

data class OrderItemWithProduct(
    val id: Int,
    val orderId: Int,
    val productId: Int,
    val productName: String?,
    val productCode: String?,
    val quantity: Int,
    val unitPrice: Double
)

@Dao
interface OrderDao {
    @Query("""
        SELECT o.id, o.customer_id as customerId,
               (c.first_name || ' ' || c.last_name) as customerName,
               o.order_date as orderDate, o.status, o.total,
               o.payment_status as paymentStatus, o.paid_amount as paidAmount,
               COALESCE(o.notes, '') as notes
        FROM orders o
        LEFT JOIN customers c ON o.customer_id = c.id
        ORDER BY o.order_date DESC
    """)
    fun getAllOrders(): Flow<List<OrderWithCustomer>>

    @Query("""
        SELECT o.id, o.customer_id as customerId,
               (c.first_name || ' ' || c.last_name) as customerName,
               o.order_date as orderDate, o.status, o.total,
               o.payment_status as paymentStatus, o.paid_amount as paidAmount,
               COALESCE(o.notes, '') as notes
        FROM orders o
        LEFT JOIN customers c ON o.customer_id = c.id
        WHERE o.status != 'Completed' AND o.status != 'Cancelled'
        ORDER BY o.order_date ASC
    """)
    fun getPendingOrders(): Flow<List<OrderWithCustomer>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: Int): OrderEntity?

    @Query("""
        SELECT od.id, od.order_id as orderId, od.product_id as productId,
               p.name as productName, p.code as productCode,
               od.quantity, od.unit_price as unitPrice
        FROM order_details od
        LEFT JOIN products p ON od.product_id = p.id
        WHERE od.order_id = :orderId
    """)
    suspend fun getOrderItems(orderId: Int): List<OrderItemWithProduct>

    @Query("""
        SELECT o.id, o.customer_id as customerId,
               (c.first_name || ' ' || c.last_name) as customerName,
               o.order_date as orderDate, o.status, o.total,
               o.payment_status as paymentStatus, o.paid_amount as paidAmount,
               COALESCE(o.notes, '') as notes
        FROM orders o
        LEFT JOIN customers c ON o.customer_id = c.id
        WHERE o.order_date BETWEEN :startDate AND :endDate
        ORDER BY o.order_date DESC
    """)
    fun getOrdersByDateRange(startDate: Long, endDate: Long): Flow<List<OrderWithCustomer>>

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertOrderItem(item: OrderItemEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("DELETE FROM order_details WHERE order_id = :orderId")
    suspend fun deleteOrderItems(orderId: Int)

    @Query("SELECT COUNT(*) FROM orders WHERE status != 'Completed' AND status != 'Cancelled'")
    suspend fun getPendingOrderCount(): Int

    @Query("""
        SELECT SUM(o.total) FROM orders o
        WHERE o.order_date >= :monthStart AND o.status != 'Cancelled'
    """)
    suspend fun getMonthRevenue(monthStart: Long): Double?

    @Query("""
        SELECT p.name,
               SUM(od.quantity) as totalQty,
               SUM(od.quantity * od.unit_price) as totalRevenue
        FROM order_details od
        JOIN products p ON od.product_id = p.id
        JOIN orders o ON od.order_id = o.id
        WHERE o.status = 'Completed'
        GROUP BY p.id, p.name
        ORDER BY totalQty DESC
        LIMIT 10
    """)
    suspend fun getTopProducts(): List<TopProductResult>
}

data class TopProductResult(
    val name: String,
    val totalQty: Int,
    val totalRevenue: Double
)
