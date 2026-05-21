package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.*
import com.warestat.android.data.database.entity.*
import com.warestat.android.util.DateUtils
import com.warestat.android.util.StockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoicesState(
    val invoices: List<InvoiceWithCustomer> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductWithSupplier> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class InvoicesViewModel @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val customerDao: CustomerDao,
    private val productDao: ProductDao,
    private val stockManager: StockManager
) : ViewModel() {

    private val _state = MutableStateFlow(InvoicesState())
    val state: StateFlow<InvoicesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            invoiceDao.getAllInvoices().collect { invoices ->
                _state.update { it.copy(invoices = invoices) }
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

    suspend fun getInvoiceItems(invoiceId: Int): List<InvoiceItemWithProduct> =
        invoiceDao.getInvoiceItems(invoiceId)

    suspend fun getNextInvoiceNumber(): String {
        val year = DateUtils.currentYear()
        val numbering = invoiceDao.getInvoiceNumbering(year)
        val next = (numbering?.lastNumber ?: 0) + 1
        invoiceDao.upsertInvoiceNumbering(InvoiceNumberingEntity(year, next))
        return String.format("%d/%04d", year, next)
    }

    fun saveInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        viewModelScope.launch {
            try {
                val isNew = invoice.id == 0
                val invoiceId: Int

                if (isNew) {
                    invoiceId = invoiceDao.insertInvoice(invoice).toInt()
                } else {
                    invoiceId = invoice.id
                    invoiceDao.deleteInvoiceItems(invoiceId)
                    invoiceDao.updateInvoice(invoice)
                }

                items.forEach { item ->
                    invoiceDao.insertInvoiceItem(item.copy(invoiceId = invoiceId))
                }

                // Decrement stock for Issued/Paid invoices
                if (invoice.status == "Issued" || invoice.status == "Paid") {
                    stockManager.completeReservationAndDecrementStock(
                        documentType = "INVOICE",
                        documentId = invoiceId,
                        documentDate = invoice.date,
                        documentNumber = invoice.number
                    )
                }

                _state.update { it.copy(successMessage = if (isNew) "Fattura creata" else "Fattura aggiornata") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteInvoice(invoice: InvoiceWithCustomer) {
        viewModelScope.launch {
            try {
                val entity = invoiceDao.getInvoiceById(invoice.id) ?: return@launch
                invoiceDao.deleteInvoiceItems(invoice.id)
                invoiceDao.deleteInvoice(entity)
                _state.update { it.copy(successMessage = "Fattura eliminata") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }
}
