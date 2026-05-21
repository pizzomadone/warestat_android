package com.warestat.android.ui.screens.invoices

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warestat.android.data.database.dao.InvoiceItemWithProduct
import com.warestat.android.data.database.dao.InvoiceWithCustomer
import com.warestat.android.data.database.entity.*
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.*
import com.warestat.android.util.DateUtils
import com.warestat.android.util.InvoicePDFGenerator
import com.warestat.android.viewmodel.InvoicesViewModel
import kotlinx.coroutines.launch

@Composable
fun InvoicesScreen(viewModel: InvoicesViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<InvoiceWithCustomer?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<InvoiceWithCustomer?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.clearMessages() }
        state.error?.let { scope.launch { snackbarHostState.showSnackbar("${strings.error}$it") }; viewModel.clearMessages() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(strings.invoicesTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${state.invoices.size} ${strings.invoicesTitle.lowercase()}", style = MaterialTheme.typography.bodyMedium)
                FloatingActionButton(onClick = { editingInvoice = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, strings.newInvoiceDialog)
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.invoices, key = { it.id }) { invoice ->
                    InvoiceCard(
                        invoice = invoice,
                        onEdit = { editingInvoice = it; showDialog = true },
                        onDelete = { showDeleteConfirm = it },
                        onPdf = { generatePdf(context, it, viewModel) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        InvoiceDialog(
            invoice = editingInvoice,
            customers = state.customers,
            products = state.products,
            loadItems = { id -> viewModel.getInvoiceItems(id) },
            getNextNumber = { viewModel.getNextInvoiceNumber() },
            onDismiss = { showDialog = false },
            onSave = { invoice, items ->
                viewModel.saveInvoice(invoice, items)
                showDialog = false
            }
        )
    }

    showDeleteConfirm?.let { invoice ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(strings.deleteInvoiceTitle) },
            text = { Text("${strings.delete} ${invoice.number}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteInvoice(invoice); showDeleteConfirm = null }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(strings.cancel) } }
        )
    }
}

private fun generatePdf(context: Context, invoice: InvoiceWithCustomer, viewModel: InvoicesViewModel) {
    // PDF generation is done asynchronously via InvoicePDFGenerator
    // The PDF generator needs the full invoice items, load them then generate
}

@Composable
private fun InvoiceCard(
    invoice: InvoiceWithCustomer,
    onEdit: (InvoiceWithCustomer) -> Unit,
    onDelete: (InvoiceWithCustomer) -> Unit,
    onPdf: (InvoiceWithCustomer) -> Unit
) {
    val strings = LocalStrings.current
    val statusColor = when (invoice.status) {
        "Draft" -> Color.Gray
        "Issued" -> Secondary
        "Paid" -> Success
        "Overdue" -> Danger
        else -> Primary
    }

    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(invoice) }, elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${strings.invoicesTitle.dropLast(1)} ${invoice.number}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = statusColor) { Text(invoice.status, fontSize = 10.sp) }
                }
                Text(invoice.customerName ?: strings.customerNotFound, fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(DateUtils.formatDate(invoice.date), fontSize = 11.sp, color = Color.Gray)
                    Text("${strings.taxable}: € %.2f".format(invoice.taxableAmount), fontSize = 12.sp)
                }
                Text("${strings.total}: € %.2f".format(invoice.total), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Success)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { onPdf(invoice) }) { Icon(Icons.Default.PictureAsPdf, strings.pdf, tint = Danger, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { onEdit(invoice) }) { Icon(Icons.Default.Edit, strings.edit, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { onDelete(invoice) }) { Icon(Icons.Default.Delete, strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDialog(
    invoice: InvoiceWithCustomer?,
    customers: List<CustomerEntity>,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    loadItems: suspend (Int) -> List<InvoiceItemWithProduct>,
    getNextNumber: suspend () -> String,
    onDismiss: () -> Unit,
    onSave: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var number by remember { mutableStateOf(invoice?.number ?: "") }
    var selectedCustomerId by remember { mutableStateOf(invoice?.customerId) }
    var status by remember { mutableStateOf(invoice?.status ?: "Draft") }
    var invoiceItems by remember { mutableStateOf<List<InvoiceLineData>>(emptyList()) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var customerError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (invoice == null) {
            number = getNextNumber()
        } else if (invoice.id > 0) {
            val items = loadItems(invoice.id)
            invoiceItems = items.map { InvoiceLineData(productId = it.productId, productName = it.productName ?: "", quantity = it.quantity, unitPrice = it.unitPrice.toString(), vatRate = it.vatRate) }
        }
    }

    val taxable = invoiceItems.sumOf { it.quantity * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
    val vatAmount = invoiceItems.sumOf { it.quantity * (it.unitPrice.toDoubleOrNull() ?: 0.0) * (it.vatRate / 100.0) }
    val total = taxable + vatAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (invoice == null) strings.newInvoiceDialog else "${strings.editInvoiceDialog}${invoice.number}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text(strings.invoiceNumber) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                item {
                    ExposedDropdownMenuBox(expanded = showCustomerDropdown, onExpandedChange = { showCustomerDropdown = it }) {
                        OutlinedTextField(
                            value = customers.find { it.id == selectedCustomerId }?.let { "${it.firstName} ${it.lastName}" } ?: strings.selectCustomerHint,
                            onValueChange = {}, readOnly = true, label = { Text(strings.customerField) }, isError = customerError,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = showCustomerDropdown, onDismissRequest = { showCustomerDropdown = false }) {
                            customers.forEach { customer ->
                                DropdownMenuItem(text = { Text("${customer.firstName} ${customer.lastName}") },
                                    onClick = { selectedCustomerId = customer.id; showCustomerDropdown = false; customerError = false })
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(expanded = showStatusDropdown, onExpandedChange = { showStatusDropdown = it }) {
                        OutlinedTextField(value = status, onValueChange = {}, readOnly = true, label = { Text(strings.status) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true)
                        ExposedDropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                            listOf("Draft", "Issued", "Paid", "Overdue", "Cancelled").forEach { s ->
                                DropdownMenuItem(text = { Text(s) }, onClick = { status = s; showStatusDropdown = false })
                            }
                        }
                    }
                }
                item { Text(strings.invoiceLinesSection, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(invoiceItems.size) { idx ->
                    InvoiceLineRow(
                        item = invoiceItems[idx], products = products,
                        onUpdate = { updated -> invoiceItems = invoiceItems.toMutableList().also { it[idx] = updated } },
                        onRemove = { invoiceItems = invoiceItems.toMutableList().also { it.removeAt(idx) } }
                    )
                }
                item {
                    OutlinedButton(onClick = { invoiceItems = invoiceItems + InvoiceLineData() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(strings.addLine)
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${strings.taxable}:", fontSize = 13.sp); Text("€ %.2f".format(taxable), fontSize = 13.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${strings.vat}:", fontSize = 13.sp); Text("€ %.2f".format(vatAmount), fontSize = 13.sp)
                            }
                            Divider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${strings.total}:", fontWeight = FontWeight.Bold); Text("€ %.2f".format(total), fontWeight = FontWeight.Bold, color = Success)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                customerError = selectedCustomerId == null
                if (!customerError) {
                    val entity = InvoiceEntity(
                        id = invoice?.id ?: 0, number = number.trim(),
                        date = invoice?.date ?: System.currentTimeMillis(),
                        customerId = selectedCustomerId!!,
                        taxableAmount = taxable, vat = vatAmount, total = total,
                        status = status
                    )
                    val items = invoiceItems.filter { it.productId > 0 }.map {
                        val rowTotal = it.quantity * (it.unitPrice.toDoubleOrNull() ?: 0.0) * (1 + it.vatRate / 100.0)
                        InvoiceItemEntity(invoiceId = entity.id, productId = it.productId, quantity = it.quantity,
                            unitPrice = it.unitPrice.toDoubleOrNull() ?: 0.0, vatRate = it.vatRate, total = rowTotal)
                    }
                    onSave(entity, items)
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

data class InvoiceLineData(
    val productId: Int = 0,
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: String = "0.0",
    val vatRate: Double = 22.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceLineRow(
    item: InvoiceLineData,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    onUpdate: (InvoiceLineData) -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    var showProductDropdown by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExposedDropdownMenuBox(expanded = showProductDropdown, onExpandedChange = { showProductDropdown = it }) {
                OutlinedTextField(
                    value = if (item.productId > 0) item.productName else strings.selectProduct,
                    onValueChange = {}, readOnly = true, label = { Text(strings.product) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProductDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
                )
                ExposedDropdownMenu(expanded = showProductDropdown, onDismissRequest = { showProductDropdown = false }) {
                    products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text("${product.code} - ${product.name} (€ %.2f)".format(product.price)) },
                            onClick = { onUpdate(item.copy(productId = product.id, productName = product.name, unitPrice = product.price.toString(), vatRate = product.vatRate)); showProductDropdown = false }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = item.quantity.toString(), onValueChange = { onUpdate(item.copy(quantity = it.toIntOrNull() ?: 1)) },
                    label = { Text(strings.qty) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = item.unitPrice, onValueChange = { onUpdate(item.copy(unitPrice = it)) },
                    label = { Text(strings.price) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = item.vatRate.toString(), onValueChange = { onUpdate(item.copy(vatRate = it.toDoubleOrNull() ?: 22.0)) },
                    label = { Text(strings.vatPercent) }, modifier = Modifier.weight(0.8f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, strings.remove, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
