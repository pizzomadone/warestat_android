package com.warestat.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.warestat.android.i18n.*
import com.warestat.android.ui.navigation.*
import com.warestat.android.ui.screens.backup.BackupScreen
import com.warestat.android.ui.screens.customers.CustomersScreen
import com.warestat.android.ui.screens.dashboard.DashboardScreen
import com.warestat.android.ui.screens.eula.EulaScreen
import com.warestat.android.ui.screens.invoices.InvoicesScreen
import com.warestat.android.ui.screens.language.LanguageSelectionScreen
import com.warestat.android.ui.screens.orders.OrdersScreen
import com.warestat.android.ui.screens.products.ProductsScreen
import com.warestat.android.ui.screens.reports.AdvancedStatsScreen
import com.warestat.android.ui.screens.reports.SalesReportScreen
import com.warestat.android.ui.screens.settings.SettingsScreen
import com.warestat.android.ui.screens.suppliers.SuppliersScreen
import com.warestat.android.ui.screens.warehouse.WarehouseScreen
import com.warestat.android.ui.theme.Primary
import com.warestat.android.ui.theme.WareStatTheme
import com.warestat.android.util.SettingsManager
import com.warestat.android.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsManager.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.warestat.android.util.AppSettings()
            )
            val scope = rememberCoroutineScope()

            WareStatTheme(darkTheme = settings.darkTheme) {
                CompositionLocalProvider(LocalStrings provides getStrings(settings.selectedLanguage)) {
                    when {
                        settings.selectedLanguage.isEmpty() -> {
                            LanguageSelectionScreen(onLanguageSelected = { code ->
                                scope.launch { settingsManager.updateLanguage(code) }
                            })
                        }
                        !settings.eulaAccepted -> {
                            EulaScreen(onAccept = {
                                scope.launch { settingsManager.markEulaAccepted() }
                            })
                        }
                        else -> {
                            WareStatApp()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WareStatApp() {
    val strings = LocalStrings.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                // Header
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column {
                        Text("WareStat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
                        Text(strings.appSubtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))
                // Drawer items
                drawerItems.forEach { screen ->
                    val label = when (screen.route) {
                        Screen.Dashboard.route -> strings.navDashboard
                        Screen.Customers.route -> strings.navCustomers
                        Screen.Products.route -> strings.navProducts
                        Screen.Orders.route -> strings.navOrders
                        Screen.Invoices.route -> strings.navInvoices
                        Screen.Suppliers.route -> strings.navSuppliers
                        Screen.Warehouse.route -> strings.navWarehouse
                        Screen.Reports.route -> strings.navReports
                        Screen.Backup.route -> strings.navBackup
                        Screen.Settings.route -> strings.navSettings
                        else -> screen.title
                    }
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, null) },
                        label = { Text(label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val screen = drawerItems.find { it.route == currentRoute }
                        val title = if (screen != null) {
                            when (screen.route) {
                                Screen.Dashboard.route -> strings.navDashboard
                                Screen.Customers.route -> strings.navCustomers
                                Screen.Products.route -> strings.navProducts
                                Screen.Orders.route -> strings.navOrders
                                Screen.Invoices.route -> strings.navInvoices
                                Screen.Suppliers.route -> strings.navSuppliers
                                Screen.Warehouse.route -> strings.navWarehouse
                                Screen.Reports.route -> strings.navReports
                                Screen.Backup.route -> strings.navBackup
                                Screen.Settings.route -> strings.navSettings
                                else -> screen.title
                            }
                        } else "WareStat"
                        Text(title, fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val label = when (screen.route) {
                            Screen.Dashboard.route -> strings.navDashboard
                            Screen.Customers.route -> strings.navCustomers
                            Screen.Products.route -> strings.navProducts
                            Screen.Orders.route -> strings.navOrders
                            Screen.Invoices.route -> strings.navInvoices
                            Screen.Suppliers.route -> strings.navSuppliers
                            Screen.Warehouse.route -> strings.navWarehouse
                            Screen.Reports.route -> strings.navReports
                            Screen.Backup.route -> strings.navBackup
                            Screen.Settings.route -> strings.navSettings
                            else -> screen.title
                        }
                        NavigationBarItem(
                            icon = { Icon(screen.icon, null) },
                            label = { Text(label, maxLines = 1) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(onNavigateTo = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    })
                }
                composable(Screen.Customers.route) { CustomersScreen() }
                composable(Screen.Products.route) { ProductsScreen() }
                composable(Screen.Orders.route) { OrdersScreen() }
                composable(Screen.Invoices.route) { InvoicesScreen() }
                composable(Screen.Suppliers.route) { SuppliersScreen() }
                composable(Screen.Warehouse.route) { WarehouseScreen() }
                composable(Screen.Reports.route) { ReportsScreen(navController) }
                composable(Screen.Backup.route) { BackupScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable("reports_sales") { SalesReportScreen() }
                composable("reports_advanced") { AdvancedStatsScreen() }
            }
        }
    }
}

@Composable
private fun ReportsScreen(navController: NavHostController) {
    val strings = LocalStrings.current
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        Text(strings.reportsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(strings.selectReportType, style = MaterialTheme.typography.bodyMedium)

        ReportCard(
            title = strings.salesReportTitle,
            description = strings.salesReportDesc,
            icon = Icons.Default.Receipt,
            onClick = { navController.navigate("reports_sales") }
        )
        ReportCard(
            title = strings.advancedStatsTitle,
            description = strings.advancedStatsDesc,
            icon = Icons.Default.BarChart,
            onClick = { navController.navigate("reports_advanced") }
        )
    }
}

@Composable
private fun ReportCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Primary)
        }
    }
}
