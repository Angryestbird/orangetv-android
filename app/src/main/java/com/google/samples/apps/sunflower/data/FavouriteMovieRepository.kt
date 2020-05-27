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

package com.google.samples.apps.sunflower.data

import javax.inject.Inject

class FavouriteMovieRepository @Inject constructor(
    private val favourDao: FavourDao
) {

    suspend fun createGardenPlanting(plantId: Int) {
        val gardenPlanting = Favour(plantId)
        favourDao.insertFavour(gardenPlanting)
    }

    suspend fun removeGardenPlanting(favour: Favour) {
        favourDao.deleteFavour(favour)
    }

    fun isPlanted(id: Int) =
            favourDao.isPlanted(id)

    fun getPlantedGardens() = favourDao.getFavouriteMovies()

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: FavouriteMovieRepository? = null

        fun getInstance(favourDao: FavourDao) =
                instance ?: synchronized(this) {
                    instance ?: FavouriteMovieRepository(favourDao).also { instance = it }
                }
    }
}