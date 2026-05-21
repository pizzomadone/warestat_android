package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.*
import com.warestat.android.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsState(
    val salesData: List<OrderWithCustomer> = emptyList(),
    val monthlySales: List<MonthlySalesData> = emptyList(),
    val topProducts: List<ProductSalesData> = emptyList(),
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val selectedPeriodMonths: Int = 6,
    val startDate: Long = DateUtils.monthsAgo(1),
    val endDate: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val reportDao: ReportDao
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            orderDao.getOrdersByDateRange(_state.value.startDate, _state.value.endDate)
                .collect { orders ->
                    _state.update { it.copy(salesData = orders, totalOrders = orders.size, totalSales = orders.sumOf { o -> o.total }) }
                }
        }
        loadAdvancedStats()
    }

    fun setDateRange(start: Long, end: Long) {
        _state.update { it.copy(startDate = start, endDate = end) }
        viewModelScope.launch {
            orderDao.getOrdersByDateRange(start, end).collect { orders ->
                _state.update { it.copy(salesData = orders, totalOrders = orders.size, totalSales = orders.sumOf { o -> o.total }) }
            }
        }
    }

    fun setPeriod(months: Int) {
        _state.update { it.copy(selectedPeriodMonths = months, isLoading = true) }
        loadAdvancedStats()
    }

    private fun loadAdvancedStats() {
        viewModelScope.launch {
            try {
                val since = DateUtils.monthsAgo(_state.value.selectedPeriodMonths)
                val monthly = reportDao.getMonthlySales(since)
                val topProducts = reportDao.getTopProductsStats(since)
                _state.update { it.copy(monthlySales = monthly, topProducts = topProducts, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
