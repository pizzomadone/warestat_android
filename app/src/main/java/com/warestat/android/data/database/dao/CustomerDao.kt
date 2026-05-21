package com.warestat.android.data.database.dao

import androidx.room.*
import com.warestat.android.data.database.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY last_name, first_name")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Query("SELECT * FROM customers WHERE first_name LIKE :query OR last_name LIKE :query OR email LIKE :query ORDER BY last_name, first_name")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int
}
