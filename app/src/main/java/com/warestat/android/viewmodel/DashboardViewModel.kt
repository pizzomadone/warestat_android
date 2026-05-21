package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.*
import com.warestat.android.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardState(
    val warehouseValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val pendingOrderCount: Int = 0,
    val monthRevenue: Double = 0.0,
    val averageMargin: Double = 0.0,
    val zeroStockCount: Int = 0,
    val lowStockAlerts: List<ProductWithSupplier> = emptyList(),
    val pendingOrders: List<OrderWithCustomer> = emptyList(),
    val topProducts: List<TopProductResult> = emptyList(),
    val abcData: ABCAnalysisData = ABCAnalysisData(),
    val isLoading: Boolean = false
)

data class ABCAnalysisData(
    val classA: ABCClass = ABCClass("A", 0, 0.0, 0.0, "Prodotti ad alto valore - Priorità massima"),
    val classB: ABCClass = ABCClass("B", 0, 0.0, 0.0, "Prodotti a medio valore - Monitorare"),
    val classC: ABCClass = ABCClass("C", 0, 0.0, 0.0, "Prodotti a basso valore - Valutare liquidazione"),
    val totalProducts: Int = 0,
    val totalRevenue: Double = 0.0
)

data class ABCClass(val name: String, val count: Int, val revenue: Double, val revenuePercent: Double, val description: String)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val reportDao: ReportDao
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val warehouseValue = productDao.getTotalWarehouseValue() ?: 0.0
                val lowStockCount = productDao.getLowStockCount()
                val zeroStockCount = productDao.getZeroStockCount()
                val pendingOrderCount = orderDao.getPendingOrderCount()
                val monthRevenue = orderDao.getMonthRevenue(DateUtils.startOfMonth()) ?: 0.0
                val avgMargin = reportDao.getAverageMargin() ?: 0.0
                val lowStockAlerts = productDao.getLowStockProducts(10)
                val topProducts = orderDao.getTopProducts()
                val abcData = computeABC()

                _state.update {
                    it.copy(
                        warehouseValue = warehouseValue,
                        lowStockCount = lowStockCount,
                        zeroStockCount = zeroStockCount,
                        pendingOrderCount = pendingOrderCount,
                        monthRevenue = monthRevenue,
                        averageMargin = avgMargin,
                        lowStockAlerts = lowStockAlerts,
                        topProducts = topProducts,
                        abcData = abcData,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }

        // Observe pending orders reactively
        viewModelScope.launch {
            orderDao.getPendingOrders().collect { orders ->
                _state.update { it.copy(pendingOrders = orders) }
            }
        }
    }

    private suspend fun computeABC(): ABCAnalysisData {
        val products = reportDao.getABCData()
        if (products.isEmpty()) return ABCAnalysisData()

        val totalRevenue = products.sumOf { it.revenue }
        if (totalRevenue == 0.0) return ABCAnalysisData(totalProducts = products.size)

        var cumulative = 0.0
        var aCount = 0; var bCount = 0; var cCount = 0
        var aRevenue = 0.0; var bRevenue = 0.0; var cRevenue = 0.0

        products.forEach { p ->
            cumulative += p.revenue
            val pct = cumulative / totalRevenue
            when {
                pct <= 0.80 -> { aCount++; aRevenue += p.revenue }
                pct <= 0.95 -> { bCount++; bRevenue += p.revenue }
                else -> { cCount++; cRevenue += p.revenue }
            }
        }

        return ABCAnalysisData(
            classA = ABCClass("A", aCount, aRevenue, if (totalRevenue > 0) aRevenue / totalRevenue * 100 else 0.0, "Prodotti ad alto valore - Priorità massima"),
            classB = ABCClass("B", bCount, bRevenue, if (totalRevenue > 0) bRevenue / totalRevenue * 100 else 0.0, "Prodotti a medio valore - Monitorare"),
            classC = ABCClass("C", cCount, cRevenue, if (totalRevenue > 0) cRevenue / totalRevenue * 100 else 0.0, "Prodotti a basso valore - Valutare liquidazione"),
            totalProducts = products.size,
            totalRevenue = totalRevenue
        )
    }
}
