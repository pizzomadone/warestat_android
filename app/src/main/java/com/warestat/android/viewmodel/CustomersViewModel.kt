package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.CustomerDao
import com.warestat.android.data.database.entity.CustomerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomersState(
    val customers: List<CustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerDao: CustomerDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _state = MutableStateFlow(CustomersState())
    val state: StateFlow<CustomersState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) customerDao.getAllCustomers()
                    else customerDao.searchCustomers("%$query%")
                }
                .collect { customers ->
                    _state.update { it.copy(customers = customers, isLoading = false) }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                if (customer.id == 0) customerDao.insertCustomer(customer)
                else customerDao.updateCustomer(customer)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try { customerDao.deleteCustomer(customer) }
            catch (e: Exception) { _state.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
