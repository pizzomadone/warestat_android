package com.warestat.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Customers : Screen("customers", "Clienti", Icons.Default.People)
    object Products : Screen("products", "Prodotti", Icons.Default.Inventory)
    object Orders : Screen("orders", "Ordini", Icons.Default.ShoppingCart)
    object Invoices : Screen("invoices", "Fatture", Icons.Default.Receipt)
    object Suppliers : Screen("suppliers", "Fornitori", Icons.Default.LocalShipping)
    object Warehouse : Screen("warehouse", "Magazzino", Icons.Default.Warehouse)
    object Reports : Screen("reports", "Report", Icons.Default.BarChart)
    object Backup : Screen("backup", "Backup", Icons.Default.Backup)
    object Settings : Screen("settings", "Impostazioni", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Orders,
    Screen.Products,
    Screen.Warehouse,
    Screen.Invoices
)

val drawerItems = listOf(
    Screen.Dashboard,
    Screen.Customers,
    Screen.Products,
    Screen.Orders,
    Screen.Invoices,
    Screen.Suppliers,
    Screen.Warehouse,
    Screen.Reports,
    Screen.Backup,
    Screen.Settings
)
