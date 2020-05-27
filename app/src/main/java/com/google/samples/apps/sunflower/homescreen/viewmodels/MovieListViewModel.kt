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

package com.google.samples.apps.sunflower.homescreen.viewmodels

import androidx.lifecycle.*
import com.google.samples.apps.sunflower.homescreen.MovieListFragment
import com.google.samples.apps.sunflower.data.MovieRepository
import javax.inject.Inject

/**
 * The ViewModel for [MovieListFragment].
 */
class MovieListViewModel @Inject internal constructor(movieRepository: MovieRepository) : ViewModel() {

    private val growZoneNumber = MutableLiveData<Int>(NO_GROW_ZONE)
    private val searchString = MutableLiveData<String>("")

    private val searchCondition = MediatorLiveData<Pair<String, Int>>().apply {
        addSource(searchString) {
            value = value?.copy(it, value!!.second) ?: it to NO_GROW_ZONE
        }
        addSource(growZoneNumber) {
            value = value?.copy(value!!.first, it) ?: "" to it
        }
    }

    val plants = searchCondition.switchMap {
        if (it.second == NO_GROW_ZONE) {

            movieRepository.getPlants(10, it.first).pagedList
        } else {
            movieRepository.getPlantsWithGrowZoneNumber(it.second)
        }
    }

    private val repoResult = Transformations.map(searchString) {
        movieRepository.getPlants(10, it)
    }

    val networkState = Transformations.switchMap(repoResult) { it.networkState }
    val refreshState = Transformations.switchMap(repoResult) { it.refreshState }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun setGrowZoneNumber(num: Int) {
        growZoneNumber.value = num
    }

    fun clearGrowZoneNumber() {
        growZoneNumber.value = NO_GROW_ZONE
    }

    fun setSearchString(query: String) {
        searchString.value = query
    }

    fun isFiltered() = growZoneNumber.value != NO_GROW_ZONE

    companion object {
        private const val NO_GROW_ZONE = -1
    }
}
