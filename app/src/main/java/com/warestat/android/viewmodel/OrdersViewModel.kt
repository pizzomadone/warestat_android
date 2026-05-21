package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.*
import com.warestat.android.data.database.entity.*
import com.warestat.android.util.StockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersState(
    val orders: List<OrderWithCustomer> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductWithSupplier> = emptyList(),
    val statusFilter: String = "ALL",
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val customerDao: CustomerDao,
    private val productDao: ProductDao,
    private val stockManager: StockManager
) : ViewModel() {

    private val _statusFilter = MutableStateFlow("ALL")
    private val _state = MutableStateFlow(OrdersState())
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _statusFilter.flatMapLatest { filter ->
                if (filter == "ALL") orderDao.getAllOrders()
                else orderDao.getAllOrders().map { orders -> orders.filter { it.status == filter } }
            }.collect { orders ->
                _state.update { it.copy(orders = orders) }
            }
        }
        viewModelScope.launch {
            customerDao.getAllCustomers().collect { customers ->
                _state.update { it.copy(customers = customers) }
            }
        }
        viewModelScope.launch {
            productDao.getAllActiveProducts().collect { products ->
                _state.update { it.copy(products = products) }
            }
        }
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
        _state.update { it.copy(statusFilter = filter) }
    }

    suspend fun getOrderItems(orderId: Int): List<OrderItemWithProduct> = orderDao.getOrderItems(orderId)

    fun saveOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        viewModelScope.launch {
            try {
                val isNew = order.id == 0
                val orderId: Int

                if (isNew) {
                    orderId = orderDao.insertOrder(order).toInt()
                } else {
                    orderId = order.id
                    orderDao.deleteOrderItems(orderId)
                    stockManager.cancelReservation("ORDER", orderId)
                    orderDao.updateOrder(order)
                }

                items.forEach { item ->
                    orderDao.insertOrderItem(item.copy(orderId = orderId))
                }

                // Create stock reservations for active orders
                if (order.status == "New" || order.status == "In Progress") {
                    items.forEach { item ->
                        stockManager.createOrUpdateReservation(
                            productId = item.productId,
                            documentType = "ORDER",
                            documentId = orderId,
                            quantity = item.quantity,
                            note = "Ordine #$orderId"
                        )
                    }
                }

                // Complete order: decrement actual stock
                if (order.status == "Completed") {
                    stockManager.completeReservationAndDecrementStock(
                        documentType = "ORDER",
                        documentId = orderId,
                        documentDate = order.orderDate,
                        documentNumber = orderId.toString()
                    )
                }

                _state.update { it.copy(successMessage = if (isNew) "Ordine creato" else "Ordine aggiornato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteOrder(order: OrderWithCustomer) {
        viewModelScope.launch {
            try {
                val entity = orderDao.getOrderById(order.id) ?: return@launch
                if (order.status == "New" || order.status == "In Progress") {
                    stockManager.cancelReservation("ORDER", order.id)
                }
                orderDao.deleteOrderItems(order.id)
                orderDao.deleteOrder(entity)
                _state.update { it.copy(successMessage = "Ordine eliminato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }
}
