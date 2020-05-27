package com.google.samples.apps.sunflower.di

import androidx.lifecycle.ViewModel
import com.google.samples.apps.sunflower.homescreen.FavoursFragment
import com.google.samples.apps.sunflower.homescreen.viewmodels.FavouriteMovieListViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * dagger module for movie list
 */
@Module
abstract class GardenModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun gardenFragment(): FavoursFragment

    @Binds
    @IntoMap
    @ViewModelKey(FavouriteMovieListViewModel::class)
    abstract fun bindViewModule(viewModule: FavouriteMovieListViewModel): ViewModel
}