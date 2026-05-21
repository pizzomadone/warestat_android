package com.warestat.android.data.database.dao

import androidx.room.*
import com.warestat.android.data.database.entity.CompanyDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDataDao {
    @Query("SELECT * FROM company_data LIMIT 1")
    fun getCompanyData(): Flow<CompanyDataEntity?>

    @Query("SELECT * FROM company_data LIMIT 1")
    suspend fun getCompanyDataOnce(): CompanyDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyData(data: CompanyDataEntity): Long

    @Update
    suspend fun updateCompanyData(data: CompanyDataEntity)

    @Query("SELECT COUNT(*) FROM company_data")
    suspend fun getCount(): Int
}
