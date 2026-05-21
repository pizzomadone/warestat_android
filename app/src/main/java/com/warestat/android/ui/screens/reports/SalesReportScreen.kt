package com.warestat.android.ui.screens.reports

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warestat.android.data.database.dao.OrderWithCustomer
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.*
import com.warestat.android.util.DateUtils
import com.warestat.android.util.ReportPDFGenerator
import com.warestat.android.viewmodel.ReportsViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun SalesReportScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(strings.salesReportTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // Date range filter
        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.periodLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(DateUtils.formatDate(state.startDate), fontSize = 12.sp)
                    }
                    Text("→", modifier = Modifier.align(Alignment.CenterVertically))
                    OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(DateUtils.formatDate(state.endDate), fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7 to strings.sevenDays, 30 to strings.thirtyDays, 90 to strings.threeMonths, 365 to strings.yearLabel).forEach { (days, label) ->
                        OutlinedButton(onClick = {
                            val end = System.currentTimeMillis()
                            val start = end - days * 86_400_000L
                            viewModel.setDateRange(start, end)
                        }, modifier = Modifier.weight(1f)) { Text(label, fontSize = 11.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Stats summary
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(Modifier.weight(1f), strings.ordersLabel, state.totalOrders.toString(), Secondary)
            StatCard(Modifier.weight(1f), strings.total, "€ %.2f".format(state.totalSales), Success)
            StatCard(Modifier.weight(1f), strings.averageLabel, "€ %.2f".format(if (state.totalOrders > 0) state.totalSales / state.totalOrders else 0.0), Warning)
        }
        Spacer(Modifier.height(8.dp))

        // Export buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exportCsv(context, state.salesData) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.exportCsv, fontSize = 12.sp)
            }
            Button(onClick = {
                scope.launch {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val columns = listOf("#", strings.customer, strings.dateCol, strings.status, strings.total)
                    val rows = state.salesData.map { o ->
                        listOf(o.id.toString(), o.customerName ?: "-", sdf.format(java.util.Date(o.orderDate)), o.status, "€ %.2f".format(o.total))
                    }
                    val title = strings.salesReportTitle
                    val subtitle = "${sdf.format(java.util.Date(state.startDate))} - ${sdf.format(java.util.Date(state.endDate))}"
                    ReportPDFGenerator.generateSalesReport(context, rows, columns, title, subtitle, state.totalSales, state.totalOrders)
                        .onSuccess { uri ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        }
                }
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.exportPdf, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Orders table
        Text("${state.salesData.size} ${strings.ordersLabel.lowercase()}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))

        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Primary).padding(8.dp)) {
                    Text("#", color = Color.White, modifier = Modifier.width(40.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(strings.customer, color = Color.White, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(strings.dateCol, color = Color.White, modifier = Modifier.width(80.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(strings.status, color = Color.White, modifier = Modifier.width(70.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(strings.total, color = Color.White, modifier = Modifier.width(70.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.salesData.take(100), key = { it.id }) { order ->
                        OrderRow(order)
                    }
                    if (state.salesData.size > 100) {
                        item {
                            Text("... ${state.salesData.size - 100} ${strings.ordersLabel.lowercase()}", modifier = Modifier.padding(8.dp), color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerModal(
            initialDate = state.startDate,
            onDateSelected = { date -> viewModel.setDateRange(date, state.endDate); showStartDatePicker = false },
            onDismiss = { showStartDatePicker = false },
            cancelLabel = strings.cancel
        )
    }
    if (showEndDatePicker) {
        DatePickerModal(
            initialDate = state.endDate,
            onDateSelected = { date -> viewModel.setDateRange(state.startDate, date); showEndDatePicker = false },
            onDismiss = { showEndDatePicker = false },
            cancelLabel = strings.cancel
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
    }
}

@Composable
private fun OrderRow(order: OrderWithCustomer) {
    val statusColor = when (order.status) {
        "Completed" -> Success; "Cancelled" -> Color.Gray; "In Progress" -> Warning; else -> Secondary
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#${order.id}", modifier = Modifier.width(40.dp), fontSize = 11.sp)
        Text(order.customerName ?: "N/A", modifier = Modifier.weight(1f), fontSize = 11.sp, maxLines = 1)
        Text(DateUtils.formatDate(order.orderDate), modifier = Modifier.width(80.dp), fontSize = 10.sp)
        Text(order.status, modifier = Modifier.width(70.dp), fontSize = 10.sp, color = statusColor)
        Text("€ %.2f".format(order.total), modifier = Modifier.width(70.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Success)
    }
    Divider(color = Color.LightGray, thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(initialDate: Long, onDateSelected: (Long) -> Unit, onDismiss: () -> Unit, cancelLabel: String) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { datePickerState.selectedDateMillis?.let { onDateSelected(it) } ?: onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun exportCsv(context: android.content.Context, orders: List<OrderWithCustomer>) {
    val sb = StringBuilder()
    sb.appendLine("ID,Cliente,Data,Stato,Pagamento,Totale")
    orders.forEach { order ->
        sb.appendLine("${order.id},\"${order.customerName ?: "N/A"}\",${DateUtils.formatDate(order.orderDate)},${order.status},${order.paymentStatus},${order.total}")
    }
    try {
        val file = java.io.File(context.getExternalFilesDir(null), "report_vendite_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString())
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export CSV"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
