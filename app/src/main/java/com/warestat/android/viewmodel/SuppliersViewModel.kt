package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.*
import com.warestat.android.data.database.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuppliersState(
    val suppliers: List<SupplierEntity> = emptyList(),
    val supplierOrders: List<SupplierOrderWithSupplier> = emptyList(),
    val priceLists: List<PriceListWithDetails> = emptyList(),
    val products: List<ProductWithSupplier> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    private val supplierDao: SupplierDao,
    private val productDao: ProductDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _state = MutableStateFlow(SuppliersState())
    val state: StateFlow<SuppliersState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery.debounce(300).flatMapLatest { query ->
                if (query.isBlank()) supplierDao.getAllSuppliers()
                else supplierDao.searchSuppliers("%$query%")
            }.collect { suppliers ->
                _state.update { it.copy(suppliers = suppliers) }
            }
        }
        viewModelScope.launch {
            supplierDao.getAllSupplierOrders().collect { orders ->
                _state.update { it.copy(supplierOrders = orders) }
            }
        }
        viewModelScope.launch {
            supplierDao.getAllPriceLists().collect { lists ->
                _state.update { it.copy(priceLists = lists) }
            }
        }
        viewModelScope.launch {
            productDao.getAllActiveProducts().collect { products ->
                _state.update { it.copy(products = products) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun saveSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            try {
                if (supplier.id == 0) supplierDao.insertSupplier(supplier)
                else supplierDao.updateSupplier(supplier)
                _state.update { it.copy(successMessage = "Fornitore salvato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            try { supplierDao.deleteSupplier(supplier); _state.update { it.copy(successMessage = "Fornitore eliminato") } }
            catch (e: Exception) { _state.update { it.copy(error = e.message) } }
        }
    }

    fun saveSupplierOrder(order: SupplierOrderEntity, items: List<SupplierOrderItemEntity>) {
        viewModelScope.launch {
            try {
                val orderId = if (order.id == 0) {
                    supplierDao.insertSupplierOrder(order).toInt()
                } else {
                    supplierDao.deleteSupplierOrderItems(order.id)
                    supplierDao.updateSupplierOrder(order)
                    order.id
                }
                items.forEach { supplierDao.insertSupplierOrderItem(it.copy(orderId = orderId)) }
                _state.update { it.copy(successMessage = "Ordine fornitore salvato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSupplierOrder(order: SupplierOrderWithSupplier) {
        viewModelScope.launch {
            try {
                val entity = supplierDao.getSupplierOrderById(order.id) ?: return@launch
                supplierDao.deleteSupplierOrderItems(order.id)
                supplierDao.deleteSupplierOrder(entity)
                _state.update { it.copy(successMessage = "Ordine eliminato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun savePriceList(priceList: SupplierPriceListEntity) {
        viewModelScope.launch {
            try {
                if (priceList.id == 0) supplierDao.insertPriceList(priceList)
                else supplierDao.updatePriceList(priceList)
                _state.update { it.copy(successMessage = "Listino salvato") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deletePriceList(priceList: SupplierPriceListEntity) {
        viewModelScope.launch {
            try { supplierDao.deletePriceList(priceList) }
            catch (e: Exception) { _state.update { it.copy(error = e.message) } }
        }
    }

    suspend fun getSupplierOrderItems(orderId: Int) = supplierDao.getSupplierOrderItems(orderId)

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }
}
