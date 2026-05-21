package com.warestat.android.ui.screens.reports

import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.warestat.android.i18n.LocalStrings
import com.warestat.android.ui.theme.*
import com.warestat.android.viewmodel.ReportsViewModel

@Composable
fun AdvancedStatsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(strings.advancedStatsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))

        // Period selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6 to strings.sixMonths, 12 to strings.twelveMonths, 24 to strings.twentyFourMonths).forEach { (months, label) ->
                FilterChip(
                    selected = state.selectedPeriodMonths == months,
                    onClick = { viewModel.setPeriod(months) },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.trendSalesTab) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.topProductsTab) })
        }
        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            0 -> SalesTrendTab(state.monthlySales, strings)
            1 -> TopProductsTab(state.topProducts, strings)
        }
    }
}

@Composable
private fun SalesTrendTab(monthlySales: List<com.warestat.android.data.database.dao.MonthlySalesData>, strings: com.warestat.android.i18n.AppStrings) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(strings.monthlyRevenueTrend, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            if (monthlySales.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(strings.noDataAvailable, color = Color.Gray)
                    }
                }
            } else {
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(250.dp).padding(8.dp),
                        factory = { ctx ->
                            LineChart(ctx).apply {
                                description.isEnabled = false
                                setTouchEnabled(true)
                                isDragEnabled = true
                                setScaleEnabled(true)
                                setPinchZoom(true)
                                legend.isEnabled = true
                                axisRight.isEnabled = false
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.granularity = 1f
                                xAxis.labelRotationAngle = -45f
                                xAxis.textSize = 9f
                                axisLeft.textSize = 9f
                            }
                        },
                        update = { chart ->
                            val labels = monthlySales.map { it.month }
                            val entries = monthlySales.mapIndexed { i, data -> Entry(i.toFloat(), data.total.toFloat()) }
                            val dataSet = LineDataSet(entries, strings.revenueCol + " (€)").apply {
                                color = AndroidColor.parseColor("#3498DB")
                                valueTextSize = 8f
                                setCircleColor(AndroidColor.parseColor("#3498DB"))
                                circleRadius = 4f
                                lineWidth = 2f
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                                setDrawFilled(true)
                                fillColor = AndroidColor.parseColor("#803498DB")
                            }
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.data = LineData(dataSet)
                            chart.invalidate()
                        }
                    )
                }
            }
        }
        item {
            Text(strings.monthlyOrderCount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            if (monthlySales.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp),
                        factory = { ctx ->
                            LineChart(ctx).apply {
                                description.isEnabled = false; setTouchEnabled(true); isDragEnabled = true; setScaleEnabled(true)
                                legend.isEnabled = true; axisRight.isEnabled = false
                                xAxis.position = XAxis.XAxisPosition.BOTTOM; xAxis.granularity = 1f; xAxis.labelRotationAngle = -45f; xAxis.textSize = 9f
                            }
                        },
                        update = { chart ->
                            val labels = monthlySales.map { it.month }
                            val entries = monthlySales.mapIndexed { i, data -> Entry(i.toFloat(), data.numOrders.toFloat()) }
                            val dataSet = LineDataSet(entries, strings.ordersLabel).apply {
                                color = AndroidColor.parseColor("#27AE60"); valueTextSize = 8f
                                setCircleColor(AndroidColor.parseColor("#27AE60")); circleRadius = 4f; lineWidth = 2f
                                mode = LineDataSet.Mode.CUBIC_BEZIER; setDrawFilled(true); fillColor = AndroidColor.parseColor("#8027AE60")
                            }
                            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            chart.data = LineData(dataSet); chart.invalidate()
                        }
                    )
                }
            }
        }
        // Monthly summary table
        if (monthlySales.isNotEmpty()) {
            item {
                Text(strings.monthlySummary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            items(monthlySales.reversed()) { data ->
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(data.month, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("${data.numOrders} ${strings.ordersLabel.lowercase()}", modifier = Modifier.width(90.dp), fontSize = 12.sp, color = Color.Gray)
                        Text("€ %.2f".format(data.total), fontWeight = FontWeight.SemiBold, color = Success)
                    }
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TopProductsTab(topProducts: List<com.warestat.android.data.database.dao.ProductSalesData>, strings: com.warestat.android.i18n.AppStrings) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (topProducts.isNotEmpty()) {
            item {
                Text(strings.revenueDistribution, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    val pieColors = listOf(
                        AndroidColor.parseColor("#3498DB"), AndroidColor.parseColor("#2ECC71"),
                        AndroidColor.parseColor("#E74C3C"), AndroidColor.parseColor("#F39C12"),
                        AndroidColor.parseColor("#9B59B6"), AndroidColor.parseColor("#1ABC9C"),
                        AndroidColor.parseColor("#E67E22"), AndroidColor.parseColor("#34495E"),
                        AndroidColor.parseColor("#E91E63"), AndroidColor.parseColor("#607D8B")
                    )
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(280.dp).padding(8.dp),
                        factory = { ctx ->
                            PieChart(ctx).apply {
                                description.isEnabled = false; isRotationEnabled = true; isHighlightPerTapEnabled = true
                                legend.isEnabled = true; setUsePercentValues(true); setEntryLabelTextSize(10f)
                                holeRadius = 40f; transparentCircleRadius = 45f
                            }
                        },
                        update = { chart ->
                            val entries = topProducts.take(10).mapIndexed { _, data ->
                                PieEntry(data.revenue.toFloat(), data.name.take(15))
                            }
                            val dataSet = PieDataSet(entries, "").apply {
                                colors = pieColors; valueTextSize = 9f; valueTextColor = AndroidColor.WHITE
                                sliceSpace = 2f
                            }
                            chart.data = PieData(dataSet); chart.invalidate()
                        }
                    )
                }
            }
        }
        item { Text(strings.top10ByRevenue, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
        if (topProducts.isEmpty()) {
            item { Text(strings.noDataAvailable, color = Color.Gray, modifier = Modifier.padding(8.dp)) }
        } else {
            items(topProducts.take(10).withIndex().toList(), key = { it.index }) { (index, product) ->
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, color = Primary, fontSize = 14.sp)
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            Text("${product.numOrders} ${strings.ordersLabel.lowercase()} · ${product.totalQuantity} ${strings.qtyCol.lowercase()}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text("€ %.2f".format(product.revenue), fontWeight = FontWeight.Bold, color = Success, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
