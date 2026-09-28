package com.callankan.poloapp.di

import android.content.Context
import androidx.room.Room
import com.callankan.poloapp.data.db.PoloDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): PoloDatabase =
        Room.databaseBuilder(context, PoloDatabase::class.java, PoloDatabase.NAME).build()

    @Provides fun vehicleDao(db: PoloDatabase) = db.vehicleDao()
    @Provides fun odometerDao(db: PoloDatabase) = db.odometerDao()
    @Provides fun specDao(db: PoloDatabase) = db.specDao()
    @Provides fun refuelDao(db: PoloDatabase) = db.refuelDao()
    @Provides fun maintenanceDao(db: PoloDatabase) = db.maintenanceDao()
    @Provides fun loanDao(db: PoloDatabase) = db.loanDao()
    @Provides fun expenseDao(db: PoloDatabase) = db.expenseDao()
    @Provides fun itvDao(db: PoloDatabase) = db.itvDao()
    @Provides fun insuranceDao(db: PoloDatabase) = db.insuranceDao()
    @Provides fun tripDao(db: PoloDatabase) = db.tripDao()
    @Provides fun noteDao(db: PoloDatabase) = db.noteDao()
    @Provides fun tirePressureDao(db: PoloDatabase) = db.tirePressureDao()
    @Provides fun damageDao(db: PoloDatabase) = db.damageDao()
    @Provides fun documentDao(db: PoloDatabase) = db.documentDao()
    @Provides fun attachmentDao(db: PoloDatabase) = db.attachmentDao()
    @Provides fun backupDao(db: PoloDatabase) = db.backupDao()
}
