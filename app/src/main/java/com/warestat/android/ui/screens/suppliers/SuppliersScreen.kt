package com.warestat.android.ui.screens.suppliers

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warestat.android.data.database.dao.PriceListWithDetails
import com.warestat.android.data.database.dao.SupplierOrderItemWithProduct
import com.warestat.android.data.database.dao.SupplierOrderWithSupplier
import com.warestat.android.data.database.entity.*
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.*
import com.warestat.android.util.DateUtils
import com.warestat.android.viewmodel.SuppliersViewModel
import kotlinx.coroutines.launch

@Composable
fun SuppliersScreen(viewModel: SuppliersViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.clearMessages() }
        state.error?.let { scope.launch { snackbarHostState.showSnackbar("${strings.error}$it") }; viewModel.clearMessages() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
            Text(strings.suppliersTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.suppliersTitle) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.ordersTab) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.priceListTab) })
            }
            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> SupplierListTab(state.suppliers, state.searchQuery, viewModel)
                1 -> SupplierOrdersTab(state.supplierOrders, state.suppliers, state.products, viewModel)
                2 -> PriceListTab(state.priceLists, state.suppliers, state.products, viewModel)
            }
        }
    }
}

@Composable
private fun SupplierListTab(
    suppliers: List<SupplierEntity>,
    searchQuery: String,
    viewModel: SuppliersViewModel
) {
    val strings = LocalStrings.current
    var showDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<SupplierEntity?>(null) }

    Column {
        OutlinedTextField(
            value = searchQuery, onValueChange = viewModel::setSearchQuery,
            placeholder = { Text(strings.searchSuppliers) }, leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${suppliers.size} ${strings.suppliersTitle.lowercase()}", style = MaterialTheme.typography.bodyMedium)
            FloatingActionButton(onClick = { editingSupplier = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, strings.newSupplier)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suppliers, key = { it.id }) { supplier ->
                SupplierCard(supplier, onEdit = { editingSupplier = it; showDialog = true }, onDelete = { showDeleteConfirm = it })
            }
        }
    }

    if (showDialog) {
        SupplierDialog(supplier = editingSupplier, onDismiss = { showDialog = false }, onSave = { viewModel.saveSupplier(it); showDialog = false })
    }
    showDeleteConfirm?.let { supplier ->
        AlertDialog(onDismissRequest = { showDeleteConfirm = null },
            title = { Text(strings.deleteSupplierTitle) }, text = { Text("${strings.delete} ${supplier.companyName}?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteSupplier(supplier); showDeleteConfirm = null }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun SupplierCard(supplier: SupplierEntity, onEdit: (SupplierEntity) -> Unit, onDelete: (SupplierEntity) -> Unit) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(supplier) }, elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalShipping, null, tint = Primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(supplier.companyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (supplier.vatNumber.isNotEmpty()) Text("${strings.vatNumber}: ${supplier.vatNumber}", fontSize = 11.sp, color = Color.Gray)
                if (supplier.email.isNotEmpty()) Text(supplier.email, fontSize = 12.sp)
                if (supplier.phone.isNotEmpty()) Text(supplier.phone, fontSize = 12.sp)
            }
            IconButton(onClick = { onEdit(supplier) }) { Icon(Icons.Default.Edit, strings.edit, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { onDelete(supplier) }) { Icon(Icons.Default.Delete, strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun SupplierDialog(supplier: SupplierEntity?, onDismiss: () -> Unit, onSave: (SupplierEntity) -> Unit) {
    val strings = LocalStrings.current
    var companyName by remember { mutableStateOf(supplier?.companyName ?: "") }
    var vatNumber by remember { mutableStateOf(supplier?.vatNumber ?: "") }
    var taxCode by remember { mutableStateOf(supplier?.taxCode ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var certEmail by remember { mutableStateOf(supplier?.certifiedEmail ?: "") }
    var website by remember { mutableStateOf(supplier?.website ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) strings.newSupplier else strings.editSupplier) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = companyName, onValueChange = { companyName = it; nameError = false }, label = { Text(strings.companyNameField) }, isError = nameError, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = vatNumber, onValueChange = { vatNumber = it }, label = { Text(strings.vatNumber) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = taxCode, onValueChange = { taxCode = it }, label = { Text(strings.taxCode) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                item { OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(strings.address) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.phone) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(strings.email) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                item { OutlinedTextField(value = certEmail, onValueChange = { certEmail = it }, label = { Text(strings.pec) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text(strings.website) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(strings.notes) }, modifier = Modifier.fillMaxWidth(), maxLines = 3) }
            }
        },
        confirmButton = {
            Button(onClick = {
                nameError = companyName.isBlank()
                if (!nameError) onSave(SupplierEntity(id = supplier?.id ?: 0, companyName = companyName.trim(), vatNumber = vatNumber.trim(),
                    taxCode = taxCode.trim(), address = address.trim(), phone = phone.trim(), email = email.trim(),
                    certifiedEmail = certEmail.trim(), website = website.trim(), notes = notes.trim()))
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun SupplierOrdersTab(
    orders: List<SupplierOrderWithSupplier>,
    suppliers: List<SupplierEntity>,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    viewModel: SuppliersViewModel
) {
    val strings = LocalStrings.current
    var showDialog by remember { mutableStateOf(false) }
    var editingOrder by remember { mutableStateOf<SupplierOrderWithSupplier?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<SupplierOrderWithSupplier?>(null) }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${orders.size} ${strings.ordersTab.lowercase()}", style = MaterialTheme.typography.bodyMedium)
            FloatingActionButton(onClick = { editingOrder = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, strings.newSupplierOrder)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders, key = { it.id }) { order ->
                SupplierOrderCard(order, onEdit = { editingOrder = it; showDialog = true }, onDelete = { showDeleteConfirm = it })
            }
        }
    }

    if (showDialog) {
        SupplierOrderDialog(order = editingOrder, suppliers = suppliers, products = products,
            loadItems = { id -> viewModel.getSupplierOrderItems(id) },
            onDismiss = { showDialog = false },
            onSave = { order, items -> viewModel.saveSupplierOrder(order, items); showDialog = false })
    }
    showDeleteConfirm?.let { order ->
        AlertDialog(onDismissRequest = { showDeleteConfirm = null },
            title = { Text(strings.deleteSupplierOrderTitle) }, text = { Text("${strings.delete} ${order.number}?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteSupplierOrder(order); showDeleteConfirm = null }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun SupplierOrderCard(order: SupplierOrderWithSupplier, onEdit: (SupplierOrderWithSupplier) -> Unit, onDelete: (SupplierOrderWithSupplier) -> Unit) {
    val strings = LocalStrings.current
    val statusColor = when (order.status) {
        "Sent" -> Secondary; "Received" -> Success; "Cancelled" -> Color.Gray; else -> Warning
    }
    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(order) }, elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = statusColor) { Text(order.status, fontSize = 10.sp) }
                }
                Text(order.supplierName, fontSize = 12.sp, color = Color.Gray)
                Text("${DateUtils.formatDate(order.orderDate)} — € %.2f".format(order.total), fontSize = 12.sp)
            }
            IconButton(onClick = { onEdit(order) }) { Icon(Icons.Default.Edit, strings.edit, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { onDelete(order) }) { Icon(Icons.Default.Delete, strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierOrderDialog(
    order: SupplierOrderWithSupplier?,
    suppliers: List<SupplierEntity>,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    loadItems: suspend (Int) -> List<SupplierOrderItemWithProduct>,
    onDismiss: () -> Unit,
    onSave: (SupplierOrderEntity, List<SupplierOrderItemEntity>) -> Unit
) {
    val strings = LocalStrings.current
    var selectedSupplierId by remember { mutableStateOf(order?.supplierId) }
    var number by remember { mutableStateOf(order?.number ?: "") }
    var status by remember { mutableStateOf(order?.status ?: "Draft") }
    var notes by remember { mutableStateOf(order?.notes ?: "") }
    var orderItems by remember { mutableStateOf<List<SupplierItemData>>(emptyList()) }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var supplierError by remember { mutableStateOf(false) }
    var numberError by remember { mutableStateOf(false) }

    LaunchedEffect(order?.id) {
        if (order != null && order.id > 0) {
            val items = loadItems(order.id)
            orderItems = items.map { SupplierItemData(productId = it.productId, productName = it.productName ?: "", quantity = it.quantity, unitPrice = it.unitPrice.toString()) }
        }
    }

    val total = orderItems.sumOf { it.quantity * (it.unitPrice.toDoubleOrNull() ?: 0.0) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (order == null) strings.newSupplierOrder else "${strings.editSupplierOrder}${order.number}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ExposedDropdownMenuBox(expanded = showSupplierDropdown, onExpandedChange = { showSupplierDropdown = it }) {
                        OutlinedTextField(value = suppliers.find { it.id == selectedSupplierId }?.companyName ?: strings.selectSupplierRequired,
                            onValueChange = {}, readOnly = true, label = { Text(strings.supplier) }, isError = supplierError,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor())
                        ExposedDropdownMenu(expanded = showSupplierDropdown, onDismissRequest = { showSupplierDropdown = false }) {
                            suppliers.forEach { s -> DropdownMenuItem(text = { Text(s.companyName) }, onClick = { selectedSupplierId = s.id; showSupplierDropdown = false; supplierError = false }) }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = number, onValueChange = { number = it; numberError = false }, label = { Text(strings.numberRequired) }, isError = numberError, modifier = Modifier.weight(1f), singleLine = true)
                        ExposedDropdownMenuBox(expanded = showStatusDropdown, onExpandedChange = { showStatusDropdown = it }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(value = status, onValueChange = {}, readOnly = true, label = { Text(strings.status) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) }, modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true)
                            ExposedDropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                                listOf("Draft", "Sent", "Received", "Cancelled").forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { status = s; showStatusDropdown = false }) }
                            }
                        }
                    }
                }
                item { Text(strings.addItem, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                items(orderItems.size) { idx ->
                    SupplierItemRow(item = orderItems[idx], products = products,
                        onUpdate = { updated -> orderItems = orderItems.toMutableList().also { it[idx] = updated } },
                        onRemove = { orderItems = orderItems.toMutableList().also { it.removeAt(idx) } })
                }
                item {
                    OutlinedButton(onClick = { orderItems = orderItems + SupplierItemData() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(strings.addItem)
                    }
                }
                item { Text("${strings.total}: € %.2f".format(total), fontWeight = FontWeight.Bold, color = Success) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(strings.notes) }, modifier = Modifier.fillMaxWidth(), maxLines = 3) }
            }
        },
        confirmButton = {
            Button(onClick = {
                supplierError = selectedSupplierId == null; numberError = number.isBlank()
                if (!supplierError && !numberError) {
                    val entity = SupplierOrderEntity(id = order?.id ?: 0, supplierId = selectedSupplierId!!, number = number.trim(),
                        orderDate = order?.orderDate ?: System.currentTimeMillis(), status = status, total = total, notes = notes.trim())
                    val items = orderItems.filter { it.productId > 0 }.map {
                        val lineTotal = it.quantity * (it.unitPrice.toDoubleOrNull() ?: 0.0)
                        SupplierOrderItemEntity(orderId = entity.id, productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice.toDoubleOrNull() ?: 0.0, total = lineTotal)
                    }
                    onSave(entity, items)
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

data class SupplierItemData(val productId: Int = 0, val productName: String = "", val quantity: Int = 1, val unitPrice: String = "0.0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierItemRow(
    item: SupplierItemData,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    onUpdate: (SupplierItemData) -> Unit,
    onRemove: () -> Unit
) {
    val strings = LocalStrings.current
    var showDropdown by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ExposedDropdownMenuBox(expanded = showDropdown, onExpandedChange = { showDropdown = it }) {
                OutlinedTextField(value = if (item.productId > 0) item.productName else strings.selectProduct, onValueChange = {}, readOnly = true, label = { Text(strings.product) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) }, modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true)
                ExposedDropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                    products.forEach { p -> DropdownMenuItem(text = { Text("${p.code} - ${p.name}") }, onClick = { onUpdate(item.copy(productId = p.id, productName = p.name)); showDropdown = false }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = item.quantity.toString(), onValueChange = { onUpdate(item.copy(quantity = it.toIntOrNull() ?: 1)) },
                    label = { Text(strings.qty) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = item.unitPrice, onValueChange = { onUpdate(item.copy(unitPrice = it)) },
                    label = { Text(strings.price) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                Text("€ %.2f".format(item.quantity * (item.unitPrice.toDoubleOrNull() ?: 0.0)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, strings.remove, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun PriceListTab(
    priceLists: List<PriceListWithDetails>,
    suppliers: List<SupplierEntity>,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    viewModel: SuppliersViewModel
) {
    val strings = LocalStrings.current
    var showDialog by remember { mutableStateOf(false) }
    var editingPriceList by remember { mutableStateOf<PriceListWithDetails?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<SupplierPriceListEntity?>(null) }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${priceLists.size} ${strings.priceListTab.lowercase()}", style = MaterialTheme.typography.bodyMedium)
            FloatingActionButton(onClick = { editingPriceList = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, strings.newPriceListEntry)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(priceLists, key = { it.id }) { pl ->
                PriceListCard(pl, onEdit = { editingPriceList = it; showDialog = true }, onDelete = { showDeleteConfirm = SupplierPriceListEntity(id = pl.id, supplierId = pl.supplierId, productId = pl.productId, supplierProductCode = pl.supplierProductCode, price = pl.price, minimumQuantity = pl.minimumQuantity) })
            }
        }
    }

    if (showDialog) {
        PriceListDialog(priceList = editingPriceList, suppliers = suppliers, products = products,
            onDismiss = { showDialog = false }, onSave = { viewModel.savePriceList(it); showDialog = false })
    }
    showDeleteConfirm?.let { pl ->
        AlertDialog(onDismissRequest = { showDeleteConfirm = null },
            title = { Text(strings.deletePriceListTitle) }, text = { Text("${strings.delete}?") },
            confirmButton = { TextButton(onClick = { viewModel.deletePriceList(pl); showDeleteConfirm = null }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun PriceListCard(pl: PriceListWithDetails, onEdit: (PriceListWithDetails) -> Unit, onDelete: (PriceListWithDetails) -> Unit) {
    val strings = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit(pl) }, elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pl.productName ?: strings.product, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${strings.supplier}: ${pl.supplierName}", fontSize = 11.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("€ %.2f".format(pl.price), fontWeight = FontWeight.SemiBold, color = Success, fontSize = 13.sp)
                    Text("${strings.minQtyLabel}: ${pl.minimumQuantity}", fontSize = 11.sp)
                    if (pl.supplierProductCode.isNotEmpty()) Text("${strings.supplierProductCode}: ${pl.supplierProductCode}", fontSize = 11.sp, color = Color.Gray)
                }
            }
            IconButton(onClick = { onEdit(pl) }) { Icon(Icons.Default.Edit, strings.edit, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { onDelete(pl) }) { Icon(Icons.Default.Delete, strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceListDialog(
    priceList: PriceListWithDetails?,
    suppliers: List<SupplierEntity>,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    onDismiss: () -> Unit,
    onSave: (SupplierPriceListEntity) -> Unit
) {
    val strings = LocalStrings.current
    var selectedSupplierId by remember { mutableStateOf(priceList?.supplierId) }
    var selectedProductId by remember { mutableStateOf(priceList?.productId) }
    var supplierCode by remember { mutableStateOf(priceList?.supplierProductCode ?: "") }
    var price by remember { mutableStateOf(priceList?.price?.toString() ?: "") }
    var minQty by remember { mutableStateOf(priceList?.minimumQuantity?.toString() ?: "1") }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    var showProductDropdown by remember { mutableStateOf(false) }
    var supplierError by remember { mutableStateOf(false) }
    var productError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (priceList == null) strings.newPriceListEntry else strings.editPriceListEntry) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = showSupplierDropdown, onExpandedChange = { showSupplierDropdown = it }) {
                    OutlinedTextField(value = suppliers.find { it.id == selectedSupplierId }?.companyName ?: strings.selectSupplierRequired,
                        onValueChange = {}, readOnly = true, label = { Text(strings.supplier) }, isError = supplierError,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierDropdown) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = showSupplierDropdown, onDismissRequest = { showSupplierDropdown = false }) {
                        suppliers.forEach { s -> DropdownMenuItem(text = { Text(s.companyName) }, onClick = { selectedSupplierId = s.id; showSupplierDropdown = false; supplierError = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = showProductDropdown, onExpandedChange = { showProductDropdown = it }) {
                    OutlinedTextField(value = products.find { it.id == selectedProductId }?.name ?: strings.selectProduct,
                        onValueChange = {}, readOnly = true, label = { Text(strings.product) }, isError = productError,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProductDropdown) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = showProductDropdown, onDismissRequest = { showProductDropdown = false }) {
                        products.forEach { p -> DropdownMenuItem(text = { Text("${p.code} - ${p.name}") }, onClick = { selectedProductId = p.id; showProductDropdown = false; productError = false }) }
                    }
                }
                OutlinedTextField(value = supplierCode, onValueChange = { supplierCode = it }, label = { Text(strings.supplierProductCode) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it; priceError = false }, label = { Text(strings.price) }, isError = priceError, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = minQty, onValueChange = { minQty = it }, label = { Text(strings.minQtyLabel) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                supplierError = selectedSupplierId == null; productError = selectedProductId == null; priceError = price.toDoubleOrNull() == null
                if (!supplierError && !productError && !priceError) {
                    onSave(SupplierPriceListEntity(id = priceList?.id ?: 0, supplierId = selectedSupplierId!!, productId = selectedProductId!!,
                        supplierProductCode = supplierCode.trim(), price = price.toDoubleOrNull() ?: 0.0, minimumQuantity = minQty.toIntOrNull() ?: 1))
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
