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

data class WarehouseState(
    val stockStatus: List<StockStatus> = emptyList(),
    val movements: List<MovementWithProduct> = emptyList(),
    val notifications: List<NotificationWithProduct> = emptyList(),
    val searchQuery: String = "",
    val suppliers: List<SupplierEntity> = emptyList(),
    val products: List<ProductWithSupplier> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class WarehouseViewModel @Inject constructor(
    private val warehouseDao: WarehouseDao,
    private val supplierDao: SupplierDao,
    private val productDao: ProductDao,
    private val stockManager: StockManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _state = MutableStateFlow(WarehouseState())
    val state: StateFlow<WarehouseState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            warehouseDao.getStockStatus().collect { stock ->
                _state.update { it.copy(stockStatus = stock) }
            }
        }
        viewModelScope.launch {
            _searchQuery.debounce(300).flatMapLatest { query ->
                if (query.isBlank()) warehouseDao.getAllMovements()
                else warehouseDao.searchMovements("%$query%")
            }.collect { movements ->
                _state.update { it.copy(movements = movements) }
            }
        }
        viewModelScope.launch {
            warehouseDao.getActiveNotifications().collect { notifications ->
                _state.update { it.copy(notifications = notifications) }
            }
        }
        viewModelScope.launch {
            supplierDao.getAllSuppliers().collect { suppliers ->
                _state.update { it.copy(suppliers = suppliers) }
            }
        }
        viewModelScope.launch {
            productDao.getAllActiveProducts().collect { products ->
                _state.update { it.copy(products = products) }
            }
        }

        // Check for low stock on load
        viewModelScope.launch { stockManager.checkAndCreateLowStockNotifications() }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun saveMovement(movement: WarehouseMovementEntity) {
        viewModelScope.launch {
            try {
                val stockDelta = if (movement.type == "INWARD") movement.quantity else -movement.quantity
                if (movement.id == 0) {
                    warehouseDao.insertMovement(movement)
                } else {
                    val old = warehouseDao.getMovementById(movement.id)
                    if (old != null) {
                        // Reverse old movement before applying new
                        val reversal = if (old.type == "INWARD") -old.quantity else old.quantity
                        productDao.updateStock(old.productId, reversal)
                    }
                    warehouseDao.updateMovement(movement)
                }
                productDao.updateStock(movement.productId, stockDelta)
                stockManager.checkAndCreateLowStockNotifications()
                _state.update { it.copy(successMessage = "Movimento salvato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteMovement(movement: MovementWithProduct) {
        viewModelScope.launch {
            try {
                val reversal = if (movement.type == "INWARD") -movement.quantity else movement.quantity
                warehouseDao.deleteMovement(movement.id)
                productDao.updateStock(movement.productId, reversal)
                _state.update { it.copy(successMessage = "Movimento eliminato, stock aggiornato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveMinimumStock(minStock: MinimumStockEntity) {
        viewModelScope.launch {
            try {
                warehouseDao.upsertMinimumStock(minStock)
                _state.update { it.copy(successMessage = "Stock minimo aggiornato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun markNotifications(ids: List<Int>, status: String) {
        viewModelScope.launch {
            try { warehouseDao.updateNotificationStatuses(ids, status) }
            catch (e: Exception) { _state.update { it.copy(error = e.message) } }
        }
    }

    suspend fun getMinimumStock(productId: Int) = warehouseDao.getMinimumStock(productId)

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }
}
