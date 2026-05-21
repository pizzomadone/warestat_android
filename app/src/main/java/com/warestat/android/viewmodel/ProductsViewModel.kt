package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.ProductDao
import com.warestat.android.data.database.dao.ProductWithSupplier
import com.warestat.android.data.database.dao.SupplierDao
import com.warestat.android.data.database.entity.ProductEntity
import com.warestat.android.data.database.entity.SupplierEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductsState(
    val products: List<ProductWithSupplier> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val searchQuery: String = "",
    val showInactiveProducts: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showInactive = MutableStateFlow(false)
    private val _state = MutableStateFlow(ProductsState())
    val state: StateFlow<ProductsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            supplierDao.getAllSuppliers().collect { suppliers ->
                _state.update { it.copy(suppliers = suppliers) }
            }
        }
        viewModelScope.launch {
            combine(_searchQuery.debounce(300), _showInactive) { query, showInactive -> query to showInactive }
                .flatMapLatest { (query, showInactive) ->
                    when {
                        query.isNotBlank() -> productDao.searchProducts("%$query%")
                        showInactive -> productDao.getAllProducts()
                        else -> productDao.getAllActiveProducts()
                    }
                }
                .collect { products ->
                    _state.update { it.copy(products = products) }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun setShowInactive(show: Boolean) {
        _showInactive.value = show
        _state.update { it.copy(showInactiveProducts = show) }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                if (product.id == 0) productDao.insertProduct(product)
                else productDao.updateProduct(product)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try { productDao.deleteProduct(product) }
            catch (e: Exception) { _state.update { it.copy(error = e.message) } }
        }
    }

    suspend fun getProductByCode(code: String): ProductEntity? = productDao.getProductByCode(code)

    fun clearError() = _state.update { it.copy(error = null) }
}
