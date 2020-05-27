package com.google.samples.apps.sunflower.di

import android.content.Context
import com.google.samples.apps.sunflower.OrangeTvApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
    ApplicationModule::class,
    AndroidSupportInjectionModule::class,

    // main activity
    MainActivityModule::class,

    // home screen
    PlantListModule::class,
    PlantDetailModule::class,
    GardenModule::class,

    // info screen
    InfoOverviewModule::class,
    InfoDetailModule::class
])
interface ApplicationComponent : AndroidInjector<OrangeTvApplication> {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): ApplicationComponent
    }
}