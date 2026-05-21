package com.warestat.android.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.warestat.android.data.database.dao.ProductWithSupplier
import com.warestat.android.data.database.entity.ProductEntity
import com.warestat.android.data.database.entity.SupplierEntity
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.*
import com.warestat.android.viewmodel.ProductsViewModel

@Composable
fun ProductsScreen(viewModel: ProductsViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductWithSupplier?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ProductWithSupplier?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(strings.productsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = { Text(strings.searchProducts) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.setSearchQuery("") }) {
                    Icon(Icons.Default.Clear, null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${state.products.size} ${strings.productsTitle.lowercase()}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.showInactiveProducts, onCheckedChange = viewModel::setShowInactive)
                    Text(strings.inactiveLabel, fontSize = 12.sp)
                }
            }
            FloatingActionButton(onClick = { editingProduct = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, strings.newProduct)
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.products, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onEdit = { editingProduct = it; showDialog = true },
                    onDelete = { showDeleteConfirm = it }
                )
            }
        }
    }

    if (showDialog) {
        ProductDialog(
            product = editingProduct,
            suppliers = state.suppliers,
            onDismiss = { showDialog = false },
            onSave = { entity ->
                viewModel.saveProduct(entity)
                showDialog = false
            }
        )
    }

    showDeleteConfirm?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(strings.deleteProductTitle) },
            text = { Text("${strings.delete} ${product.name} (${product.code})?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProduct(ProductEntity(
                        id = product.id, code = product.code, name = product.name,
                        description = product.description, price = product.price, quantity = product.quantity
                    ))
                    showDeleteConfirm = null
                }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun ProductCard(product: ProductWithSupplier, onEdit: (ProductWithSupplier) -> Unit, onDelete: (ProductWithSupplier) -> Unit) {
    val strings = LocalStrings.current
    val stockColor = when {
        product.quantity == 0 -> Danger
        product.minimumQuantity > 0 && product.quantity < product.minimumQuantity -> Warning
        else -> Success
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(product) },
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(stockColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory, null, tint = stockColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    if (!product.active) {
                        Badge { Text(strings.inactiveLabel) }
                    }
                }
                Text("Cod: ${product.code}", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("€ %.2f".format(product.price), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Stock: ${product.quantity}", fontSize = 12.sp, color = stockColor, fontWeight = FontWeight.Medium)
                    if (product.category.isNotEmpty()) Text(product.category, fontSize = 11.sp, color = Color.Gray)
                }
                if (product.supplierName != null) Text("${strings.supplier}: ${product.supplierName}", fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { onEdit(product) }) { Icon(Icons.Default.Edit, strings.edit, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = { onDelete(product) }) { Icon(Icons.Default.Delete, strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDialog(
    product: ProductWithSupplier?,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val strings = LocalStrings.current
    var code by remember { mutableStateOf(product?.code ?: "") }
    var name by remember { mutableStateOf(product?.name ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var quantity by remember { mutableStateOf(product?.quantity?.toString() ?: "0") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var altSku by remember { mutableStateOf(product?.alternativeSku ?: "") }
    var weight by remember { mutableStateOf(product?.weight?.toString() ?: "0.0") }
    var uom by remember { mutableStateOf(product?.unitOfMeasure ?: "pcs") }
    var minQty by remember { mutableStateOf(product?.minimumQuantity?.toString() ?: "0") }
    var acquisitionCost by remember { mutableStateOf(product?.acquisitionCost?.toString() ?: "0.0") }
    var active by remember { mutableStateOf(product?.active ?: true) }
    var warehousePosition by remember { mutableStateOf(product?.warehousePosition ?: "") }
    var vatRate by remember { mutableStateOf(product?.vatRate?.toString() ?: "22.0") }
    var selectedSupplierId by remember { mutableStateOf(product?.supplierId) }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) strings.newProduct else strings.editProduct) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = code, onValueChange = { code = it; codeError = false }, label = { Text(strings.productCode) }, isError = codeError, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it; nameError = false }, label = { Text(strings.productName) }, isError = nameError, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                item {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(strings.description) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = price, onValueChange = { price = it; priceError = false }, label = { Text(strings.price) }, isError = priceError, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        OutlinedTextField(value = acquisitionCost, onValueChange = { acquisitionCost = it }, label = { Text(strings.acquisitionCost) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text(strings.quantity) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(value = minQty, onValueChange = { minQty = it }, label = { Text(strings.minimumQuantity) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = vatRate, onValueChange = { vatRate = it }, label = { Text(strings.vatRate) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(strings.category) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = altSku, onValueChange = { altSku = it }, label = { Text(strings.alternativeSku) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = uom, onValueChange = { uom = it }, label = { Text(strings.unitOfMeasure) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = warehousePosition, onValueChange = { warehousePosition = it }, label = { Text(strings.warehousePosition) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text(strings.weight) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                }
                item {
                    // Supplier dropdown
                    ExposedDropdownMenuBox(expanded = showSupplierDropdown, onExpandedChange = { showSupplierDropdown = it }) {
                        OutlinedTextField(
                            value = suppliers.find { it.id == selectedSupplierId }?.companyName ?: strings.selectSupplier,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.supplier) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSupplierDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = showSupplierDropdown, onDismissRequest = { showSupplierDropdown = false }) {
                            DropdownMenuItem(text = { Text(strings.selectSupplier) }, onClick = { selectedSupplierId = null; showSupplierDropdown = false })
                            suppliers.forEach { supplier ->
                                DropdownMenuItem(text = { Text(supplier.companyName) }, onClick = { selectedSupplierId = supplier.id; showSupplierDropdown = false })
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = active, onCheckedChange = { active = it })
                        Text(strings.activeLabel)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                codeError = code.isBlank()
                nameError = name.isBlank()
                priceError = price.toDoubleOrNull() == null
                if (!codeError && !nameError && !priceError) {
                    onSave(ProductEntity(
                        id = product?.id ?: 0, code = code.trim(), name = name.trim(),
                        description = description.trim(), price = price.toDoubleOrNull() ?: 0.0,
                        quantity = quantity.toIntOrNull() ?: 0,
                        category = category.trim(), alternativeSku = altSku.trim(),
                        weight = weight.toDoubleOrNull() ?: 0.0, unitOfMeasure = uom.trim(),
                        minimumQuantity = minQty.toIntOrNull() ?: 0,
                        acquisitionCost = acquisitionCost.toDoubleOrNull() ?: 0.0,
                        active = active, supplierId = selectedSupplierId,
                        warehousePosition = warehousePosition.trim(),
                        vatRate = vatRate.toDoubleOrNull() ?: 22.0
                    ))
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
