package com.google.samples.apps.sunflower.di

import androidx.lifecycle.ViewModel
import com.google.samples.apps.sunflower.MainActivity
import com.google.samples.apps.sunflower.viewmodels.MainActivityViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * dagger module for movie list
 */
@Module
abstract class MainActivityModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun mainActivity(): MainActivity

    @Binds
    @IntoMap
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindViewModule(viewModule: MainActivityViewModel): ViewModel
}