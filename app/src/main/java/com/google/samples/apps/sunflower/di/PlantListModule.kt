package com.google.samples.apps.sunflower.di

import androidx.lifecycle.ViewModel
import com.google.samples.apps.sunflower.homescreen.MovieListFragment
import com.google.samples.apps.sunflower.homescreen.viewmodels.MovieListViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * dagger module for movie list
 */
@Module
abstract class PlantListModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun plantListFragment(): MovieListFragment

    @Binds
    @IntoMap
    @ViewModelKey(MovieListViewModel::class)
    abstract fun bindViewModule(viewModule: MovieListViewModel): ViewModel
}