package com.google.samples.apps.sunflower.di

import androidx.lifecycle.ViewModel
import com.google.samples.apps.sunflower.homescreen.MovieDetailFragment
import com.google.samples.apps.sunflower.homescreen.viewmodels.MovieDetailViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

/**
 * dagger module for movie list
 */
@Module
abstract class PlantDetailModule {

    @ContributesAndroidInjector(modules = [ViewModelBuilder::class])
    internal abstract fun plantDetailFragment(): MovieDetailFragment

    @Binds
    @IntoMap
    @ViewModelKey(MovieDetailViewModel::class)
    abstract fun bindViewModule(viewModule: MovieDetailViewModel): ViewModel
}