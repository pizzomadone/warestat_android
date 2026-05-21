package com.warestat.android.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warestat.android.data.database.dao.OrderWithCustomer
import com.warestat.android.data.database.dao.ProductWithSupplier
import com.warestat.android.data.database.dao.TopProductResult
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.*
import com.warestat.android.util.DateUtils
import com.warestat.android.viewmodel.ABCClass
import com.warestat.android.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateTo: (String) -> Unit
) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.dashboardTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, strings.refresh, tint = Primary)
                }
            }
        }

        // KPI Cards
        item {
            Text("KPI", style = MaterialTheme.typography.titleMedium, color = Primary)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(Modifier.weight(1f), strings.warehouseValue, "€ %.2f".format(state.warehouseValue), Icons.Default.Warehouse, Secondary)
                    KpiCard(Modifier.weight(1f), strings.lowStockKpi, state.lowStockCount.toString(), Icons.Default.Warning, Danger)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(Modifier.weight(1f), strings.pendingOrdersKpi, state.pendingOrderCount.toString(), Icons.Default.ShoppingCart, Warning)
                    KpiCard(Modifier.weight(1f), strings.monthRevenue, "€ %.2f".format(state.monthRevenue), Icons.Default.TrendingUp, Success)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(Modifier.weight(1f), strings.avgMargin, "%.1f%%".format(state.averageMargin), Icons.Default.Percent, Secondary)
                    KpiCard(Modifier.weight(1f), strings.zeroStock, state.zeroStockCount.toString(), Icons.Default.Block, Warning)
                }
            }
        }

        // Quick Actions
        item {
            Text(strings.quickActions, style = MaterialTheme.typography.titleMedium, color = Primary)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(Modifier.weight(1f), strings.newOrder, Icons.Default.Add, Secondary) { onNavigateTo("orders") }
                QuickActionButton(Modifier.weight(1f), strings.newInvoice, Icons.Default.Receipt, Success) { onNavigateTo("invoices") }
                QuickActionButton(Modifier.weight(1f), strings.warehouse, Icons.Default.Warehouse, Primary) { onNavigateTo("warehouse") }
            }
        }

        // Low Stock Alerts
        if (state.lowStockAlerts.isNotEmpty()) {
            item {
                SectionHeader(strings.lowStockSection, Danger)
            }
            items(state.lowStockAlerts.take(5)) { product ->
                LowStockAlertItem(product) { onNavigateTo("products") }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF4))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Success)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.noLowStock, color = Success)
                    }
                }
            }
        }

        // Pending Orders
        item { SectionHeader(strings.pendingOrdersSection, Primary) }
        if (state.pendingOrders.isEmpty()) {
            item { Text(strings.noPendingOrders, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
        } else {
            items(state.pendingOrders.take(8)) { order ->
                PendingOrderItem(order) { onNavigateTo("orders") }
            }
        }

        // Top Products
        item { SectionHeader(strings.top10Products, Primary) }
        item {
            if (state.topProducts.isEmpty()) {
                Text(strings.noDataAvailable, color = Color.Gray, modifier = Modifier.padding(8.dp))
            } else {
                TopProductsTable(state.topProducts)
            }
        }

        // ABC Analysis
        item { SectionHeader(strings.abcAnalysis, Primary) }
        item {
            ABCAnalysisTable(
                classA = state.abcData.classA,
                classB = state.abcData.classB,
                classC = state.abcData.classC,
                totalProducts = state.abcData.totalProducts
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun KpiCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun QuickActionButton(modifier: Modifier, text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
            Text(text, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Divider(Modifier.weight(0.05f), color = color, thickness = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Divider(Modifier.weight(1f), color = color.copy(alpha = 0.3f))
    }
}

@Composable
private fun LowStockAlertItem(product: ProductWithSupplier, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${product.code} - ${product.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Stock: ${product.quantity} / Min: ${product.minimumQuantity} (${strings.missing}: ${product.minimumQuantity - product.quantity})",
                    fontSize = 11.sp, color = Color(0xFF8B0000)
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Danger)
        }
    }
}

@Composable
private fun PendingOrderItem(order: OrderWithCustomer, onClick: () -> Unit) {
    val strings = LocalStrings.current
    val daysOld = ((System.currentTimeMillis() - order.orderDate) / 86_400_000).toInt()
    val cardColor = if (daysOld > 7) Color(0xFFFFF5F5) else Color(0xFFFFFBF0)
    val borderColor = if (daysOld > 7) Danger else Warning

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${strings.navOrders} #${order.id} - ${order.status}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "${order.customerName ?: "N/A"} - ${DateUtils.formatDate(order.orderDate)} ($daysOld ${strings.daysAgo})",
                    fontSize = 11.sp, color = Color.Gray
                )
            }
            Text("€ %.2f".format(order.total), fontWeight = FontWeight.Bold, color = Success, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TopProductsTable(products: List<TopProductResult>) {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(Primary).padding(8.dp)
            ) {
                Text("#", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), fontSize = 12.sp)
                Text(strings.productCol, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Text(strings.qtyCol, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp), fontSize = 12.sp)
                Text(strings.revenueCol, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), fontSize = 12.sp)
            }
            products.forEachIndexed { index, product ->
                val bg = if (index % 2 == 0) Color.White else LightGray
                Row(
                    Modifier.fillMaxWidth().background(bg).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", modifier = Modifier.width(30.dp), fontSize = 12.sp)
                    Text(product.name, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 1)
                    Text(product.totalQty.toString(), modifier = Modifier.width(50.dp), fontSize = 12.sp)
                    Text("€ %.2f".format(product.totalRevenue), modifier = Modifier.width(80.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ABCAnalysisTable(classA: ABCClass, classB: ABCClass, classC: ABCClass, totalProducts: Int) {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().background(Primary).padding(8.dp)) {
                listOf(strings.classCol, strings.productsCol, strings.pctProductsCol, strings.totalRevenueCol, strings.pctRevenueCol).forEach { col ->
                    Text(col, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 11.sp)
                }
            }
            listOf(
                classA to Color(0xFFE8F5E9),
                classB to Color(0xFFFFF9C4),
                classC to Color(0xFFFFEBEE)
            ).forEach { (cls, bg) ->
                Row(Modifier.fillMaxWidth().background(bg).padding(8.dp)) {
                    Text(cls.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 12.sp)
                    Text(cls.count.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp)
                    Text(if (totalProducts > 0) "%.1f%%".format(cls.count * 100.0 / totalProducts) else "0%", modifier = Modifier.weight(1f), fontSize = 12.sp)
                    Text("€ %.2f".format(cls.revenue), modifier = Modifier.weight(1f), fontSize = 12.sp)
                    Text("%.1f%%".format(cls.revenuePercent), modifier = Modifier.weight(1f), fontSize = 12.sp)
                }
            }
        }
    }
}
