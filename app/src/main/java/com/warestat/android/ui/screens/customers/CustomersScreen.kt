package com.warestat.android.ui.screens.customers

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warestat.android.data.database.entity.CustomerEntity
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.viewmodel.CustomersViewModel

@Composable
fun CustomersScreen(viewModel: CustomersViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CustomerEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(strings.customersTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = { Text(strings.searchCustomers) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${state.customers.size} ${strings.customersTitle.lowercase()}", style = MaterialTheme.typography.bodyMedium)
            FloatingActionButton(onClick = { editingCustomer = null; showDialog = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, strings.newCustomer)
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.customers, key = { it.id }) { customer ->
                CustomerCard(
                    customer = customer,
                    onEdit = { editingCustomer = it; showDialog = true },
                    onDelete = { showDeleteConfirm = it }
                )
            }
        }
    }

    if (showDialog) {
        CustomerDialog(
            customer = editingCustomer,
            onDismiss = { showDialog = false },
            onSave = { customer ->
                viewModel.saveCustomer(customer)
                showDialog = false
            }
        )
    }

    showDeleteConfirm?.let { customer ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(strings.deleteCustomerTitle) },
            text = { Text("${strings.delete} ${customer.firstName} ${customer.lastName}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCustomer(customer); showDeleteConfirm = null }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(strings.cancel) } }
        )
    }

    state.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
        Snackbar { Text(error) }
    }
}

@Composable
private fun CustomerCard(customer: CustomerEntity, onEdit: (CustomerEntity) -> Unit, onDelete: (CustomerEntity) -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(customer) },
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${customer.firstName} ${customer.lastName}", fontWeight = FontWeight.Bold)
                if (customer.email.isNotEmpty()) Text(customer.email, style = MaterialTheme.typography.bodyMedium)
                if (customer.phone.isNotEmpty()) Text(customer.phone, style = MaterialTheme.typography.bodyMedium)
                if (customer.address.isNotEmpty()) Text(customer.address, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { onEdit(customer) }) { Icon(Icons.Default.Edit, strings.edit) }
            IconButton(onClick = { onDelete(customer) }) { Icon(Icons.Default.Delete, strings.delete, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun CustomerDialog(customer: CustomerEntity?, onDismiss: () -> Unit, onSave: (CustomerEntity) -> Unit) {
    val strings = LocalStrings.current
    var firstName by remember { mutableStateOf(customer?.firstName ?: "") }
    var lastName by remember { mutableStateOf(customer?.lastName ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) strings.newCustomer else strings.editCustomer) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = firstName, onValueChange = { firstName = it; firstNameError = false },
                    label = { Text(strings.firstName) }, isError = firstNameError, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = lastName, onValueChange = { lastName = it; lastNameError = false },
                    label = { Text(strings.lastName) }, isError = lastNameError, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(strings.email) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.phone) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(strings.address) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                firstNameError = firstName.isBlank()
                lastNameError = lastName.isBlank()
                if (!firstNameError && !lastNameError) {
                    onSave(CustomerEntity(id = customer?.id ?: 0, firstName = firstName.trim(), lastName = lastName.trim(),
                        email = email.trim(), phone = phone.trim(), address = address.trim()))
                }
            }) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
