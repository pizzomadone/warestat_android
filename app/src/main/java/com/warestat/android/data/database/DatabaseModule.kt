package com.warestat.android.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WareStatDatabase {
        return Room.databaseBuilder(
            context,
            WareStatDatabase::class.java,
            "warestat.db"
        )
            .enableMultiInstanceInvalidation()
            .build()
    }

    @Provides fun provideCustomerDao(db: WareStatDatabase) = db.customerDao()
    @Provides fun provideProductDao(db: WareStatDatabase) = db.productDao()
    @Provides fun provideOrderDao(db: WareStatDatabase) = db.orderDao()
    @Provides fun provideInvoiceDao(db: WareStatDatabase) = db.invoiceDao()
    @Provides fun provideSupplierDao(db: WareStatDatabase) = db.supplierDao()
    @Provides fun provideWarehouseDao(db: WareStatDatabase) = db.warehouseDao()
    @Provides fun provideCompanyDataDao(db: WareStatDatabase) = db.companyDataDao()
    @Provides fun provideReportDao(db: WareStatDatabase) = db.reportDao()
}
