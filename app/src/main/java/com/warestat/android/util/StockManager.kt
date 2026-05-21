package com.warestat.android.util

import com.warestat.android.data.database.dao.WarehouseDao
import com.warestat.android.data.database.entity.StockReservationEntity
import com.warestat.android.data.database.entity.WarehouseMovementEntity
import javax.inject.Inject
import javax.inject.Singleton

data class StockItem(val productId: Int, val productName: String, val quantity: Int)

data class StockAvailability(
    val physicalStock: Int,
    val reservedStock: Int,
    val availableStock: Int,
    val requestedQuantity: Int
) {
    fun formattedMessage() =
        "Fisico: $physicalStock, Riservato: $reservedStock, Disponibile: $availableStock, Richiesto: $requestedQuantity"
}

@Singleton
class StockManager @Inject constructor(
    private val warehouseDao: WarehouseDao
) {
    suspend fun createOrUpdateReservation(
        productId: Int,
        documentType: String,
        documentId: Int,
        quantity: Int,
        note: String = ""
    ) {
        val existing = warehouseDao.getReservation(productId, documentType, documentId)
        if (existing != null) {
            warehouseDao.updateReservation(existing.id, quantity, note)
        } else {
            warehouseDao.insertReservation(
                StockReservationEntity(
                    productId = productId,
                    documentType = documentType,
                    documentId = documentId,
                    reservedQuantity = quantity,
                    status = "ACTIVE",
                    notes = note
                )
            )
        }
    }

    suspend fun cancelReservation(documentType: String, documentId: Int) {
        warehouseDao.cancelReservations(documentType, documentId)
    }

    suspend fun completeReservationAndDecrementStock(
        documentType: String,
        documentId: Int,
        documentDate: Long,
        documentNumber: String
    ) {
        val reservations = warehouseDao.getActiveReservations(documentType, documentId)
        reservations.forEach { reservation ->
            warehouseDao.insertMovement(
                WarehouseMovementEntity(
                    productId = reservation.productId,
                    date = documentDate,
                    type = "OUTWARD",
                    quantity = reservation.reservedQuantity,
                    reason = "SALE",
                    documentNumber = documentNumber,
                    documentType = documentType,
                    notes = "$documentType $documentNumber"
                )
            )
        }
        warehouseDao.completeReservations(documentType, documentId)
    }

    suspend fun incrementStock(
        items: List<StockItem>,
        documentDate: Long,
        documentNumber: String,
        documentType: String
    ) {
        items.forEach { item ->
            warehouseDao.insertMovement(
                WarehouseMovementEntity(
                    productId = item.productId,
                    date = documentDate,
                    type = "INWARD",
                    quantity = item.quantity,
                    reason = "PURCHASE",
                    documentNumber = documentNumber,
                    documentType = documentType,
                    notes = "Ordine fornitore $documentNumber"
                )
            )
        }
    }

    suspend fun checkAndCreateLowStockNotifications() {
        val startOfDay = System.currentTimeMillis() / 86_400_000 * 86_400_000
        val lowStockProducts = warehouseDao.getProductsBelowMinStock()
        lowStockProducts.forEach { product ->
            val alreadyNotified = warehouseDao.hasTodayNotification(product.productId, startOfDay)
            if (!alreadyNotified) {
                val message = "Stock sotto il minimo (${product.minimumQuantity}). Quantità attuale: ${product.quantity}"
                warehouseDao.insertNotification(
                    com.warestat.android.data.database.entity.WarehouseNotificationEntity(
                        productId = product.productId,
                        type = "MIN_STOCK",
                        message = message,
                        status = "NEW"
                    )
                )
            }
        }
    }
}
