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

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import com.google.samples.apps.sunflower.api.Api
import com.google.samples.apps.sunflower.di.ApplicationModule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * Repository module for handling data operations.
 */
class MovieRepository @Inject constructor(
        private val db: AppDatabase,
        @ApplicationModule.NetworkPageSize private val networkPageSize: Int,
        private val ioExecutor: Executor,
        private val api: Api) {
    private val movieDao = db.movieDao()

    fun getPlants(pageSize: Int, subReddit: String): Listing<Movie> {

        // create a boundary callback which will observe when the user reaches to the edges of
        // the list and update the database with extra data.
        val boundaryCallback = MovieBoundaryCallback(
                webservice = api,
                subredditName = subReddit,
                handleResponse = this::insertResultIntoDb,
                ioExecutor = ioExecutor,
                db = db)
        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh(subReddit)
        }

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList = if (subReddit.isBlank()) {
            movieDao.getPlants().toLiveData(
                    pageSize = pageSize,
                    boundaryCallback = boundaryCallback)
        } else {
            movieDao.getPlantsByName("%${subReddit}%").toLiveData(
                    pageSize = pageSize)
        }

        return Listing(
                pagedList = livePagedList,
                networkState = boundaryCallback.networkState,
                retry = {
                    boundaryCallback.helper.retryAllFailed()
                },
                refresh = {
                    refreshTrigger.value = null
                },
                refreshState = refreshState
        )
    }

    fun getPlant(plantId: Int) = movieDao.getPlant(plantId)

    fun getPlantsWithGrowZoneNumber(growZoneNumber: Int) =
            movieDao.getPlants().toLiveData(pageSize = 10)

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(subredditName: String): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        networkState.value = NetworkState.LOADING
        api.getTop().enqueue(
                object : Callback<List<Movie>> {
                    override fun onFailure(call: Call<List<Movie>>, t: Throwable) {
                        // retrofit calls this on main thread so safe to call set value
                        networkState.value = NetworkState.error(t.message)
                    }

                    override fun onResponse(
                            call: Call<List<Movie>>,
                            response: Response<List<Movie>>) {
                        ioExecutor.execute {
                            db.runInTransaction {
                                movieDao.deleteAll()
                                insertResultIntoDb(subredditName, response.body())
                            }
                            // since we are in bg thread now, post the result.
                            networkState.postValue(NetworkState.LOADED)
                        }
                    }
                }
        )
        return networkState
    }

    /**
     * Inserts the response into the database while also assigning position indices to items.
     */
    private fun insertResultIntoDb(subredditName: String, body: List<Movie>?) {
        body!!.let { posts ->
            db.runInTransaction {
                val start = movieDao.getNextIndexInPlants()
                val items = posts.mapIndexed { index, child ->
                    child.indexInResponse = start + index
                    child
                }
                movieDao.insertAllSync(items)
            }
        }
    }

    companion object {

        private const val DEFAULT_NETWORK_PAGE_SIZE = 10

        // For Singleton instantiation
        @Volatile
        private var instance: MovieRepository? = null

        fun getInstance(db: AppDatabase,
                        ioExecutor: Executor,
                        api: Api,
                        networkPageSize: Int = DEFAULT_NETWORK_PAGE_SIZE) =
                instance ?: synchronized(this) {
                    instance
                            ?: MovieRepository(db, networkPageSize, ioExecutor, api).also { instance = it }
                }
    }
}
