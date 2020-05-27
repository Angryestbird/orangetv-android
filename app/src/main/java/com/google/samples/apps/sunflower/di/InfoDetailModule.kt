package com.google.samples.apps.sunflower.di

import androidx.lifecycle.ViewModel
import com.google.samples.apps.sunflower.newsscreen.InfoDetailFragment
import com.google.samples.apps.sunflower.newsscreen.viewmodels.InfoDetailViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * dagger module for movie list
 */
@Module
abstract class InfoDetailModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun plantDetailFragment(): InfoDetailFragment

    @Binds
    @IntoMap
    @ViewModelKey(InfoDetailViewModel::class)
    abstract fun bindViewModule(viewModule: InfoDetailViewModel): ViewModel
}