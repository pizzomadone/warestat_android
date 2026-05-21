package com.warestat.android.ui.screens.warehouse

import androidx.compose.foundation.background
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
import com.warestat.android.data.database.dao.MovementWithProduct
import com.warestat.android.data.database.dao.NotificationWithProduct
import com.warestat.android.data.database.dao.StockStatus
import com.warestat.android.data.database.entity.*
import com.warestat.android.ui.theme.*
import com.warestat.android.util.DateUtils
import com.warestat.android.viewmodel.WarehouseViewModel
import kotlinx.coroutines.launch

@Composable
fun WarehouseScreen(viewModel: WarehouseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) }; viewModel.clearMessages() }
        state.error?.let { scope.launch { snackbarHostState.showSnackbar("Errore: $it") }; viewModel.clearMessages() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
            Text("Magazzino", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stock") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Movimenti") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Notifiche") })
            }
            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> StockStatusTab(state.stockStatus, state.products, state.suppliers, viewModel)
                1 -> MovementsTab(state.movements, state.searchQuery, state.products, viewModel)
                2 -> NotificationsTab(state.notifications, viewModel)
            }
        }
    }
}

@Composable
private fun StockStatusTab(
    stockStatus: List<StockStatus>,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    suppliers: List<SupplierEntity>,
    viewModel: WarehouseViewModel
) {
    var showMinStockDialog by remember { mutableStateOf<StockStatus?>(null) }

    Column {
        Text("${stockStatus.size} prodotti in magazzino", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        // Header
        Card(colors = CardDefaults.cardColors(containerColor = Primary)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text("Prodotto", color = Color.White, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Stock", color = Color.White, modifier = Modifier.width(50.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Ris.", color = Color.White, modifier = Modifier.width(40.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Disp.", color = Color.White, modifier = Modifier.width(45.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Min", color = Color.White, modifier = Modifier.width(40.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(36.dp))
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(stockStatus, key = { it.productId }) { stock ->
                StockStatusRow(stock, onClick = { showMinStockDialog = it })
            }
        }
    }

    showMinStockDialog?.let { stock ->
        MinimumStockDialog(
            stock = stock, suppliers = suppliers,
            loadMinStock = { id -> viewModel.getMinimumStock(id) },
            onDismiss = { showMinStockDialog = null },
            onSave = { viewModel.saveMinimumStock(it); showMinStockDialog = null }
        )
    }
}

@Composable
private fun StockStatusRow(stock: StockStatus, onClick: (StockStatus) -> Unit) {
    val minQty = stock.minimumQuantity ?: 0
    val statusColor = when {
        stock.physicalStock == 0 -> Danger
        minQty > 0 && stock.physicalStock < minQty -> Warning
        else -> Success
    }
    val bgColor = if (stock.physicalStock == 0) Color(0xFFFFF0F0) else if (minQty > 0 && stock.physicalStock < minQty) Color(0xFFFFFBF0) else Color.White

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stock.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(stock.code, fontSize = 10.sp, color = Color.Gray)
            }
            Text(stock.physicalStock.toString(), modifier = Modifier.width(50.dp), fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Bold)
            Text(stock.reservedStock.toString(), modifier = Modifier.width(40.dp), fontSize = 12.sp, color = if (stock.reservedStock > 0) Warning else Color.Gray)
            Text(stock.availableStock.toString(), modifier = Modifier.width(45.dp), fontSize = 12.sp, color = if (stock.availableStock < 0) Danger else Success, fontWeight = FontWeight.SemiBold)
            Text(minQty.toString(), modifier = Modifier.width(40.dp), fontSize = 12.sp, color = Color.Gray)
            IconButton(onClick = { onClick(stock) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Imposta min", modifier = Modifier.size(16.dp), tint = Primary)
            }
        }
    }
}

@Composable
private fun MinimumStockDialog(
    stock: StockStatus,
    suppliers: List<SupplierEntity>,
    loadMinStock: suspend (Int) -> MinimumStockEntity?,
    onDismiss: () -> Unit,
    onSave: (MinimumStockEntity) -> Unit
) {
    var minQty by remember { mutableStateOf("0") }
    var reorderQty by remember { mutableStateOf("0") }
    var leadTime by remember { mutableStateOf("0") }
    var preferredSupplierId by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf("") }
    var showSupplierDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(stock.productId) {
        loadMinStock(stock.productId)?.let { ms ->
            minQty = ms.minimumQuantity.toString(); reorderQty = ms.reorderQuantity.toString()
            leadTime = ms.leadTimeDays.toString(); preferredSupplierId = ms.preferredSupplierId; notes = ms.notes
        }
    }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Stock minimo: ${stock.productName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = minQty, onValueChange = { minQty = it }, label = { Text("Qtà minima") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = reorderQty, onValueChange = { reorderQty = it }, label = { Text("Qtà riordino") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                OutlinedTextField(value = leadTime, onValueChange = { leadTime = it }, label = { Text("Lead time (giorni)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(expanded = showSupplierDropdown, onExpandedChange = { showSupplierDropdown = it }) {
                    OutlinedTextField(value = suppliers.find { it.id == preferredSupplierId }?.companyName ?: "Nessun fornitore preferito", onValueChange = {}, readOnly = true, label = { Text("Fornitore preferito") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierDropdown) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = showSupplierDropdown, onDismissRequest = { showSupplierDropdown = false }) {
                        DropdownMenuItem(text = { Text("Nessuno") }, onClick = { preferredSupplierId = null; showSupplierDropdown = false })
                        suppliers.forEach { s -> DropdownMenuItem(text = { Text(s.companyName) }, onClick = { preferredSupplierId = s.id; showSupplierDropdown = false }) }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(MinimumStockEntity(productId = stock.productId, minimumQuantity = minQty.toIntOrNull() ?: 0, reorderQuantity = reorderQty.toIntOrNull() ?: 0, leadTimeDays = leadTime.toIntOrNull() ?: 0, preferredSupplierId = preferredSupplierId, notes = notes.trim()))
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun MovementsTab(
    movements: List<MovementWithProduct>,
    searchQuery: String,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    viewModel: WarehouseViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingMovement by remember { mutableStateOf<MovementWithProduct?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<MovementWithProduct?>(null) }

    Column {
        OutlinedTextField(value = searchQuery, onValueChange = viewModel::setSearchQuery, placeholder = { Text("Cerca movimenti...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${movements.size} movimenti", style = MaterialTheme.typography.bodyMedium)
            FloatingActionButton(onClick = { editingMovement = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, "Nuovo movimento")
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(movements, key = { it.id }) { movement ->
                MovementCard(movement, onEdit = { editingMovement = it; showDialog = true }, onDelete = { showDeleteConfirm = it })
            }
        }
    }

    if (showDialog) {
        MovementDialog(movement = editingMovement, products = products, onDismiss = { showDialog = false }, onSave = { viewModel.saveMovement(it); showDialog = false })
    }
    showDeleteConfirm?.let { movement ->
        AlertDialog(onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Elimina movimento") }, text = { Text("Eliminare il movimento per ${movement.productName}? Lo stock verrà ripristinato.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteMovement(movement); showDeleteConfirm = null }) { Text("Elimina", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun MovementCard(movement: MovementWithProduct, onEdit: (MovementWithProduct) -> Unit, onDelete: (MovementWithProduct) -> Unit) {
    val isInward = movement.type == "INWARD"
    val typeColor = if (isInward) Success else Warning
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isInward) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = typeColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(movement.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isInward) "+${movement.quantity}" else "-${movement.quantity}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = typeColor)
                    Text(movement.reason.ifEmpty { movement.type }, fontSize = 12.sp, color = Color.Gray)
                }
                Text(DateUtils.formatDate(movement.date), fontSize = 11.sp, color = Color.Gray)
                if (movement.documentNumber.isNotEmpty()) Text("Doc: ${movement.documentType} ${movement.documentNumber}", fontSize = 11.sp, color = Color.Gray)
            }
            IconButton(onClick = { onEdit(movement) }) { Icon(Icons.Default.Edit, "Modifica", modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { onDelete(movement) }) { Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovementDialog(
    movement: MovementWithProduct?,
    products: List<com.warestat.android.data.database.dao.ProductWithSupplier>,
    onDismiss: () -> Unit,
    onSave: (WarehouseMovementEntity) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(movement?.productId) }
    var type by remember { mutableStateOf(movement?.type ?: "INWARD") }
    var quantity by remember { mutableStateOf(movement?.quantity?.toString() ?: "1") }
    var reason by remember { mutableStateOf(movement?.reason ?: "") }
    var docNumber by remember { mutableStateOf(movement?.documentNumber ?: "") }
    var docType by remember { mutableStateOf(movement?.documentType ?: "") }
    var notes by remember { mutableStateOf(movement?.notes ?: "") }
    var showProductDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var productError by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (movement == null) "Nuovo movimento" else "Modifica movimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = showProductDropdown, onExpandedChange = { showProductDropdown = it }) {
                    OutlinedTextField(value = products.find { it.id == selectedProductId }?.let { "${it.code} - ${it.name}" } ?: "Seleziona prodotto *",
                        onValueChange = {}, readOnly = true, label = { Text("Prodotto *") }, isError = productError,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProductDropdown) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = showProductDropdown, onDismissRequest = { showProductDropdown = false }) {
                        products.forEach { p -> DropdownMenuItem(text = { Text("${p.code} - ${p.name}") }, onClick = { selectedProductId = p.id; showProductDropdown = false; productError = false }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = showTypeDropdown, onExpandedChange = { showTypeDropdown = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = if (type == "INWARD") "Entrata" else "Uscita", onValueChange = {}, readOnly = true, label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) }, modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true)
                        ExposedDropdownMenu(expanded = showTypeDropdown, onDismissRequest = { showTypeDropdown = false }) {
                            DropdownMenuItem(text = { Text("Entrata") }, onClick = { type = "INWARD"; showTypeDropdown = false })
                            DropdownMenuItem(text = { Text("Uscita") }, onClick = { type = "OUTWARD"; showTypeDropdown = false })
                        }
                    }
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantità") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Causale") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = docType, onValueChange = { docType = it }, label = { Text("Tipo doc.") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = docNumber, onValueChange = { docNumber = it }, label = { Text("Num. doc.") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                productError = selectedProductId == null
                if (!productError) {
                    onSave(WarehouseMovementEntity(id = movement?.id ?: 0, productId = selectedProductId!!, date = movement?.date ?: System.currentTimeMillis(),
                        type = type, quantity = quantity.toIntOrNull() ?: 1, reason = reason.trim(), documentNumber = docNumber.trim(), documentType = docType.trim(), notes = notes.trim()))
                }
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}

@Composable
private fun NotificationsTab(notifications: List<NotificationWithProduct>, viewModel: WarehouseViewModel) {
    val selectedIds = remember { mutableStateListOf<Int>() }

    Column {
        if (notifications.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.markNotifications(notifications.map { it.id }, "READ"); selectedIds.clear() }, modifier = Modifier.weight(1f)) {
                    Text("Segna lette", fontSize = 12.sp)
                }
                Button(onClick = { viewModel.markNotifications(notifications.map { it.id }, "HANDLED"); selectedIds.clear() }, modifier = Modifier.weight(1f)) {
                    Text("Segna gestite", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Text("${notifications.size} notifiche attive", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        if (notifications.isEmpty()) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF4))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Success)
                    Spacer(Modifier.width(8.dp))
                    Text("Nessuna notifica attiva", color = Success)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationWithProduct) {
    val bgColor = when (notification.status) {
        "NEW" -> Color(0xFFFFF5F5)
        "READ" -> Color(0xFFFFFBF0)
        else -> Color.White
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(20.dp).padding(top = 2.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = if (notification.status == "NEW") Danger else Warning) { Text(notification.status, fontSize = 9.sp) }
                }
                Text(notification.message, fontSize = 12.sp, color = Color(0xFF8B0000))
                Text(DateUtils.formatDate(notification.date), fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
