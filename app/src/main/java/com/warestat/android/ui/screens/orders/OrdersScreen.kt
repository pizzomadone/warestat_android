package com.warestat.android.ui.screens.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warestat.android.data.database.dao.OrderItemWithProduct
import com.warestat.android.data.database.dao.OrderWithCustomer
import com.warestat.android.data.database.dao.ProductWithSupplier
import com.warestat.android.data.database.entity.*
import com.warestat.android.ui.theme.*
import com.warestat.android.util.DateUtils
import com.warestat.android.viewmodel.OrdersViewModel
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(viewModel: OrdersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingOrder by remember { mutableStateOf<OrderWithCustomer?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<OrderWithCustomer?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.clearMessages() }
        state.error?.let { scope.launch { snackbarHostState.showSnackbar("Errore: $it") }; viewModel.clearMessages() }
    }

    val statusFilters = listOf("ALL", "New", "In Progress", "Completed", "Cancelled")
    val statusLabels = mapOf("ALL" to "Tutti", "New" to "Nuovi", "In Progress" to "In Corso", "Completed" to "Completati", "Cancelled" to "Annullati")

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Ordini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            ScrollableTabRow(
                selectedTabIndex = statusFilters.indexOf(state.statusFilter).coerceAtLeast(0),
                edgePadding = 0.dp
            ) {
                statusFilters.forEachIndexed { index, status ->
                    Tab(
                        selected = state.statusFilter == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        text = { Text(statusLabels[status] ?: status, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${state.orders.size} ordini", style = MaterialTheme.typography.bodyMedium)
                FloatingActionButton(onClick = { editingOrder = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, "Nuovo ordine")
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onEdit = { editingOrder = it; showDialog = true },
                        onDelete = { showDeleteConfirm = it }
                    )
                }
            }
        }
    }

    if (showDialog) {
        OrderDialog(
            order = editingOrder,
            customers = state.customers,
            products = state.products,
            loadItems = { orderId -> viewModel.getOrderItems(orderId) },
            onDismiss = { showDialog = false },
            onSave = { order, items ->
                viewModel.saveOrder(order, items)
                showDialog = false
            }
        )
    }

    showDeleteConfirm?.let { order ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Elimina ordine") },
            text = { Text("Eliminare ordine #${order.id}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteOrder(order); showDeleteConfirm = null }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun OrderCard(order: OrderWithCustomer, onEdit: (OrderWithCustomer) -> Unit, onDelete: (OrderWithCustomer) -> Unit) {
    val statusColor = when (order.status) {
        "New" -> Secondary
        "In Progress" -> Warning
        "Completed" -> Success
        "Cancelled" -> Color.Gray
        else -> Primary
    }

    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(order) }, elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ordine #${order.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = statusColor) { Text(order.status, fontSize = 10.sp) }
                }
                Text(order.customerName ?: "Cliente non trovato", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(DateUtils.formatDate(order.orderDate), fontSize = 11.sp, color = Color.Gray)
                    Text("€ %.2f".format(order.total), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Success)
                }
                if (order.paymentStatus != "Unpaid") {
                    Text("Pagamento: ${order.paymentStatus}", fontSize = 11.sp, color = if (order.paymentStatus == "Paid") Success else Warning)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { onEdit(order) }) { Icon(Icons.Default.Edit, "Modifica", modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { onDelete(order) }) { Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDialog(
    order: OrderWithCustomer?,
    customers: List<CustomerEntity>,
    products: List<ProductWithSupplier>,
    loadItems: suspend (Int) -> List<OrderItemWithProduct>,
    onDismiss: () -> Unit,
    onSave: (OrderEntity, List<OrderItemEntity>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedCustomerId by remember { mutableStateOf(order?.customerId) }
    var status by remember { mutableStateOf(order?.status ?: "New") }
    var paymentStatus by remember { mutableStateOf(order?.paymentStatus ?: "Unpaid") }
    var paidAmount by remember { mutableStateOf(order?.paidAmount?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(order?.notes ?: "") }
    var orderItems by remember { mutableStateOf<List<OrderItemData>>(emptyList()) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showPaymentDropdown by remember { mutableStateOf(false) }
    var customerError by remember { mutableStateOf(false) }

    LaunchedEffect(order?.id) {
        if (order != null && order.id > 0) {
            val items = loadItems(order.id)
            orderItems = items.map { OrderItemData(productId = it.productId, productName = it.productName ?: "", quantity = it.quantity, unitPrice = it.unitPrice.toString()) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (order == null) "Nuovo ordine" else "Modifica ordine #${order.id}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ExposedDropdownMenuBox(expanded = showCustomerDropdown, onExpandedChange = { showCustomerDropdown = it }) {
                        OutlinedTextField(
                            value = customers.find { it.id == selectedCustomerId }?.let { "${it.firstName} ${it.lastName}" } ?: "Seleziona cliente *",
                            onValueChange = {}, readOnly = true, label = { Text("Cliente *") },
                            isError = customerError,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = showCustomerDropdown, onDismissRequest = { showCustomerDropdown = false }) {
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text("${customer.firstName} ${customer.lastName}") },
                                    onClick = { selectedCustomerId = customer.id; showCustomerDropdown = false; customerError = false }
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(expanded = showStatusDropdown, onExpandedChange = { showStatusDropdown = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = status, onValueChange = {}, readOnly = true, label = { Text("Stato") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true)
                            ExposedDropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                                listOf("New", "In Progress", "Completed", "Cancelled").forEach { s ->
                                    DropdownMenuItem(text = { Text(s) }, onClick = { status = s; showStatusDropdown = false })
                                }
                            }
                        }
                        ExposedDropdownMenuBox(expanded = showPaymentDropdown, onExpandedChange = { showPaymentDropdown = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = paymentStatus, onValueChange = {}, readOnly = true, label = { Text("Pagamento") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true)
                            ExposedDropdownMenu(expanded = showPaymentDropdown, onDismissRequest = { showPaymentDropdown = false }) {
                                listOf("Unpaid", "Partial", "Paid").forEach { s ->
                                    DropdownMenuItem(text = { Text(s) }, onClick = { paymentStatus = s; showPaymentDropdown = false })
                                }
                            }
                        }
                    }
                }
                if (paymentStatus == "Partial") {
                    item {
                        OutlinedTextField(value = paidAmount, onValueChange = { paidAmount = it }, label = { Text("Importo pagato") },
                            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                }
                item {
                    Text("Articoli ordine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                items(orderItems.size) { idx ->
                    OrderItemRow(
                        item = orderItems[idx],
                        products = products,
                        onUpdate = { updated -> orderItems = orderItems.toMutableList().also { it[idx] = updated } },
                        onRemove = { orderItems = orderItems.toMutableList().also { it.removeAt(idx) } }
                    )
                }
                item {
                    OutlinedButton(onClick = { orderItems = orderItems + OrderItemData() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Aggiungi articolo")
                    }
                }
                val total = orderItems.sumOf { it.quantity * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                item {
                    Text("Totale: € %.2f".format(total), fontWeight = FontWeight.Bold, color = Success)
                }
                item {
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                customerError = selectedCustomerId == null
                if (!customerError) {
                    val total = orderItems.sumOf { it.quantity * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                    val entity = OrderEntity(
                        id = order?.id ?: 0,
                        customerId = selectedCustomerId!!,
                        orderDate = order?.orderDate ?: System.currentTimeMillis(),
                        status = status, total = total,
                        paymentStatus = paymentStatus,
                        paidAmount = paidAmount.toDoubleOrNull() ?: 0.0
                    )
                    val items = orderItems.filter { it.productId > 0 }.map {
                        OrderItemEntity(orderId = entity.id, productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice.toDoubleOrNull() ?: 0.0)
                    }
                    onSave(entity, items)
                }
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

data class OrderItemData(
    val productId: Int = 0,
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: String = "0.0"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderItemRow(
    item: OrderItemData,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    onUpdate: (OrderItemData) -> Unit,
    onRemove: () -> Unit
) {
    var showProductDropdown by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExposedDropdownMenuBox(expanded = showProductDropdown, onExpandedChange = { showProductDropdown = it }) {
                OutlinedTextField(
                    value = if (item.productId > 0) "${item.productName} (${products.find { p -> p.id == item.productId }?.code ?: ""})" else "Seleziona prodotto",
                    onValueChange = {}, readOnly = true, label = { Text("Prodotto") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProductDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
                )
                ExposedDropdownMenu(expanded = showProductDropdown, onDismissRequest = { showProductDropdown = false }) {
                    products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text("${product.code} - ${product.name} (€ %.2f)".format(product.price)) },
                            onClick = { onUpdate(item.copy(productId = product.id, productName = product.name, unitPrice = product.price.toString())); showProductDropdown = false }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = item.quantity.toString(), onValueChange = { onUpdate(item.copy(quantity = it.toIntOrNull() ?: 1)) },
                    label = { Text("Qtà") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = item.unitPrice, onValueChange = { onUpdate(item.copy(unitPrice = it)) },
                    label = { Text("Prezzo") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                val rowTotal = item.quantity * (item.unitPrice.toDoubleOrNull() ?: 0.0)
                Text("€ %.2f".format(rowTotal), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Rimuovi", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
