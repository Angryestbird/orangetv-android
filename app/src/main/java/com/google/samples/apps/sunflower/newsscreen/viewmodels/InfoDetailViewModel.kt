/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.sunflower.newsscreen.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.sunflower.homescreen.MovieDetailFragment
import com.google.samples.apps.sunflower.data.FavouriteMovieRepository
import com.google.samples.apps.sunflower.data.MovieRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The ViewModel used in [MovieDetailFragment].
 */
class InfoDetailViewModel @Inject constructor(
        movieRepository: MovieRepository,
        private val favouriteMovieRepository: FavouriteMovieRepository
) : ViewModel() {

    val plantId: MutableLiveData<Int> = MutableLiveData()

    val isPlanted = plantId.switchMap {
        favouriteMovieRepository.isPlanted(it)
    }

    val plant = plantId.switchMap {
        movieRepository.getPlant(it)
    }

    fun addPlantToGarden() {
        viewModelScope.launch {
            favouriteMovieRepository.createGardenPlanting(plantId.value!!)
        }
    }
}
