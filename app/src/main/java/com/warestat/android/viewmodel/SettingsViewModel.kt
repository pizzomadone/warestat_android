package com.warestat.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warestat.android.data.database.dao.CompanyDataDao
import com.warestat.android.data.database.entity.CompanyDataEntity
import com.warestat.android.util.AppSettings
import com.warestat.android.util.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsScreenState(
    val settings: AppSettings = AppSettings(),
    val companyData: CompanyDataEntity? = null,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val companyDataDao: CompanyDataDao
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsScreenState())
    val state: StateFlow<SettingsScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            companyDataDao.getCompanyData().collect { companyData ->
                _state.update { it.copy(companyData = companyData) }
            }
        }
    }

    fun updateCurrency(currency: String, symbol: String) {
        viewModelScope.launch { settingsManager.updateCurrency(currency, symbol) }
    }

    fun updateDefaultVat(vat: Double) {
        viewModelScope.launch { settingsManager.updateDefaultVat(vat) }
    }

    fun updateAutoBackup(enabled: Boolean) {
        viewModelScope.launch { settingsManager.updateAutoBackup(enabled) }
    }

    fun updateRetentionDays(days: Int) {
        viewModelScope.launch { settingsManager.updateRetentionDays(days) }
    }

    fun updateDarkTheme(dark: Boolean) {
        viewModelScope.launch { settingsManager.updateDarkTheme(dark) }
    }

    fun updateLanguage(code: String) {
        viewModelScope.launch { settingsManager.updateLanguage(code) }
    }

    fun saveCompanyData(data: CompanyDataEntity) {
        viewModelScope.launch {
            try {
                val count = companyDataDao.getCount()
                if (count == 0) companyDataDao.insertCompanyData(data)
                else companyDataDao.updateCompanyData(data)
                _state.update { it.copy(successMessage = "Dati azienda salvati") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(successMessage = null, error = null) }
}
