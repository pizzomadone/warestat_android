package com.warestat.android.util

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val KEY_RETENTION_DAYS = intPreferencesKey("backup_retention_days")
        private const val DB_NAME = "warestat.db"
    }

    private fun getBackupDir(): File {
        val dir = File(context.getExternalFilesDir(null), "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun performBackup(): Result<String> {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return Result.failure(Exception("Database non trovato"))

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val backupName = "warestat_${sdf.format(Date())}.db"
            val backupFile = File(getBackupDir(), backupName)

            dbFile.copyTo(backupFile, overwrite = true)
            cleanOldBackups()
            Result.success(backupFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(backupUri: Uri): Result<Unit> {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val inputStream = context.contentResolver.openInputStream(backupUri)
                ?: return Result.failure(Exception("Impossibile aprire il file di backup"))

            // Pre-restore safety copy
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val safetyBackup = File(getBackupDir(), "pre_restore_${sdf.format(Date())}.db")
            if (dbFile.exists()) dbFile.copyTo(safetyBackup, overwrite = true)

            inputStream.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cleanOldBackups() {
        val prefs = dataStore.data.first()
        val retentionDays = prefs[KEY_RETENTION_DAYS] ?: 7
        val cutoff = System.currentTimeMillis() - (retentionDays * 24L * 3600L * 1000L)
        getBackupDir().listFiles { f -> f.name.endsWith(".db") && f.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    fun listBackups(): List<File> {
        return getBackupDir().listFiles { f -> f.name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun isAutoBackupEnabled(): Boolean {
        return dataStore.data.first()[KEY_AUTO_BACKUP] ?: true
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_BACKUP] = enabled }
    }

    suspend fun getRetentionDays(): Int {
        return dataStore.data.first()[KEY_RETENTION_DAYS] ?: 7
    }

    suspend fun setRetentionDays(days: Int) {
        dataStore.edit { it[KEY_RETENTION_DAYS] = days }
    }
}
