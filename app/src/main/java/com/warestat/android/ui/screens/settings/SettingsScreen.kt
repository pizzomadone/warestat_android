package com.warestat.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.warestat.android.data.database.entity.CompanyDataEntity
import com.warestat.android.ui.theme.*
import com.warestat.android.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
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
            Text("Impostazioni", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Generali") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Azienda") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Aspetto") })
            }
            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> GeneralSettingsTab(state.settings, viewModel)
                1 -> CompanyDataTab(state.companyData, viewModel)
                2 -> AppearanceTab(state.settings, viewModel)
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(settings: com.warestat.android.util.AppSettings, viewModel: SettingsViewModel) {
    var currency by remember(settings.currency) { mutableStateOf(settings.currency) }
    var currencySymbol by remember(settings.currencySymbol) { mutableStateOf(settings.currencySymbol) }
    var defaultVat by remember(settings.defaultVatRate) { mutableStateOf(settings.defaultVatRate.toString()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Currency settings
        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Euro, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Valuta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Codice valuta") }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("EUR") })
                    OutlinedTextField(value = currencySymbol, onValueChange = { currencySymbol = it }, label = { Text("Simbolo") }, modifier = Modifier.weight(0.5f), singleLine = true, placeholder = { Text("€") })
                }
                Button(onClick = { viewModel.updateCurrency(currency.trim(), currencySymbol.trim()) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Salva valuta")
                }
            }
        }

        // VAT settings
        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Percent, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("IVA predefinita", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = defaultVat, onValueChange = { defaultVat = it },
                    label = { Text("Aliquota IVA (%)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                )
                Button(onClick = { viewModel.updateDefaultVat(defaultVat.toDoubleOrNull() ?: 22.0) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Salva IVA")
                }
            }
        }

        // Backup settings
        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Backup automatico", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Abilita backup automatico", modifier = Modifier.weight(1f))
                    Switch(checked = settings.autoBackup, onCheckedChange = { viewModel.updateAutoBackup(it) })
                }
                Text("Vai alla sezione Backup per gestire i file e la conservazione.", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun CompanyDataTab(companyData: CompanyDataEntity?, viewModel: SettingsViewModel) {
    var companyName by remember(companyData) { mutableStateOf(companyData?.companyName ?: "") }
    var vatNumber by remember(companyData) { mutableStateOf(companyData?.vatNumber ?: "") }
    var taxCode by remember(companyData) { mutableStateOf(companyData?.taxCode ?: "") }
    var address by remember(companyData) { mutableStateOf(companyData?.address ?: "") }
    var city by remember(companyData) { mutableStateOf(companyData?.city ?: "") }
    var postalCode by remember(companyData) { mutableStateOf(companyData?.postalCode ?: "") }
    var country by remember(companyData) { mutableStateOf(companyData?.country ?: "Italy") }
    var phone by remember(companyData) { mutableStateOf(companyData?.phone ?: "") }
    var email by remember(companyData) { mutableStateOf(companyData?.email ?: "") }
    var website by remember(companyData) { mutableStateOf(companyData?.website ?: "") }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dati azienda", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(value = companyName, onValueChange = { companyName = it; nameError = false }, label = { Text("Ragione sociale *") }, isError = nameError, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = vatNumber, onValueChange = { vatNumber = it }, label = { Text("Partita IVA") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = taxCode, onValueChange = { taxCode = it }, label = { Text("Cod. Fiscale") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Indirizzo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Città") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = postalCode, onValueChange = { postalCode = it }, label = { Text("CAP") }, modifier = Modifier.weight(0.6f), singleLine = true)
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Paese") }, modifier = Modifier.weight(0.8f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefono") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Sito web") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Spacer(Modifier.height(4.dp))
                Button(onClick = {
                    nameError = companyName.isBlank()
                    if (!nameError) {
                        viewModel.saveCompanyData(CompanyDataEntity(
                            id = companyData?.id ?: 0, companyName = companyName.trim(), vatNumber = vatNumber.trim(),
                            taxCode = taxCode.trim(), address = address.trim(), city = city.trim(),
                            postalCode = postalCode.trim(), country = country.trim(), phone = phone.trim(),
                            email = email.trim(), website = website.trim(), logoPath = companyData?.logoPath ?: ""
                        ))
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Salva dati azienda")
                }
            }
        }
    }
}

@Composable
private fun AppearanceTab(settings: com.warestat.android.util.AppSettings, viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tema scuro", fontWeight = FontWeight.Medium)
                        Text("Usa il tema scuro dell'applicazione", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = settings.darkTheme, onCheckedChange = { viewModel.updateDarkTheme(it) })
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Secondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Informazioni app", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                InfoRow("Versione", "1.0.0")
                InfoRow("Piattaforma", "Android")
                InfoRow("Database", "SQLite via Room")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(100.dp), color = Color.Gray, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
