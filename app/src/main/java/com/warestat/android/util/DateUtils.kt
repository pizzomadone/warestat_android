package com.warestat.android.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    val DEFAULT_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    val DATETIME_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ITALY)

    fun formatDate(millis: Long?): String {
        if (millis == null || millis == 0L) return ""
        return DEFAULT_FORMAT.format(Date(millis))
    }

    fun formatDateTime(millis: Long?): String {
        if (millis == null || millis == 0L) return ""
        return DATETIME_FORMAT.format(Date(millis))
    }

    fun parseDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        return try { DEFAULT_FORMAT.parse(dateStr)?.time }
        catch (e: Exception) {
            try { ISO_FORMAT.parse(dateStr)?.time }
            catch (e2: Exception) { null }
        }
    }

    fun startOfMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun startOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun monthsAgo(months: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.MONTH, -months)
        }.timeInMillis
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
