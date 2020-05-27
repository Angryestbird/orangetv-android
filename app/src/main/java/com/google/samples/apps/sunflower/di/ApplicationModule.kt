package com.google.samples.apps.sunflower.di

import android.content.Context
import com.google.samples.apps.sunflower.api.Api
import com.google.samples.apps.sunflower.data.AppDatabase
import com.google.samples.apps.sunflower.data.FavourDao
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
object ApplicationModule {

    @JvmStatic
    @Provides
    fun provideApi(context: Context): Api {
        return Api.create(context)
    }

    @JvmStatic
    @Singleton
    @Provides
    @NetworkPageSize
    fun provideNetworkPageSize(context: Context): Int {
        return 10
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideDiskIOExecutor(): Executor = Executors.newSingleThreadExecutor()

    @JvmStatic
    @Singleton
    @Provides
    fun provideDataBase(context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideGardenPlantingDao(database: AppDatabase): FavourDao {
        return database.gardenPlantingDao()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO

    @Qualifier
    @Retention(AnnotationRetention.RUNTIME)
    annotation class NetworkPageSize
}