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

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*

/**
 * The Data Access Object for the Plant class.
 */
@Dao
interface MovieDao {
    @Query("SELECT * FROM movie ORDER BY name")
    fun getPlants(): DataSource.Factory<Int, Movie>

    @Query("SELECT COUNT(*) FROM movie ORDER BY name")
    fun getPlantsCount(): Int

    @Query("SELECT * FROM movie WHERE name LIKE :name ORDER BY name")
    fun getPlantsByName(name: String): DataSource.Factory<Int, Movie>

    @Query("SELECT MAX(indexInResponse) + 1 FROM movie")
    fun getNextIndexInPlants(): Int

    @Query("SELECT * FROM movie WHERE id = :plantId")
    fun getPlant(plantId: Int): LiveData<Movie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(movies: List<Movie>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<Movie>)

    @Query("DELETE FROM movie")
    fun deleteAll()
}
