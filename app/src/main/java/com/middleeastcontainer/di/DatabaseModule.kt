package com.middleeastcontainer.di

import android.content.Context
import androidx.room.Room
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.data.database.MecrcDatabase
import com.middleeastcontainer.data.database.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MecrcDatabase =
        Room.databaseBuilder(context, MecrcDatabase::class.java, Constants.DB_NAME)
            // Additive migrations only. An inspector may be carrying captured
            // containers that have not uploaded yet; those exist nowhere else.
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun containerDao(db: MecrcDatabase) = db.containerDao()
    @Provides fun sideTablesDao(db: MecrcDatabase) = db.sideTablesDao()
    @Provides fun extraImageDao(db: MecrcDatabase) = db.extraImageDao()
    @Provides fun imagesDao(db: MecrcDatabase) = db.imagesDao()
    @Provides fun inventoryDao(db: MecrcDatabase) = db.inventoryDao()
}
