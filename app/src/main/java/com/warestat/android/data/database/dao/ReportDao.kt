package com.warestat.android.data.database.dao

import androidx.room.*

data class MonthlySalesData(
    val month: String,
    val total: Double,
    val numOrders: Int
)

data class ProductSalesData(
    val name: String,
    val totalQuantity: Int,
    val revenue: Double,
    val numOrders: Int
)

data class ABCProductData(
    val productId: Int,
    val name: String,
    val revenue: Double
)

@Dao
interface ReportDao {
    @Query("""
        SELECT strftime('%Y-%m', datetime(order_date / 1000, 'unixepoch')) as month,
               SUM(total) as total, COUNT(*) as numOrders
        FROM orders
        WHERE order_date >= :since AND status != 'Cancelled'
        GROUP BY month
        ORDER BY month
    """)
    suspend fun getMonthlySales(since: Long): List<MonthlySalesData>

    @Query("""
        SELECT COALESCE(p.name, 'N/A') as name,
               SUM(d.quantity) as totalQuantity,
               SUM(d.quantity * d.unit_price) as revenue,
               COUNT(DISTINCT o.id) as numOrders
        FROM order_details d
        LEFT JOIN products p ON d.product_id = p.id
        LEFT JOIN orders o ON d.order_id = o.id
        WHERE o.order_date >= :since AND o.status != 'Cancelled'
        GROUP BY d.product_id, p.name
        ORDER BY revenue DESC
        LIMIT 10
    """)
    suspend fun getTopProductsStats(since: Long): List<ProductSalesData>

    @Query("""
        SELECT p.id as productId, p.name,
               COALESCE(SUM(od.quantity * od.unit_price), 0) as revenue
        FROM products p
        LEFT JOIN order_details od ON p.id = od.product_id
        LEFT JOIN orders o ON od.order_id = o.id AND o.status = 'Completed'
        WHERE p.active = 1
        GROUP BY p.id, p.name
        ORDER BY revenue DESC
    """)
    suspend fun getABCData(): List<ABCProductData>

    @Query("""
        SELECT AVG(CASE
            WHEN p.price > 0 AND p.acquisition_cost > 0
            THEN ((p.price - p.acquisition_cost) / p.price) * 100
            ELSE 0
        END)
        FROM products p WHERE p.active = 1 AND p.price > 0
    """)
    suspend fun getAverageMargin(): Double?
}
