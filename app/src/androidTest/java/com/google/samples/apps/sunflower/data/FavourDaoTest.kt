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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.platform.app.InstrumentationRegistry
import com.google.samples.apps.sunflower.utilities.getValue
import com.google.samples.apps.sunflower.utilities.testCalendar
import com.google.samples.apps.sunflower.utilities.testGardenPlanting
import com.google.samples.apps.sunflower.utilities.testPlant
import com.google.samples.apps.sunflower.utilities.testPlants
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FavourDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var favourDao: FavourDao
    private var testGardenPlantingId: Long = 0

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before fun createDb() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        favourDao = database.gardenPlantingDao()

        database.movieDao().insertAll(testPlants)
        testGardenPlantingId = favourDao.insertFavour(testGardenPlanting)
    }

    @After fun closeDb() {
        database.close()
    }

    @Test fun testGetGardenPlantings() = runBlocking {
        val gardenPlanting2 = Favour(
            testPlants[1].id,
            testCalendar,
            testCalendar
        ).also { it.gardenPlantingId = 2 }
        favourDao.insertFavour(gardenPlanting2)
        assertThat(getValue(favourDao.getGardenPlantings()).size, equalTo(2))
    }

    @Test fun testDeleteGardenPlanting() = runBlocking {
        val gardenPlanting2 = Favour(
                testPlants[1].id,
                testCalendar,
                testCalendar
        ).also { it.gardenPlantingId = 2 }
        favourDao.insertFavour(gardenPlanting2)
        assertThat(getValue(favourDao.getGardenPlantings()).size, equalTo(2))
        favourDao.deleteFavour(gardenPlanting2)
        assertThat(getValue(favourDao.getGardenPlantings()).size, equalTo(1))
    }

    @Test fun testGetGardenPlantingForPlant() {
        assertTrue(getValue(favourDao.isPlanted(testPlant.id)))
    }

    @Test fun testGetGardenPlantingForPlant_notFound() {
        assertFalse(getValue(favourDao.isPlanted(testPlants[2].id)))
    }

    @Test fun testGetPlantAndGardenPlantings() {
        val plantAndGardenPlantings = getValue(favourDao.getFavouriteMovies())
        assertThat(plantAndGardenPlantings.size, equalTo(1))

        /**
         * Only the [testPlant] has been planted, and thus has an associated [Favour]
         */
        assertThat(plantAndGardenPlantings[0].movie, equalTo(testPlant))
        assertThat(plantAndGardenPlantings[0].favours.size, equalTo(1))
        assertThat(plantAndGardenPlantings[0].favours[0], equalTo(testGardenPlanting))
    }
}