package com.warestat.android.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "warestat_settings")

data class AppSettings(
    val currency: String = "EUR",
    val currencySymbol: String = "€",
    val defaultVatRate: Double = 22.0,
    val autoBackup: Boolean = true,
    val backupIntervalHours: Int = 24,
    val retentionDays: Int = 7,
    val darkTheme: Boolean = false,
    val companyName: String = "",
    val eulaAccepted: Boolean = false
)

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val KEY_DEFAULT_VAT = doublePreferencesKey("default_vat")
        val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val KEY_BACKUP_INTERVAL = intPreferencesKey("backup_interval_hours")
        val KEY_RETENTION_DAYS = intPreferencesKey("retention_days")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_EULA_ACCEPTED = booleanPreferencesKey("eula_accepted")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            currency = prefs[KEY_CURRENCY] ?: "EUR",
            currencySymbol = prefs[KEY_CURRENCY_SYMBOL] ?: "€",
            defaultVatRate = prefs[KEY_DEFAULT_VAT] ?: 22.0,
            autoBackup = prefs[KEY_AUTO_BACKUP] ?: true,
            backupIntervalHours = prefs[KEY_BACKUP_INTERVAL] ?: 24,
            retentionDays = prefs[KEY_RETENTION_DAYS] ?: 7,
            darkTheme = prefs[KEY_DARK_THEME] ?: false,
            eulaAccepted = prefs[KEY_EULA_ACCEPTED] ?: false
        )
    }

    suspend fun updateCurrency(currency: String, symbol: String) {
        context.dataStore.edit {
            it[KEY_CURRENCY] = currency
            it[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun updateDefaultVat(vat: Double) {
        context.dataStore.edit { it[KEY_DEFAULT_VAT] = vat }
    }

    suspend fun updateAutoBackup(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_BACKUP] = enabled }
    }

    suspend fun updateBackupInterval(hours: Int) {
        context.dataStore.edit { it[KEY_BACKUP_INTERVAL] = hours }
    }

    suspend fun updateRetentionDays(days: Int) {
        context.dataStore.edit { it[KEY_RETENTION_DAYS] = days }
    }

    suspend fun updateDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = dark }
    }

    suspend fun markEulaAccepted() {
        context.dataStore.edit { it[KEY_EULA_ACCEPTED] = true }
    }
}
