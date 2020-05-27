package com.google.samples.apps.sunflower.di

import androidx.lifecycle.ViewModel
import com.google.samples.apps.sunflower.newsscreen.InfoOverviewFragment
import com.google.samples.apps.sunflower.newsscreen.viewmodels.InfoOverviewViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * dagger module for movie list
 */
@Module
abstract class InfoOverviewModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun infoOverviewFragment(): InfoOverviewFragment

    @Binds
    @IntoMap
    @ViewModelKey(InfoOverviewViewModel::class)
    abstract fun bindViewModule(viewModule: InfoOverviewViewModel): ViewModel
}